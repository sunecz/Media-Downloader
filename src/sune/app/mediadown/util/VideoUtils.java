package sune.app.mediadown.util;

import java.nio.file.Path;

import sune.api.process.ReadOnlyProcess;
import sune.app.mediadown.ffmpeg.FFProbe;

public final class VideoUtils {
	
	private static final String COMMAND_DURATION;
	
	static {
		COMMAND_DURATION = "-v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 \"%s\"";
	}
	
	public static final double duration(Path file) {
		try(ReadOnlyProcess process = FFProbe.createSynchronousProcess()) {
			String command = String.format(COMMAND_DURATION, file.getFileName().toString());
			return Double.parseDouble(process.execute(command));
		} catch(Exception ex) {
			// Ignore
		}
		// Failed to get the duration of the file, return a special value
		return Double.NEGATIVE_INFINITY;
	}
	
	public static final double tryGetDuration(Path... files) {
		for(Path file : files) {
			double duration = duration(file);
			// Check whether we succeeded to get the duration
			if(!Double.isInfinite(duration))
				return duration;
				
		}
		// Failed to get the duration of the file, return a special value
		return Double.NEGATIVE_INFINITY;
	}
	
	// Forbid anyone to create an instance of this class
	private VideoUtils() {
	}
}