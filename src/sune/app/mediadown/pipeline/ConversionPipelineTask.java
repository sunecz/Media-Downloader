package sune.app.mediadown.pipeline;

import java.util.List;

import sune.app.mediadown.convert.ConversionMedia;
import sune.app.mediadown.convert.Converter;
import sune.app.mediadown.event.ConversionEvent;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.manager.ConversionManager;
import sune.app.mediadown.manager.ManagerSubmitResult;
import sune.app.mediadown.util.Metadata;

/** @since 00.01.26 */
public final class ConversionPipelineTask implements PipelineTask<ConversionPipelineResult> {
	
	/** @since 00.02.08 */
	private final ConversionMedia output;
	/** @since 00.02.08 */
	private final List<ConversionMedia> inputs;
	/** @since 00.02.08 */
	private final Metadata metadata;
	
	private ManagerSubmitResult<Converter, Void> result;
	
	/** @since 00.02.08 */
	private ConversionPipelineTask(ConversionMedia output, List<ConversionMedia> inputs, Metadata metadata) {
		if(output == null || inputs == null || inputs.isEmpty() || metadata == null) {
			throw new IllegalArgumentException();
		}
		
		this.output = output;
		this.inputs = inputs;
		this.metadata = metadata;
	}
	
	/** @since 00.02.08 */
	public static final ConversionPipelineTask of(ConversionMedia output, List<ConversionMedia> inputs,
			Metadata metadata) {
		return new ConversionPipelineTask(output, inputs, metadata);
	}
	
	@Override
	public final ConversionPipelineResult run(Pipeline pipeline) throws Exception {
		// Ensure that the input formats are not explicitly stated in the command
		Metadata altered = metadata.copy();
		altered.set("noExplicitInputFormat", true);
		
		result = ConversionManager.submit(output, inputs, altered);
		
		// Bind all events from the pipeline
		EventRegistry<EventType> eventRegistry = pipeline.getEventRegistry();
		Converter converter = result.getValue();
		eventRegistry.bindAll(converter, ConversionEvent.values());
		
		result.get(); // Wait for the conversion to finish
		return ConversionPipelineResult.noConversion();
	}
	
	private final Converter converter() {
		return result.getValue();
	}
	
	@Override
	public final void stop() throws Exception {
		converter().stop();
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