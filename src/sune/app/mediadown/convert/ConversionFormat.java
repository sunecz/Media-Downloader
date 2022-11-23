package sune.app.mediadown.convert;

import java.util.Objects;

import sune.app.mediadown.convert.ConversionCommand.Input;
import sune.app.mediadown.convert.ConversionCommand.Output;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.util.Metadata;

/** @since 00.02.08 */
public abstract class ConversionFormat {
	
	protected final MediaFormat format;
	
	protected ConversionFormat(MediaFormat format) {
		this.format = Objects.requireNonNull(format);
	}
	
	public abstract void from(Media media, int index, Input.Builder input, Output.Builder output, Metadata metadata);
	
	public MediaFormat format() {
		return format;
	}
}