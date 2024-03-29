package sune.app.mediadown.manager;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import sune.app.mediadown.Disposables;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.concurrent.PositionAwareQueueTaskExecutor;
import sune.app.mediadown.concurrent.PositionAwareQueueTaskExecutor.PositionAwareQueueTaskResult;
import sune.app.mediadown.concurrent.QueueTaskExecutor.QueueTask;
import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.conversion.ConversionCommand;
import sune.app.mediadown.conversion.ConversionMedia;
import sune.app.mediadown.conversion.ConversionProvider;
import sune.app.mediadown.entity.Converter;
import sune.app.mediadown.event.ConversionEvent;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.tracker.PipelineStates;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.event.tracker.WaitTracker;
import sune.app.mediadown.exception.WrappedReportContextException;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.media.MediaConversionContext;
import sune.app.mediadown.report.ReportContext;
import sune.app.mediadown.util.CheckedConsumer;
import sune.app.mediadown.util.NIO;
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
	public final PositionAwareManagerSubmitResult<Converter, Void> submit(
			List<ConversionMedia> inputs, ResolvedMedia output
	) {
		if(inputs == null || inputs.isEmpty() || output == null) {
			throw new IllegalArgumentException();
		}
		
		ConversionProvider provider = conversionProvider();
		ConversionTask task = new ConversionTask(provider, inputs, output);
		ConversionDelegate converter = new ConversionDelegate(task);
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
	
	/** @since 00.02.09 */
	private static final class ConversionDelegate implements Converter {
		
		private final ConversionTask task;
		private EventRegistry<ConversionEvent> eventDelegate;
		
		public ConversionDelegate(ConversionTask task) {
			this.task = task;
			this.task.delegate = this;
		}
		
		private final Converter delegate() {
			return task.converter;
		}
		
		private final EventRegistry<ConversionEvent> eventDelegate() {
			EventRegistry<ConversionEvent> ref;
			if((ref = eventDelegate) == null) {
				eventDelegate = ref = new EventRegistry<>();
			}
			
			return ref;
		}
		
		private final void doAction(CheckedConsumer<Converter> action) throws Exception {
			Converter delegate = delegate();
			
			if(delegate == null) {
				return;
			}
			
			action.accept(delegate);
		}
		
		private final <T> T doAction(Function<Converter, T> action, T defaultValue) {
			Converter delegate = delegate();
			
			if(delegate == null) {
				return defaultValue;
			}
			
			return action.apply(delegate);
		}
		
		protected final void converterCreated() {
			EventRegistry<ConversionEvent> ref;
			if((ref = eventDelegate) == null) {
				return; // No events to transfer
			}
			
			ref.transferListenersTo(delegate());
			eventDelegate = null;
		}
		
		@Override public void start(ConversionCommand command) throws Exception { doAction((c) -> c.start(command)); }
		@Override public void stop() throws Exception { doAction(Converter::stop); }
		@Override public void pause() throws Exception  { doAction(Converter::pause); }
		@Override public void resume() throws Exception { doAction(Converter::resume); }
		@Override public void close() throws Exception  { doAction(Converter::close); }
		@Override public ConversionCommand command() { return doAction(Converter::command, null); }
		@Override public Exception exception() { return doAction(Converter::exception, null); }
		@Override public boolean isRunning() { return doAction(Converter::isRunning, false); }
		@Override public boolean isDone() { return doAction(Converter::isDone, false); }
		@Override public boolean isStarted() { return doAction(Converter::isStarted, false); }
		@Override public boolean isPaused() { return doAction(Converter::isPaused, false); }
		@Override public boolean isStopped() { return doAction(Converter::isStopped, false); }
		@Override public boolean isError() { return doAction(Converter::isError, false); }
		@Override public TrackerManager trackerManager() { return doAction(Converter::trackerManager, null); }
		
		@Override
		public <V> void addEventListener(Event<? extends ConversionEvent, V> event, Listener<V> listener) {
			Converter delegate;
			if((delegate = delegate()) != null) {
				delegate.addEventListener(event, listener);
			} else {
				// Delegate the action to an event delegate. The final state of the event delegate
				// will then be transfered to the actual action delegate.
				eventDelegate().add(event, listener);
			}
		}
		
		@Override
		public <V> void removeEventListener(Event<? extends ConversionEvent, V> event, Listener<V> listener) {
			Converter delegate;
			if((delegate = delegate()) != null) {
				delegate.removeEventListener(event, listener);
			} else {
				// Delegate the action to an event delegate. The final state of the event delegate
				// will then be transfered to the actual action delegate.
				eventDelegate().remove(event, listener);
			}
		}
	}
	
	/** @since 00.02.08 */
	private static final class ConversionTask implements QueueTask<Void>, MediaConversionContext {
		
		/** @since 00.02.09 */
		private final ConversionProvider provider;
		private final List<ConversionMedia> inputs;
		private final ResolvedMedia output;
		private Converter converter;
		/** @since 00.02.09 */
		private ConversionDelegate delegate;
		
		public ConversionTask(ConversionProvider provider, List<ConversionMedia> inputs, ResolvedMedia output) {
			this.provider = Objects.requireNonNull(provider);
			this.inputs = Objects.requireNonNull(inputs);
			this.output = Objects.requireNonNull(output);
		}
		
		/** @since 00.02.09 */
		private final ReportContext createContext() {
			return ReportContext.ofConversion(this);
		}
		
		@Override
		public Void call() throws Exception {
			try {
				ConversionCommand command = provider.createCommand(inputs, output);
				
				if(command == ConversionCommand.Constants.RENAME) {
					// Check the number of inputs and throw an exception, rather than silently deleting
					// possible other inputs, apart from the first one.
					if(inputs.size() > 1) {
						throw new IllegalStateException("Cannot rename multiple files into a single file");
					}
					
					// Direct rename (only the first input file)
					NIO.moveForce(inputs.get(0).path(), output.path());
					return null; // Do not continue
				}
				
				converter = provider.createConverter(new TrackerManager(new WaitTracker()));
				
				// Must notify the bound delegate so that already added events may be transferred
				// correctly. That means that any event added before the converter was created will be
				// also added to the newly created converter.
				if(delegate != null) {
					delegate.converterCreated();
				}
				
				converter.start(command);
				return null;
			} catch(Exception ex) {
				throw new WrappedReportContextException(ex, createContext());
			}
		}
		
		/** @since 00.02.09 */
		@Override
		public List<ConversionMedia> inputs() {
			return inputs;
		}
		
		/** @since 00.02.09 */
		@Override
		public ResolvedMedia output() {
			return output;
		}
	}
}