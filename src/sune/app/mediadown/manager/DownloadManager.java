package sune.app.mediadown.manager;

import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Objects;

import sune.app.mediadown.Disposables;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.concurrent.PositionAwareQueueTaskExecutor;
import sune.app.mediadown.concurrent.PositionAwareQueueTaskExecutor.PositionAwareQueueTaskResult;
import sune.app.mediadown.concurrent.QueueTaskExecutor.QueueTask;
import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.download.Download;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.DownloadResult;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.entity.Downloader;
import sune.app.mediadown.entity.Downloaders;
import sune.app.mediadown.event.tracker.PipelineStates;
import sune.app.mediadown.exception.WrappedReportContextException;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaDownloadContext;
import sune.app.mediadown.report.ReportContext;
import sune.app.mediadown.util.QueueContext;

/** @since 00.01.26 */
public final class DownloadManager implements QueueContext {
	
	/** @since 00.02.08 */
	private static final VarLoader<DownloadManager> instance = VarLoader.of(DownloadManager::new);
	
	private final PositionAwareQueueTaskExecutor<Long> executor;
	
	private DownloadManager() {
		executor = new PositionAwareQueueTaskExecutor<>(MediaDownloader.configuration().parallelDownloads());
		Disposables.add(this::dispose);
	}
	
	private final DownloadResult createDownloadResult(Media media, Path destination,
			MediaDownloadConfiguration mediaConfiguration) throws Exception {
		Downloader downloader = Downloaders.forMedia(media);
		
		if(downloader == null) {
			throw new NoSuchElementException("No downloader found for " + media);
		}
		
		return downloader.download(media, destination, mediaConfiguration);
	}
	
	private final DownloadManagerTask createTask(DownloadResult result, Media media, Path destination,
			MediaDownloadConfiguration mediaConfiguration, DownloadConfiguration configuration) {
		return new DownloadManagerTask(result, media, destination, mediaConfiguration, configuration);
	}
	
	/** @since 00.02.08 */
	public static final DownloadManager instance() {
		return instance.value();
	}
	
	public final PositionAwareManagerSubmitResult<DownloadResult, Long> submit(Media media, Path destination,
			MediaDownloadConfiguration mediaConfiguration, DownloadConfiguration configuration) throws Exception {
		if(media == null || destination == null || mediaConfiguration == null || configuration == null) {
			throw new IllegalArgumentException();
		}
		
		DownloadResult result = createDownloadResult(media, destination, mediaConfiguration);
		DownloadManagerTask task = createTask(result, media, destination, mediaConfiguration, configuration);
		PositionAwareQueueTaskResult<Long> taskResult = executor.submit(task);
		
		return new PositionAwareManagerSubmitResult<>(result, taskResult, this);
	}
	
	public final void dispose() throws Exception {
		if(!isRunning()) {
			return; // Nothing to do
		}
		
		executor.stop();
	}
	
	public final boolean isRunning() {
		return executor.isRunning();
	}
	
	/** @since 00.02.08 */
	@Override
	public String contextState() {
		return PipelineStates.DOWNLOAD;
	}
	
	/** @since 00.02.08 */
	private static final class DownloadManagerTask implements QueueTask<Long>, MediaDownloadContext {
		
		private final DownloadResult result;
		private final Media media;
		private final Path destination;
		private final MediaDownloadConfiguration mediaConfiguration;
		private final DownloadConfiguration configuration;
		
		private DownloadManagerTask(DownloadResult result, Media media, Path destination,
				MediaDownloadConfiguration mediaConfiguration, DownloadConfiguration configuration) {
			this.result = Objects.requireNonNull(result);
			this.media = Objects.requireNonNull(media);
			this.destination = Objects.requireNonNull(destination);
			this.mediaConfiguration = Objects.requireNonNull(mediaConfiguration);
			this.configuration = Objects.requireNonNull(configuration);
		}
		
		/** @since 00.02.09 */
		private final ReportContext createContext() {
			return ReportContext.ofDownload(this);
		}
		
		@Override
		public Long call() throws Exception {
			try(Download download = result.download()) {
				download.start();
				return 0L;
			} catch(Exception ex) {
				throw new WrappedReportContextException(ex, createContext());
			}
		}
		
		/** @since 00.02.09 */
		@Override
		public Media media() {
			return media;
		}
		
		/** @since 00.02.09 */
		@Override
		public Path destination() {
			return destination;
		}
		
		/** @since 00.02.09 */
		@Override
		public MediaDownloadConfiguration mediaConfiguration() {
			return mediaConfiguration;
		}
		
		/** @since 00.02.09 */
		@Override
		public DownloadConfiguration configuration() {
			return configuration;
		}
	}
}