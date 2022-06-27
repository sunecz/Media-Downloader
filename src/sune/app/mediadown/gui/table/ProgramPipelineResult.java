package sune.app.mediadown.gui.table;

import java.util.List;

import sune.app.mediadown.Episode;
import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineTask;
import sune.app.mediadown.util.TriFunction;

/** @since 00.01.27 */
public final class ProgramPipelineResult implements TablePipelineResult<Episode, TablePipelineResult<?, ?>> {
	
	private final TableWindow window;
	private final MediaEngine engine;
	private final List<Episode> episodes;
	
	public ProgramPipelineResult(TableWindow window, MediaEngine engine, List<Episode> episodes) {
		this.window = window;
		this.engine = engine;
		this.episodes = episodes;
	}
	
	@Override
	public final PipelineTask<? extends TablePipelineResult<?, ?>> process(Pipeline pipeline) throws Exception {
		List<Episode> selected = window.waitAndGetSelection(episodes);
		TriFunction<TableWindow, MediaEngine, List<Episode>,
				PipelineTask<? extends TablePipelineResult<?, ?>>> function
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