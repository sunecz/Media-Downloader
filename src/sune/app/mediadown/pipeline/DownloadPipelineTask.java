package sune.app.mediadown.pipeline;

import java.nio.file.Path;

import sune.app.mediadown.Download;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.manager.DownloadManager;
import sune.app.mediadown.manager.ManagerSubmitResult;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.pipeline.Pipeline.PipelineEventRegistry;

/** @since 00.01.26 */
public final class DownloadPipelineTask implements PipelineTask<DownloadPipelineResult> {
	
	private final Media media;
	private final Path destination;
	private final MediaDownloadConfiguration mediaConfiguration;
	private final DownloadConfiguration configuration;
	
	private ManagerSubmitResult<Download, Long> result;
	
	private DownloadPipelineTask(Media media, Path destination, MediaDownloadConfiguration mediaConfiguration,
			DownloadConfiguration configuration) {
		if(media == null || destination == null || mediaConfiguration == null || configuration == null)
			throw new IllegalArgumentException();
		this.media = media;
		this.destination = destination;
		this.mediaConfiguration = mediaConfiguration;
		this.configuration = configuration;
	}
	
	public static final DownloadPipelineTask of(Media media, Path destination,
			MediaDownloadConfiguration mediaConfiguration, DownloadConfiguration configuration) {
		return new DownloadPipelineTask(media, destination, mediaConfiguration, configuration);
	}
	
	@Override
	public final DownloadPipelineResult run(Pipeline pipeline) throws Exception {
		result = DownloadManager.submit(media, destination, mediaConfiguration, configuration);
		// Bind all events from the pipeline
		PipelineEventRegistry eventRegistry = pipeline.getEventRegistry();
		Download download = result.getValue();
		for(EventType<DownloadEvent, ?> type : DownloadEvent.values()) {
			eventRegistry.bindEvents(download, type);
		}
		result.get(); // Wait for the download to finish
		return download.getResult();
	}
	
	private final Download download() {
		return result.getValue();
	}
	
	@Override
	public final void stop() throws Exception {
		download().stop();
	}
	
	@Override
	public final void pause() throws Exception {
		download().pause();
	}
	
	@Override
	public final void resume() throws Exception {
		download().resume();
	}
	
	@Override
	public final boolean isRunning() {
		return download().isRunning();
	}
	
	@Override
	public final boolean isStarted() {
		return download().isStarted();
	}
	
	@Override
	public final boolean isDone() {
		return download().isDone();
	}
	
	@Override
	public final boolean isPaused() {
		return download().isPaused();
	}
	
	@Override
	public final boolean isStopped() {
		return download().isStopped();
	}
}