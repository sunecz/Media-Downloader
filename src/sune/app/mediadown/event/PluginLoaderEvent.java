package sune.app.mediadown.event;

import java.util.List;

import sune.app.mediadown.plugin.PluginFile;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;

public final class PluginLoaderEvent implements EventType {
	
	public static final Event<PluginLoaderEvent, PluginFile>                  LOADING       = new Event<>();
	public static final Event<PluginLoaderEvent, Pair<PluginFile, Boolean>>   LOADED        = new Event<>();
	public static final Event<PluginLoaderEvent, List<PluginFile>>            NOT_LOADED    = new Event<>();
	public static final Event<PluginLoaderEvent, Pair<PluginFile, Exception>> ERROR_LOAD    = new Event<>();
	public static final Event<PluginLoaderEvent, Pair<PluginFile, Exception>> ERROR_DISPOSE = new Event<>();
	
	private static Event<PluginLoaderEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private PluginLoaderEvent() {
	}
	
	public static final Event<PluginLoaderEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(LOADING, LOADED, NOT_LOADED);
		}
		
		return values;
	}
}