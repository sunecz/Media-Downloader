package sune.app.mediadown.download;

import java.nio.file.Path;

import sune.app.mediadown.Download;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventCallable;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.util.Range;
import sune.app.mediadown.util.Web.GetRequest;

@Deprecated(forRemoval=true)
public interface IInternalDownloader extends EventBindable<DownloadEvent>, EventCallable<DownloadEvent> {
	
	long start(GetRequest request, Path file, Download download);
	long start(GetRequest request, Path file, Download download, long total);
	long start(GetRequest request, Path file, Download download, Range<Long> rangeRequest);
	long start(GetRequest request, Path file, Download download, Range<Long> rangeRequest, Range<Long> rangeFile);
	long start(GetRequest request, Path file, Download download, long total, Range<Long> rangeRequest);
	long start(GetRequest request, Path file, Download download, long total, Range<Long> rangeRequest, Range<Long> rangeFile);
	
	void pause();
	void resume();
	void stop();
	/**
	 * @since 00.01.18
	 * @see Download#revive()*/
	long revive();
	
	void setTracker(DownloadTracker tracker);
	
	GetRequest getRequest();
	Path getFile();
	long getSize();
	
	boolean isRunning();
	boolean isDone();
	boolean isStarted();
	boolean isPaused();
	boolean isStopped();
}