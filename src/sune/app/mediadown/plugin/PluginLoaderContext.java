package sune.app.mediadown.plugin;

public final class PluginLoaderContext {
	
	private static PluginFile theContext;
	
	public static final void setContext(PluginFile context) {
		theContext = context;
	}
	
	public static final PluginFile getContext() {
		return theContext;
	}
	
	// Forbid anyone to create an instance of this class
	private PluginLoaderContext() {
	}
}