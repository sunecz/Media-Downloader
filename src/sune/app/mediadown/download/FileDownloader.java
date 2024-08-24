package sune.app.mediadown.download;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

import sune.app.mediadown.InternalState;
import sune.app.mediadown.TaskStates;
import sune.app.mediadown.concurrent.SyncObject;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.exception.RejectedResponseException;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.net.Web.Response;
import sune.app.mediadown.util.Range;
import sune.app.mediadown.util.Utils;

/** @since 00.02.08 */
public class FileDownloader implements InternalDownloader, AutoCloseable {
	
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
	
	protected static final Range<Long> RANGE_UNSET = new Range<>(-1L, -1L);
	protected static final int DEFAULT_BUFFER_SIZE = 8192;
	protected static final int FILE_STORE_BLOCKS_COUNT = 16;
	
	protected final InternalState state = new InternalState(TaskStates.INITIAL);
	protected final EventRegistry<DownloadEvent> eventRegistry = new EventRegistry<>();
	protected final SyncObject lockPause = new SyncObject();
	protected final TrackerManager trackerManager;
	
	protected Request request;
	protected Path output;
	protected DownloadConfiguration configuration;
	
	protected DownloadTracker tracker;
	protected InputStreamFactory responseStreamFactory;
	
	protected String identifier;
	protected final AtomicLong bytes = new AtomicLong();
	/** @since 00.02.09 */
	protected final AtomicLong written = new AtomicLong();
	
	protected long totalBytes;
	protected Range<Long> rangeRequest;
	protected Range<Long> rangeOutput;
	
	protected FileChannel channel;
	protected Response.OfStream response;
	protected ByteBuffer buffer;
	
	protected Exception exception;
	protected Path prevOutput;
	
	public FileDownloader(TrackerManager trackerManager) {
		this.trackerManager = Objects.requireNonNull(trackerManager);
	}
	
	protected static final Range<Long> newRange(long from, long to) {
		return from < 0L && to < 0L ? RANGE_UNSET : new Range<>(from, to);
	}
	
	protected static final boolean isValidRange(Range<Long> range) {
		return range.from() >= 0L && range.to() >= 0L;
	}
	
