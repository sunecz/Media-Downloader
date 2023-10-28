package sune.app.mediadown.event;

import sune.app.mediadown.download.DownloadState;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public final class DownloadStateEvent implements EventType {
	
	public static final Event<DownloadStateEvent, DownloadState> UPDATE = new Event<>();
	
	private static Event<DownloadStateEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private DownloadStateEvent() {
	}
	
	public static final Event<DownloadStateEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(UPDATE);
		}
		
		return values;
	}
}