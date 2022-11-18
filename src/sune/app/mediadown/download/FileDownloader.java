package sune.app.mediadown.download;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import sune.app.mediadown.InternalState;
import sune.app.mediadown.TaskStates;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventRegistry;
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
	
	/* Implementation note:
	 * All ranges passed to this class as arguments must be exclusive,
	 * they will be treated as such and will be converted to inclusive
	 * versions to be passed to the Web API.
	 * 
	 * Range(s, e)
	 *     - s = start of range (inclusive)
	 *     - e = end   of range (exclusive)
	 *     - suppose s < e
	 *     - cases:
	 *         - if s <= -1 and e <= -1, the the range is not set
	 *             - all bytes are requested
	 *         - if s == -1, then the range starts at some relative offset
	 *             - bytes are requested from the relative offset
	 *         - if s > -1, then the range starts at offset = s
	 *             - bytes are requested from offset = s
	 *         - if e == 0, then the range represents the zero range
	 *             - value of s is ignored and no bytes are requested
	 *         - if e > 0, then the range ends at offset = e - 1
	 *             - bytes are requested to and including offset = e - 1
	 *         - if e > 0 and s >= 0, then the range is a normal range
	 *             - bytes are requested from offset = s to and including
	 *               offset = e -1
	 */
	
	private static final Range<Long> RANGE_UNSET = new Range<>(-1L, -1L);
	private static final int DEFAULT_BUFFER_SIZE = 8192;
	private static final int FILE_STORE_BLOCKS_COUNT = 16;
	
	private final InternalState state = new InternalState(TaskStates.INITIAL);
	private final EventRegistry<DownloadEvent> eventRegistry = new EventRegistry<>();
	private final SyncObject lockPause = new SyncObject();
	private final TrackerManager trackerManager;
	
	private Request request;
	private Path output;
	private DownloadConfiguration configuration;
	
	private DownloadTracker tracker;
	private InputStreamChannelFactory responseChannelFactory;
	
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
	
	private static final Range<Long> newRange(long from, long to) {
		return from < 0L && to < 0L ? RANGE_UNSET : new Range<>(from, to);
	}
	
	private static final boolean isValidRange(Range<Long> range) {
		return range.from() >= 0L && range.to() >= 0L;
	}
	
	private static final Range<Long> checkRange(Range<Long> range, long limit) {
		long from = range.from(), to = range.to();
		
		if(from < 0L && to < 0L) {
			return RANGE_UNSET;
		}
		
		if(from < 0L) {
			return newRange(-1L, Math.min(to, limit));
		} else if(to < 0L) {
			return newRange(from, -1L);
		} else {
			long min = Math.min(from, to);
			long max = Math.max(from, to);
			return newRange(min, limit < 0L ? max : Math.min(max, min + limit));
		}
	}
	
	private static final Range<Long> offsetRange(Range<Long> range, long start, long end) {
		return range.setFrom(range.from() + start).setTo(end);
	}
	
	private static final long rangeLength(Range<Long> range) {
		return range.from() < 0L || range.to() < 0L ? -1L : range.to() - range.from();
	}
	
	private static final int bufferSize(Path path) {
		try {
			return (int) (FILE_STORE_BLOCKS_COUNT * Files.getFileStore(path).getBlockSize());
		} catch(IOException ex) {
			// Ignore
		}
		
		return DEFAULT_BUFFER_SIZE;
	}
	
	// Utility method. Will be removed when the Web API is updated and it supports this functionality.
	private static final Request toRangedRequest(Request request, String identifier, Range<Long> range) {
		if(request instanceof HeadRequest) {
			return request;
		}
		
		if(request instanceof GetRequest) {
			return new GetRequest(request.url, request.userAgent, request.cookies, request.headers,
				request.followRedirects, identifier, range.from(), range.to() - 1L, request.timeout);
		} else if(request instanceof PostRequest) {
			return new PostRequest(request.url, request.userAgent, request.params, request.cookies, request.headers,
				request.followRedirects, identifier, range.from(), range.to() - 1L, request.timeout);
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
		long size = totalBytes;
		
		// Make sure the request range is correct, if required
		if(isValidRange(range)) {
			req = toRangedRequest(request, identifier, range);
		}
		
		// Prepare the response
		response = Web.requestStream(req);
		
		// Try to obtain the total size, if not set
		if(size <= 0L) {
			size = totalBytes = Web.size(response.headers);
		}
		
		// Update the total size of the file, if acquired
		if(size > 0L) {
			rangeOutput = checkRange(rangeOutput, size);
			tracker.updateTotal(size);
		} else if(isValidRange(rangeOutput)) {
			rangeOutput = newRange(rangeOutput.from(), -1L);
			tracker.updateTotal(-1L);
		} else {
			rangeOutput = RANGE_UNSET;
			tracker.updateTotal(-1L);
		}
		
		// Also update the identifier, if needed
		if(identifier == null) {
			identifier = response.identifier;
		}
		
		// Finally, convert the response to readable channel
		ReadableByteChannel channel = null;
		InternalInputStream iis = null;
		InputStream stream = response.stream;
		
		if(responseChannelFactory != null) {
			iis = new InternalInputStream(stream);
			channel = responseChannelFactory.create(iis);
		}
		
		// Default case, but the factory may also return null
		if(channel == null) {
			iis = null;
			channel = Channels.newChannel(stream);
		}
		
		return InternalChannel.maybeWrap(channel, iis);
	}
	
	private final ByteBuffer buffer() {
		return buffer == null ? (buffer = ByteBuffer.allocate(bufferSize(output))) : buffer.clear();
	}
	
	private final void update(long readBytes) {
		bytes.getAndAdd(readBytes);
		tracker.update(readBytes);
		eventRegistry.call(DownloadEvent.UPDATE, new Pair<>(this, trackerManager));
	}
	
	private final boolean doDownload() throws Exception {
		boolean reachedEOF = false;
		long prevBytes = bytes.get();
		
		rangeOutput = checkRange(rangeOutput, -1L);
		rangeRequest = checkRange(rangeRequest, rangeLength(rangeOutput));
		
		if(rangeRequest.to() == 0L) {
			// Given Range(s, e), does not make logical sense to
			// download anything, because:
			//     if s <= -1, then starts from some relative offset,
			//     if s >=  0, then starts from offset = s,
			//     if e <= -1, then ends at EOF,
			//     if e >   0, then ends at offset = e - 1,
			// but if e ==  0, then even the byte at offset = 0
			//         is not included, therefore nothing should
			//         be downloaded, regardless of the value of s.
			//         It is debatable whether to allow such
			//         ranges where e <= s, but we ignore it here.
			return true;
		}
		
		if(tracker == null) {
			tracker = new DownloadTracker();
			trackerManager.tracker(tracker);
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
			long downloadedBytes = bytes.get() - prevBytes;
			rangeRequest = offsetRange(rangeRequest, downloadedBytes, totalBytes);
			rangeOutput = offsetRange(rangeOutput, downloadedBytes, -1L);
			
			closeFile();
		}
		
		return reachedEOF;
	}
	
	private final void checkIfDone(boolean reachedEOF) {
		Range<Long> rangeRequest = configuration.rangeRequest();
		
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
		if(isStopped() || isDone()) {
			return;
		}
		
		state.unset(TaskStates.RUNNING);
		state.unset(TaskStates.PAUSED);
		identifier = null;
		
		if(!isDone()) {
			state.set(TaskStates.STOPPED);
		}
	}
	
	@Override
	public long start(Request request, Path output, DownloadConfiguration configuration) throws Exception {
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
			eventRegistry.call(DownloadEvent.BEGIN, this);
			doStart();
		} catch(Exception ex) {
			state.set(TaskStates.ERROR);
			eventRegistry.call(DownloadEvent.ERROR, new Pair<>(this, ex));
			throw ex; // Propagate the error
		} finally {
			doStop();
			
			if(isDone()) {
				eventRegistry.call(DownloadEvent.END, this);
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
		
		eventRegistry.call(DownloadEvent.PAUSE, this);
	}
	
	@Override
	public void resume() {
		if(!isStarted() || !isPaused()) {
			return;
		}
		
		state.unset(TaskStates.PAUSED);
		state.set(TaskStates.RUNNING);
		lockPause.unlock();
		
		eventRegistry.call(DownloadEvent.RESUME, this);
	}
	
	@Override
	public void stop() {
		if(!isStarted() || !isStopped() || isDone()) {
			return;
		}
		
		doStop();
	}
	
	@Override
	public void setTracker(DownloadTracker tracker) {
		this.tracker = tracker;
		trackerManager.tracker(tracker);
	}
	
	@Override
	public void setResponseChannelFactory(InputStreamChannelFactory factory) {
		this.responseChannelFactory = factory;
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
		eventRegistry.call(event);
	}
	
	@Override
	public <V> void call(Event<? extends DownloadEvent, V> event, V value) {
		eventRegistry.call(event, value);
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
	
	private static final class InternalInputStream extends InputStream {
		
		private final InputStream stream;
		private int lastBytesCount;
		
		public InternalInputStream(InputStream stream) {
			this.stream = Objects.requireNonNull(stream);
		}
		
		@Override
		public int read() throws IOException {
			int b = stream.read();
			lastBytesCount = 1;
			return b;
		}
		
		@Override
		public int read(byte[] b) throws IOException {
			return lastBytesCount = stream.read(b);
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return lastBytesCount = stream.read(b, off, len);
		}
		
		public void resetLastBytesCount() {
			lastBytesCount = 0;
		}
		
		public int lastBytesCount() {
			return lastBytesCount;
		}
	}
	
	private static final class InternalChannel implements ReadableByteChannel {
		
		private final ReadableByteChannel channel;
		private final InternalInputStream stream;
		
		private InternalChannel(ReadableByteChannel channel, InternalInputStream stream) {
			this.channel = Objects.requireNonNull(channel);
			this.stream = Objects.requireNonNull(stream);
		}
		
		public static final ReadableByteChannel maybeWrap(ReadableByteChannel channel, InternalInputStream stream) {
			return stream != null ? new InternalChannel(channel, stream) : channel;
		}
		
		@Override public boolean isOpen() { return channel.isOpen(); }
		@Override public void close() throws IOException { channel.close(); }
		
		@Override
		public int read(ByteBuffer dst) throws IOException {
			stream.resetLastBytesCount();
			
			if(channel.read(dst) < 0) {
				return -1; // Forward EOS
			}
			
			return stream.lastBytesCount();
		}
	}
}