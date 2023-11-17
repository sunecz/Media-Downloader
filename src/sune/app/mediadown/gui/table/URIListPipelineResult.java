package sune.app.mediadown.gui.table;

import java.util.List;

import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineTask;
import sune.app.mediadown.pipeline.TerminatingPipelineTask;

/** @since 00.02.07 */
public final class URIListPipelineResult implements TablePipelineResult<ResolvedMedia> {
	
	private final List<ResolvedMedia> media;
	
	public URIListPipelineResult(List<ResolvedMedia> media) {
		this.media = media;
	}
	
	@Override
	public final PipelineTask process(Pipeline pipeline) throws Exception {
		return TerminatingPipelineTask.getTypedInstance();
	}
	
	@Override
	public final boolean isTerminating() {
		return !media.isEmpty();
	}
	
	@Override
	public final List<ResolvedMedia> getValue() {
		return media;
	}
}