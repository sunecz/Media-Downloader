package sune.app.mediadown.event;

import sune.app.mediadown.Download;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;

public final class DownloadEvent implements IEventType {
	
	public static final EventType<DownloadEvent, Download>                       BEGIN  = new EventType<>();
	public static final EventType<DownloadEvent, Pair<Download, TrackerManager>> UPDATE = new EventType<>();
	public static final EventType<DownloadEvent, Download>                       END    = new EventType<>();
	public static final EventType<DownloadEvent, Pair<Download, Exception>>      ERROR  = new EventType<>();
	/** @since 00.01.18 */
	public static final EventType<DownloadEvent, Download>                       PAUSE  = new EventType<>();
	/** @since 00.01.18 */
	public static final EventType<DownloadEvent, Download>                       RESUME = new EventType<>();
	
	private static final EventType<DownloadEvent, ?>[] VALUES = Utils.array(BEGIN, UPDATE, END, ERROR, PAUSE, RESUME);
	public  static final EventType<DownloadEvent, ?>[] values() {
		return VALUES;
	}
	
	// Forbid anyone to create an instance of this class
	private DownloadEvent() {
	}
}