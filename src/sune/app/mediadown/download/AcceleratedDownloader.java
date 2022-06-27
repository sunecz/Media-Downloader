package sune.app.mediadown.download;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import sune.app.mediadown.Download;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.util.CounterLock;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Range;
import sune.app.mediadown.util.Threads;
import sune.app.mediadown.util.Web;
import sune.app.mediadown.util.Web.GetRequest;

public class AcceleratedDownloader implements IInternalDownloader {
	
	private static int acceleratedDownloaderCount;
	private static int acceleratedDownloaderCount() {
		if((acceleratedDownloaderCount == 0)) {
			acceleratedDownloaderCount = MediaDownloader.configuration().acceleratedDownload();
			// Always have at least one downloader
			acceleratedDownloaderCount = Math.max(1, acceleratedDownloaderCount);
		}
		return acceleratedDownloaderCount;
	}
	
	private final List<SingleFileDownloader> downloaders;
	private final int count;
	
	private final EventRegistry<DownloadEvent> eventRegistry;
	
	private GetRequest request;
	private Path file;
	private long total;
	
	public AcceleratedDownloader(TrackerManager manager) {
		this(manager, acceleratedDownloaderCount());
	}
	
	public AcceleratedDownloader(TrackerManager manager, int count) {
		this(manager, count, new SingleFileDownloaderConfiguration(count == 1, false));
	}
	
	public AcceleratedDownloader(TrackerManager manager, int count, SingleFileDownloaderConfiguration configuration) {
		if((manager == null || configuration == null))
			throw new IllegalArgumentException();
		// Ensure valid downloaders count
		if((count <= 0))
			count = acceleratedDownloaderCount();
		this.downloaders   = new ArrayList<>(count);
		this.count         = count;
		this.eventRegistry = new EventRegistry<>();
		for(int i = 0; i < count; ++i) {
			SingleFileDownloader downloader = new SingleFileDownloader(manager, configuration);
			downloader.addEventListener(DownloadEvent.BEGIN,  this::event_BEGIN);
			downloader.addEventListener(DownloadEvent.UPDATE, this::event_UPDATE);
			downloader.addEventListener(DownloadEvent.END,    this::event_END);
			downloader.addEventListener(DownloadEvent.ERROR,  this::event_ERROR);
			downloaders.add(downloader);
		}
	}
	
	private final void doAction(Consumer<SingleFileDownloader> action) {
		for(int i = 0, l = downloaders.size(); i < l; ++i) {
			action.accept(downloaders.get(i));
		}
	}
	
	private final boolean checkState(Function<SingleFileDownloader, Boolean> func) {
		for(int i = 0, l = downloaders.size(); i < l; ++i) {
			if(!func.apply(downloaders.get(i)))
				return false;
		}
		return true;
	}
	
	private boolean eventFlag_BEGIN = false;
	private final void event_BEGIN(Download download) {
		// Only the first begin notification is propagated
		if((eventFlag_BEGIN)) return;
		eventFlag_BEGIN = true;
		// Notify the event registry
		eventRegistry.call(DownloadEvent.BEGIN, download);
	}
	
	private final void event_UPDATE(Pair<Download, TrackerManager> pair) {
		// Update events are always propagated
		eventRegistry.call(DownloadEvent.UPDATE, pair);
	}
	
	private final void event_END(Download download) {
		// Only if all the downloaders are either done or stopped
		if(!checkState((d) -> d.isDone() || d.isStopped()))
			return;
		// Notify the event registry
		eventRegistry.call(DownloadEvent.END, download);
	}
	
	private final void event_ERROR(Pair<Download, Exception> pair) {
		// Stop the downloaders
		doAction(SingleFileDownloader::stop);
		// Notify the event registry
		eventRegistry.call(DownloadEvent.ERROR, pair);
	}
	
	@Override
	public <E> void addEventListener(EventType<DownloadEvent, E> type, Listener<E> listener) {
		eventRegistry.add(type, listener);
	}
	
	@Override
	public <E> void removeEventListener(EventType<DownloadEvent, E> type, Listener<E> listener) {
		eventRegistry.remove(type, listener);
	}
	
	public <E> void call(EventType<DownloadEvent, E> type) {
		doAction((downloader) -> downloader.getEventRegistry().call(type));
	}
	
	public <E> void call(EventType<DownloadEvent, E> type, E value) {
		doAction((downloader) -> downloader.getEventRegistry().call(type, value));
	}
	
	@Override
	public long start(GetRequest request, Path file, Download download) {
		return start(request, file, download, -1L, new Range<>(-1L, -1L), new Range<>(-1L, -1L));
	}
	
	@Override
	public long start(GetRequest request, Path file, Download download, long total) {
		return start(request, file, download, total, new Range<>(-1L, -1L), new Range<>(-1L, -1L));
	}
	
	@Override
	public long start(GetRequest request, Path file, Download download, Range<Long> rangeRequest) {
		return start(request, file, download, -1L, rangeRequest, rangeRequest);
	}
	
	@Override
	public long start(GetRequest request, Path file, Download download, long total, Range<Long> rangeRequest) {
		return start(request, file, download, total, rangeRequest, rangeRequest);
	}
	
	@Override
	public long start(GetRequest request, Path file, Download download, Range<Long> rangeRequest, Range<Long> rangeFile) {
		return start(request, file, download, -1L, rangeRequest, rangeFile);
	}
	
