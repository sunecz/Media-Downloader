package sune.app.mediadown.manager;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import sune.app.mediadown.Disposables;
import sune.app.mediadown.Download;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.Downloader;
import sune.app.mediadown.download.Downloaders;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.util.Threads;
import sune.app.mediadown.util.Utils;

/** @since 00.01.26 */
public final class DownloadManager {
	
	private static final ExecutorService EXECUTOR;
	
	static {
		int numOfThreads = MediaDownloader.configuration().parallelDownloads();
		EXECUTOR = Threads.Pools.newFixed(numOfThreads);
		Disposables.add(DownloadManager::dispose);
	}
	
	private static final void error(String message) {
		MediaDownloader.error(new RuntimeException(message));
	}
	
	private static final Download createDownload(Media media, Path destination,
			MediaDownloadConfiguration mediaConfiguration) throws Exception {
		Downloader downloader = Downloaders.forMedia(media);
		if(downloader == null) {
			error("No downloader found for " + media);
			return null;
		}
		return downloader.download(media, destination, mediaConfiguration);
	}
	
	private static final Callable<Long> createCallable(Download download) {
		return Utils.callable(download::start, 0L);
	}
	
	public static final ManagerSubmitResult<Download, Long> submit(Media media, Path destination,
			MediaDownloadConfiguration mediaConfiguration, DownloadConfiguration configuration) throws Exception {
		if(media == null || destination == null || mediaConfiguration == null || configuration == null)
			throw new IllegalArgumentException();
		Download download = createDownload(media, destination, mediaConfiguration);
		Future<Long> future = EXECUTOR.submit(createCallable(download));
		return new ManagerSubmitResult<>(download, future);
	}
	
	public static final void dispose() {
		EXECUTOR.shutdownNow();
	}
	
	public static final boolean isRunning() {
		return !EXECUTOR.isShutdown();
	}
}