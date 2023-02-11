package sune.app.mediadown.pipeline;

import java.nio.file.Path;
import java.util.Objects;

import sune.app.mediadown.concurrent.StateMutex;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.media.Media;

/** @since 00.02.08 */
public class PipelineMedia {
	
	private final Media media;
	private final Path destination;
	private final MediaDownloadConfiguration mediaConfiguration;
	private final DownloadConfiguration configuration;
	
	private final StateMutex mtxSubmitted = new StateMutex();
	
	private PipelineMedia(Media media, Path destination, MediaDownloadConfiguration mediaConfiguration,
	        DownloadConfiguration configuration) {
		this.media = Objects.requireNonNull(media);
		this.destination = Objects.requireNonNull(destination);
		this.mediaConfiguration = Objects.requireNonNull(mediaConfiguration);
		this.configuration = Objects.requireNonNull(configuration);
	}
	
	public static final PipelineMedia of(Media media, Path destination, MediaDownloadConfiguration mediaConfiguration,
	        DownloadConfiguration configuration) {
		return new PipelineMedia(media, destination, mediaConfiguration, configuration);
	}
	
	public void awaitSubmitted() {
		mtxSubmitted.await();
	}
	
	public void submit() {
		mtxSubmitted.unlock();
	}
	
	public Media media() {
		return media;
	}
	
	public Path destination() {
		return destination;
	}
	
	public MediaDownloadConfiguration mediaConfiguration() {
		return mediaConfiguration;
	}
	
	public DownloadConfiguration configuration() {
		return configuration;
	}
}