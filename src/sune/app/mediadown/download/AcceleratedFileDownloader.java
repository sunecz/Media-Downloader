package sune.app.mediadown.download;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.concurrent.CounterLock;
import sune.app.mediadown.concurrent.Threads;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.net.Web.Response;
import sune.app.mediadown.util.CheckedConsumer;
import sune.app.mediadown.util.Range;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.08 */
public class AcceleratedFileDownloader implements InternalDownloader {
	
	private static final Range<Long> RANGE_UNSET = new Range<>(-1L, -1L);
	
	private final TrackerManager trackerManager;
	private final int count;
	private final List<InternalDownloader> downloaders;
	private final EventRegistry<DownloadEvent> eventRegistry;
	
	private final AtomicBoolean flagBegin = new AtomicBoolean();
	
	/** @since 00.02.09 */
	private final AtomicLong bytes = new AtomicLong();
	/** @since 00.02.09 */
	private final AtomicLong written = new AtomicLong();
	
	private Request request;
	private Path output;
	private DownloadConfiguration configuration;
	
	private volatile Response response;
	private long totalBytes;
	
	private Exception exception;
	
	public AcceleratedFileDownloader(TrackerManager manager) {
		this(manager, acceleratedDownloaderCount());
	}
	
	public AcceleratedFileDownloader(TrackerManager manager, int count) {
		this.trackerManager = Objects.requireNonNull(manager);
		this.count          = checkCount(count);
		this.downloaders    = new ArrayList<>(this.count);
		this.eventRegistry  = new EventRegistry<>();
	}
	
	private static final int acceleratedDownloaderCount() {
		return Math.max(1, MediaDownloader.configuration().acceleratedDownload());
	}
	
	private static final int checkCount(int count) {
		if(count <= 0) {
			throw new IllegalArgumentException("Invalid number of threads");
		}
		
		return count;
	}
	
	private static final Range<Long> checkRange(Range<Long> range, long limit) {
		// When this method is called the limit is > 0
		long from = range.from(), to = range.to();
		
		if(from < 0L && to < 0L) {
			return new Range<>(0L, Math.max(0L, limit));
		} else if(from < 0L) {
			return new Range<>(0L, Math.min(to, limit));
		} else if(to < 0L) {
			return new Range<>(from, from + limit);
		} else {
			return new Range<>(from, Math.min(to, from + limit));
		}
	}
	
	private static final Range<Long> checkRange(Range<Long> rangeOutput, Range<Long> rangeRequest) {
		long from = rangeOutput.from(), to = rangeOutput.to();
		long size = rangeRequest.to() - rangeRequest.from();
		
		if(from < 0L && to < 0L) {
			return new Range<>(0L, size);
		}
		
		if(to - from != size) {
			to = from + size;
		}
		
		return new Range<>(from, to);
	}
	
	private final void doAction(CheckedConsumer<InternalDownloader> action) throws Exception {
		for(InternalDownloader downloader : downloaders) {
			action.accept(downloader);
		}
	}
	
	private final boolean checkState(Predicate<InternalDownloader> func) {
		return downloaders.stream().allMatch(func);
	}
	
	private final void onBegin(DownloadContext context) {
		// Only the first begin notification is propagated
		if(!flagBegin.compareAndSet(false, true))
			return;
		
		// Notify the event registry
		eventRegistry.call(DownloadEvent.BEGIN, this);
	}
	
	private final void onUpdate(DownloadContext context) {
		// Update events are always propagated
		eventRegistry.call(DownloadEvent.UPDATE, this);
	}
	
	private final void onEnd(DownloadContext context) {
		// Only if all the downloaders are either done or stopped
		if(!checkState((d) -> d.isDone() || d.isStopped()))
			return;
		
		// Notify the event registry
		eventRegistry.call(DownloadEvent.END, this);
	}
	
	private final void onError(DownloadContext context) {
		// Stop the downloaders
		Ignore.callVoid(() -> doAction(InternalDownloader::stop));
		// Remember the exception
		exception = context.exception();
		// Notify the event registry
		eventRegistry.call(DownloadEvent.ERROR, this);
	}
	
	private final InternalDownloader createDownloader() {
		return new FileDownloader(trackerManager);
	}
	
	private final void maybeSetResponse(Response responseToSet) {
		// Only set the response once
		if(response == null) {
			synchronized(this) {
				if(response == null) {
					response = responseToSet;
				}
			}
		}
	}
	
