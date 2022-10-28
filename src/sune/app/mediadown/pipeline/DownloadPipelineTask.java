package sune.app.mediadown.pipeline;

import java.nio.file.Path;
import java.util.Objects;

import sune.app.mediadown.Download;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.DownloadResult;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.manager.DownloadManager;
import sune.app.mediadown.manager.ManagerSubmitResult;
import sune.app.mediadown.media.Media;

/** @since 00.01.26 */
public final class DownloadPipelineTask implements PipelineTask<DownloadPipelineResult> {
	
	private final Media media;
	private final Path destination;
	private final MediaDownloadConfiguration mediaConfiguration;
	private final DownloadConfiguration configuration;
	
	private ManagerSubmitResult<DownloadResult, Long> result;
	
	private DownloadPipelineTask(Media media, Path destination, MediaDownloadConfiguration mediaConfiguration,
			DownloadConfiguration configuration) {
		this.media = Objects.requireNonNull(media);
		this.destination = Objects.requireNonNull(destination);
		this.mediaConfiguration = Objects.requireNonNull(mediaConfiguration);
		this.configuration = Objects.requireNonNull(configuration);
	}
	
	public static final DownloadPipelineTask of(Media media, Path destination,
			MediaDownloadConfiguration mediaConfiguration, DownloadConfiguration configuration) {
		return new DownloadPipelineTask(media, destination, mediaConfiguration, configuration);
	}
	
	@Override
	public final DownloadPipelineResult run(Pipeline pipeline) throws Exception {
		result = DownloadManager.submit(media, destination, mediaConfiguration, configuration);
		DownloadResult downloadResult = result.getValue();
		
		// Bind all events from the pipeline
		EventRegistry<EventType> eventRegistry = pipeline.getEventRegistry();
		Download download = downloadResult.download();
		eventRegistry.bindAll(download, DownloadEvent.values());
		
		result.get(); // Wait for the download to finish
		return (DownloadPipelineResult) downloadResult.pipelineResult();
	}
	
	private final Download download() {
		return result.getValue().download();
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