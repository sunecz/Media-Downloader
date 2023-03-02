package sune.app.mediadown.manager;

import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Objects;

import sune.app.mediadown.Disposables;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.concurrent.PositionAwareQueueTaskExecutor;
import sune.app.mediadown.concurrent.PositionAwareQueueTaskExecutor.PositionAwareQueueTaskResult;
import sune.app.mediadown.concurrent.QueueTaskExecutor.QueueTask;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.DownloadResult;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.entity.Downloader;
import sune.app.mediadown.entity.Downloaders;
import sune.app.mediadown.event.tracker.PipelineStates;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.util.QueueContext;

/** @since 00.01.26 */
public final class DownloadManager implements QueueContext {
	
	/** @since 00.02.08 */
	private static DownloadManager instance;
	
	private volatile PositionAwareQueueTaskExecutor<Long> executor;
	
	// Forbid anyone to create an instance of this class
	private DownloadManager() {
	}
	
	private final PositionAwareQueueTaskExecutor<Long> executor() {
		if(executor == null) {
			synchronized(this) {
				if(executor == null) {
					executor = new PositionAwareQueueTaskExecutor<>(MediaDownloader.configuration().parallelDownloads());
					Disposables.add(this::dispose);
				}
			}
		}
		
		return executor;
	}
	
	private final DownloadResult createDownloadResult(Media media, Path destination,
			MediaDownloadConfiguration mediaConfiguration) throws Exception {
		Downloader downloader = Downloaders.forMedia(media);
		
		if(downloader == null) {
			throw new NoSuchElementException("No downloader found for " + media);
		}
		
		return downloader.download(media, destination, mediaConfiguration);
	}
	
	private final DownloadManagerTask createTask(DownloadResult result) {
		return new DownloadManagerTask(result);
	}
	
	/** @since 00.02.08 */
	public static final DownloadManager instance() {
		return instance == null ? instance = new DownloadManager() : instance;
	}
	
	public final PositionAwareManagerSubmitResult<DownloadResult, Long> submit(Media media, Path destination,
			MediaDownloadConfiguration mediaConfiguration, DownloadConfiguration configuration) throws Exception {
		if(media == null || destination == null || mediaConfiguration == null || configuration == null) {
			throw new IllegalArgumentException();
		}
		
		DownloadResult result = createDownloadResult(media, destination, mediaConfiguration);
		PositionAwareQueueTaskResult<Long> taskResult = executor().submit(createTask(result));
		
		return new PositionAwareManagerSubmitResult<>(result, taskResult, this);
	}
	
	public final void dispose() throws Exception {
		if(executor != null) {
			synchronized(this) {
				if(executor != null) {
					executor.stop();
				}
			}
		}
	}
	
	public final boolean isRunning() {
		synchronized(this) {
			return executor != null && executor.isRunning();
		}
	}
	
	/** @since 00.02.08 */
	@Override
	public String contextState() {
		return PipelineStates.DOWNLOAD;
	}
	
	/** @since 00.02.08 */
	private static final class DownloadManagerTask implements QueueTask<Long> {
		
		private final DownloadResult result;
		
		private DownloadManagerTask(DownloadResult result) {
			this.result = Objects.requireNonNull(result);
		}
		
		@Override
		public Long call() throws Exception {
			result.download().start();
			return 0L;
		}
	}
}