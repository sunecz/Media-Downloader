package sune.app.mediadown.download;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/** @since 00.02.09 */
public final class DownloadCommon {
	
	private static final int DEFAULT_BUFFER_SIZE = 8192;
	private static final int DEFAULT_FILE_STORE_BLOCKS_COUNT = 16;
	
	// Forbid anyone to create an instance of this class
	private DownloadCommon() {
	}
	
	public static final int bufferSize(Path path) {
		return bufferSize(path, DEFAULT_FILE_STORE_BLOCKS_COUNT);
	}
	
	public static final int bufferSize(Path path, int numOfBlocks) {
		if(path == null || numOfBlocks <= 0) {
			return DEFAULT_BUFFER_SIZE;
		}
		
		try {
			return (int) (numOfBlocks * Files.getFileStore(path).getBlockSize());
		} catch(IOException ex) {
			// Ignore
		}
		
		return DEFAULT_BUFFER_SIZE;
	}
	
	public static final ByteBuffer newDirectBuffer(Path path) {
		return newDirectBuffer(path, DEFAULT_FILE_STORE_BLOCKS_COUNT);
	}
	
	public static final ByteBuffer newDirectBuffer(Path path, int numOfBlocks) {
		return ByteBuffer
					.allocateDirect(bufferSize(path, numOfBlocks))
					.order(ByteOrder.nativeOrder());
	}
}