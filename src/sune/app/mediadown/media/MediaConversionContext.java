package sune.app.mediadown.media;

import java.util.List;

import sune.app.mediadown.conversion.ConversionMedia;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.util.Metadata;

/** @since 00.02.09 */
public interface MediaConversionContext {
	
	ResolvedMedia output();
	List<ConversionMedia> inputs();
	Metadata metadata();
}