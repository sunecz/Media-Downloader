package sune.app.mediadown.pipeline;

import sune.app.mediadown.concurrent.VarLoader;

/** @since 00.02.09 */
// Package-private
class DefaultPipelineTransformer implements PipelineTransformer {
	
	private static final VarLoader<DefaultPipelineTransformer> instance = VarLoader.of(DefaultPipelineTransformer::create);
	
	// Forbid anyone to create an instance of this class
	private DefaultPipelineTransformer() {
	}
	
	private static final DefaultPipelineTransformer create() {
		return new DefaultPipelineTransformer();
	}
	
	public static final DefaultPipelineTransformer instance() {
		return instance.value();
	}
	
	@Override
	public PipelineTask transform(PipelineTask task) {
		return task; // Do nothing
	}
	
	@Override
	public PipelineResult transform(PipelineResult result) {
		return result; // Do nothing
	}
}