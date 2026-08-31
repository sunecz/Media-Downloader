package sune.app.mediadown.update;

import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public final class ArtifactCheckEvent implements EventType {
	
	public static final Event<ArtifactCheckEvent, ArtifactCheckContext> BEGIN  = new Event<>();
	public static final Event<ArtifactCheckEvent, ArtifactCheckContext> END    = new Event<>();
	public static final Event<ArtifactCheckEvent, ArtifactCheckContext> ERROR  = new Event<>();
	
	private static Event<ArtifactCheckEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private ArtifactCheckEvent() {
	}
	
	public static final Event<ArtifactCheckEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(BEGIN, END, ERROR);
		}
		
		return values;
	}
}
