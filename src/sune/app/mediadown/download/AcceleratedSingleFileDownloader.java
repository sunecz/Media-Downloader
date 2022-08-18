package sune.app.mediadown.download;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Web;
import sune.app.mediadown.util.Web.GetRequest;

public class AcceleratedSingleFileDownloader implements IInternalDownloader {
	
	private final TrackerManager manager;
	private final SingleFileDownloaderConfiguration configuration;
	private final int count;
	private final List<SingleFileDownloader> downloaders;
	private final EventRegistry<DownloadEvent> eventRegistry;
	
	private final AtomicBoolean flagBegin = new AtomicBoolean();
	
	private GetRequest request;
	private Path file;
	private long total;
	
	public AcceleratedSingleFileDownloader(TrackerManager manager) {
		this(manager, acceleratedDownloaderCount());
	}
	
	public AcceleratedSingleFileDownloader(TrackerManager manager, int count) {
		this(manager, count, new SingleFileDownloaderConfiguration(count == 1, false));
	}
	
	public AcceleratedSingleFileDownloader(TrackerManager manager, int count,
			SingleFileDownloaderConfiguration configuration) {
		this.manager       = Objects.requireNonNull(manager);
		this.configuration = Objects.requireNonNull(configuration);
		this.count         = checkCount(count);
		this.downloaders   = new ArrayList<>(this.count);
		this.eventRegistry = new EventRegistry<>();
	}
	
	private static final int acceleratedDownloaderCount() {
		return Math.max(1, MediaDownloader.configuration().acceleratedDownload());
	}
	
	/** @since 00.02.07 */
	private static final int checkCount(int count) {
		return count > 0 ? count : acceleratedDownloaderCount();
	}
	
	/** @since 00.02.07 */
	private static final boolean isUnsetRange(Range<Long> range) {
		return range.from() < 0L && range.to() < 0L;
	}
	
	private static final Range<Long> checkRequestRange(Range<Long> rangeRequest, long total) {
		if(isUnsetRange(rangeRequest)) {
			return new Range<>(0L, Math.max(0L, total - 1L));
		}
		
		long from = Math.max(0L, rangeRequest.from());
		long to   = Math.max(0L, Math.min(rangeRequest.to(), total - 1L)); // -1L because end index is inclusive
		return new Range<>(from, to);
	}
	
	private static final Range<Long> checkRequestRange(Range<Long> rangeFile, Range<Long> rangeRequest) {
		long from = Math.max(0L, rangeFile.from());
		long to   = Math.max(0L, rangeFile.to());
		
		if(isUnsetRange(rangeFile)) {
			long length = rangeRequest.to() - rangeRequest.from();
			return new Range<>(from, from + length);
		}
		
		// The length of file range and request range must be equal
		long length = rangeRequest.to() - rangeRequest.from();
		if(to - from != length) to = from + length;
		
		return new Range<>(from, to);
	}
	
	private final void doAction(Consumer<SingleFileDownloader> action) {
		downloaders.stream().forEach(action);
	}
	
	private final boolean checkState(Predicate<SingleFileDownloader> func) {
		return downloaders.stream().allMatch(func);
	}
	
	private final void onBegin(Download download) {
		// Only the first begin notification is propagated
		if(!flagBegin.compareAndSet(false, true))
			return;
		
		// Notify the event registry
		eventRegistry.call(DownloadEvent.BEGIN, download);
	}
	
	private final void onUpdate(Pair<Download, TrackerManager> pair) {
		// Update events are always propagated
		eventRegistry.call(DownloadEvent.UPDATE, pair);
	}
	
	private final void onEnd(Download download) {
		// Only if all the downloaders are either done or stopped
		if(!checkState((d) -> d.isDone() || d.isStopped()))
			return;
		
		// Notify the event registry
		eventRegistry.call(DownloadEvent.END, download);
	}
	
