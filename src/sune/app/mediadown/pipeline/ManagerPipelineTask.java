package sune.app.mediadown.pipeline;

import java.util.function.Function;
import java.util.function.Supplier;

import sune.app.mediadown.concurrent.PositionAwareQueueTaskExecutor.PositionAwareQueueTaskResult;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.QueueEvent;
import sune.app.mediadown.event.tracker.Trackable;
import sune.app.mediadown.event.tracker.TrackerEvent;
import sune.app.mediadown.manager.PositionAwareManagerSubmitResult;
import sune.app.mediadown.util.CheckedConsumer;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.QueueContext;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.09 */
public abstract class ManagerPipelineTask<R, V> implements PipelineTask {
	
	private PositionAwareManagerSubmitResult<R, V> result;
	
	protected ManagerPipelineTask() {
	}
	
	private static final void callTrackerEventUpdate(EventRegistry<EventType> eventRegistry, Trackable trackable) {
		eventRegistry.call(TrackerEvent.UPDATE, trackable.trackerManager().tracker());
	}
	
	protected static final <T extends EventType> void bindAllEvents(
			EventRegistry<EventType> eventRegistry,
			EventBindable<? super T> bindable,
			Supplier<Event<? extends T, ?>[]> events
	) {
		for(Event<? extends T, ?> event : events.get()) {
			bindable.addEventListener(event, (ctx) -> callTrackerEventUpdate(eventRegistry, (Trackable) ctx));
		}
	}
	
	protected final <T, P> P doAction(Function<R, T> cast, Function<T, P> action, P defaultValue) {
		T value;
		return result == null || (value = cast.apply(result.value())) == null
					? defaultValue
					: action.apply(value);
	}
	
	protected final <T, P> void doAction(Function<R, T> cast, CheckedConsumer<T> action) throws Exception {
		T value;
		if(result == null || (value = cast.apply(result.value())) == null) {
			return;
		}
		
		action.accept(value);
	}
	
	protected final <P> P doAction(Function<R, P> action, P defaultValue) {
		return doAction(Function.identity(), action, defaultValue);
	}
	
	protected final <P> void doAction(CheckedConsumer<R> action) throws Exception {
		doAction(Function.identity(), action);
	}
	
	protected final PositionAwareManagerSubmitResult<R, V> result() {
		return result;
	}
	
	protected abstract PositionAwareManagerSubmitResult<R, V> submit(Pipeline pipeline) throws Exception;
	protected abstract void bindEvents(Pipeline pipeline) throws Exception;
	protected abstract PipelineResult pipelineResult() throws Exception;
	
	protected void doStop() throws Exception {}
	protected void doPause() throws Exception {}
	protected void doResume() throws Exception {}
	
	@Override
	public final PipelineResult run(Pipeline pipeline) throws Exception {
		result = submit(pipeline);
		QueueContext context = result.context();
		
		// Notify the pipeline if the position in a queue changed
		PositionAwareQueueTaskResult<Long> positionAwareTaskResult = Utils.cast(result.taskResult());
		positionAwareTaskResult.queuePositionProperty().addListener((o, ov, queuePosition) -> {
			pipeline.getEventRegistry().call(
				QueueEvent.POSITION_UPDATE,
				new Pair<>(context, queuePosition.intValue())
			);
		});
		
		pipeline.getEventRegistry().call(
			QueueEvent.POSITION_UPDATE,
			new Pair<>(context, positionAwareTaskResult.queuePosition())
		);
		
		bindEvents(pipeline);
		Ignore.Cancellation.call(result::get); // Wait for the download to finish
		return pipelineResult();
	}
	
	@Override
	public final void stop() throws Exception {
		doStop();
		
		if(result != null) {
			result.cancel();
		}
	}
	
	@Override
	public final void pause() throws Exception {
		doPause();
		
		if(result != null) {
			result.pause();
		}
	}
	
	@Override
	public final void resume() throws Exception {
		doResume();
		
		if(result != null) {
			result.resume();
		}
	}
}