	private final Range<Long> checkRequestRange(Range<Long> rangeRequest, long total) {
		long req_rangeStart = rangeRequest.from();
		long req_rangeEnd   = rangeRequest.to();
		if((req_rangeStart < 0L))
			req_rangeStart = 0L;
		if((req_rangeEnd < 0L || req_rangeEnd >= total))
			// -1L because end index inclusive
			req_rangeEnd = total - 1L;
		// Update the request range
		return new Range<>(req_rangeStart, req_rangeEnd);
	}
	
	private final Range<Long> checkRequestRange(Range<Long> rangeFile, Range<Long> rangeRequest) {
		long req_rangeLength = rangeRequest.to() - rangeRequest.from();
		long file_rangeStart = rangeFile.from();
		long file_rangeEnd   = rangeFile.to();
		if((file_rangeStart < 0L))
			file_rangeStart = 0L;
		if((file_rangeEnd < 0L || file_rangeEnd - file_rangeStart > req_rangeLength))
			file_rangeEnd = file_rangeStart + req_rangeLength;
		// Update the file range
		return new Range<>(file_rangeStart, file_rangeEnd);
	}
	
	@Override
	public long start(GetRequest request, Path file, Download download, long total, Range<Long> rangeRequest, Range<Long> rangeFile) {
		eventFlag_BEGIN = false; // Important to reset the flag
		CounterLock lock = new CounterLock(count);
		if((total < 0L)) {
			try {
				total = Web.size(request.toHeadRequest());
			} catch(Exception ex) {
				// Ignore
			}
		}
		// Check the request range
		rangeRequest = checkRequestRange(rangeRequest, total);
		// Check the file range
		rangeFile = checkRequestRange(rangeFile, rangeRequest);
		AtomicLong bytes = new AtomicLong();
		this.total = total; // Must be set to be seen outside of this class
		if((count == 1)) {
			final Range<Long> _rangeRequest = rangeRequest;
			final Range<Long> _rangeFile    = rangeFile;
			SingleFileDownloader downloader = downloaders.get(0);
			Threads.execute(() -> {
				bytes.getAndAdd(downloader.start(request, file, download, this.total, _rangeRequest, _rangeFile));
				lock.decrement();
			});
		} else {
			long req_total = rangeRequest.to() - rangeRequest.from() + 1L;
			long step = req_total / count;
			if((req_total % count > 0L))
				++step;
			DownloadTracker tracker = new DownloadTracker(req_total);
			long reqRange_start  = rangeRequest.from();
			long reqRange_end    = reqRange_start + step;
			long fileRange_start = rangeFile.from();
			long fileRange_end   = fileRange_start + step;
			for(int i = 0; i < count; ++i) {
				final long _reqRange_start  = reqRange_start;
				final long _reqRange_end    = reqRange_end;
				final long _fileRange_start = fileRange_start;
				final long _fileRange_end   = fileRange_end;
				SingleFileDownloader downloader = downloaders.get(i);
				downloader.setTracker(tracker);
				Threads.execute(() -> {
					bytes.getAndAdd(downloader.start(request, file, download, this.total,
						new Range<>(_reqRange_start, _reqRange_end),
						new Range<>(_fileRange_start, _fileRange_end)));
					lock.decrement();
				});
				reqRange_start  = reqRange_end + 1L;
				reqRange_end   += step;
				fileRange_start = fileRange_end + 1L;
				fileRange_end  += step;
				if((reqRange_end > rangeRequest.to()))
					reqRange_end = rangeRequest.to();
				long req_rangeLength  = reqRange_end - reqRange_start;
				long file_rangeLength = fileRange_end - fileRange_start;
				if((file_rangeLength > req_rangeLength))
					fileRange_end = fileRange_start + req_rangeLength;
			}
		}
		lock.await();
		return bytes.get();
	}
	
	@Override
	public void pause() {
		doAction(SingleFileDownloader::pause);
	}
	
	@Override
	public void resume() {
		doAction(SingleFileDownloader::resume);
	}
	
	@Override
	public void stop() {
		doAction(SingleFileDownloader::stop);
	}
	
	@Override
	public long revive() {
		AtomicLong bytes = new AtomicLong();
		doAction((downloader) -> {
			bytes.getAndAdd(downloader.revive());
		});
		return bytes.get();
	}
	
	@Override
	public void setTracker(DownloadTracker tracker) {
		doAction((downloader) -> downloader.setTracker(tracker));
	}
	
	@Override
	public GetRequest getRequest() {
		return request;
	}
	
	@Override
	public Path getFile() {
		return file;
	}
	
	@Override
	public long getSize() {
		return total;
	}
	
	public int getThreadCount() {
		return count;
	}
	
	@Override
	public boolean isRunning() {
		return checkState(SingleFileDownloader::isRunning);
	}
	
	@Override
	public boolean isDone() {
		return checkState(SingleFileDownloader::isDone);
	}
	
	@Override
	public boolean isStarted() {
		return checkState(SingleFileDownloader::isStarted);
	}
	
	@Override
	public boolean isPaused() {
		return checkState(SingleFileDownloader::isPaused);
	}
	
	@Override
	public boolean isStopped() {
		return checkState(SingleFileDownloader::isStopped);
	}
}