package sune.app.mediadown.gui.table;

import java.util.List;

import sune.app.mediadown.entity.MediaEngine;
import sune.app.mediadown.entity.Program;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.pipeline.Pipeline;

/** @since 00.01.27 */
public final class MediaEnginePipelineResult implements TablePipelineResult<Program> {
	
	private final TableWindow window;
	private final MediaEngine engine;
	private final List<Program> programs;
	
	public MediaEnginePipelineResult(TableWindow window, MediaEngine engine, List<Program> programs) {
		this.window = window;
		this.engine = engine;
		this.programs = programs;
	}
	
	@Override
	public final ProgramPipelineTask process(Pipeline pipeline) throws Exception {
		return new ProgramPipelineTask(window, engine, window.waitAndGetSelection(programs));
	}
	
	@Override
	public final boolean isTerminating() {
		return false;
	}
	
	@Override
	public final List<Program> getValue() {
		return programs;
	}
}