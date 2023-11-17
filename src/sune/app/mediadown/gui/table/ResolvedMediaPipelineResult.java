package sune.app.mediadown.gui.table;

import java.util.List;

import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineTask;
import sune.app.mediadown.pipeline.TerminatingPipelineTask;

/** @since 00.01.27 */
public final class ResolvedMediaPipelineResult implements TablePipelineResult<ResolvedMedia> {
	
	private final TableWindow window;
	private final List<ResolvedMedia> media;
	
	public ResolvedMediaPipelineResult(TableWindow window, List<ResolvedMedia> media) {
		this.window = window;
		this.media = media;
	}
	
	@Override
	public final PipelineTask process(Pipeline pipeline) throws Exception {
		if(!isTerminating()) {
			// Return to the previous task, so it can be run again
			PipelineTask casted = pipeline.getTasksHistory().backwardAndGet();
			pipeline.getTasksHistory().backward(); // Add the next task to the correct position in the future
			window.goBack(); // Must be called if the Go Back button should work correctly
			return casted;
		}
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