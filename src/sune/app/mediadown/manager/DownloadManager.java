package sune.app.mediadown.manager;

import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import sune.app.mediadown.Disposables;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.DownloadResult;
import sune.app.mediadown.download.Downloader;
import sune.app.mediadown.download.Downloaders;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.util.Threads;
import sune.app.mediadown.util.Utils;

/** @since 00.01.26 */
public final class DownloadManager {
	
	private static ExecutorService executor;
	
	// Forbid anyone to create an instance of this class
	private DownloadManager() {
	}
	
	private static final ExecutorService executor() {
		synchronized(DownloadManager.class) {
			if(executor == null) {
				executor = Threads.Pools.newFixed(MediaDownloader.configuration().parallelDownloads());
				Disposables.add(DownloadManager::dispose);
			}
			
			return executor;
		}
	}
	
	private static final DownloadResult createDownloadResult(Media media, Path destination,
			MediaDownloadConfiguration mediaConfiguration) throws Exception {
		Downloader downloader = Downloaders.forMedia(media);
		
		if(downloader == null) {
			throw new NoSuchElementException("No downloader found for " + media);
		}
		
		return downloader.download(media, destination, mediaConfiguration);
	}
	
	private static final Callable<Long> createCallable(DownloadResult result) {
		return Utils.callable(result.download()::start, 0L);
	}
	
	public static final ManagerSubmitResult<DownloadResult, Long> submit(Media media, Path destination,
			MediaDownloadConfiguration mediaConfiguration, DownloadConfiguration configuration) throws Exception {
		if(media == null || destination == null || mediaConfiguration == null || configuration == null)
			throw new IllegalArgumentException();
		
		DownloadResult result = createDownloadResult(media, destination, mediaConfiguration);
		Future<Long> future = executor().submit(createCallable(result));
		
		return new ManagerSubmitResult<>(result, future);
	}
	
	public static final void dispose() {
		synchronized(DownloadManager.class) {
			if(executor == null)
				return;
			
			executor.shutdownNow();
		}
	}
	
	public static final boolean isRunning() {
		synchronized(DownloadManager.class) {
			return executor != null && !executor.isShutdown();
		}
	}
}