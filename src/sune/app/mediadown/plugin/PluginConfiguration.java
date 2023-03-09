package sune.app.mediadown.plugin;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import sune.app.mediadown.configuration.Configuration;
import sune.app.mediadown.configuration.ConfigurationLocatable;
import sune.app.mediadown.configuration.ConfigurationReloadable;
import sune.util.ssdf2.SSDCollection;

/** @since 00.02.04 */
public class PluginConfiguration extends Configuration implements ConfigurationLocatable, ConfigurationReloadable {
	
	private static PluginConfiguration EMPTY;
	
	/** @since 00.02.08 */
	private final Path path;
	
	protected PluginConfiguration(String name, Path path, SSDCollection data,
			Map<String, ConfigurationProperty<?>> properties) {
		super(name, data, properties);
		this.path = path;
	}
	
	public static final PluginConfiguration empty() {
		if(EMPTY == null) {
			EMPTY = new PluginConfiguration("", null, SSDCollection.empty(), new LinkedHashMap<>());
		}
		
		return EMPTY;
	}
	
	@Override
	public boolean reload() {
		// Do nothing here. Plugin configuration does not have any cached fields,
		// therefore any changed property that is changed using its Writer will be
		// reflected when calling *Value methods() and similar.
		return true;
	}
	
	@Override
	public Path path() {
		return path;
	}
	
	public static class Builder extends Configuration.Builder {
		
		/** @since 00.02.08 */
		private Path path;
		
		public Builder(String name) {
			super(name);
		}
		
		@Override
		public PluginConfiguration build() {
			Map<String, ConfigurationProperty<?>> builtProperties = new LinkedHashMap<>();
			SSDCollection data = data(builtProperties);
			return new PluginConfiguration(name, path, data, builtProperties);
		}
		
		/** @since 00.02.08 */
		public void path(Path path) {
			this.path = path;
		}
		
		/** @since 00.02.08 */
		public Path path() {
			return path;
		}
	}
}