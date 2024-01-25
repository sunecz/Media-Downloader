package sune.app.mediadown.conversion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import sune.app.mediadown.media.MediaFormat;

/** @since 00.02.09 */
public abstract class AbstractConversionProvider implements ConversionProvider {
	
	protected final Map<String, ConversionFormat> formats = new LinkedHashMap<>();
	protected List<ConversionFormat> allFormats;
	
	protected AbstractConversionProvider() {
	}
	
	protected static final String key(MediaFormat format) {
		return format.name().toLowerCase();
	}
	
	protected static final String key(ConversionFormat format) {
		return key(format.format());
	}
	
	@Override
	public void register(ConversionFormat format) {
		Objects.requireNonNull(format);
		
		synchronized(formats) {
			formats.put(key(format), format);
			allFormats = null; // Invalidate
		}
	}
	
	@Override
	public void unregister(ConversionFormat format) {
		Objects.requireNonNull(format);
		
		synchronized(formats) {
			if(formats.remove(key(format), format)) {
				allFormats = null; // Invalidate
			}
		}
	}
	
	@Override
	public ConversionFormat formatOf(MediaFormat format) {
		if(format == null || format.is(MediaFormat.UNKNOWN)) {
			throw new IllegalArgumentException("Unknown format");
		}
		
		synchronized(formats) {
			return formats.get(key(format));
		}
	}
	
	@Override
	public List<ConversionFormat> formats() {
		if(allFormats == null) {
			synchronized(formats) {
				if(allFormats == null) {
					allFormats = List.copyOf(formats.values());
				}
			}
		}
		
		return allFormats;
	}
}