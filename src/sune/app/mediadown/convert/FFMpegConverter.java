package sune.app.mediadown.convert;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sune.api.process.ReadOnlyProcess;
import sune.app.mediadown.event.ConversionEvent;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.tracker.ConversionTracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.event.tracker.WaitTracker;
import sune.app.mediadown.ffmpeg.FFMpeg;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.ProcessUtils;
import sune.app.mediadown.util.Utils;

/** @since 00.01.26 */
public final class FFMpegConverter implements Converter {
	
	private static final Pattern PATTERN_LINE_PROGRESS = Pattern.compile("^(?:frame|size)=.*?time=(.*?)\\s.*$");
	
	private final ConversionConfiguration configuration;
	private ConversionTracker conversionTracker;
	private ReadOnlyProcess process;
	
	private final EventRegistry<ConversionEvent> eventRegistry = new EventRegistry<>();
	private final TrackerManager manager = new TrackerManager();
	
	private final AtomicBoolean running = new AtomicBoolean();
	private final AtomicBoolean started = new AtomicBoolean();
	private final AtomicBoolean done = new AtomicBoolean();
	private final AtomicBoolean paused = new AtomicBoolean();
	private final AtomicBoolean stopped = new AtomicBoolean();
	
	private final MediaFormat formatInput;
	private final MediaFormat formatOutput;
	private final Path fileOutput;
	private final Path[] filesInput;
	
	public FFMpegConverter(ConversionConfiguration configuration, MediaFormat formatInput, MediaFormat formatOutput,
			Path fileOutput, Path... filesInput) {
		this.configuration = Objects.requireNonNull(configuration);
		this.formatInput = Objects.requireNonNull(formatInput);
		this.formatOutput = Objects.requireNonNull(formatOutput);
		this.fileOutput = Objects.requireNonNull(fileOutput);
		this.filesInput = checkNonEmptyArray(filesInput);
		manager.setTracker(new WaitTracker());
	}
	
	private static final <T> T[] checkNonEmptyArray(T[] array) {
		if(array == null || array.length <= 0)
			throw new IllegalArgumentException();
		return array;
	}
	
	private final void ffmpegOutputHandler(String line) {
		Matcher matcher = PATTERN_LINE_PROGRESS.matcher(line);
		if(!matcher.matches()) return; // Not a progress info
		String time = matcher.group(1);
		conversionTracker.update(Utils.convertToSeconds(time));
	}
	
	private final boolean convertOp(MediaFormat formatInput, MediaFormat formatOutput, Path fileOutput, Path... filesInput)
			throws Exception {
		try {
			process = FFMpeg.createAsynchronousProcess(this::ffmpegOutputHandler);
			if(process == null)
				throw new IllegalStateException("Unable to create conversion process.");
			Path parentDir = filesInput[0].getParent();
			String command = FFMpegConversionCommand.get(formatInput, formatOutput, fileOutput, filesInput);
			process.execute(command, parentDir);
			return process.waitFor() == 0;
		} catch(Exception ex) {
			eventRegistry.call(ConversionEvent.ERROR, new Pair<>(this, ex));
			throw ex; // Forward the exception
		} finally {
			stop();
		}
	}
	
	@Override
	public void start() throws Exception {
		try {
			running.set(true);
			started.set(true);
			eventRegistry.call(ConversionEvent.BEGIN, this);
			manager.setUpdateListener(() -> eventRegistry.call(ConversionEvent.UPDATE, new Pair<>(this, manager)));
			conversionTracker = new ConversionTracker(configuration.getDuration());
			manager.setTracker(conversionTracker);
			if(convertOp(formatInput, formatOutput, fileOutput, filesInput)) {
				Path dest = configuration.getDestination();
				NIO.deleteFile(dest);
				NIO.move(fileOutput, dest);
				done.set(true);
			} else {
				Exception ex = new IllegalStateException();
				eventRegistry.call(ConversionEvent.ERROR, new Pair<>(this, ex));
				throw ex; // Forward the exception
			}
		} finally {
			if(done.get()) {
				// Delete files iff the conversion is successfully done
				Utils.toList(filesInput).forEach((f) -> Utils.ignore(() -> NIO.deleteFile(f)));
			}
			stop();
		}
	}
	
	@Override
	public void stop() throws Exception {
		if(stopped.get()) return; // Nothing to do
		running.set(false);
		paused .set(false);
		if(process != null)
			process.close();
		if(!done.get())
			stopped.set(true);
		eventRegistry.call(ConversionEvent.END, this);
	}
	
	@Override
	public void pause() {
		if(paused.get()) return; // Nothing to do
		if(process != null)
			ProcessUtils.pause(process.process());
		running.set(false);
		paused .set(true);
		eventRegistry.call(ConversionEvent.PAUSE, this);
	}
	
	@Override
	public void resume() {
		if(!paused.get()) return; // Nothing to do
		if(process != null)
			ProcessUtils.resume(process.process());
		paused .set(false);
		running.set(true);
		eventRegistry.call(ConversionEvent.RESUME, this);
	}
	
	@Override
	public final boolean isRunning() {
		return running.get();
	}
	
	@Override
	public final boolean isStarted() {
		return started.get();
	}
	
	@Override
	public final boolean isDone() {
		return done.get();
	}
	
	@Override
	public final boolean isPaused() {
		return paused.get();
	}
	
	@Override
	public final boolean isStopped() {
		return stopped.get();
	}
	
	@Override
	public void close() throws IOException {
		try {
			stop();
		} catch(Exception ex) {
			// Convert the exception type
			throw new IOException(ex);
		}
	}
	
	@Override
	public <T> void addEventListener(EventType<ConversionEvent, T> type, Listener<T> listener) {
		eventRegistry.add(type, listener);
	}
	
	@Override
	public <T> void removeEventListener(EventType<ConversionEvent, T> type, Listener<T> listener) {
		eventRegistry.remove(type, listener);
	}
}