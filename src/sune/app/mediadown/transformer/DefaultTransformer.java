package sune.app.mediadown.transformer;

import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.media.ResolvedMedia;
import sune.app.mediadown.pipeline.PipelineTransformer;

/** @since 00.02.09 */
//Package-private
class DefaultTransformer implements Transformer {
	
	private static final VarLoader<DefaultTransformer> instance = VarLoader.of(DefaultTransformer::new);
	
	// Package-private
	private DefaultTransformer() {
	}
	
	public static final DefaultTransformer instance() {
		return instance.value();
	}
	
	@Override
	public boolean isUsable(ResolvedMedia media) {
		return true; // All are usable
	}
	
	@Override
	public PipelineTransformer pipelineTransformer() {
		return PipelineTransformer.ofDefault();
	}
}