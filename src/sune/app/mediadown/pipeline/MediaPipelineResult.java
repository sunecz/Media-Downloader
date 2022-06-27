package sune.app.mediadown.pipeline;

import java.nio.file.Path;

import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.pipeline.PipelineTaskRegistry.PipelineTaskInputData;
import sune.app.mediadown.util.Utils;

public final class MediaPipelineResult implements PipelineResult<DownloadPipelineResult> {
	
	private final Media media;
	private final Path destination;
	private final MediaDownloadConfiguration mediaConfiguration;
	private final DownloadConfiguration configuration;
	
	private MediaPipelineResult(Media media, Path destination, MediaDownloadConfiguration mediaConfiguration,
			DownloadConfiguration configuration) {
		if(media == null || destination == null || mediaConfiguration == null || configuration == null)
			throw new IllegalArgumentException();
		this.media = media;
		this.destination = destination;
		this.mediaConfiguration = mediaConfiguration;
		this.configuration = configuration;
	}
	
	public static final MediaPipelineResult of(Media media, Path destination,
			MediaDownloadConfiguration mediaConfiguration, DownloadConfiguration configuration) {
		return new MediaPipelineResult(media, destination, mediaConfiguration, configuration);
	}
	
	@Override
	public final PipelineTask<DownloadPipelineResult> process(Pipeline pipeline) throws Exception {
		if(media.metadata().get("isProtected", false)) {
			PipelineTaskInputData data = new PipelineTaskInputData(Utils.toMap(
				"media", media, "destination", destination, "configuration", configuration));
			PipelineTask<DownloadPipelineResult> task
				= Utils.ignore(() -> PipelineTaskRegistry.instance("ProtectedMediaPipelineTask", data),
				               (PipelineTask<DownloadPipelineResult>) null);
			if(task == null)
				throw new IllegalStateException("No pipeline task found for protected media.");
			return task;
		}
		return DownloadPipelineTask.of(media, destination, mediaConfiguration, configuration);
	}
	
	@Override
	public final boolean isTerminating() {
		return false;
	}
}