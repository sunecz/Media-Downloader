package sune.app.mediadown.conversion;

import java.util.List;
import java.util.Objects;

import sune.app.mediadown.conversion.ConversionCommand.Input;
import sune.app.mediadown.conversion.ConversionCommand.Output;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaFormat;

/** @since 00.02.08 */
public abstract class ConversionFormat {
	
	protected final MediaFormat format;
	
	protected ConversionFormat(MediaFormat format) {
		this.format = Objects.requireNonNull(format);
	}
	
	public abstract void from(Media media, int index, Input.Builder input, Output.Builder output);
	/** @since 00.02.09 */
	public boolean isConversionNeeded(List<ConversionMedia> inputs, ResolvedMedia output) {
		return true; // Always convert, by default
	}
	
	public MediaFormat format() {
		return format;
	}
}