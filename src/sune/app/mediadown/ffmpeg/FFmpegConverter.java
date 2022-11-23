package sune.app.mediadown.ffmpeg;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import sune.api.process.ReadOnlyProcess;
import sune.app.mediadown.InternalState;
import sune.app.mediadown.TaskStates;
import sune.app.mediadown.convert.ConversionCommand;
import sune.app.mediadown.convert.ConversionCommand.Input;
import sune.app.mediadown.convert.ConversionCommand.Output;
import sune.app.mediadown.convert.Converter;
import sune.app.mediadown.event.ConversionEvent;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.tracker.ConversionTracker;
import sune.app.mediadown.event.tracker.TrackerEvent;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.logging.Log;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.ProcessUtils;
import sune.app.mediadown.util.Utils;

/** @since 00.02.08 */
public final class FFmpegConverter implements Converter {
	
	private static final Pattern REGEX_LINE_PROGRESS = Pattern.compile("^(?:frame|size)=.*?time=(.*?)\\s.*$");
	
	private final InternalState state = new InternalState(TaskStates.INITIAL);
	private final EventRegistry<ConversionEvent> eventRegistry = new EventRegistry<>();
	private final TrackerManager trackerManager;
	private final StringBuilder processOutput = new StringBuilder();
	
	private ReadOnlyProcess process;
	private ConversionTracker tracker;
	private FFmpeg.Command command;
	
	public FFmpegConverter(TrackerManager trackerManager) {
		this.trackerManager = Objects.requireNonNull(trackerManager);
	}
	
	private static final FFmpeg.Command alterOutputs(FFmpeg.Command command) {
		FFmpeg.Command.Builder builder = FFmpeg.Command.builder(command);
		
		List<Output> newOutputs = command.outputs().stream()
			.map((output) -> {
				Utils.OfPath.Info info = Utils.OfPath.info(output.path());
				String newName = info.fileName() + ".convert." + info.extension();
				Path newPath = output.path().resolveSibling(newName);
				return Output.of(newPath, output.format(), output.options());
			})
			.collect(Collectors.toList());
		
		builder.removeOutputs(command.outputs());
		builder.addOutputs(newOutputs);
		
		return builder.build();
	}
	
	private final void outputHandler(String line) {
		processOutput.append(line).append('\n');
		Matcher matcher = REGEX_LINE_PROGRESS.matcher(line);
		if(!matcher.matches()) return; // Not a progress info
		String time = matcher.group(1);
		tracker.update(Utils.convertToSeconds(time));
	}
	
	private final int doConversion(FFmpeg.Command command) throws Exception {
		process = FFmpeg.createAsynchronousProcess(this::outputHandler);
		
		if(process == null) {
			throw new IllegalStateException("Unable to create conversion process.");
		}
		
		Path dir = command.outputs().get(0).path().getParent();
		String cmd = command.string();
		processOutput.append("ffmpeg ").append(cmd).append('\n');
		process.execute(cmd, dir);
		
		return process.waitFor();
	}
	
	private final void doStart() throws Exception {
		state.set(TaskStates.RUNNING);
		
		double duration = command.metadata().get("duration");
		FFmpeg.Command altered = alterOutputs(command);
		
		tracker = new ConversionTracker(duration);
		trackerManager.addEventListener(TrackerEvent.UPDATE, (t) -> eventRegistry.call(ConversionEvent.UPDATE, new Pair<>(this, trackerManager)));
		trackerManager.tracker(tracker);
		
		for(Output output : command.outputs()) {
			NIO.deleteFile(output.path());
			NIO.createFile(output.path());
		}
		
		int exitCode;
		if((exitCode = doConversion(altered)) != 0 && !isStopped()) {
			// Log the whole output of the process to a unique log file so that more information
			// can be obtained, not just the exit code.
			Path logPath = Log.toUniquePath("ffmpeg-converter-", processOutput.toString());
			throw new IllegalStateException(String.format(
				"FFmpeg exited with non-zero code: %d. See %s for details.",
				exitCode, logPath.toAbsolutePath().toString()
			));
		}
		
		for(Pair<Output, Output> pair : Utils.zipIterable(
		                                	command.outputs().stream(),
		                                	altered.outputs().stream(),
		                                	Pair::new
		                                )) {
			NIO.move_force(pair.b.path(), pair.a.path());
		}
	}
	
	private final void doStop(int stopState) throws Exception {
		if(isStopped() || isDone()) {
			return;
		}
		
		state.unset(TaskStates.RUNNING);
		state.unset(TaskStates.PAUSED);
		
		state.set(stopState);
		
		if(process != null) {
			process.close();
		}
	}
	
	@Override
	public void start(ConversionCommand command) throws Exception {
		this.command = (FFmpeg.Command) command;
		processOutput.setLength(0);
		state.clear(TaskStates.STARTED);
		
		try {
			eventRegistry.call(ConversionEvent.BEGIN, this);
			doStart();
		} catch(Exception ex) {
			state.set(TaskStates.ERROR);
			eventRegistry.call(ConversionEvent.ERROR, new Pair<>(this, ex));
			throw ex; // Propagate the error
		} finally {
			doStop(TaskStates.DONE);
			
			if(isDone()) {
				// Delete input files if and only if the conversion is successfully done
				for(Input input : command.inputs()) {
					Utils.ignore(() -> NIO.deleteFile(input.path()));
				}
			}
			
			eventRegistry.call(ConversionEvent.END, this);
		}
	}
	
	@Override
	public void pause() {
		if(!isStarted() || isPaused()) {
			return;
		}
		
		state.set(TaskStates.PAUSED);
		state.unset(TaskStates.RUNNING);
		
		if(process != null) {
			ProcessUtils.pause(process.process());
		}
		
		eventRegistry.call(ConversionEvent.PAUSE, this);
	}
	
	@Override
	public void resume() {
		if(!isStarted() || !isPaused()) {
			return;
		}
		
		state.unset(TaskStates.PAUSED);
		state.set(TaskStates.RUNNING);
		
		if(process != null) {
			ProcessUtils.resume(process.process());
		}
		
		eventRegistry.call(ConversionEvent.RESUME, this);
	}
	
	@Override
	public void stop() throws Exception {
		if(!isStarted() || isStopped() || isDone()) {
			return;
		}
		
		doStop(TaskStates.STOPPED);
	}
	
	@Override
	public final boolean isRunning() {
		return state.is(TaskStates.RUNNING);
	}
	
	@Override
	public final boolean isDone() {
		return state.is(TaskStates.DONE);
	}
	
	@Override
	public final boolean isStarted() {
		return state.is(TaskStates.STARTED);
	}
	
	@Override
	public final boolean isPaused() {
		return state.is(TaskStates.PAUSED);
	}
	
	@Override
	public final boolean isStopped() {
		return state.is(TaskStates.STOPPED);
	}
	
	@Override
	public boolean isError() {
		return state.is(TaskStates.ERROR);
	}
	
	@Override
	public void close() throws Exception {
		stop();
	}
	
	@Override
	public <V> void addEventListener(Event<? extends ConversionEvent, V> event, Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	@Override
	public <V> void removeEventListener(Event<? extends ConversionEvent, V> event, Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
}