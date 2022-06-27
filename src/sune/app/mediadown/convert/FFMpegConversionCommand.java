package sune.app.mediadown.convert;

import java.nio.file.Path;

import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.util.Utils;

/** @since 00.01.26 */
public final class FFMpegConversionCommand {
	
	// TODO: Add support for multiple inputs and their individual formats
	
	private static final void addCommand(MediaFormat input, MediaFormat output, StringBuilder builder) {
		// Fast path for the same formats
		if(input.is(output)) {
			builder.append(" -c copy");
			return; // Do not continue
		}
		// Fast-path for audio output only
		if(output.mediaType().is(MediaType.AUDIO)) {
			String acodec = null;
			if(output.is(MediaFormat.MP3)) acodec = "mp3";       else
			if(output.is(MediaFormat.WAV)) acodec = "pcm_s16le"; else
			if(output.is(MediaFormat.WMA)) acodec = "wmav2";     else
			if(output.is(MediaFormat.M4A)) acodec = "libfaac";   else
			; /* else: Do nothing */
			if(acodec != null)
				builder.append(String.format(" -acodec %s -ab 248k", acodec));
			return; // Do not continue
		}
		if(input.mediaType().is(MediaType.VIDEO)) {
			// Special case for OGG format, since it causes some trouble
			if(output.is(MediaFormat.OGG)) {
				builder.append(" -c:v libtheora -qscale:v 7");
				builder.append(" -c:a libvorbis -qscale:a 6");
			} else {
				if(input.is(MediaFormat.M3U8)) {
					builder.append(" -c copy -bsf:a aac_adtstoasc");
				} else if(input.is(MediaFormat.DASH)) {
					builder.append(" -c:v copy -c:a aac");
				} else if(input.is(MediaFormat.OGG)) {
					builder.append(" -c:v libx264 -preset fast -crf 22");
					builder.append(" -c:a libmp3lame -qscale:a 2 -ac 2 -ar 44100");
				} else {
					builder.append(" -c copy");
				}
			}
		}
	}
	
	public static final String get(Path fileOutput, Path... filesInput) {
		if(fileOutput == null || filesInput == null || filesInput.length <= 0)
			throw new IllegalArgumentException();
		MediaFormat formatInput  = MediaFormat.fromPath(filesInput[0]);
		MediaFormat formatOutput = MediaFormat.fromPath(fileOutput);
		return get(formatInput, formatOutput, fileOutput, filesInput);
	}
	
	public static final String get(MediaFormat formatInput, MediaFormat formatOutput, Path fileOutput, Path... filesInput) {
		if(formatInput == null || formatOutput == null || fileOutput == null
				|| filesInput == null || filesInput.length <= 0)
			throw new IllegalArgumentException();
		StringBuilder builder = new StringBuilder();
		builder.append("-y " + Utils.repeat("-i \"%s\"", filesInput.length, " "));
		addCommand(formatInput, formatOutput, builder);
		builder.append(" -hide_banner -loglevel warning -stats");
		builder.append(" \"%s\"");
		return String.format(builder.toString(), Utils.merge(filesInput, fileOutput));
	}
	
	// Forbid anyone to create an instance of this class
	private FFMpegConversionCommand() {
	}
}