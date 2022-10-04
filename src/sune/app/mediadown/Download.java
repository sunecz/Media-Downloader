package sune.app.mediadown;

import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.pipeline.DownloadPipelineResult;

public interface Download extends EventBindable<DownloadEvent> {
	
	// Functional methods
	void start() throws Exception;
	void stop() throws Exception;
	void pause() throws Exception;
	void resume() throws Exception;
	/**
	 * Restarts the download, meaning that the download is first stopped and then started
	 * again. The default implementation just calls the {@linkplain #stop()} and
	 * {@linkplain #start()} method, exactly in that order.
	 * @deprecated Download API temporarily drops the support of restarting a download.
	 * This should be done higher in the application for now.
	 * @since 00.01.15 */
	@Deprecated(forRemoval=true)
	default void restart() throws Exception {
		stop();
		start();
	}
	/**
	 * Revives the download, if possible. That means if, for example, an error occurred and thus
	 * the download has been stopped, then after calling this method the downloading should continue
	 * at the point when the error occurred. The implementation is downloader-dependent, thus
	 * not specified. The default implementation just calls the {@linkplain #restart()} method
	 * and always returns {@code 0L}.
	 * @return Number of downloaded bytes, if {@code this} download was revived, otherwise {@code -1L}
	 * @deprecated Download API temporarily drops the support of reviving a download.
	 * This should be done higher in the application for now.
	 * @since 00.01.18 */
	@Deprecated(forRemoval=true)
	default long revive() throws Exception {
		restart();
		return 0L;
	}
	
	/**
	 * @deprecated Will be replaced by more robust API in the future, where
	 * the Pipeline API and Download API is separated.
	 * @since 00.01.26
	 */
	@Deprecated(forRemoval=true)
	default DownloadPipelineResult getResult() {
		// By default do not convert anything
		return DownloadPipelineResult.noConversion();
	}
	
	boolean isRunning();
	boolean isDone();
	boolean isStarted();
	boolean isPaused();
	/** @since 00.01.14 */
	boolean isStopped();
	/** @since 00.02.08 */
	boolean isError();
}