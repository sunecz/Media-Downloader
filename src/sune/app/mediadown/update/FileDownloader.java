package sune.app.mediadown.update;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class FileDownloader {
	
	// interface used as a listener for DownloadByteChannel
	private static interface DownloadChannelListener {
		
		void update(long current, long total);
	}
	
	// ReadableByteChannel used for downloading
	private static final class DownloadByteChannel implements ReadableByteChannel {
		
		// the underlying channel
		final ReadableByteChannel channel;
		// download information fields
		final long total;
		long current;
		// listener
		final DownloadChannelListener listener;
		
		public DownloadByteChannel(ReadableByteChannel channel, long total, DownloadChannelListener listener) {
			this.channel  = channel;
			this.total    = total;
			this.current  = 0L;
			this.listener = listener;
		}
		
		@Override
		public boolean isOpen() {
			return channel.isOpen();
		}
		
		@Override
		public void close() throws IOException {
			channel.close();
		}
		
		@Override
		public int read(ByteBuffer dst) throws IOException {
			int len  = channel.read(dst);
			current += len;
			// notify the listener, if needed
			if((listener != null))
				listener.update(current, total);
			return len;
		}
	}
	
	// forbid anyone to create an instance of this class
	private FileDownloader() {
	}
	
	private static final DownloadChannelListener channelListener(String url, Path file, FileDownloadListener listener) {
		return listener != null ? new DownloadChannelListener() {
			
			private final String theURL  = url;
			private final Path   theFile = file;
			
			@Override
			public void update(long current, long total) {
				// call the actual download listener's method
				listener.update(theURL, theFile, current, total);
			}
		} : null;
	}
	
	private static final DownloadByteChannel downloadChannel(String url, Path file, InputStream stream, long size,
	                                                         FileDownloadListener listener) {
		return new DownloadByteChannel(Channels.newChannel(stream), size,
		                               channelListener(url, file, listener));
	}
	
	private static final FileChannel fileChannel(Path file) throws IOException {
		return FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
	}
	
	private static final long urlSize(URLConnection connection) {
		// get the header from the connection headers
		String length = connection.getHeaderField("Content-Length");
		// return the long converted header field
		return length != null && !length.isEmpty()
					? Long.parseLong(length)
					// if the header is not present, return the -1 special value
					: -1L;
	}
	
	public static final boolean download(String url, Path file, FileDownloadListener listener) {
		// notify the listener, if needed
		if((listener != null))
			listener.begin(url, file);
		try {
			URLConnection con = new URL(url).openConnection();
			long          len = urlSize(con);
			// if the file already exists, remove it
			Files.deleteIfExists(file);
			// create all needed directories
			Files.createDirectories(file.getParent());
			// transfer the byte from the URL to the local file
			try(ReadableByteChannel input  = downloadChannel(url, file, con.getInputStream(), len, listener);
				FileChannel         output = fileChannel(file)) {
				// efficiently transfer all the bytes
				output.transferFrom(input, 0L, Long.MAX_VALUE);
			}
			// notify the listener, if needed
			if((listener != null))
				listener.end(url, file);
			return true;
		} catch(IOException ex) {
			// notify the listener, if needed
			if((listener != null))
				listener.error(ex);
		}
		return false;
	}
}