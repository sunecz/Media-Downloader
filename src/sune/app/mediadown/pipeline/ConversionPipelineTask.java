package sune.app.mediadown.pipeline;

import java.util.List;
import java.util.function.Function;

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
import sune.app.mediadown.media.MediaConversionContext;
import sune.app.mediadown.util.CheckedConsumer;
import sune.app.mediadown.util.Metadata;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.QueueContext;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.01.26 */
public final class ConversionPipelineTask implements PipelineTask, MediaConversionContext {
	
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
	
	/** @since 00.02.09 */
	private final <T> T converterAction(Function<Converter, T> action, T defaultValue) {
		Converter converter;
		
		// Check the chain of values to avoid NPE
		if(result == null
				|| (converter = result.value()) == null) {
			return defaultValue;
		}
		
		return action.apply(converter);
	}
	
	/** @since 00.02.09 */
	private final void converterAction(CheckedConsumer<Converter> action) throws Exception {
		Converter converter;
		
		// Check the chain of values to avoid NPE
		if(result == null
				|| (converter = result.value()) == null) {
			return;
		}
		
		action.accept(converter);
	}
	
	@Override
	public ConversionPipelineResult run(Pipeline pipeline) throws Exception {
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
	public void stop() throws Exception {
		converterAction(Converter::stop);
		
		if(result != null) {
			result.cancel();
		}
	}
	
	@Override
	public void pause() throws Exception {
		converterAction(Converter::pause);
	}
	
	@Override
	public void resume() throws Exception {
		converterAction(Converter::resume);
	}
	
	@Override
	public boolean isRunning() {
		return converterAction(Converter::isRunning, false);
	}
	
	@Override
	public boolean isStarted() {
		return converterAction(Converter::isStarted, false);
	}
	
	@Override
	public boolean isDone() {
		return converterAction(Converter::isDone, false);
	}
	
	@Override
	public boolean isPaused() {
		return converterAction(Converter::isPaused, false);
	}
	
	@Override
	public boolean isStopped() {
		return converterAction(Converter::isStopped, false);
	}
	
	@Override
	public boolean isError() {
		return converterAction(Converter::isError, false);
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