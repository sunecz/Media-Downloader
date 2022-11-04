package sune.app.mediadown.plugin;

import java.util.Collection;

import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.PluginLoaderEvent;

public interface PluginLoader extends EventBindable<PluginLoaderEvent> {
	
	void load(Collection<PluginFile> plugins) throws Exception;
	void dispose() throws Exception;
	Collection<PluginFile> getLoadedPlugins();
}