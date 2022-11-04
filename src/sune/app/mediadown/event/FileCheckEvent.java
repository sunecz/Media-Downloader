package sune.app.mediadown.event;

import java.nio.file.Path;

import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;

public final class FileCheckEvent implements EventType {
	
	public static final Event<FileCheckEvent, Path>               BEGIN  = new Event<>();
	public static final Event<FileCheckEvent, Pair<Path, String>> UPDATE = new Event<>();
	public static final Event<FileCheckEvent, Path>               END    = new Event<>();
	public static final Event<FileCheckEvent, Exception>          ERROR  = new Event<>();
	
	private static Event<FileCheckEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private FileCheckEvent() {
	}
	
	public static final Event<FileCheckEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(BEGIN, UPDATE, END, ERROR);
		}
		
		return values;
	}
}