	protected static final Range<Long> checkRange(Range<Long> range, long limit) {
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
	
	protected static final Range<Long> offsetRange(Range<Long> range, long start, long end) {
		return range.setFrom(range.from() + start).setTo(end);
	}
	
	protected static final long rangeLength(Range<Long> range) {
		return range.from() < 0L || range.to() < 0L ? -1L : range.to() - range.from();
	}
	
	protected static final int bufferSize(Path path) {
		try {
			return (int) (FILE_STORE_BLOCKS_COUNT * Files.getFileStore(path).getBlockSize());
		} catch(IOException ex) {
			// Ignore
		}
		
		return DEFAULT_BUFFER_SIZE;
	}
	
	protected void openFile(Path output, Range<Long> range) throws IOException {
		FileChannel ch = channel;
		
		// Do not open multiple file channels if it is still the same file
		// as in the previous run.
		if(prevOutput == null || !prevOutput.equals(output)) {
			if(ch != null) {
				// Close the previous channel
				ch.close();
			}
			
			channel = ch = FileChannel.open(output, CREATE, WRITE);
			prevOutput = output;
		}
		
		ch.position(Math.max(0L, range.from()));
	}
	
	protected void closeFile() throws IOException {
		FileChannel ch;
		if((ch = channel) == null) {
			return;
		}
		
		ch.close();
		channel = null;
		prevOutput = null;
	}
	
	protected int write(ByteBuffer buffer) throws IOException {
		FileChannel ch = channel;
		int count = 0;
		
		while(buffer.hasRemaining()) {
			count += ch.write(buffer);
		}
		
		return count;
	}
	
	/** @since 00.02.09 */
	protected String[] responseEncodings() {
		return response.headers()
			.firstValue("Content-Encoding")
			.map((s) -> Utils.OfString.split(s, ","))
			.orElse(null);
	}
	
	/** @since 00.02.09 */
	protected InputStream decodeResponseStream(InputStream stream, String[] encodings) throws Exception {
		for(int i = encodings.length - 1; i >= 0; --i) {
			String encoding = encodings[i];
			
			switch(encoding) {
				case "gzip":
					stream = new GZIPInputStream(stream);
					break;
				default:
					throw new IllegalStateException("Unsupported encoding: " + encoding);
			}
		}
		
		return stream;
	}
	
	/** @since 00.02.09 */
	protected boolean shouldModifyResponseStream(InputStream stream) throws Exception {
		return false; // By default do not modify
	}
	
	/** @since 00.02.09 */
	protected InputStream modifyResponseStream(InputStream stream) throws Exception {
		return stream; // By default do not modify
	}
	
	@SuppressWarnings("resource")
	protected ReadableByteChannel doRequest(Range<Long> range) throws Exception {
		Request req = request;
		long size = totalBytes;
		
		// Make sure the request range is correct, if required
		if(isValidRange(range)) {
			req = request.toRanged(range, identifier);
		}
		
		// Prepare the response
		response = Web.requestStream(req);
		
		// Filter the response, if a filter is specified
		Predicate<Response> filter;
		if((filter = configuration.responseFilter()) != null && !filter.test(response)) {
			throw new RejectedResponseException();
		}
		
		// Try to obtain the total size, if not set
		if(size <= 0L) {
			size = totalBytes = Web.size(response);
		}
		
		// Update the total size of the file, if acquired
		if(size > 0L) {
			rangeOutput = checkRange(rangeOutput, size);
			tracker.updateTotal(size);
		} else {
			rangeOutput = newRange(rangeOutput.from(), -1L);
			tracker.updateTotal(-1L);
		}
		
		// Also update the identifier, if needed
		if(identifier == null) {
			identifier = response.identifier();
		}
		
		InputStream stream = response.stream();
		InputStream modifiedStream = null;
		
		if(responseStreamFactory != null) {
			// Always wrap the original stream to correctly calculate progress
			stream = new InternalInputStream(stream);
			modifiedStream = responseStreamFactory.create(stream);
		}
		
		// Default case, but the factory may also return null
		if(modifiedStream == null) {
			String[] encodings;
			
			if((encodings = responseEncodings()) != null) {
				if(!(stream instanceof InternalInputStream)) {
					// Always wrap the original stream to correctly calculate progress
					stream = new InternalInputStream(stream);
				}
				
				modifiedStream = decodeResponseStream(stream, encodings);
			} else {
				modifiedStream = stream;
			}
		}
		
		// Allow modification of the response stream
		if(shouldModifyResponseStream(modifiedStream)) {
			// No decoding or modification took place
			if(stream == modifiedStream
					&& !(stream instanceof InternalInputStream)) {
				// Always wrap the original stream to correctly calculate progress
				stream = new InternalInputStream(stream);
				modifiedStream = stream;
			}
			
			modifiedStream = modifyResponseStream(modifiedStream);
		}
		
		// Final check of the modified stream to avoid null value and to allow it
		// to ignore the response content altogether.
		if(modifiedStream == null) {
			modifiedStream = InputStream.nullInputStream();
		}
		
		// No decoding takes place, return the original stream's channel
		if(stream == modifiedStream) {
			// Unwrap the internal stream, since it is not needed
			if(stream instanceof InternalInputStream) {
				stream = ((InternalInputStream) stream).stream;
			}
			
			return Channels.newChannel(stream);
		}
		
		return InternalChannel.of(stream, modifiedStream);
	}
	
	/** @since 00.02.09 */
	protected ByteBuffer createBuffer() {
		return ByteBuffer.allocateDirect(bufferSize(output)).order(ByteOrder.nativeOrder());
	}
	
	protected ByteBuffer buffer() {
		return buffer == null ? (buffer = createBuffer()) : buffer.clear();
	}
	
	protected void update(long readBytes, long writtenByes) {
		bytes.getAndAdd(readBytes);
		written.getAndAdd(writtenByes);
		tracker.update(readBytes);
		eventRegistry.call(DownloadEvent.UPDATE, this);
	}
	
	protected boolean doDownload() throws Exception {
		boolean reachedEOF = false;
		long prevBytes = bytes.get();
		long prevWritten = written.get();
		
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
			
			for(int read, written; isRunning()
					// Read the bytes to the buffer
					&& ((read = input.read(buffer)) >= 0L 
							// If read < 0L bytes, set the EOF flag and exit the loop
							|| !(reachedEOF = true));) {
				// Write the buffer to the output
				buffer.flip();
				written = write(buffer);
				buffer.clear();
				
				update(read, written);
			}
		} finally {
			if(response != null) {
				response.close();
			}
			
			// Update the ranges, used when the download is resumed
			long downloadedBytes = bytes.get() - prevBytes;
			long writtenBytes = written.get() - prevWritten;
			rangeRequest = offsetRange(rangeRequest, downloadedBytes, totalBytes);
			rangeOutput = offsetRange(rangeOutput, writtenBytes, -1L);
		}
		
		return reachedEOF;
	}
	
	protected void checkIfDone(boolean reachedEOF) {
		Range<Long> rangeRequest = configuration.rangeRequest();
		
		if((isValidRange(rangeRequest) && bytes.get() >= rangeLength(rangeRequest))
				|| reachedEOF) {
			state.set(TaskStates.DONE);
		}
	}
	
	protected void doStart() throws Exception {
		while(!isDone() && !isStopped()) {
			state.set(TaskStates.RUNNING);
			checkIfDone(doDownload());
			state.unset(TaskStates.RUNNING);
			
			if(isPaused()) {
				lockPause.await();
			}
		}
	}
	
