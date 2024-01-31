package sune.app.mediadown.manager;

import java.util.List;
import java.util.Objects;

import sune.app.mediadown.Disposables;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.concurrent.PositionAwareQueueTaskExecutor;
import sune.app.mediadown.concurrent.PositionAwareQueueTaskExecutor.PositionAwareQueueTaskResult;
import sune.app.mediadown.concurrent.QueueTaskExecutor.QueueTask;
import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.conversion.ConversionMedia;
import sune.app.mediadown.event.tracker.PipelineStates;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.event.tracker.WaitTracker;
import sune.app.mediadown.exception.WrappedReportContextException;
import sune.app.mediadown.ffmpeg.FFmpegFixer;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.media.MediaConversionContext;
import sune.app.mediadown.media.fix.MediaFixer;
import sune.app.mediadown.report.ReportContext;
import sune.app.mediadown.util.QueueContext;

/** @since 00.02.09 */
public final class MediaFixManager implements QueueContext {
	
	private static final VarLoader<MediaFixManager> instance = VarLoader.of(MediaFixManager::new);
	
	private final PositionAwareQueueTaskExecutor<Void> executor;
	
	private MediaFixManager() {
		executor = new PositionAwareQueueTaskExecutor<>(MediaDownloader.configuration().parallelConversions());
		Disposables.add(this::dispose);
	}
	
	public static final MediaFixManager instance() {
		return instance.value();
	}
	
	private final MediaFixer mediaFixer() {
		return new FFmpegFixer(new TrackerManager(new WaitTracker()));
	}
	
	public final PositionAwareManagerSubmitResult<MediaFixer, Void> submit(
			List<ConversionMedia> inputs, ResolvedMedia output
	) {
		if(inputs == null || inputs.isEmpty() || output == null) {
			throw new IllegalArgumentException();
		}
		
		MediaFixer fixer = mediaFixer();
		FFmpegTask task = new FFmpegTask(fixer, inputs, output);
		PositionAwareQueueTaskResult<Void> taskResult = executor.submit(task);
		
		return new PositionAwareManagerSubmitResult<>(fixer, taskResult, this);
	}
	
	public final void dispose() throws Exception {
		if(!isRunning()) {
			return; // Nothing to do
		}
		
		executor.stop();
	}
	
	public final boolean isRunning() {
		return executor.isRunning();
	}
	
	@Override
	public String contextState() {
		return PipelineStates.MEDIA_FIX;
	}
	
	private static final class FFmpegTask implements QueueTask<Void>, MediaConversionContext {
		
		private final MediaFixer fixer;
		private final List<ConversionMedia> inputs;
		private final ResolvedMedia output;
		
		public FFmpegTask(MediaFixer fixer, List<ConversionMedia> inputs, ResolvedMedia output) {
			this.fixer = Objects.requireNonNull(fixer);
			this.inputs = Objects.requireNonNull(inputs);
			this.output = Objects.requireNonNull(output);
		}
		
		private final ReportContext createContext() {
			// Not really a conversion, but still treat it as if it is a conversion
			return ReportContext.ofConversion(this);
		}
		
		@Override
		public Void call() throws Exception {
			try {
				fixer.start(inputs, output);
				return null;
			} catch(Exception ex) {
				throw new WrappedReportContextException(ex, createContext());
			}
		}
		
		@Override
		public List<ConversionMedia> inputs() {
			return inputs;
		}
		
		@Override
		public ResolvedMedia output() {
			return output;
		}
	}
}