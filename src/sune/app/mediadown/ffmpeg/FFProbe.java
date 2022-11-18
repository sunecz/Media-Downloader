package sune.app.mediadown.ffmpeg;

import java.nio.file.Path;
import java.util.function.Consumer;

import sune.api.process.Processes;
import sune.api.process.ReadOnlyProcess;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;

/** @since 00.02.08 */
public final class FFprobe {
	
	private static Path path;
	
	// Forbid anyone to create an instance of this class
	private FFprobe() {
	}
	
	private static final Path ensureBinary() {
		if(path == null) {
			path = NIO.localPath("resources/binary", OSUtils.getExecutableName("ffprobe"));
			
			if(!NIO.isRegularFile(path)) {
				throw new IllegalStateException("FFprobe was not found at " + path.toAbsolutePath().toString());
			}
		}
		
		return path;
	}
	
	public static final Path path() {
		return ensureBinary();
	}
	
	public static final ReadOnlyProcess createSynchronousProcess() {
		return Processes.createSynchronous(path());
	}
	
	public static final ReadOnlyProcess createAsynchronousProcess(Consumer<String> listener) {
		return Processes.createAsynchronous(path(), listener);
	}
}