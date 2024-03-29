package sune.app.mediadown.pipeline;

import java.util.List;

import sune.app.mediadown.conversion.ConversionMedia;
import sune.app.mediadown.entity.Converter;
import sune.app.mediadown.event.ConversionEvent;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.manager.ConversionManager;
import sune.app.mediadown.manager.PositionAwareManagerSubmitResult;
import sune.app.mediadown.media.MediaConversionContext;

/** @since 00.01.26 */
public final class ConversionPipelineTask
		extends ManagerPipelineTask<Converter, Void>
		implements MediaConversionContext {
	
	/** @since 00.02.08 */
	private final List<ConversionMedia> inputs;
	/** @since 00.02.08 */
	private final ResolvedMedia output;
	
	/** @since 00.02.08 */
	private ConversionPipelineTask(List<ConversionMedia> inputs, ResolvedMedia output) {
		if(inputs == null || inputs.isEmpty() || output == null) {
			throw new IllegalArgumentException();
		}
		
		this.inputs = inputs;
		this.output = output;
	}
	
	/** @since 00.02.08 */
	public static final ConversionPipelineTask of(List<ConversionMedia> inputs, ResolvedMedia output) {
		return new ConversionPipelineTask(inputs, output);
	}
	
	@Override
	protected PositionAwareManagerSubmitResult<Converter, Void> submit(Pipeline pipeline) throws Exception {
		return ConversionManager.instance().submit(inputs, output);
	}
	
	@Override
	protected void bindEvents(Pipeline pipeline) throws Exception {
		bindAllEvents(pipeline.getEventRegistry(), result().value(), ConversionEvent::values);
	}
	
	@Override
	protected PipelineResult pipelineResult() throws Exception {
		return TerminatingPipelineResult.getTypedInstance();
	}
	
	@Override protected void doStop() throws Exception { doAction(Converter::stop); }
	@Override protected void doPause() throws Exception { doAction(Converter::pause); }
	@Override protected void doResume() throws Exception { doAction(Converter::resume); }
	
	@Override public boolean isRunning() { return doAction(Converter::isRunning, false); }
	@Override public boolean isStarted() { return doAction(Converter::isStarted, false); }
	@Override public boolean isDone() { return doAction(Converter::isDone, false); }
	@Override public boolean isPaused() { return doAction(Converter::isPaused, false); }
	@Override public boolean isStopped() { return doAction(Converter::isStopped, false); }
	@Override public boolean isError() { return doAction(Converter::isError, false); }
	
	/** @since 00.02.09 */
	@Override public List<ConversionMedia> inputs() { return inputs; }
	/** @since 00.02.09 */
	@Override public ResolvedMedia output() { return output; }
}