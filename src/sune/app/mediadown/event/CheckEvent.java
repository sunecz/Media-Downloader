package sune.app.mediadown.event;

import sune.app.mediadown.util.Utils;

public final class CheckEvent implements EventType {
	
	public static final Event<CheckEvent, Void>   BEGIN   = new Event<>();
	public static final Event<CheckEvent, String> COMPARE = new Event<>();
	public static final Event<CheckEvent, Void>   END     = new Event<>();
	
	private static Event<CheckEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private CheckEvent() {
	}
	
	public static final Event<CheckEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(BEGIN, COMPARE, END);
		}
		
		return values;
	}
}