	@Override
	public long start(Request request, Path output, DownloadConfiguration configuration) throws Exception {
		this.configuration = configuration;
		flagBegin.set(false);
		bytes.set(0L);
		written.set(0L);
		
		int numOfThreads = count;
		Range<Long> rangeOutput = configuration.rangeOutput();
		Range<Long> rangeRequest = configuration.rangeRequest();
		totalBytes = configuration.totalBytes();
		
		if(totalBytes < 0L) {
			totalBytes = Ignore.defaultValue(() -> Web.size(request), -1L);
		}
		
		// If the total size is still unknown, we cannot split the file into chunks,
		// therefore just use a single file downloader with unset ranges.
		if(totalBytes <= 0L) {
			rangeRequest = RANGE_UNSET;
			rangeOutput = RANGE_UNSET;
			numOfThreads = 1; // Use just one downloader
		} else {
			rangeRequest = checkRange(rangeRequest, totalBytes);
			rangeOutput = checkRange(rangeOutput, rangeRequest);
		}
		
		for(int i = 0; i < numOfThreads; ++i) {
			InternalDownloader downloader = createDownloader();
			
			downloader.addEventListener(DownloadEvent.BEGIN,  this::onBegin);
			downloader.addEventListener(DownloadEvent.UPDATE, this::onUpdate);
			downloader.addEventListener(DownloadEvent.END,    this::onEnd);
			downloader.addEventListener(DownloadEvent.ERROR,  this::onError);
			
			downloaders.add(downloader);
		}
		
		long size = rangeRequest.to() - rangeRequest.from();
		
		if(numOfThreads == 1) {
			InternalDownloader downloader = downloaders.get(0);
			DownloadConfiguration downloadConfiguration
				= DownloadConfiguration.ofRanges(rangeOutput, rangeRequest, size);
			long downloadedBytes = downloader.start(request, output, downloadConfiguration);
			long writtenBytes = downloader.writtenBytes();
			maybeSetResponse(downloader.response());
			
			if(downloadedBytes > 0L) {
				bytes.getAndAdd(downloadedBytes);
			}
			
			if(writtenBytes > 0L) {
				written.getAndAdd(writtenBytes);
			}
		} else {
			CounterLock lock = new CounterLock(numOfThreads);
			ExecutorService executor = Threads.Pools.newFixed(numOfThreads);
			DownloadTracker tracker = new DownloadTracker(size);
			long step = (size + numOfThreads - 1L) / numOfThreads;
			
			long reqFrom = rangeRequest.from();
			long reqTo   = reqFrom + step;
			long outFrom = rangeOutput.from();
			long outTo   = outFrom + step;
			
			for(int i = 0; i < numOfThreads; ++i) {
				InternalDownloader downloader = downloaders.get(i);
				downloader.setTracker(tracker);
				
				Range<Long> rangeReq = new Range<>(reqFrom, reqTo);
				Range<Long> rangeOut = new Range<>(outFrom, outTo);
				executor.submit(() -> {
					try {
						DownloadConfiguration downloadConfiguration
							= DownloadConfiguration.ofRanges(rangeOut, rangeReq, size);
						long downloadedBytes = downloader.start(request, output, downloadConfiguration);
						long writtenBytes = downloader.writtenBytes();
						maybeSetResponse(downloader.response());
						
						if(downloadedBytes > 0L) {
							bytes.getAndAdd(downloadedBytes);
						}
						
						if(writtenBytes > 0L) {
							written.getAndAdd(writtenBytes);
						}
					} catch(Exception ex) {
						exception = ex;
						onError(this);
					} finally {
						lock.decrement();
					}
				});
				
				reqFrom  = reqTo;
				reqTo   += step;
				outFrom  = outTo;
				outTo   += step;
				
				// Correct the end if it was exceeded
				if(reqTo > rangeRequest.to()) {
					reqTo = rangeRequest.to();
					
					long lenReq = reqTo - reqFrom;
					long lenOut = outTo - outFrom;
					
					// Correct the lengths if they are not equal
					if(lenOut != lenReq) {
						outTo = outFrom + lenReq;
					}
				}
			}
			
			// Wait for all the downloaders to finish downloading
			lock.await();
			
			// Gracefully shutdown the executor and wait, even though at this point all threads should exited
			executor.shutdown();
			Ignore.call(() -> executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS));
		}
		
		return bytes.get();
	}
	
	@Override
	public void pause() throws Exception {
		doAction(InternalDownloader::pause);
	}
	
	@Override
	public void resume() throws Exception {
		doAction(InternalDownloader::resume);
	}
	
	@Override
	public void stop() throws Exception {
		doAction(InternalDownloader::stop);
	}
	
	@Override
	public void setTracker(DownloadTracker tracker) {
		Ignore.callVoid(() -> doAction((downloader) -> downloader.setTracker(tracker)));
	}
	
	@Override
	public void setResponseStreamFactory(InputStreamFactory factory) {
		Ignore.callVoid(() -> doAction((downloader) -> downloader.setResponseStreamFactory(factory)));
	}
	
	@Override
	public long writtenBytes() {
		return written.get();
	}
	
	@Override
	public Request request() {
		return request;
	}
	
	@Override
	public Path output() {
		return output;
	}
	
	@Override
	public DownloadConfiguration configuration() {
		return configuration;
	}
	
	@Override
	public Response response() {
		return response;
	}
	
	@Override
	public long totalBytes() {
		return totalBytes;
	}
	
	public int count() {
		return count;
	}
	
	@Override
	public boolean isRunning() {
		return checkState(InternalDownloader::isRunning);
	}
	
	@Override
	public boolean isDone() {
		return checkState(InternalDownloader::isDone);
	}
	
	@Override
	public boolean isStarted() {
		return checkState(InternalDownloader::isStarted);
	}
	
	@Override
	public boolean isPaused() {
		return checkState(InternalDownloader::isPaused);
	}
	
	@Override
	public boolean isStopped() {
		return checkState(InternalDownloader::isStopped);
	}
	
	@Override
	public boolean isError() {
		return checkState(InternalDownloader::isError);
	}
	
	@Override
	public <V> void addEventListener(Event<? extends DownloadEvent, V> event, Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	@Override
	public <V> void removeEventListener(Event<? extends DownloadEvent, V> event, Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
	
	@Override
	public <V> void call(Event<? extends DownloadEvent, V> event) {
		Ignore.callVoid(() -> doAction((downloader) -> downloader.call(event)));
	}
	
	@Override
	public <V> void call(Event<? extends DownloadEvent, V> event, V value) {
		Ignore.callVoid(() -> doAction((downloader) -> downloader.call(event, value)));
	}
	
	@Override
	public TrackerManager trackerManager() {
		return trackerManager;
	}
	
	@Override
	public Exception exception() {
		return exception;
	}
}