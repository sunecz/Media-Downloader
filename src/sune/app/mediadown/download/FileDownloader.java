package sune.app.mediadown.download;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import sune.app.mediadown.Download;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Range;
import sune.app.mediadown.util.SyncObject;
import sune.app.mediadown.util.Web;
import sune.app.mediadown.util.Web.GetRequest;
import sune.app.mediadown.util.Web.HeadRequest;
import sune.app.mediadown.util.Web.PostRequest;
import sune.app.mediadown.util.Web.Request;
import sune.app.mediadown.util.Web.StreamResponse;

/** @since 00.02.08 */
public class FileDownloader implements InternalDownloader {
	
	private final InternalState state = new InternalState(TaskStates.INITIAL);
	private final EventRegistry<DownloadEvent> eventRegistry = new EventRegistry<>();
	private final SyncObject lockPause = new SyncObject();
	
	private Request request;
	private Path output;
	private DownloadConfiguration configuration;
	
	// ----- Not needed properties, just here for compatibility
	@Deprecated(forRemoval=true)
	private Download download;
	@Deprecated(forRemoval=true)
	private final TrackerManager trackerManager;
	// -----
	
	private DownloadTracker tracker;
	
	private String identifier;
	private AtomicLong bytes = new AtomicLong();
	
	private long totalBytes;
	private Range<Long> rangeRequest;
	private Range<Long> rangeOutput;
	
	private FileChannel channel;
	private StreamResponse response;
	private ByteBuffer buffer;
	
	public FileDownloader(TrackerManager trackerManager) {
		this.trackerManager = Objects.requireNonNull(trackerManager);
	}
	
	private static final boolean isValidRange(Range<Long> range) {
		return range.from() >= 0L && range.to() >= 0L;
	}
	
	private static final Range<Long> checkRange(Range<Long> range, long limit) {
		long from = range.from(), to = range.to();
		
		if(from == -1L && to == -1L) {
			return range;
		}
		
		if(from < 0L && to < 0L) {
			return new Range<>(-1L, -1L);
		}
		
		if(from < 0L) {
			return new Range<>(-1L, Math.min(to, limit));
		} else if(to < 0L) {
			return new Range<>(from, -1L);
		} else {
			long min = Math.min(from, to);
			long max = Math.max(from, to);
			return new Range<>(min, Math.min(max, min + limit));
		}
	}
	
	private static final Range<Long> startRange(Range<Long> range, long start) {
		return isValidRange(range) ? range.setFrom(range.from() + start) : range;
	}
	
	private static final long rangeLength(Range<Long> range) {
		return range.from() < 0L || range.to() < 0L ? -1L : range.to() - range.from();
	}
	
	private static final int bufferSize(Path path) {
		try { return (int) (16 * Files.getFileStore(path).getBlockSize()); } catch(Exception ex) {} return 8192;
	}
	
	// Utility method. Will be removed when the Web API is updated and it supports this functionality.
	private static final Request toRangedRequest(Request request, String identifier, Range<Long> range) {
		if(request instanceof GetRequest) {
			return new GetRequest(request.url, request.userAgent, request.cookies, request.headers,
			                      request.followRedirects, identifier, range.from(), range.to(), request.timeout);
		} else if(request instanceof PostRequest) {
			return new PostRequest(request.url, request.userAgent, request.params, request.cookies, request.headers,
			                       request.followRedirects, identifier, range.from(), range.to(), request.timeout);
		} else if(request instanceof HeadRequest) {
			return new HeadRequest(request.url, request.userAgent, request.cookies, request.headers,
			                       request.followRedirects, request.timeout);
		}
		
		throw new IllegalStateException("Invalid request type");
	}
	
	private final void openFile(Path output, Range<Long> range) throws IOException {
		channel = FileChannel.open(output, CREATE, WRITE);
		channel.position(Math.max(0L, range.from()));
	}
	
	private final void closeFile() throws IOException {
		if(channel == null) {
			return;
		}
		
		channel.close();
		channel = null;
	}
	
	private final void write(ByteBuffer buffer) throws IOException {
		while(buffer.hasRemaining()) channel.write(buffer);
	}
	
	private final ReadableByteChannel doRequest(Range<Long> range) throws Exception {
		Request req = request;
		
		// Make sure the request range is correct, if required
		if(isValidRange(range)) {
			req = toRangedRequest(request, identifier, range);
		}
		
		// Prepare the response
		response = Web.requestStream(req);
		
		// Update the total size of the file, if acquired
		long size = totalBytes <= 0L ? Web.size(response.headers) : totalBytes;
		if(size > 0L) {
			rangeOutput = checkRange(rangeOutput, size);
			tracker.updateTotal(size);
		} else if(isValidRange(rangeOutput)) {
			rangeOutput = new Range<>(rangeOutput.from(), -1L);
			tracker.updateTotal(-1L);
		} else {
			rangeOutput = new Range<>(-1L, -1L);
			tracker.updateTotal(-1L);
		}
		
		// Also update the identifier, if needed
		if(identifier == null) {
			identifier = response.identifier;
		}
		
		// Finally, convert the response to readable channel
		return Channels.newChannel(response.stream);
	}
	
	private final ByteBuffer buffer() {
		return buffer == null ? (buffer = ByteBuffer.allocate(bufferSize(output))) : buffer.clear();
	}
	
