package sune.app.mediadown.pipeline;

import sune.app.mediadown.HasTaskState;

/** @since 00.01.26 */
public interface PipelineTask extends HasTaskState {
	
	PipelineResult run(Pipeline pipeline) throws Exception;
	
	void stop() throws Exception;
	void pause() throws Exception;
	void resume() throws Exception;
}