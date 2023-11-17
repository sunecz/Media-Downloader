package sune.app.mediadown.pipeline;

/** @since 00.02.09 */
public interface PipelineTransformer {
	
	PipelineResult transform(PipelineResult result);
	PipelineTask transform(PipelineTask task);
	
	static PipelineTransformer ofDefault() {
		return DefaultPipelineTransformer.instance();
	}
}