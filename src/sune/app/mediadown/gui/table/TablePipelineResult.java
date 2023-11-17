package sune.app.mediadown.gui.table;

import java.util.List;

import sune.app.mediadown.pipeline.PipelineResult;

/** @since 00.01.27 */
public interface TablePipelineResult<V> extends PipelineResult {
	
	List<V> getValue();
}