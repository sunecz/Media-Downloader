package sune.app.mediadown.event.tracker;

import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.util.Utils;

/** @since 00.02.08 */
public final class TrackerEvent implements EventType {
	
	public static final Event<TrackerEvent, Tracker> UPDATE = new Event<>();
	
	private static Event<TrackerEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private TrackerEvent() {
	}
	
	public static final Event<TrackerEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(UPDATE);
		}
		
		return values;
	}
}