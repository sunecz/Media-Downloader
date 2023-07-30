package sune.app.mediadown.event;

import sune.app.mediadown.conversion.ConversionContext;
import sune.app.mediadown.util.Utils;

/** @since 00.01.26 */
public final class ConversionEvent implements EventType {
	
	public static final Event<ConversionEvent, ConversionContext> BEGIN  = new Event<>();
	public static final Event<ConversionEvent, ConversionContext> UPDATE = new Event<>();
	public static final Event<ConversionEvent, ConversionContext> END    = new Event<>();
	public static final Event<ConversionEvent, ConversionContext> ERROR  = new Event<>();
	public static final Event<ConversionEvent, ConversionContext> PAUSE  = new Event<>();
	public static final Event<ConversionEvent, ConversionContext> RESUME = new Event<>();
	
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