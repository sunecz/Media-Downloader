package sune.app.mediadown.pipeline;

import java.util.List;

import sune.app.mediadown.concurrent.PositionAwareQueueTaskExecutor.PositionAwareQueueTaskResult;
import sune.app.mediadown.conversion.ConversionMedia;
import sune.app.mediadown.entity.Converter;
import sune.app.mediadown.event.ConversionEvent;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.QueueEvent;
import sune.app.mediadown.event.tracker.Trackable;
import sune.app.mediadown.event.tracker.TrackerEvent;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.manager.ConversionManager;
import sune.app.mediadown.manager.ManagerSubmitResult;
import sune.app.mediadown.util.Metadata;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.QueueContext;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.01.26 */
public final class ConversionPipelineTask implements PipelineTask<ConversionPipelineResult> {
	
	/** @since 00.02.08 */
	private final ResolvedMedia output;
	/** @since 00.02.08 */
	private final List<ConversionMedia> inputs;
	/** @since 00.02.08 */
	private final Metadata metadata;
	
	private ManagerSubmitResult<Converter, Void> result;
	
	/** @since 00.02.08 */
	private ConversionPipelineTask(ResolvedMedia output, List<ConversionMedia> inputs, Metadata metadata) {
		if(output == null || inputs == null || inputs.isEmpty() || metadata == null) {
			throw new IllegalArgumentException();
		}
		
		this.output = output;
		this.inputs = inputs;
		this.metadata = metadata;
	}
	
	/** @since 00.02.09 */
	private static final void callTrackerEventUpdate(EventRegistry<EventType> eventRegistry, Trackable trackable) {
		eventRegistry.call(TrackerEvent.UPDATE, trackable.trackerManager().tracker());
	}
	
	/** @since 00.02.08 */
	public static final ConversionPipelineTask of(ResolvedMedia output, List<ConversionMedia> inputs,
			Metadata metadata) {
		return new ConversionPipelineTask(output, inputs, metadata);
	}
	
	/** @since 00.02.09 */
	private final void bindAllConversionEvents(EventRegistry<EventType> eventRegistry, Converter converter) {
		for(Event<ConversionEvent, ?> event : ConversionEvent.values()) {
			converter.addEventListener(event, (ctx) -> callTrackerEventUpdate(eventRegistry, (Trackable) ctx));
		}
	}
	
	private final Converter converter() {
		return result.value();
	}
	
	@Override
	public final ConversionPipelineResult run(Pipeline pipeline) throws Exception {
		// Ensure that the input formats are not explicitly stated in the command
		Metadata altered = metadata.copy();
		altered.set("noExplicitInputFormat", true);
		
		result = ConversionManager.instance().submit(output, inputs, altered);
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
		
		// Bind all events from the pipeline
		EventRegistry<EventType> eventRegistry = pipeline.getEventRegistry();
		Converter converter = result.value();
		bindAllConversionEvents(eventRegistry, converter);
		
		Ignore.Cancellation.call(result::get); // Wait for the conversion to finish
		return ConversionPipelineResult.noConversion();
	}
	
	@Override
	public final void stop() throws Exception {
		converter().stop();
		result.cancel();
	}
	
	@Override
	public final void pause() throws Exception {
		converter().pause();
	}
	
	@Override
	public final void resume() throws Exception {
		converter().resume();
	}
	
	@Override
	public final boolean isRunning() {
		return converter().isRunning();
	}
	
	@Override
	public final boolean isStarted() {
		return converter().isStarted();
	}
	
	@Override
	public final boolean isDone() {
		return converter().isDone();
	}
	
	@Override
	public final boolean isPaused() {
		return converter().isPaused();
	}
	
	@Override
	public final boolean isStopped() {
		return converter().isStopped();
	}
}