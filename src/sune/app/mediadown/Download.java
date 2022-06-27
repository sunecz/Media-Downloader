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
	 * @since 00.01.15 */
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
	 * @since 00.01.18 */
	default long revive() throws Exception {
		restart();
		return 0L;
	}
	
	/** @since 00.01.26 */
	default DownloadPipelineResult getResult() {
		// By default do not convert anything
		return DownloadPipelineResult.noConversion();
	}
	
	// Informative methods
	boolean isRunning();
	boolean isStarted();
	boolean isDone();
	boolean isPaused();
	/** @since 00.01.14 */
	boolean isStopped();
}