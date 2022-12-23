package sune.app.mediadown.plugin;

import java.util.LinkedHashMap;
import java.util.Map;

import sune.app.mediadown.configuration.Configuration;
import sune.util.ssdf2.SSDCollection;

/** @since 00.02.04 */
public class PluginConfiguration extends Configuration {
	
	private static PluginConfiguration EMPTY;
	
	protected PluginConfiguration(String name, SSDCollection data, Map<String, ConfigurationProperty<?>> properties) {
		super(name, data, properties);
	}
	
	public static final PluginConfiguration empty() {
		if(EMPTY == null) {
			EMPTY = new PluginConfiguration("", SSDCollection.empty(), new LinkedHashMap<>());
		}
		return EMPTY;
	}
	
	public static class Builder extends Configuration.Builder {
		
		public Builder(String name) {
			super(name);
		}
		
		@Override
		public PluginConfiguration build() {
			Map<String, ConfigurationProperty<?>> builtProperties = new LinkedHashMap<>();
			SSDCollection data = data(builtProperties);
			return new PluginConfiguration(name, data, builtProperties);
		}
	}
}