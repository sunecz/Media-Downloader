package sune.app.mediadown.download;

import java.nio.file.Path;

import sune.app.mediadown.HasTaskState;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.tracker.Trackable;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.net.Web.Response;

/** @since 00.02.09 */
public interface DownloadContext extends EventBindable<DownloadEvent>, HasTaskState, Trackable {
	
	Request request();
	Path output();
	DownloadConfiguration configuration();
	Response response();
	long totalBytes();
	Exception exception();
}