	private final void onError(Pair<Download, Exception> pair) {
		// Stop the downloaders
		doAction(SingleFileDownloader::stop);
		// Notify the event registry
		eventRegistry.call(DownloadEvent.ERROR, pair);
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
	
	@Override
	public long start(GetRequest request, Path file, Download download, long total, Range<Long> rangeRequest, Range<Long> rangeFile) {
		flagBegin.set(false); // Important to reset the flag
		int numOfThreads = count;
		
		if(total < 0L) {
			total = Utils.ignore(() -> Web.size(request.toHeadRequest()), -1L);
		}
		
		// If the total size is still unknown, we cannot split the file into chunks,
		// therefore just use a single file downloader with unset ranges.
		if(total < 0L) {
			rangeRequest = new Range<>(-1L, -1L);
			rangeFile = new Range<>(-1L, -1L);
			numOfThreads = 1; // Use just one downloader
		} else {
			rangeRequest = checkRequestRange(rangeRequest, total);
			rangeFile = checkRequestRange(rangeFile, rangeRequest);
		}
		
		this.total = total; // Must be set to be seen outside of this class
		
		for(int i = 0; i < numOfThreads; ++i) {
			SingleFileDownloader downloader = new SingleFileDownloader(manager, configuration);
			downloader.addEventListener(DownloadEvent.BEGIN,  this::onBegin);
			downloader.addEventListener(DownloadEvent.UPDATE, this::onUpdate);
			downloader.addEventListener(DownloadEvent.END,    this::onEnd);
			downloader.addEventListener(DownloadEvent.ERROR,  this::onError);
			downloaders.add(downloader);
		}
		
		AtomicLong bytes = new AtomicLong();
		if(numOfThreads == 1) {
			SingleFileDownloader downloader = downloaders.get(0);
			long downloaded = downloader.start(request, file, download, this.total, rangeRequest, rangeFile);
			if(downloaded > 0L) bytes.getAndAdd(downloaded);
		} else {
			CounterLock lock = new CounterLock(numOfThreads);
			ExecutorService executor = Threads.Pools.newFixed(numOfThreads);
			
			long size = rangeRequest.to() - rangeRequest.from() + 1L;
			long step = (size + numOfThreads - 1L) / numOfThreads;
			DownloadTracker tracker = new DownloadTracker(size);
			
			long reqFrom  = rangeRequest.from();
			long reqTo    = reqFrom + step;
			long fileFrom = rangeFile.from();
			long fileTo   = fileFrom + step;
			for(int i = 0; i < numOfThreads; ++i) {
				SingleFileDownloader downloader = downloaders.get(i);
				downloader.setTracker(tracker);
				
				Range<Long> _rangeReq  = new Range<>(reqFrom, reqTo);
				Range<Long> _rangeFile = new Range<>(fileFrom, fileTo);
				executor.submit(() -> {
					long downloaded = downloader.start(request, file, download, this.total, _rangeReq, _rangeFile);
					if(downloaded > 0L) bytes.getAndAdd(downloaded);
					lock.decrement();
				});
				
				reqFrom  = reqTo + 1L;
				reqTo   += step;
				fileFrom = fileTo + 1L;
				fileTo  += step;
				
				// Correct the end if it was exceeded
				if(reqTo > rangeRequest.to()) {
					reqTo = rangeRequest.to();
				}
				
				// Correct the lengths if they are not equal
				long lenReq  = reqTo  - reqFrom;
				long lenFile = fileTo - fileFrom;
				if(lenFile != lenReq) {
					fileTo = fileFrom + lenReq;
				}
			}
			
			// Wait for all the downloaders to finish downloading
			lock.await();
			
			// Gracefully shutdown the executor and wait, even though at this point all threads should exited
			executor.shutdown();
			Utils.ignore(() -> executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS));
		}
		
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
	
	/** @since 00.02.07 */
	public int count() {
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
	
	@Override
	public <E> void addEventListener(EventType<DownloadEvent, E> type, Listener<E> listener) {
		eventRegistry.add(type, listener);
	}
	
	@Override
	public <E> void removeEventListener(EventType<DownloadEvent, E> type, Listener<E> listener) {
		eventRegistry.remove(type, listener);
	}
}