	protected void doStop(int stopState) {
		if(isStopped() || isDone()) {
			return;
		}
		
		state.unset(TaskStates.RUNNING);
		state.unset(TaskStates.PAUSED);
		
		state.set(stopState);
		
		identifier = null;
		lockPause.unlock();
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
		written.set(0L);
		state.clear(TaskStates.STARTED);
		
		try {
			eventRegistry.call(DownloadEvent.BEGIN, this);
			doStart();
		} catch(Exception ex) {
			exception = ex;
			state.set(TaskStates.ERROR);
			eventRegistry.call(DownloadEvent.ERROR, this);
			throw ex; // Propagate the error
		} finally {
			doStop(TaskStates.DONE);
			eventRegistry.call(DownloadEvent.END, this);
		}
		
		return bytes.get();
	}
	
	@Override
	public void pause() {
		if(!isStarted() || isPaused() || isStopped() || isDone()) {
			return;
		}
		
		state.set(TaskStates.PAUSED);
		state.unset(TaskStates.RUNNING);
		
		eventRegistry.call(DownloadEvent.PAUSE, this);
	}
	
	@Override
	public void resume() {
		if(!isStarted() || !isPaused() || isStopped() || isDone()) {
			return;
		}
		
		state.unset(TaskStates.PAUSED);
		state.set(TaskStates.RUNNING);
		lockPause.unlock();
		
		eventRegistry.call(DownloadEvent.RESUME, this);
	}
	
	@Override
	public void stop() {
		if(!isStarted() || isStopped() || isDone()) {
			return;
		}
		
		doStop(TaskStates.STOPPED);
	}
	
	@Override
	public void close() throws Exception {
		closeFile();
	}
	
	@Override
	public void setTracker(DownloadTracker tracker) {
		this.tracker = tracker;
		trackerManager.tracker(tracker);
	}
	
	@Override
	public void setResponseStreamFactory(InputStreamFactory factory) {
		this.responseStreamFactory = factory;
	}
	
	@Override
	public long writtenBytes() {
		return written.get();
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
	public Response response() {
		return response;
	}
	
	@Override
	public long totalBytes() {
		return totalBytes;
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
	
	@Override
	public TrackerManager trackerManager() {
		return trackerManager;
	}
	
	@Override
	public Exception exception() {
		return exception;
	}
	
	protected static final class InternalInputStream extends InputStream {
		
		private final InputStream stream;
		private int count;
		private boolean reset = true;
		
		public InternalInputStream(InputStream stream) {
			this.stream = Objects.requireNonNull(stream);
		}
		
		private final void add(int n) {
			count += n;
			reset = false;
		}
		
		@Override
		public int read() throws IOException {
			int b;
			if((b = stream.read()) >= 0) {
				add(1);
			}
			
			return b;
		}
		
		@Override
		public int read(byte[] b) throws IOException {
			int v;
			if((v = stream.read(b)) >= 0) {
				add(v);
			}
			
			return v;
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int v;
			if((v = stream.read(b, off, len)) >= 0) {
				add(v);
			}
			
			return v;
		}
		
		public void reset() {
			count = 0;
			reset = true;
		}
		
		public int count() {
			return reset ? -1 : count;
		}
		
		public int countAndReset() {
			int n = count();
			reset();
			return n;
		}
	}
	
	protected static final class InternalChannel implements ReadableByteChannel {
		
		private final ReadableByteChannel channel;
		private final InternalInputStream stream;
		
		private InternalChannel(ReadableByteChannel channel, InternalInputStream stream) {
			this.channel = Objects.requireNonNull(channel);
			this.stream = Objects.requireNonNull(stream);
		}
		
		public static final InternalChannel of(InputStream original, InputStream wrapped) {
			return new InternalChannel(
				Channels.newChannel(wrapped),
				original instanceof InternalInputStream
					? (InternalInputStream) original
					: new InternalInputStream(original)
			);
		}
		
		@Override public boolean isOpen() { return channel.isOpen(); }
		@Override public void close() throws IOException { channel.close(); }
		
		@Override
		public int read(ByteBuffer dst) throws IOException {
			// Since the stream may be either an inflating or deflating (or, in general,
			// any) stream, the number of bytes read from the channel may not be the same
			// as the number of bytes read from the actual original stream. Therefore,
			// we must take extra care to not return EOS (-1) when there are still data
			// available.
			
			// Delegate the read to the channel and load the actual data to the buffer.
			int read = channel.read(dst);
			// Read the actual number of bytes read from the original stream.
			int count = stream.countAndReset();
			
			// Check whether we actually read from the orignal stream.
			if(count < 0) {
				// Send EOS if and only if both of the channel and the stream return EOS.
				if(read < 0) {
					return -1;
				}
				
				// Since we did not read from the original stream at this point, return
				// zero but not EOS. This may happen when the wrapped stream is an inflating
				// stream.
				return 0;
			}
			
			// We read from the original stream, just return the actual bytes count.
			return count;
		}
	}
}