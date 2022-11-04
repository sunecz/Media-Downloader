package sune.app.mediadown.plugin;

@Deprecated
public interface PluginLoadListener {
	
	void onLoading  (PluginFile   plugin);
	void onLoaded   (PluginFile   plugin, boolean success);
	void onNotLoaded(PluginFile[] plugins);
}