package sune.app.mediadown.pipeline;

/** @since 00.01.26 */
public interface PipelineResult<R extends PipelineResult<?>> {
	
	PipelineTask<? extends R> process(Pipeline pipeline) throws Exception;
	boolean isTerminating();
}