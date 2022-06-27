package sune.app.mediadown.plugin;

import java.util.Collection;

public interface PluginLoader {
	
	void load(Collection<PluginFile> plugins, PluginLoadListener listener) throws Exception;
	void dispose() throws Exception;
	Collection<PluginFile> getLoadedPlugins();
}