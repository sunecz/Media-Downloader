package sune.app.mediadown.ffmpeg;

import java.nio.file.Path;
import java.util.function.Consumer;

import sune.api.process.Processes;
import sune.api.process.ReadOnlyProcess;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;

public final class FFProbe {
	
	private static Path file_ffprobe;
	private static final Path ensureFFProbe() {
		if((file_ffprobe == null)) {
			file_ffprobe = NIO.localPath("resources/binary", OSUtils.getExecutableName("ffprobe"));
			if(!NIO.isRegularFile(file_ffprobe))
				throw new IllegalStateException("ffprobe was not found at: " + file_ffprobe.toAbsolutePath().toString());
		}
		return file_ffprobe;
	}
	
	public static final Path ffprobe() {
		return ensureFFProbe();
	}
	
	public static final ReadOnlyProcess createSynchronousProcess() {
		return Processes.createSynchronous(ffprobe());
	}
	
	public static final ReadOnlyProcess createAsynchronousProcess(Consumer<String> listener) {
		return Processes.createAsynchronous(ffprobe(), listener);
	}
	
	// Forbid anyone to create an instance of this class
	private FFProbe() {
	}
}