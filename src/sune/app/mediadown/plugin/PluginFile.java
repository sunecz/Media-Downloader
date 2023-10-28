package sune.app.mediadown.plugin;

import java.nio.file.Path;

import eu.infomas.annotation.AnnotationDetector;

public class PluginFile {
	
	private final String path;
	private final AnnotatedClass<Plugin> plugin;
	private final AnnotatedClass<PluginBootstrap> pluginBootstrap;
	private PluginMemory memory;
	private PluginBase instance;
	private PluginBootstrapBase instanceBootstrap;
	/** @since 00.02.04 */
	private PluginConfiguration configuration = PluginConfiguration.empty();
	
	public PluginFile(String path, AnnotatedClass<Plugin> plugin, AnnotatedClass<PluginBootstrap> pluginBootstrap) {
		this.path = path;
		this.plugin = plugin;
		this.pluginBootstrap = pluginBootstrap;
	}
	
	public static final PluginFile from(Path file) throws Exception {
		PluginTypeReporter reporter = new PluginTypeReporter(file);
		AnnotationDetector detector = new AnnotationDetector(reporter);
		detector.detect(file.toFile());
		String path = file.toAbsolutePath().toString();
		AnnotatedClass<Plugin> plugin = reporter.getPlugin();
		AnnotatedClass<PluginBootstrap> pluginBootstrap = reporter.getPluginBootstrap();
		// Report any issues with the loading
		Exception exception;
		if((exception = reporter.getException()) != null)
			throw exception;
		if((plugin == null))
			throw new IllegalStateException("Unable to load plugin at '" + file.toAbsolutePath().toString() + "'.");
		// All done, return the plugin file
		return new PluginFile(path, plugin, pluginBootstrap);
	}
	
	public static final void resetPluginFileLoader() {
		PluginTypeReporter.resetClassLoader();
	}
	
	protected void setMemory(PluginMemory memory) {
		this.memory = memory;
	}
	
	protected void setInstance(PluginBase instance) {
		this.instance = instance;
	}
	
	protected void setBootstrapInstance(PluginBootstrapBase instanceBootstrap) {
		this.instanceBootstrap = instanceBootstrap;
	}
	
	/** @since 00.02.04 */
	protected void setConfiguration(PluginConfiguration configuration) {
		this.configuration = configuration;
	}
	
	public String getPath() {
		return path;
	}
	
	/** @since 00.02.02 */
	public AnnotatedClass<Plugin> getPlugin() {
		return plugin;
	}
	
	/** @since 00.02.02 */
	public AnnotatedClass<PluginBootstrap> getPluginBootstrap() {
		return pluginBootstrap;
	}
	
	public PluginMemory getMemory() {
		return memory;
	}
	
	public PluginBase getInstance() {
		return instance;
	}
	
	public PluginBootstrapBase getBootstrapInstance() {
		return instanceBootstrap;
	}
	
	/** @since 00.02.09 */
	public PluginInstance getPluginInstance() {
		return instance == null ? instanceBootstrap : instance;
	}
	
	/** @since 00.02.04 */
	public PluginConfiguration getConfiguration() {
		return configuration;
	}
}