	private final void update(long readBytes) {
		bytes.getAndAdd(readBytes);
		tracker.update(readBytes);
		eventRegistry.call(DownloadEvent.UPDATE, new Pair<>(download, trackerManager));
	}
	
	private final boolean doDownload() throws Exception {
		boolean reachedEOF = false;
		rangeOutput  = checkRange(rangeOutput, -1L);
		rangeRequest = checkRange(rangeRequest, rangeLength(rangeOutput));
		
		if(tracker == null) {
			tracker = new DownloadTracker(-1L);
			trackerManager.setTracker(tracker);
		}
		
		try(ReadableByteChannel input = doRequest(rangeRequest)) {
			openFile(output, rangeOutput);
			ByteBuffer buffer = buffer();
			
			for(int read; isRunning()
					// Read the bytes to the buffer
					&& ((read = input.read(buffer)) >= 0L 
							// If read < 0L bytes, set the EOF flag and exit the loop
							|| !(reachedEOF = true));) {
				// Write the buffer to the output
				buffer.flip();
				write(buffer);
				buffer.clear();
				
				update(read);
			}
		} finally {
			if(response != null) {
				response.close();
			}
			
			// Update the ranges, used when the download is resumed
			long downloadedBytes = bytes.get();
			rangeRequest = startRange(rangeRequest, downloadedBytes);
			rangeOutput  = startRange(rangeRequest, downloadedBytes);
			
			closeFile();
		}
		
		return reachedEOF;
	}
	
	private final void checkIfDone(boolean reachedEOF) {
		if((isValidRange(rangeRequest) && bytes.get() >= rangeLength(rangeRequest))
				|| reachedEOF) {
			state.set(TaskStates.DONE);
		}
	}
	
	private final void doStart() throws Exception {
		while(!isDone() && !isStopped()) {
			state.set(TaskStates.RUNNING);
			checkIfDone(doDownload());
			state.unset(TaskStates.RUNNING);
			
			if(isPaused()) {
				lockPause.await();
			}
		}
	}
	
	private final void doStop() {
		identifier = null;
		state.set(TaskStates.STOPPED);
		state.unset(TaskStates.RUNNING);
	}
	
	@Override
	public long start(Download download, Request request, Path output, DownloadConfiguration configuration) throws Exception {
		this.download      = Objects.requireNonNull(download);
		this.request       = Objects.requireNonNull(request);
		this.output        = Objects.requireNonNull(output);
		this.configuration = Objects.requireNonNull(configuration);
		identifier         = null;
		totalBytes         = configuration.totalBytes();
		rangeRequest       = configuration.rangeRequest();
		rangeOutput        = configuration.rangeOutput();
		buffer             = null;
		bytes.set(0L);
		state.clear(TaskStates.STARTED);
		
		try {
			eventRegistry.call(DownloadEvent.BEGIN);
			doStart();
		} catch(Exception ex) {
			state.set(TaskStates.ERROR);
			eventRegistry.call(DownloadEvent.ERROR, new Pair<>(download, ex));
			throw ex; // Propagate the error
		} finally {
			doStop();
			
			if(isDone()) {
				eventRegistry.call(DownloadEvent.END);
			}
		}
		
		return bytes.get();
	}
	
	@Override
	public void pause() {
		if(!isStarted() || isPaused()) {
			return;
		}
		
		state.set(TaskStates.PAUSED);
		state.unset(TaskStates.RUNNING);
	}
	
	@Override
	public void resume() {
		if(!isStarted() || !isPaused()) {
			return;
		}
		
		state.unset(TaskStates.PAUSED);
		state.set(TaskStates.RUNNING);
		lockPause.unlock();
	}
	
	@Override
	public void stop() {
		if(!isStarted()) {
			return;
		}
		
		doStop();
	}
	
	@Override
	public void setTracker(DownloadTracker tracker) {
		this.tracker = tracker;
		trackerManager.setTracker(tracker);
	}
	
	@Override
	public <E> void addEventListener(EventType<DownloadEvent, E> type, Listener<E> listener) {
		eventRegistry.add(type, listener);
	}
	
	@Override
	public <E> void removeEventListener(EventType<DownloadEvent, E> type, Listener<E> listener) {
		eventRegistry.remove(type, listener);
	}
	
	@Override
	public <E> void call(EventType<DownloadEvent, E> type) {
		eventRegistry.call(type);
	}
	
	@Override
	public <E> void call(EventType<DownloadEvent, E> type, E value) {
		eventRegistry.call(type, value);
	}
	
	public EventRegistry<DownloadEvent> getEventRegistry() {
		return eventRegistry;
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
	public boolean isRunning() {
		return state.is(TaskStates.RUNNING);
	}
	
	@Override
	public boolean isDone() {
		return state.is(TaskStates.DONE);
	}
	
	@Override
	public boolean isStarted() {
		return state.is(TaskStates.STARTED);
	}
	
	@Override
	public boolean isPaused() {
		return state.is(TaskStates.PAUSED);
	}
	
	@Override
	public boolean isStopped() {
		return state.is(TaskStates.STOPPED);
	}
	
	@Override
	public boolean isError() {
		return state.is(TaskStates.ERROR);
	}
}