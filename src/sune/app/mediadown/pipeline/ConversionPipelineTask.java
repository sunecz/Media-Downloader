package sune.app.mediadown.pipeline;

import java.nio.file.Path;

import sune.app.mediadown.convert.ConversionConfiguration;
import sune.app.mediadown.convert.Converter;
import sune.app.mediadown.event.ConversionEvent;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.manager.ConversionManager;
import sune.app.mediadown.manager.ManagerSubmitResult;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.pipeline.Pipeline.PipelineEventRegistry;

/** @since 00.01.26 */
public final class ConversionPipelineTask implements PipelineTask<ConversionPipelineResult> {
	
	private final ConversionConfiguration configuration;
	private final MediaFormat formatInput;
	private final MediaFormat formatOutput;
	private final Path fileOutput;
	private final Path[] filesInput;
	
	private ManagerSubmitResult<Converter, Void> result;
	
	private ConversionPipelineTask(ConversionConfiguration configuration,
			MediaFormat formatInput, MediaFormat formatOutput, Path fileOutput, Path[] filesInput) {
		if((configuration == null || formatInput == null || formatOutput == null || fileOutput == null
				|| filesInput == null || filesInput.length <= 0))
			throw new IllegalArgumentException();
		this.configuration = configuration;
		this.formatInput = formatInput;
		this.formatOutput = formatOutput;
		this.fileOutput = fileOutput;
		this.filesInput = filesInput;
	}
	
	public static final ConversionPipelineTask of(ConversionConfiguration configuration, MediaFormat formatInput,
			MediaFormat formatOutput, Path fileOutput, Path... filesInput) {
		return new ConversionPipelineTask(configuration, formatInput, formatOutput, fileOutput, filesInput);
	}
	
	@Override
	public final ConversionPipelineResult run(Pipeline pipeline) throws Exception {
		result = ConversionManager.submit(configuration, formatInput, formatOutput, fileOutput, filesInput);
		// Bind all events from the pipeline
		PipelineEventRegistry eventRegistry = pipeline.getEventRegistry();
		Converter converter = result.getValue();
		for(EventType<ConversionEvent, ?> type : ConversionEvent.values()) {
			eventRegistry.bindEvents(converter, type);
		}
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