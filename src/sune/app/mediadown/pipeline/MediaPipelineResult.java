package sune.app.mediadown.pipeline;

import java.util.Objects;

import sune.app.mediadown.pipeline.PipelineTaskRegistry.PipelineTaskInputData;
import sune.app.mediadown.util.Utils;

public final class MediaPipelineResult implements PipelineResult<DownloadPipelineResult> {
	
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
	public final PipelineTask<DownloadPipelineResult> process(Pipeline pipeline) throws Exception {
		if(media.media().metadata().isProtected()) {
			PipelineTaskInputData data = new PipelineTaskInputData(Utils.toMap(
				"media", media.media(), "destination", media.destination(), "configuration", media.configuration()
			));
			PipelineTask<DownloadPipelineResult> task
				= Utils.ignore(() -> PipelineTaskRegistry.instance("ProtectedMediaPipelineTask", data),
				               (PipelineTask<DownloadPipelineResult>) null);
			
			// Notify, regardless of the task
			media.submitted();
			
			if(task == null) {
				throw new IllegalStateException("No pipeline task found for protected media.");
			}
			
			return task;
		}
		
		return DownloadPipelineTask.of(media);
	}
	
	@Override
	public final boolean isTerminating() {
		return false;
	}
}