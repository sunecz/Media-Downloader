package sune.app.mediadown.download;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import sune.app.mediadown.Download;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Range;
import sune.app.mediadown.util.SyncObject;
import sune.app.mediadown.util.Web;
import sune.app.mediadown.util.Web.GetRequest;
import sune.app.mediadown.util.Web.StreamResponse;

@Deprecated(forRemoval=true)
public class SingleFileDownloader implements IInternalDownloader {
	
	private final TrackerManager manager;
	private final EventRegistry<DownloadEvent> eventRegistry;
	
	private final SyncObject lockPause = new SyncObject();
	private final SingleFileDownloaderConfiguration configuration;
	
	// ----- Properties
	
	private GetRequest request;
	private Path file;
	
	private String identifier;
	private long total;
	private long bytes;
	
	private Range<Long> rangeRequest;
	private Range<Long> rangeFile;
	
	private DownloadTracker tracker;
	private Download download;
	
	private FileChannel channel;
	private StreamResponse response;
	private MappedByteBuffer mapped;
	private ByteBuffer buffer;
	
	// Used for determing if EOF in URL stream is reached
	private boolean flag_EOF;
	
	// ----- States
	
	private final AtomicBoolean running = new AtomicBoolean();
	private final AtomicBoolean done    = new AtomicBoolean();
	private final AtomicBoolean started = new AtomicBoolean();
	private final AtomicBoolean paused  = new AtomicBoolean();
	private final AtomicBoolean stopped = new AtomicBoolean();
	
	public SingleFileDownloader(TrackerManager manager) {
		this(manager, SingleFileDownloaderConfiguration.getDefault());
	}
	
	public SingleFileDownloader(TrackerManager manager, SingleFileDownloaderConfiguration configuration) {
		this.manager       = manager;
		this.eventRegistry = new EventRegistry<>();
		this.configuration = configuration;
	}
	
	private final void openFile(Path file, long rangeStart, long rangeEnd) throws IOException {
		if((configuration.isAppendMode())) {
			if((configuration.isNoSizeCheck())) {
				// Initialize the append mode
				channel = FileChannel.open(file, CREATE, WRITE, APPEND);
			} else {
				// Since READ + APPEND is not allowed, throw an exception
				throw new IOException("Append mode is available only with noSizeCheck flag");
			}
		} else {
			// Initialize the regular mode
			channel = FileChannel.open(file, CREATE, READ, WRITE);
		}
		if(!configuration.isNoSizeCheck())
			mapped = channel.map(MapMode.READ_WRITE, rangeStart, rangeEnd - rangeStart + 1L);
	}
	
	private final void closeFile() throws Exception {
		if((channel != null)) {
			channel.close();
			NIO.unmap(mapped);
			mapped  = null;
			channel = null;
		}
	}
	
	private final ReadableByteChannel urlChannel(long rangeStart, long rangeEnd) throws Exception {
		response = rangeStart >= 0L && (rangeEnd < 0L || rangeStart <= rangeEnd)
				? Web.requestStream(request.toRangedRequest(identifier, rangeStart, rangeEnd))
				: Web.requestStream(request);
		// If not set, try to get the total size of the file from the headers
		if((total < 0L))
			setTotalSize(Web.size(response.headers));
		if((identifier == null))
			identifier = response.identifier;
		return Channels.newChannel(response.stream);
	}
	
	private final void write(ByteBuffer buffer) throws IOException {
		if((mapped != null)) mapped .put  (buffer);
		else                 channel.write(buffer);
	}
	
	private final void setTotalSize(long size) {
		total = size;
		// Check the ranges of both request and file
		checkRequestRange();
		checkFileRange();
		if((rangeRequest.to() - rangeRequest.from() != rangeFile.to() - rangeFile.from()))
			throw new IllegalArgumentException("Lengths of ranges do not match"
				+ " (r=" + (rangeRequest.to() - rangeRequest.from())
				+ ", f=" + (rangeFile   .to() - rangeFile   .from()) + ")");
		// Create a new download tracker, if needed
		if((tracker == null))
			setTracker(new DownloadTracker(total));
		// Notify the tracker
		tracker.updateTotal(total);
	}
	
	private final void checkRequestRange() {
		long req_rangeStart = rangeRequest.from();
		long req_rangeEnd   = rangeRequest.to();
		if((req_rangeStart < 0L))
			req_rangeStart = 0L;
		if((req_rangeEnd < 0L || req_rangeEnd >= total))
			// -1L because end index inclusive
			req_rangeEnd = total - 1L;
		// Update the request range
		rangeRequest = new Range<>(req_rangeStart, req_rangeEnd);
	}
	
	private final void checkFileRange() {
		long req_rangeLength = rangeRequest.to() - rangeRequest.from();
		long file_rangeStart = rangeFile.from();
		long file_rangeEnd   = rangeFile.to();
		if((file_rangeStart < 0L))
			file_rangeStart = 0L;
		if((file_rangeEnd < 0L || file_rangeEnd - file_rangeStart > req_rangeLength))
			file_rangeEnd = file_rangeStart + req_rangeLength;
		// Update the file range
		rangeFile = new Range<>(file_rangeStart, file_rangeEnd);
	}
	
	private final ByteBuffer buffer() {
		return buffer == null ? (buffer = ByteBuffer.allocate(8192)) : (ByteBuffer) buffer.clear();
	}
	
