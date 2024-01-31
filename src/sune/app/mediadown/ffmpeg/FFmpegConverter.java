package sune.app.mediadown.ffmpeg;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import sune.api.process.ReadOnlyProcess;
import sune.app.mediadown.InternalState;
import sune.app.mediadown.TaskStates;
import sune.app.mediadown.conversion.ConversionCommand;
import sune.app.mediadown.conversion.ConversionCommand.Input;
import sune.app.mediadown.conversion.ConversionCommand.Output;
import sune.app.mediadown.entity.Converter;
import sune.app.mediadown.event.ConversionEvent;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.tracker.ConversionTracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.ffmpeg.FFmpeg.Options;
import sune.app.mediadown.media.MediaConstants;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.ProcessUtils;
import sune.app.mediadown.util.Regex;
import sune.app.mediadown.util.Regex.ReusableMatcher;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.08 */
public final class FFmpegConverter implements Converter {
	
	private static final Regex REGEX_LINE_PROGRESS = Regex.of("^(?:frame|size)=.*?time=(.*?)\\s.*$");
	
	private final InternalState state = new InternalState(TaskStates.INITIAL);
	private final EventRegistry<ConversionEvent> eventRegistry = new EventRegistry<>();
	private final TrackerManager trackerManager;
	
	private ReadOnlyProcess process;
	private ConversionTracker tracker;
	private FFmpeg.Command command;
	
	private Exception exception;
	private final ReusableMatcher matcher = REGEX_LINE_PROGRESS.reusableMatcher();
	/** @since 00.02.09 */
	private BufferedWriter writerLog;
	
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
	
	/** @since 00.02.09 */
	private final void log(String... parts) {
		if(writerLog == null) {
			return;
		}
		
		try {
			for(String part : parts) {
				writerLog.write(part);
			}
			
			writerLog.write('\n');
		} catch(IOException ex) {
			// Ignore
		}
	}
	
	private final void outputHandler(String line) {
		log(line); // Always log the line
		matcher.reset(line);
		if(!matcher.matches()) return; // Not a progress info
		String time = matcher.group(1);
		tracker.update(Utils.convertToSeconds(time));
		eventRegistry.call(ConversionEvent.UPDATE, this);
	}
	
	private final int doConversion(FFmpeg.Command command) throws Exception {
		process = FFmpeg.createAsynchronousProcess(this::outputHandler);
		
		if(process == null) {
			throw new IllegalStateException("Unable to create conversion process.");
		}
		
		Path dir = command.outputs().get(0).path().getParent();
		String cmd = command.string();
		log("ffmpeg ", cmd); // Always log the line
		process.execute(cmd, dir);
		
		return process.waitFor();
	}
	
	private final void doStart() throws Exception {
		state.set(TaskStates.RUNNING);
		
		double duration = command.metadata().get("duration", MediaConstants.UNKNOWN_DURATION);
		FFmpeg.Command altered = alterOutputs(command);
		boolean isMerge = altered.outputs().stream().allMatch((o) -> o.options().contains(Options.codecCopy()));
		
		tracker = new ConversionTracker(duration, isMerge);
		trackerManager.tracker(tracker);
		
		for(Output output : command.outputs()) {
			NIO.deleteFile(output.path());
			NIO.createFile(output.path());
		}
		
		Path logPath = null;
		
		try {
			Path uniquePath = NIO.uniqueFile("ffmpeg-converter-", ".log");
			writerLog = Files.newBufferedWriter(
				uniquePath,
				StandardOpenOption.CREATE, StandardOpenOption.WRITE
			);
			logPath = uniquePath;
		} catch(IOException ex) {
			// Ignore
		}
		
		int exitCode;
		if((exitCode = doConversion(altered)) != 0 && !isStopped()) {
			String message = String.format(
				"FFmpeg exited with non-zero code: %d.%s", exitCode,
				logPath != null
					? String.format("See %s for details.", logPath.toAbsolutePath().toString())
					: ""
			);
			
			throw new IllegalStateException(message);
		}
		
		try {
			writerLog.close();
			writerLog = null;
			NIO.deleteFile(logPath);
		} catch(IOException ex) {
			// Ignore
		}
		
		for(Pair<Output, Output> pair : Utils.zipIterable(
			command.outputs().stream(),
			altered.outputs().stream(),
			Pair::new
		)) {
			NIO.moveForce(pair.b.path(), pair.a.path());
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
		
		if(writerLog != null) {
			writerLog.close();
			writerLog = null;
		}
		
		matcher.dispose();
	}
	
	@Override
	public void start(ConversionCommand command) throws Exception {
		this.command = (FFmpeg.Command) command;
		state.clear(TaskStates.STARTED);
		
		try {
			eventRegistry.call(ConversionEvent.BEGIN, this);
			doStart();
		} catch(Exception ex) {
			exception = ex;
			state.set(TaskStates.ERROR);
			eventRegistry.call(ConversionEvent.ERROR, this);
			throw ex; // Propagate the error
		} finally {
			doStop(TaskStates.DONE);
			
			if(isDone()) {
				// Delete input files if and only if the conversion is successfully done
				for(Input input : command.inputs()) {
					Ignore.callVoid(() -> NIO.deleteFile(input.path()));
				}
			}
			
			eventRegistry.call(ConversionEvent.END, this);
		}
	}
	
	@Override
	public void pause() {
		if(!isStarted() || isPaused() || isStopped() || isDone()) {
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
		if(!isStarted() || !isPaused() || isStopped() || isDone()) {
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
	
	@Override
	public TrackerManager trackerManager() {
		return trackerManager;
	}
	
	@Override
	public FFmpeg.Command command() {
		return command;
	}
	
	@Override
	public Exception exception() {
		return exception;
	}
}