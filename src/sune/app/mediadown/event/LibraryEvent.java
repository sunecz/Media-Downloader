package sune.app.mediadown.event;

import sune.app.mediadown.library.Library;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;

/** @since 00.02.08 */
public final class LibraryEvent implements EventType {
	
	public static final Event<LibraryEvent, Library>                  LOADING    = new Event<>();
	public static final Event<LibraryEvent, Library>                  LOADED     = new Event<>();
	public static final Event<LibraryEvent, Pair<Library, Exception>> NOT_LOADED = new Event<>();
	
	private static Event<LibraryEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private LibraryEvent() {
	}
	
	public static final Event<LibraryEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(LOADING, LOADED, NOT_LOADED);
		}
		
		return values;
	}
}