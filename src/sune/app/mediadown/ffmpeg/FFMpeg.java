package sune.app.mediadown.ffmpeg;

import java.nio.file.Path;
import java.util.function.Consumer;

import sune.api.process.Processes;
import sune.api.process.ReadOnlyProcess;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;

public final class FFMpeg {
	
	private static Path file_ffmpeg;
	private static final Path ensureFFMpeg() {
		if((file_ffmpeg == null)) {
			file_ffmpeg = NIO.localPath("resources/binary", OSUtils.getExecutableName("ffmpeg"));
			if(!NIO.isRegularFile(file_ffmpeg))
				throw new IllegalStateException("ffmpeg was not found at: " + file_ffmpeg.toAbsolutePath().toString());
		}
		return file_ffmpeg;
	}
	
	public static final Path ffmpeg() {
		return ensureFFMpeg();
	}
	
	public static final ReadOnlyProcess createSynchronousProcess() {
		return Processes.createSynchronous(ffmpeg());
	}
	
	public static final ReadOnlyProcess createAsynchronousProcess(Consumer<String> listener) {
		return Processes.createAsynchronous(ffmpeg(), listener);
	}
	
	// Forbid anyone to create an instance of this class
	private FFMpeg() {
	}
}