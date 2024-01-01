package sune.app.mediadown.event;

import sune.app.mediadown.media.fix.MediaFixerContext;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public final class MediaFixEvent implements EventType {
	
	public static final Event<MediaFixEvent, MediaFixerContext> BEGIN  = new Event<>();
	public static final Event<MediaFixEvent, MediaFixerContext> UPDATE = new Event<>();
	public static final Event<MediaFixEvent, MediaFixerContext> END    = new Event<>();
	public static final Event<MediaFixEvent, MediaFixerContext> ERROR  = new Event<>();
	public static final Event<MediaFixEvent, MediaFixerContext> PAUSE  = new Event<>();
	public static final Event<MediaFixEvent, MediaFixerContext> RESUME = new Event<>();
	
	private static Event<MediaFixEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private MediaFixEvent() {
	}
	
	public static final Event<MediaFixEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(BEGIN, UPDATE, END, ERROR, PAUSE, RESUME);
		}
		
		return values;
	}
}