package sune.app.mediadown.conversion;

import java.util.List;

import sune.app.mediadown.entity.Converter;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.media.MediaFormat;

/** @since 00.02.09 */
public interface ConversionProvider {
	
	void register(ConversionFormat format);
	void unregister(ConversionFormat format);
	
	Converter createConverter(TrackerManager trackerManager);
	ConversionCommand createCommand(List<ConversionMedia> inputs, ResolvedMedia output);
	ConversionFormat formatOf(MediaFormat format);
	
	List<ConversionFormat> formats();
	String name();
}