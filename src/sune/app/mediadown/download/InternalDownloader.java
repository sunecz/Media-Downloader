package sune.app.mediadown.download;

import java.nio.file.Path;

import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventCallable;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.net.Web.Request;

/** @since 00.02.08 */
public interface InternalDownloader
		extends DownloadContext, AutoCloseable, EventCallable<DownloadEvent> {
	
	long start(Request request, Path output, DownloadConfiguration configuration) throws Exception;
	void pause() throws Exception;
	void resume() throws Exception;
	void stop() throws Exception;
	
	void setTracker(DownloadTracker tracker);
	void setResponseStreamFactory(InputStreamFactory factory);
	
	/** @since 00.02.09 */
	long writtenBytes();
}