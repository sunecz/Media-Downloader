package sune.app.mediadown.event;

import sune.app.mediadown.download.InternalDownloader;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;

public final class DownloadEvent implements IEventType {
	
	public static final EventType<DownloadEvent, InternalDownloader>                       BEGIN  = new EventType<>();
	public static final EventType<DownloadEvent, Pair<InternalDownloader, TrackerManager>> UPDATE = new EventType<>();
	public static final EventType<DownloadEvent, InternalDownloader>                       END    = new EventType<>();
	public static final EventType<DownloadEvent, Pair<InternalDownloader, Exception>>      ERROR  = new EventType<>();
	public static final EventType<DownloadEvent, InternalDownloader>                       PAUSE  = new EventType<>();
	public static final EventType<DownloadEvent, InternalDownloader>                       RESUME = new EventType<>();
	
	private static EventType<DownloadEvent, ?>[] VALUES;
	
	// Forbid anyone to create an instance of this class
	private DownloadEvent() {
	}
	
	public static final EventType<DownloadEvent, ?>[] values() {
		if(VALUES == null) {
			VALUES = Utils.array(BEGIN, UPDATE, END, ERROR, PAUSE, RESUME);
		}
		
		return VALUES;
	}
}