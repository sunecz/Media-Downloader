package sune.app.mediadown.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.PluginLoaderEvent;

public final class Plugins {
	
	private static final PluginLoader LOADER = new DefaultPluginLoader();
	private static final Set<PluginFile> PLUGINS = new LinkedHashSet<>();
	
	// Forbid anyone to create an instance of this class
	private Plugins() {
	}
	
	public static final void add(PluginFile plugin) {
		PLUGINS.add(plugin);
	}
	
	public static final void remove(PluginFile plugin) {
		PLUGINS.remove(plugin);
	}
	
	public static final boolean has(PluginFile plugin) {
		return PLUGINS.contains(plugin);
	}
	
	public static final Collection<PluginFile> all() {
		return Collections.unmodifiableCollection(PLUGINS);
	}
	
	public static final Collection<PluginFile> allLoaded() {
		return Collections.unmodifiableCollection(LOADER.getLoadedPlugins());
	}
	
	public static final PluginFile getLoaded(String name) {
		for(PluginFile plugin : LOADER.getLoadedPlugins()) {
			if((plugin.getPlugin().instance().name().equals(name)))
				return plugin;
		}
		return null;
	}
	
	public static final PluginFile getLoaded(Class<?> clazz) {
		for(PluginFile plugin : LOADER.getLoadedPlugins()) {
			Object instance = plugin.getInstance();
			if((instance != null
					&& instance.getClass() == clazz))
				return plugin;
		}
		return null;
	}
	
	public static final void loadAll() throws Exception {
		LOADER.load(PLUGINS);
	}
	
	public static final void dispose() throws Exception {
		LOADER.dispose();
	}
	
	public static final <V> void addEventListener(Event<? extends PluginLoaderEvent, V> event, Listener<V> listener) {
		LOADER.addEventListener(event, listener);
	}
	
	public static final <V> void removeEventListener(Event<? extends PluginLoaderEvent, V> event, Listener<V> listener) {
		LOADER.removeEventListener(event, listener);
	}
}