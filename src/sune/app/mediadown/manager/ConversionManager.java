package sune.app.mediadown.manager;

import java.util.List;
import java.util.Objects;

import sune.app.mediadown.Disposables;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.concurrent.PositionAwareQueueTaskExecutor;
import sune.app.mediadown.concurrent.PositionAwareQueueTaskExecutor.PositionAwareQueueTaskResult;
import sune.app.mediadown.concurrent.QueueTaskExecutor.QueueTask;
import sune.app.mediadown.convert.ConversionMedia;
import sune.app.mediadown.convert.Converter;
import sune.app.mediadown.event.tracker.PipelineStates;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.event.tracker.WaitTracker;
import sune.app.mediadown.ffmpeg.FFmpeg;
import sune.app.mediadown.ffmpeg.FFmpegConverter;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.util.Metadata;
import sune.app.mediadown.util.QueueContext;

/** @since 00.01.26 */
public final class ConversionManager implements QueueContext {
	
	/** @since 00.02.08 */
	private static ConversionManager instance;
	
	/** @since 00.02.08 */
	private PositionAwareQueueTaskExecutor<Void> executor;
	
	// Forbid anyone to create an instance of this class
	private ConversionManager() {
	}
	
	/** @since 00.02.08 */
	private final PositionAwareQueueTaskExecutor<Void> executor() {
		synchronized(ConversionManager.class) {
			if(executor == null) {
				executor = new PositionAwareQueueTaskExecutor<>(MediaDownloader.configuration().parallelConversions());
				Disposables.add(this::dispose);
			}
			
			return executor;
		}
	}
	
	/** @since 00.02.08 */
	private final Converter createConverter() {
		return new FFmpegConverter(new TrackerManager(new WaitTracker()));
	}
	
	/** @since 00.02.08 */
	private final QueueTask<Void> createTask(Converter converter, ResolvedMedia output,
			List<ConversionMedia> inputs, Metadata metadata) {
		return new FFmpegTask(converter, output, inputs, metadata);
	}
	
	/** @since 00.02.08 */
	public static final ConversionManager instance() {
		return instance == null ? instance = new ConversionManager() : instance;
	}
	
	/** @since 00.02.08 */
	public final PositionAwareManagerSubmitResult<Converter, Void> submit(ResolvedMedia output,
			List<ConversionMedia> inputs, Metadata metadata) {
		if(output == null || inputs == null || inputs.isEmpty() || metadata == null) {
			throw new IllegalArgumentException();
		}
		
		Converter converter = createConverter();
		PositionAwareQueueTaskResult<Void> taskResult = executor().submit(createTask(converter, output, inputs, metadata));
		
		return new PositionAwareManagerSubmitResult<>(converter, taskResult, this);
	}
	
	public final void dispose() throws Exception {
		synchronized(ConversionManager.class) {
			if(executor == null) {
				return;
			}
			
			executor.stop();
		}
	}
	
	public final boolean isRunning() {
		synchronized(ConversionManager.class) {
			return executor != null && executor.isRunning();
		}
	}
	
	/** @since 00.02.08 */
	@Override
	public String contextState() {
		return PipelineStates.CONVERSION;
	}
	
	/** @since 00.02.08 */
	private static final class FFmpegTask implements QueueTask<Void> {
		
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