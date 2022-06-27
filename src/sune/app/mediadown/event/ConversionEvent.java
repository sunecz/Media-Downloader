package sune.app.mediadown.event;

import sune.app.mediadown.convert.Converter;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;

/** @since 00.01.26 */
public final class ConversionEvent implements IEventType {
	
	public static final EventType<ConversionEvent, Converter>                       BEGIN  = new EventType<>();
	public static final EventType<ConversionEvent, Pair<Converter, TrackerManager>> UPDATE = new EventType<>();
	public static final EventType<ConversionEvent, Converter>                       END    = new EventType<>();
	public static final EventType<ConversionEvent, Pair<Converter, Exception>>      ERROR  = new EventType<>();
	public static final EventType<ConversionEvent, Converter>                       PAUSE  = new EventType<>();
	public static final EventType<ConversionEvent, Converter>                       RESUME = new EventType<>();
	
	private static final EventType<ConversionEvent, ?>[] VALUES = Utils.array(BEGIN, UPDATE, END, ERROR, PAUSE, RESUME);
	public  static final EventType<ConversionEvent, ?>[] values() {
		return VALUES;
	}
	
	// Forbid anyone to create an instance of this class
	private ConversionEvent() {
	}
}