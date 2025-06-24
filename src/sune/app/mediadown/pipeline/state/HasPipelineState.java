package sune.app.mediadown.pipeline.state;

/** @since 00.02.09 */
public interface HasPipelineState {
	
	default PipelineState state() {
		return null; // No state by default for compatibility reasons
	}
}
