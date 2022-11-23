package sune.app.mediadown.manager;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import sune.app.mediadown.Disposables;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.convert.ConversionMedia;
import sune.app.mediadown.convert.Converter;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.event.tracker.WaitTracker;
import sune.app.mediadown.ffmpeg.FFmpeg;
import sune.app.mediadown.ffmpeg.FFmpegConverter;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.util.Metadata;
import sune.app.mediadown.util.Threads;

/** @since 00.01.26 */
public class ConversionManager {
	
	private static final ExecutorService EXECUTOR;
	
	static {
		int numOfThreads = MediaDownloader.configuration().parallelConversions();
		EXECUTOR = Threads.Pools.newFixed(numOfThreads);
		Disposables.add(ConversionManager::dispose);
	}
	
	/** @since 00.02.08 */
	private static final Converter createConverter() {
		return new FFmpegConverter(new TrackerManager(new WaitTracker()));
	}
	
	/** @since 00.02.08 */
	private static final Callable<Void> createTask(Converter converter, ResolvedMedia output,
			List<ConversionMedia> inputs, Metadata metadata) {
		return new FFmpegTask(converter, output, inputs, metadata);
	}
	
	/** @since 00.02.08 */
	public static final ManagerSubmitResult<Converter, Void> submit(ResolvedMedia output,
			List<ConversionMedia> inputs, Metadata metadata) {
		if(output == null || inputs == null || inputs.isEmpty() || metadata == null) {
			throw new IllegalArgumentException();
		}
		
		Converter converter = createConverter();
		Future<Void> future = EXECUTOR.submit(createTask(converter, output, inputs, metadata));
		return new ManagerSubmitResult<>(converter, future);
	}
	
	public static final void dispose() {
		EXECUTOR.shutdownNow();
	}
	
	public static final boolean isRunning() {
		return !EXECUTOR.isShutdown();
	}
	
	/** @since 00.02.08 */
	private static final class FFmpegTask implements Callable<Void> {
		
		private final Converter converter;
		private final ResolvedMedia output;
		private final List<ConversionMedia> inputs;
		private final Metadata metadata;
		
		public FFmpegTask(Converter converter, ResolvedMedia output, List<ConversionMedia> inputs, Metadata metadata) {
			this.converter = Objects.requireNonNull(converter);
			this.output = Objects.requireNonNull(output);
			this.inputs = Objects.requireNonNull(inputs);
			this.metadata = Objects.requireNonNull(metadata);
		}
		
		@Override
		public Void call() throws Exception {
			converter.start(FFmpeg.Command.of(output, inputs, metadata));
			return null;
		}
	}
}