	private final void internal_download() throws Exception {
		if((total >= 0L)) setTotalSize(total); // updates the ranges and the tracker
		try(ReadableByteChannel input = urlChannel(rangeRequest.from(), rangeRequest.to())) {
			openFile(file, rangeFile.from(), rangeFile.to());
			ByteBuffer buffer = buffer();
			while(true) {
				// Terminate the transfer when not running
				if(!running.get()) {
					input.close();
					// Do not continue
					break;
				}
				int read = input.read(buffer);
				if((read <= 0L)) {
					flag_EOF = true;
					break;
				}
				// Write the buffer to the output
				buffer.flip();
				write(buffer);
				buffer.clear();
				// Update the bytes
				bytes += read;
				// Update the tracker
				tracker.update(read);
			}
		} finally {
			// Also close the stream response
			if((response != null)) response.close();
			// Change the range start (used when download is resumed)
			rangeRequest = rangeRequest.setFrom(rangeRequest.from() + bytes);
			rangeFile    = rangeFile   .setFrom(rangeFile   .from() + bytes);
			// Close the opened file
			closeFile();
			// Check if the file was fully downloaded, setting the done flag
			checkIfFullyDownloaded();
		}
	}
	
	private final void checkIfFullyDownloaded() {
		long rangeLength = rangeRequest.to() - rangeRequest.from();
		if((bytes >= rangeLength || (configuration.isNoSizeCheck() && flag_EOF)))
			// Set the done flag to notify the internal loop
			done.set(true);
	}
	
	private final void internal_start() throws Exception {
		while(!done.get() && !stopped.get()) {
			// Important to update the flag before downloading
			running.set(true);
			internal_download();
			// Important to update the flag after downloading
			running.set(false);
			// Only if the downloader is paused
			if((paused.get())) {
				// Wait for the lock to be notified (a.k.a. the resume action)
				lockPause.await();
			}
		}
	}
	
	private final void internal_stop() {
		identifier = null;
		stopped.set(true);
		running.set(false);
		paused .set(false);
		lockPause.unlock();
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
		if((request == null))
			throw new IllegalArgumentException("Request cannot be null");
		if((file == null))
			throw new IllegalArgumentException("Output file cannot be null");
		if((rangeRequest == null || rangeFile == null))
			throw new IllegalArgumentException("Ranges cannot be null");
		if((rangeRequest.to() > -1L)) { // Do not check if the total size (end index) is not known
			if((rangeRequest.to() - rangeRequest.from() != rangeFile.to() - rangeFile.from()))
				throw new IllegalArgumentException("Lengths of ranges do not match"
					+ " (r=" + (rangeRequest.to() - rangeRequest.from())
					+ ", f=" + (rangeFile   .to() - rangeFile   .from()) + ")");
		}
		this.request      = request;
		this.file         = file;
		this.identifier   = null;
		this.total        = total;
		this.bytes        = 0L;
		this.rangeRequest = rangeRequest;
		this.rangeFile    = rangeFile;
		this.download     = download;
		running.set(true);
		paused .set(false);
		done   .set(false);
		started.set(false);
		stopped.set(false);
		flag_EOF = false; // Reset the flag
		try {
			eventRegistry.call(DownloadEvent.BEGIN, download);
			if((request.url != null)) {
				started.set(true);
				internal_start();
			} else {
				Translation translation = MediaDownloader.translation();
				String errorText = translation.getSingle("errors.download.invalid_arguments");
				eventRegistry.call(DownloadEvent.ERROR, new Pair<>(download, new IOException(errorText)));
			}
		} catch(Exception ex) {
			Translation translation = MediaDownloader.translation();
			String errorText = translation.getSingle("errors.download.cannot_download", "url", request.url.toExternalForm());
			eventRegistry.call(DownloadEvent.ERROR, new Pair<>(download, new IOException(errorText, ex)));
		} finally {
			internal_stop();
			if((done.get())) {
				eventRegistry.call(DownloadEvent.END, download);
			}
		}
		return bytes;
	}
	
	@Override
	public void pause() {
		if(!isStarted() || isPaused())
			return;
		paused .set(true);
		running.set(false);
	}
	
	@Override
	public void resume() {
		if(!isStarted() || !isPaused())
			return;
		paused .set(false);
		running.set(true);
		lockPause.unlock();
	}
	
	@Override
	public void stop() {
		if(!isStarted()) return;
		internal_stop();
	}
	
	@Override
	public long revive() {
		if(!started.get() || running.get() || paused.get())
			return -1L;
		long lenRequest = rangeRequest.to() - rangeRequest.from();
		long lenFile    = rangeFile   .to() - rangeFile   .from();
		if((lenRequest <= 0L || lenFile <= 0L))
			return -1L;
		// Forward the number of downloaded bytes from the start method
		return start(request, file, download, rangeRequest, rangeFile);
	}
	
	@Override
	public void setTracker(DownloadTracker tracker) {
		this.tracker = tracker;
		// Propagate to the tracker manager
		manager.setTracker(tracker);
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
	
	@Override
	public boolean isRunning() {
		return running.get();
	}
	
	@Override
	public boolean isDone() {
		return done.get();
	}
	
	@Override
	public boolean isStarted() {
		return started.get();
	}
	
	@Override
	public boolean isPaused() {
		return paused.get();
	}
	
	@Override
	public boolean isStopped() {
		return stopped.get();
	}
}