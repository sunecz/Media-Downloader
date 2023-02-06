package sune.app.mediadown.library;

import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.util.Pair;

public final class LibraryEvent implements EventType {
	
	// TODO: Move, add missing methods, clean up
	
	public static final Event<LibraryEvent, Library>                  LOADING    = new Event<>();
	public static final Event<LibraryEvent, Library>                  LOADED     = new Event<>();
	public static final Event<LibraryEvent, Pair<Library, Exception>> NOT_LOADED = new Event<>();
	
	// Forbid anyone to create an instance of this class
	private LibraryEvent() {
	}
}