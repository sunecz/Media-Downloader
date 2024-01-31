package sune.app.mediadown.media;

import java.util.List;

import sune.app.mediadown.conversion.ConversionMedia;
import sune.app.mediadown.gui.table.ResolvedMedia;

/** @since 00.02.09 */
public interface MediaConversionContext {
	
	List<ConversionMedia> inputs();
	ResolvedMedia output();
}