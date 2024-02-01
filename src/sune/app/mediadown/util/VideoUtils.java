package sune.app.mediadown.util;

import java.nio.file.Path;
import java.util.List;

import sune.api.process.ReadOnlyProcess;
import sune.app.mediadown.ffmpeg.FFprobe;
import sune.app.mediadown.media.MediaConstants;

public final class VideoUtils {
	
	private static final String COMMAND_DURATION = ""
		+ " -v error"
		+ " -show_entries format=duration"
		+ " -of default=noprint_wrappers=1:nokey=1"
		+ " \"%{path}s\"";
	
	// Forbid anyone to create an instance of this class
	private VideoUtils() {
	}
	
	public static final double duration(Path file) {
		try(ReadOnlyProcess process = FFprobe.createSynchronousProcess()) {
			String command = Utils.format(
				COMMAND_DURATION,
				"path", file.toAbsolutePath().toString()
			);
			
			return Double.parseDouble(process.execute(command));
		} catch(Exception ex) {
			// Ignore
		}
		
		return MediaConstants.UNKNOWN_DURATION;
	}
	
	/** @since 00.02.09 */
	public static final double tryGetDuration(List<Path> files) {
		for(Path file : files) {
			double duration = duration(file);
			
			if(duration >= 0.0) {
				return duration;
			}
		}
		
		return MediaConstants.UNKNOWN_DURATION;
	}
	
	public static final double tryGetDuration(Path... files) {
		return tryGetDuration(List.of(files));
	}
}