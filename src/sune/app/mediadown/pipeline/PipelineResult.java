package sune.app.mediadown.pipeline;

/** @since 00.01.26 */
public interface PipelineResult {
	
	PipelineTask process(Pipeline pipeline) throws Exception;
	boolean isTerminating();
}