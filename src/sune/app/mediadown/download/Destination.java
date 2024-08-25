package sune.app.mediadown.download;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;

public interface Destination extends Closeable {
	
	FileChannel channel() throws IOException;
	Path path();
	
	static class OfFileChannel implements Destination {
		
		private final FileChannel channel;
		private Path path;
		
		public OfFileChannel(FileChannel channel) {
			this(channel, null);
		}
		
		public OfFileChannel(FileChannel channel, Path path) {
			this.channel = Objects.requireNonNull(channel);
			this.path = path; // May be null
		}
		
		@Override
		public FileChannel channel() throws IOException {
			return channel;
		}
		
		@Override
		public Path path() {
			return path;
		}
		
		@Override
		public void close() throws IOException {
			// Do nothing, since the channel is from an outer context
		}
		
		@Override
		public int hashCode() {
			return path != null ? path.hashCode() : channel.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj) return true;
			
			if(obj instanceof OfFileChannel) {
				OfFileChannel other = (OfFileChannel) obj;
				
				if(path != null && other.path != null) {
					return Objects.equals(path, other.path);
				}
				
				return Objects.equals(channel, other.channel);
			} else if(obj instanceof Destination) {
				Destination other = (Destination) obj;
				return Objects.equals(path, other.path());
			}
			
			return false;
		}
	}
	
	static class OfPath implements Destination {
		
		private final Path path;
		private final OpenOption[] options;
		private FileChannel channel;
		
		public OfPath(Path path) {
			this(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		}
		
		public OfPath(Path path, OpenOption... options) {
			this.path = Objects.requireNonNull(path);
			this.options = Objects.requireNonNull(options);
		}
		
		@Override
		public FileChannel channel() throws IOException {
			FileChannel ch;
			if((ch = channel) == null) {
				ch = FileChannel.open(path, options);
				channel = ch;
			}
			
			return ch;
		}
		
		@Override
		public Path path() {
			return path;
		}
		
		@Override
		public void close() throws IOException {
			FileChannel ch;
			if((ch = channel) != null) {
				ch.close();
			}
		}
		
		public OpenOption[] options() {
			return Arrays.copyOf(options, options.length);
		}
		
		@Override
		public int hashCode() {
			return path.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj) return true;
			
			if(obj instanceof Destination) {
				Destination other = (Destination) obj;
				return Objects.equals(path, other.path());
			}
			
			return false;
		}
	}
}