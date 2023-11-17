package sune.app.mediadown.gui.table;

import java.util.List;

import sune.app.mediadown.entity.Episode;
import sune.app.mediadown.entity.MediaEngine;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineTask;
import sune.app.mediadown.util.TriFunction;

/** @since 00.01.27 */
public final class ProgramPipelineResult implements TablePipelineResult<Episode> {
	
	private final TableWindow window;
	private final MediaEngine engine;
	private final List<Episode> episodes;
	
	public ProgramPipelineResult(TableWindow window, MediaEngine engine, List<Episode> episodes) {
		this.window = window;
		this.engine = engine;
		this.episodes = episodes;
	}
	
	@Override
	public final PipelineTask process(Pipeline pipeline) throws Exception {
		List<Episode> selected = window.waitAndGetSelection(episodes);
		TriFunction<TableWindow, MediaEngine, List<Episode>, PipelineTask> function
			= selected.size() > 1
				? MultipleEpisodePipelineTask::new
				: EpisodePipelineTask::new;
		return function.apply(window, engine, selected);
	}
	
	@Override
	public final boolean isTerminating() {
		return false;
	}
	
	@Override
	public final List<Episode> getValue() {
		return episodes;
	}
}