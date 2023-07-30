package sune.app.mediadown.event;

import sune.app.mediadown.download.DownloadContext;
import sune.app.mediadown.util.Utils;

public final class DownloadEvent implements EventType {
	
	public static final Event<DownloadEvent, DownloadContext> BEGIN  = new Event<>();
	public static final Event<DownloadEvent, DownloadContext> UPDATE = new Event<>();
	public static final Event<DownloadEvent, DownloadContext> END    = new Event<>();
	public static final Event<DownloadEvent, DownloadContext> ERROR  = new Event<>();
	public static final Event<DownloadEvent, DownloadContext> PAUSE  = new Event<>();
	public static final Event<DownloadEvent, DownloadContext> RESUME = new Event<>();
	
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