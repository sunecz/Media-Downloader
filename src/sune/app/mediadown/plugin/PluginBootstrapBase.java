package sune.app.mediadown.plugin;

public abstract class PluginBootstrapBase {
	
	private final PluginBootstrap plugin;
	/** @since 00.02.05 */
	private final PluginFile context;
	
	public PluginBootstrapBase() {
		plugin  = getClass().getAnnotation(PluginBootstrap.class);
		context = PluginLoaderContext.getContext();
		if((plugin == null))
			throw new IllegalStateException("Invalid plugin bootstrap. Missing Plugin bootstrap annotation.");
	}
	
	public abstract void init() throws Exception;
	public abstract void dispose() throws Exception;
	
	/** @since 00.02.05 */
	public PluginConfiguration.Builder configuration() {
		return null;
	}
	
	public PluginBootstrap annotation() {
		return plugin;
	}
	
	/** @since 00.02.05 */
	public PluginFile context() {
		return context;
	}
}