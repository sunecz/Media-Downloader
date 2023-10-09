package sune.app.mediadown.plugin;

import javafx.scene.image.Image;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.language.Translation;

public abstract class PluginBase implements PluginInstance {
	
	private final Plugin plugin;
	private final PluginFile context;
	private Image icon;
	
	public PluginBase() {
		plugin  = getClass().getAnnotation(Plugin.class);
		context = PluginLoaderContext.getContext();
		if((plugin == null))
			throw new IllegalStateException("Invalid plugin. Missing Plugin annotation.");
	}
	
	@Override public abstract void init() throws Exception;
	@Override public abstract void dispose() throws Exception;
	
	/** @since 00.02.04 */
	public PluginConfiguration.Builder configuration() {
		return null;
	}
	
	/** @since 00.02.04 */
	public Translation translation() {
		return MediaDownloader.translation().getTranslation("plugin." + getName());
	}
	
	/** @since 00.02.05 */
	public Plugin annotation() {
		return plugin;
	}
	
	/** @since 00.02.04 */
	public String getName() {
		return plugin.name();
	}
	
	public String getTitle() {
		return plugin.title();
	}
	
	public String getVersion() {
		return plugin.version();
	}
	
	public String getAuthor() {
		return plugin.author();
	}
	
	public String getUpdateBaseURL() {
		return plugin.updateBaseURL();
	}
	
	public boolean isUpdatable() {
		return plugin.updatable();
	}
	
	public String getURL() {
		return plugin.url();
	}
	
	public Image getIcon() {
		String   path;
		return !(path = plugin.icon()).isEmpty()
					? (icon == null ? icon = new Image(PluginResource.stream(context, path)) : icon)
					: null;
	}
	
	public PluginFile getContext() {
		return context;
	}
}