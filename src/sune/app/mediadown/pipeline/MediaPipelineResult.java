package sune.app.mediadown.pipeline;

import java.util.Objects;

public final class MediaPipelineResult implements PipelineResult {
	
	/** @since 00.02.08 */
	private final PipelineMedia media;
	
	private MediaPipelineResult(PipelineMedia media) {
		this.media = Objects.requireNonNull(media);
	}
	
	/** @since 00.02.08 */
	public static final MediaPipelineResult of(PipelineMedia media) {
		return new MediaPipelineResult(media);
	}
	
	@Override
	public final DownloadPipelineTask process(Pipeline pipeline) throws Exception {
		return DownloadPipelineTask.of(media);
	}
	
	@Override
	public final boolean isTerminating() {
		return false;
	}
	
	/** @since 00.02.09 */
	public PipelineMedia media() {
		return media;
	}
}