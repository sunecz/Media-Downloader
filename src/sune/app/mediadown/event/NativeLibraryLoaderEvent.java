package sune.app.mediadown.event;

import java.util.List;

import sune.app.mediadown.library.NativeLibrary;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;

public final class NativeLibraryLoaderEvent implements EventType {
	
	public static final Event<NativeLibraryLoaderEvent, NativeLibrary>                  LOADING    = new Event<>();
	public static final Event<NativeLibraryLoaderEvent, Pair<NativeLibrary, Throwable>> LOADED     = new Event<>();
	public static final Event<NativeLibraryLoaderEvent, List<NativeLibrary>>            NOT_LOADED = new Event<>();
	
	private static Event<NativeLibraryLoaderEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private NativeLibraryLoaderEvent() {
	}
	
	public static final Event<NativeLibraryLoaderEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(LOADING, LOADED, NOT_LOADED);
		}
		
		return values;
	}
}