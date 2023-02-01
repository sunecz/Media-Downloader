package sune.app.mediadown.event;

import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.QueueContext;
import sune.app.mediadown.util.Utils;

/** @since 00.02.08 */
public final class QueueEvent implements EventType {
	
	public static final Event<QueueEvent, Pair<QueueContext, Integer>> POSITION_UPDATE = new Event<>();
	
	private static Event<QueueEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private QueueEvent() {
	}
	
	public static final Event<QueueEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(POSITION_UPDATE);
		}
		
		return values;
	}
}