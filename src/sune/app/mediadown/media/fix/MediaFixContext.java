package sune.app.mediadown.media.fix;

import java.util.List;

import sune.app.mediadown.conversion.ConversionMedia;
import sune.app.mediadown.media.ResolvedMedia;

/** @since 00.02.09 */
public interface MediaFixContext {
	
	List<ConversionMedia> inputs();
	ResolvedMedia output();
}