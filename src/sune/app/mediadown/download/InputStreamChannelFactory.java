package sune.app.mediadown.download;

import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.GZIPInputStream;

/** @since 00.02.08 */
public interface InputStreamChannelFactory {
	
	ReadableByteChannel create(InputStream stream) throws Exception;
	
	static class GZIP implements InputStreamChannelFactory {
		
		private static final int DEFAULT_BUFFER_SIZE = 8192;
		private static GZIP INSTANCE_DEFAULT;
		
		private final int bufferSize;
		
		private GZIP(int bufferSize) {
			this.bufferSize = bufferSize;
		}
		
		public static GZIP ofDefault() {
			return INSTANCE_DEFAULT == null ? (INSTANCE_DEFAULT = new GZIP(DEFAULT_BUFFER_SIZE)) : INSTANCE_DEFAULT;
		}
		
		public static GZIP of(int bufferSize) {
			return bufferSize == DEFAULT_BUFFER_SIZE ? ofDefault() : new GZIP(bufferSize);
		}
		
		@Override
		public ReadableByteChannel create(InputStream stream) throws Exception {
			return Channels.newChannel(new GZIPInputStream(stream, bufferSize));
		}
	}
}