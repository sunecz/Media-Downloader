package sune.app.mediadown.pipeline;

/** @since 00.02.08 */
public interface PipelineResultable<R extends PipelineResult<?>> {
	
	PipelineResult<R> pipelineResult();
}