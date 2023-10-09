package sune.app.mediadown.plugin;

/** @since 00.02.09 */
public interface PluginInstance {
	
	default void init() throws Exception {
		// Do nothing by default
	}
	
	default void dispose() throws Exception {
		// Do nothing by default
	}
	
	default void beforeBuildConfiguration() throws Exception {
		// Do nothing by default
	}
	
	default void afterBuildConfiguration() throws Exception {
		// Do nothing by default
	}
}