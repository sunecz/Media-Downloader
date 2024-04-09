package sune.app.mediadown.event;

import sune.app.mediadown.resource.PatchContext;
import sune.app.mediadown.util.Utils;

/** @since 00.02.10 */
public final class PatchEvent implements EventType {
	
	public static final Event<PatchEvent, PatchContext> BEGIN  = new Event<>();
	public static final Event<PatchEvent, PatchContext> UPDATE = new Event<>();
	public static final Event<PatchEvent, PatchContext> END    = new Event<>();
	public static final Event<PatchEvent, PatchContext> ERROR  = new Event<>();
	
	private static Event<PatchEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private PatchEvent() {
	}
	
	public static final Event<PatchEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(BEGIN, UPDATE, END, ERROR);
		}
		
		return values;
	}
}