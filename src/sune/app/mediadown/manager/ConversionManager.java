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
import sune.app.mediadown.conversion.ConversionProvider;
import sune.app.mediadown.entity.Converter;
import sune.app.mediadown.event.tracker.PipelineStates;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.event.tracker.WaitTracker;
import sune.app.mediadown.exception.WrappedReportContextException;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.media.MediaConversionContext;
import sune.app.mediadown.report.ReportContext;
import sune.app.mediadown.util.Metadata;
import sune.app.mediadown.util.QueueContext;

/** @since 00.01.26 */
public final class ConversionManager implements QueueContext {
	
	/** @since 00.02.08 */
	private static final VarLoader<ConversionManager> instance = VarLoader.of(ConversionManager::new);
	
	/** @since 00.02.08 */
	private final PositionAwareQueueTaskExecutor<Void> executor;
	
	private ConversionManager() {
		executor = new PositionAwareQueueTaskExecutor<>(MediaDownloader.configuration().parallelConversions());
		Disposables.add(this::dispose);
	}
	
	/** @since 00.02.08 */
	public static final ConversionManager instance() {
		return instance.value();
	}
	
	/** @since 00.02.09 */
	private final ConversionProvider conversionProvider() {
		return MediaDownloader.configuration().conversionProvider();
	}
	
	/** @since 00.02.08 */
	public final PositionAwareManagerSubmitResult<Converter, Void> submit(ResolvedMedia output,
			List<ConversionMedia> inputs, Metadata metadata) {
		if(output == null || inputs == null || inputs.isEmpty() || metadata == null) {
			throw new IllegalArgumentException();
		}
		
		ConversionProvider provider = conversionProvider();
		Converter converter = provider.createConverter(new TrackerManager(new WaitTracker()));
		ConversionTask task = new ConversionTask(provider, converter, output, inputs, metadata);
		PositionAwareQueueTaskResult<Void> taskResult = executor.submit(task);
		
		return new PositionAwareManagerSubmitResult<>(converter, taskResult, this);
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
	
	/** @since 00.02.08 */
	@Override
	public String contextState() {
		return PipelineStates.CONVERSION;
	}
	
	/** @since 00.02.08 */
	private static final class ConversionTask implements QueueTask<Void>, MediaConversionContext {
		
		/** @since 00.02.09 */
		private final ConversionProvider provider;
		private final Converter converter;
		private final ResolvedMedia output;
		private final List<ConversionMedia> inputs;
		private final Metadata metadata;
		
		public ConversionTask(ConversionProvider provider, Converter converter, ResolvedMedia output,
				List<ConversionMedia> inputs, Metadata metadata) {
			this.provider = Objects.requireNonNull(provider);
			this.converter = Objects.requireNonNull(converter);
			this.output = Objects.requireNonNull(output);
			this.inputs = Objects.requireNonNull(inputs);
			this.metadata = Objects.requireNonNull(metadata);
		}
		
		/** @since 00.02.09 */
		private final ReportContext createContext() {
			return ReportContext.ofConversion(this);
		}
		
		@Override
		public Void call() throws Exception {
			try {
				converter.start(provider.createCommand(output, inputs, metadata));
				return null;
			} catch(Exception ex) {
				throw new WrappedReportContextException(ex, createContext());
			}
		}
		
		/** @since 00.02.09 */
		@Override
		public ResolvedMedia output() {
			return output;
		}
		
		/** @since 00.02.09 */
		@Override
		public List<ConversionMedia> inputs() {
			return inputs;
		}
		
		/** @since 00.02.09 */
		@Override
		public Metadata metadata() {
			return metadata;
		}
	}
}