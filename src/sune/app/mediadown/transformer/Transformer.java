package sune.app.mediadown.transformer;

import java.util.List;
import java.util.Objects;

import sune.app.mediadown.media.ResolvedMedia;
import sune.app.mediadown.pipeline.PipelineTransformer;

/** @since 00.02.09 */
public interface Transformer {
	
	boolean isUsable(ResolvedMedia media);
	PipelineTransformer pipelineTransformer();
	
	static Transformer ofDefault() {
		return DefaultTransformer.instance();
	}
	
	static Transformer of(Transformer... transformers) {
		return of(List.of(transformers));
	}
	
	static Transformer of(List<Transformer> transformers) {
		Objects.requireNonNull(transformers);
		
		if(transformers.isEmpty()) {
			throw new IllegalArgumentException();
		}
		
		if(transformers.size() == 1) {
			return transformers.get(0);
		}
		
		return new CombinedTransformer(transformers);
	}
}