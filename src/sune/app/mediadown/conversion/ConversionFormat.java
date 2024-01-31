package sune.app.mediadown.conversion;

import java.util.Objects;

import sune.app.mediadown.conversion.ConversionCommand.Input;
import sune.app.mediadown.conversion.ConversionCommand.Output;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaFormat;

/** @since 00.02.08 */
public abstract class ConversionFormat {
	
	protected final MediaFormat format;
	
	protected ConversionFormat(MediaFormat format) {
		this.format = Objects.requireNonNull(format);
	}
	
	public abstract void from(Media media, int index, Input.Builder input, Output.Builder output);
	
	public MediaFormat format() {
		return format;
	}
}