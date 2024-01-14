package sune.app.mediadown.ffmpeg;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import sune.api.process.ReadOnlyProcess;
import sune.app.mediadown.InternalState;
import sune.app.mediadown.TaskStates;
import sune.app.mediadown.conversion.ConversionCommand;
import sune.app.mediadown.conversion.ConversionCommand.Input;
import sune.app.mediadown.conversion.ConversionCommand.Output;
import sune.app.mediadown.conversion.ConversionMedia;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.MediaFixEvent;
import sune.app.mediadown.event.tracker.MediaFixTracker;
import sune.app.mediadown.event.tracker.PipelineStates;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.ffmpeg.FFmpeg.Options;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaFormat.MediaFormatType;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.media.fix.MediaFixer;
import sune.app.mediadown.util.Metadata;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.ProcessUtils;
import sune.app.mediadown.util.Regex;
import sune.app.mediadown.util.Regex.ReusableMatcher;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public final class FFmpegFixer implements MediaFixer {
	
	private static final Regex REGEX_LINE_PROGRESS = Regex.of("^(?:frame|size)=.*?time=(.*?)\\s.*$");
	
	private final InternalState state = new InternalState(TaskStates.INITIAL);
	private final EventRegistry<MediaFixEvent> eventRegistry = new EventRegistry<>();
	private final TrackerManager trackerManager;
	
	private ReadOnlyProcess process;
	private MediaFixTracker tracker;
	private ResolvedMedia output;
	private List<ConversionMedia> inputs;
	private Metadata metadata;
	
	private final Map<Integer, Path> mapInputs = new HashMap<>();
	private final List<Path> tempPaths = new ArrayList<>();
	private Exception exception;
	private final ReusableMatcher matcher = REGEX_LINE_PROGRESS.reusableMatcher();
	
	public FFmpegFixer(TrackerManager trackerManager) {
		this.trackerManager = Objects.requireNonNull(trackerManager);
	}
	
	private final void outputHandler(String line) {
		matcher.reset(line);
		if(!matcher.matches()) return; // Not a progress info
		String time = matcher.group(1);
		tracker.update(Utils.convertToSeconds(time));
		eventRegistry.call(MediaFixEvent.UPDATE, this);
	}
	
	private final int doCommand(FFmpeg.Command command) throws Exception {
		process = FFmpeg.createAsynchronousProcess(this::outputHandler);
		
		if(process == null) {
			throw new IllegalStateException("Unable to create conversion process.");
		}
		
		String cmd = command.string();
		Path dir = command.outputs().get(0).path().getParent();
		process.execute(cmd, dir);
		
		return process.waitFor();
	}
	
	private final int mediaIndexOfType(MediaType type) {
		return IntStream.range(0, inputs.size())
					.filter((i) -> inputs.get(i).media().type().is(type))
					.findFirst().orElse(-1);
	}
	
	private final void setOutput(int index, Path output) {
		mapInputs.put(index, output);
	}
	
	private final void addTempPath(Path path) {
		tempPaths.add(path);
	}
	
	private final List<FFmpeg.Command.Builder> parseStep(String step) {
		switch(step) {
			case MediaFixer.Steps.STEP_VIDEO_FIX_TIMESTAMPS: {
				int index = mediaIndexOfType(MediaType.VIDEO);
				
				if(index < 0) {
					throw new IllegalStateException("No video");
				}
				
				ConversionMedia video = inputs.get(index);
				Media media = video.media();
				
				if(!media.format().is(MediaFormat.MP4)) {
					throw new IllegalStateException("Unsupported video format: " + media.format());
				}
				
				/* Extract the raw stream:
				 * ffmpeg -y -i INPUT -map 0:v -c:v copy -bsf:v h264_mp4toannexb TEMP_OUTPUT
				 * 
				 * Regenerate the timestamps:
				 * ffmpeg -y -fflags +genpts -i TEMP_OUTPUT -c copy -f mp4 OUTPUT
				 */
				
				Metadata metadata = Metadata.of("noExplicitFormat", true);
				Path path = video.path();
				
				Path outputH264 = path.resolveSibling(path.getFileName() + ".h264");
				Path outputPath = path.resolveSibling(path.getFileName() + ".fix");
				
				FFmpeg.Command.Builder cmdRaw = FFmpeg.Command.builder();
				cmdRaw.addInputs(Input.of(path, media.format()));
				cmdRaw.addOutputs(
					(Output) Output.ofMutable(outputH264)
						.addOptions(
							ConversionCommand.Option.ofShort("map", "0:v"),
							FFmpeg.Options.videoCodecCopy(),
							ConversionCommand.Option.ofShort("bsf:v", "h264_mp4toannexb")
						)
						.asFormat(InternalMediaFormats.H264)
				);
				cmdRaw.addMetadata(metadata);
				
				FFmpeg.Command.Builder cmdFix = FFmpeg.Command.builder();
				cmdFix.addInputs(Input.of(outputH264, InternalMediaFormats.H264));
				cmdFix.addOptions(
					ConversionCommand.Option.ofShort("fflags", "+genpts")
				);
				cmdFix.addOutputs(
					(Output) Output.ofMutable(outputPath)
						.addOptions(
							FFmpeg.Options.codecCopy()
						)
						.asFormat(MediaFormat.MP4)
				);
				cmdFix.addMetadata(metadata);
				
				addTempPath(outputH264);
				setOutput(index, outputPath);
				
				return List.of(cmdRaw, cmdFix);
			}
			case MediaFixer.Steps.STEP_AUDIO_FIX_TIMESTAMPS: {
				int index = mediaIndexOfType(MediaType.AUDIO);
				
				if(index < 0) {
					throw new IllegalStateException("No audio");
				}
				
				ConversionMedia audio = inputs.get(index);
				Media media = audio.media();
				
				if(!media.format().isAnyOf(MediaFormat.M4A, MediaFormat.AAC)) {
					throw new IllegalStateException("Unsupported audio format: " + media.format());
				}
				
				/* Extract the raw stream:
				 * ffmpeg -y -i INPUT -map 0:a -c:a copy -bsf:a aac_adtstoasc -f adts TEMP_OUTPUT
				 * 
				 * Regenerate the timestamps:
				 * ffmpeg -y -fflags +genpts -i TEMP_OUTPUT -c copy -f mp4 OUTPUT
				 */
				
				Metadata metadata = Metadata.of("noExplicitFormat", true);
				Path path = audio.path();
				
				Path outputAdts = path.resolveSibling(path.getFileName() + ".adts");
				Path outputPath = path.resolveSibling(path.getFileName() + ".fix");
				
				FFmpeg.Command.Builder cmdRaw = FFmpeg.Command.builder();
				cmdRaw.addInputs(Input.of(path, media.format()));
				cmdRaw.addOutputs(
					(Output) Output.ofMutable(outputAdts)
						.addOptions(
							ConversionCommand.Option.ofShort("map", "0:a"),
							FFmpeg.Options.audioCodecCopy(),
							ConversionCommand.Option.ofShort("bsf:a", "aac_adtstoasc")
						)
						.asFormat(InternalMediaFormats.ADTS)
				);
				cmdRaw.addMetadata(metadata);
				
				FFmpeg.Command.Builder cmdFix = FFmpeg.Command.builder();
				cmdFix.addInputs(Input.of(outputAdts, InternalMediaFormats.ADTS));
				cmdFix.addOptions(
					ConversionCommand.Option.ofShort("fflags", "+genpts")
				);
				cmdFix.addOutputs(
					(Output) Output.ofMutable(outputPath)
						.addOptions(
							FFmpeg.Options.codecCopy()
						)
						.asFormat(MediaFormat.MP4)
				);
				cmdFix.addMetadata(metadata);
				
				addTempPath(outputAdts);
				setOutput(index, outputPath);
				
				return List.of(cmdRaw, cmdFix);
			}
			default: {
				return null;
			}
		}
	}
	
	private final FFmpeg.Command.Builder addCommonOptions(FFmpeg.Command.Builder command) {
		command.addOptions(
			Options.yes(),
			Options.hideBanner(),
			Options.logWarning(),
			Options.stats()
		);
		
		return command;
	}
	
	private final void doStart() throws Exception {
		state.set(TaskStates.RUNNING);
		
		tempPaths.clear();
		mapInputs.clear();
		
		List<String> steps = output.media().metadata().get("media.fix.steps");
		double duration = inputs.stream().mapToDouble(ConversionMedia::duration).max().getAsDouble();
		
		tracker = new MediaFixTracker(duration);
		tracker.updateState(PipelineStates.MEDIA_FIX);
		trackerManager.tracker(tracker);
		
		for(String step : steps) {
			// Notify the tracker of step change, before parsing the step
			tracker.updateStep(step);
			
			List<FFmpeg.Command.Builder> commands = parseStep(step);
			
			if(commands == null) {
				break; // Stop the whole step chain
			}
			
			for(FFmpeg.Command.Builder builder : commands) {
				FFmpeg.Command command = addCommonOptions(builder).build();
				
				for(Output output : command.outputs()) {
					NIO.deleteFile(output.path());
					NIO.createFile(output.path());
				}
				
				int exitCode;
				if((exitCode = doCommand(command)) != 0 && !isStopped()) {
					throw new IllegalStateException(String.format(
						"FFmpeg exited with non-zero code: %d.", exitCode
					));
				}
			}
			
			for(Path path : tempPaths) {
				try {
					NIO.delete(path);
				} catch(IOException ex) {
					// Ignore
				}
			}
			
			tempPaths.clear();
		}
		
		tracker.updateState(PipelineStates.MEDIA_FIX);
		
		for(int i = 0, l = inputs.size(); i < l; ++i) {
			ConversionMedia input = inputs.get(i);
			Path newPath = mapInputs.get(i);
			
			if(newPath != null) {
				Path oldPath = input.path();
				NIO.delete(oldPath);
				NIO.move(newPath, oldPath);
			}
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
		
		matcher.dispose();
	}
	
	@Override
	public void start(ResolvedMedia output, List<ConversionMedia> inputs, Metadata metadata) throws Exception {
		this.output = output;
		this.inputs = inputs;
		this.metadata = metadata;
		
		state.clear(TaskStates.STARTED);
		
		try {
			eventRegistry.call(MediaFixEvent.BEGIN, this);
			doStart();
		} catch(Exception ex) {
			exception = ex;
			state.set(TaskStates.ERROR);
			eventRegistry.call(MediaFixEvent.ERROR, this);
			throw ex; // Propagate the error
		} finally {
			doStop(TaskStates.DONE);
			eventRegistry.call(MediaFixEvent.END, this);
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
		
		eventRegistry.call(MediaFixEvent.PAUSE, this);
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
		
		eventRegistry.call(MediaFixEvent.RESUME, this);
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
	public <V> void addEventListener(Event<? extends MediaFixEvent, V> event, Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	@Override
	public <V> void removeEventListener(Event<? extends MediaFixEvent, V> event, Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
	
	@Override
	public TrackerManager trackerManager() {
		return trackerManager;
	}
	
	@Override
	public ResolvedMedia output() {
		return output;
	}
	
	@Override
	public List<ConversionMedia> inputs() {
		return inputs;
	}
	
	@Override
	public Metadata metadata() {
		return metadata;
	}
	
	@Override
	public Exception exception() {
		return exception;
	}
	
	private static final class InternalMediaFormats {
		
		private static final MediaFormat H264 = formatH264();
		private static final MediaFormat ADTS = formatADTS();
		
		private InternalMediaFormats() {
		}
		
		private static final MediaFormat formatH264() {
			MediaFormat format = MediaFormat.ofName("h264");
			
			if(format.is(MediaFormat.UNKNOWN)) {
				format = new MediaFormat.Builder()
					.name("h264")
					.mediaType(MediaType.VIDEO)
					.formatType(MediaFormatType.BOTH)
					.string("H264")
					.fileExtensions("h264")
					.build();
			}
			
			return format;
		}
		
		private static final MediaFormat formatADTS() {
			MediaFormat format = MediaFormat.ofName("adts");
			
			if(format.is(MediaFormat.UNKNOWN)) {
				format = new MediaFormat.Builder()
						.name("adts")
						.mediaType(MediaType.AUDIO)
						.formatType(MediaFormatType.BOTH)
						.string("ADTS")
						.fileExtensions("adts")
						.build();
			}
			
			return format;
		}
	}
}