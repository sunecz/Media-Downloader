package sune.app.mediadown.event;

import sune.app.mediadown.download.InternalDownloader;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;

public final class DownloadEvent implements EventType {
	
	public static final Event<DownloadEvent, InternalDownloader>                       BEGIN  = new Event<>();
	public static final Event<DownloadEvent, Pair<InternalDownloader, TrackerManager>> UPDATE = new Event<>();
	public static final Event<DownloadEvent, InternalDownloader>                       END    = new Event<>();
	public static final Event<DownloadEvent, Pair<InternalDownloader, Exception>>      ERROR  = new Event<>();
	public static final Event<DownloadEvent, InternalDownloader>                       PAUSE  = new Event<>();
	public static final Event<DownloadEvent, InternalDownloader>                       RESUME = new Event<>();
	
	private static Event<DownloadEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private DownloadEvent() {
	}
	
	public static final Event<DownloadEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(BEGIN, UPDATE, END, ERROR, PAUSE, RESUME);
		}
		
		return values;
	}
}