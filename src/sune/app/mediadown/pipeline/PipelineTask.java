package sune.app.mediadown.pipeline;

/** @since 00.01.26 */
public interface PipelineTask<R extends PipelineResult<?>> {
	
	R run(Pipeline pipeline) throws Exception;
	
	void stop() throws Exception;
	void pause() throws Exception;
	void resume() throws Exception;
	boolean isRunning() throws Exception;
	boolean isStarted() throws Exception;
	boolean isDone() throws Exception;
	boolean isPaused() throws Exception;
	boolean isStopped() throws Exception;
}