package sune.app.mediadown.pipeline;

import java.util.Objects;

import sune.app.mediadown.pipeline.PipelineTaskRegistry.PipelineTaskInputData;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

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
				= Ignore.defaultValue(() -> PipelineTaskRegistry.instance("ProtectedMediaPipelineTask", data), (PipelineTask<DownloadPipelineResult>) null);
			
			if(task == null) {
				throw new IllegalStateException("No pipeline task found for protected media.");
			}
			
			// Notify, regardless of the task
			media.submit();
			
			return task;
		}
		
		return DownloadPipelineTask.of(media);
	}
	
	@Override
	public final boolean isTerminating() {
		return false;
	}
}