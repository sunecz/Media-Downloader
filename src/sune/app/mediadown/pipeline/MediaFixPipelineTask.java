package sune.app.mediadown.pipeline;

import java.util.List;

import sune.app.mediadown.conversion.ConversionMedia;
import sune.app.mediadown.event.MediaFixEvent;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.manager.MediaFixManager;
import sune.app.mediadown.manager.PositionAwareManagerSubmitResult;
import sune.app.mediadown.media.fix.MediaFixContext;
import sune.app.mediadown.media.fix.MediaFixer;
import sune.app.mediadown.util.Metadata;

/** @since 00.02.09 */
public final class MediaFixPipelineTask
		extends ManagerPipelineTask<MediaFixer, Void>
		implements MediaFixContext {
	
	private final boolean needConversion;
	private final ResolvedMedia output;
	private final List<ConversionMedia> inputs;
	private final Metadata metadata;
	
	private MediaFixPipelineTask(
			boolean needConversion, ResolvedMedia output, List<ConversionMedia> inputs, Metadata metadata
	) {
		if(output == null || inputs == null || inputs.isEmpty() || metadata == null) {
			throw new IllegalArgumentException();
		}
		
		this.needConversion = needConversion;
		this.output = output;
		this.inputs = inputs;
		this.metadata = metadata;
	}
	
	public static final MediaFixPipelineTask of(
			boolean needConversion, ResolvedMedia output, List<ConversionMedia> inputs, Metadata metadata
	) {
		return new MediaFixPipelineTask(needConversion, output, inputs, metadata);
	}
	
	@Override
	protected PositionAwareManagerSubmitResult<MediaFixer, Void> submit(Pipeline pipeline) throws Exception {
		return MediaFixManager.instance().submit(output, inputs, metadata);
	}
	
	@Override
	protected void bindEvents(Pipeline pipeline) throws Exception {
		bindAllEvents(pipeline.getEventRegistry(), result().value(), MediaFixEvent::values);
	}
	
	@Override
	protected PipelineResult pipelineResult() throws Exception {
		if(needConversion) {
			return ConversionPipelineResult.doConversion(output, inputs, metadata);
		}
		
		return ConversionPipelineResult.noConversion();
	}
	
	@Override protected void doStop() throws Exception { doAction(MediaFixer::stop); }
	@Override protected void doPause() throws Exception { doAction(MediaFixer::pause); }
	@Override protected void doResume() throws Exception { doAction(MediaFixer::resume); }
	
	@Override public boolean isRunning() { return doAction(MediaFixer::isRunning, false); }
	@Override public boolean isStarted() { return doAction(MediaFixer::isStarted, false); }
	@Override public boolean isDone() { return doAction(MediaFixer::isDone, false); }
	@Override public boolean isPaused() { return doAction(MediaFixer::isPaused, false); }
	@Override public boolean isStopped() { return doAction(MediaFixer::isStopped, false); }
	@Override public boolean isError() { return doAction(MediaFixer::isError, false); }
	
	@Override public ResolvedMedia output() { return output; }
	@Override public List<ConversionMedia> inputs() { return inputs; }
	@Override public Metadata metadata() { return metadata; }
}