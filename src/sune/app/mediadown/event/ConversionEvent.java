package sune.app.mediadown.event;

import sune.app.mediadown.entity.Converter;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;

/** @since 00.01.26 */
public final class ConversionEvent implements EventType {
	
	public static final Event<ConversionEvent, Converter>                       BEGIN  = new Event<>();
	public static final Event<ConversionEvent, Pair<Converter, TrackerManager>> UPDATE = new Event<>();
	public static final Event<ConversionEvent, Converter>                       END    = new Event<>();
	public static final Event<ConversionEvent, Pair<Converter, Exception>>      ERROR  = new Event<>();
	public static final Event<ConversionEvent, Converter>                       PAUSE  = new Event<>();
	public static final Event<ConversionEvent, Converter>                       RESUME = new Event<>();
	
	private static Event<ConversionEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private ConversionEvent() {
	}
	
	public static final Event<ConversionEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(BEGIN, UPDATE, END, ERROR, PAUSE, RESUME);
		}
		
		return values;
	}
}