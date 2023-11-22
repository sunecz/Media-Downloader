package sune.app.mediadown.download;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import sune.app.mediadown.concurrent.VarLoader;

/** @since 00.02.09 */
public interface InputStreamFactory {
	
	InputStream create(InputStream stream) throws Exception;
	
	static class GZIP implements InputStreamFactory {
		
		private static final int DEFAULT_BUFFER_SIZE = 8192;
		private static final VarLoader<GZIP> DEFAULT = VarLoader.of(GZIP::new);
		
		private final int bufferSize;
		
		private GZIP() {
			this.bufferSize = DEFAULT_BUFFER_SIZE;
		}
		
		private GZIP(int bufferSize) {
			this.bufferSize = bufferSize;
		}
		
		public static GZIP ofDefault() {
			return DEFAULT.value();
		}
		
		public static GZIP of(int bufferSize) {
			return bufferSize == DEFAULT_BUFFER_SIZE ? ofDefault() : new GZIP(bufferSize);
		}
		
		@Override
		public InputStream create(InputStream stream) throws Exception {
			return new GZIPInputStream(stream, bufferSize);
		}
	}
}