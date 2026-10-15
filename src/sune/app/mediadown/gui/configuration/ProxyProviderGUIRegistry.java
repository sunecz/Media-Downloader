package sune.app.mediadown.gui.configuration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import sune.app.mediadown.util.unsafe.Reflection;

/** @since 00.02.09 */
public final class ProxyProviderGUIRegistry {
	
	private static final Map<String, ProxyProviderGUI.Factory> factories = new ConcurrentHashMap<>();
	
	private ProxyProviderGUIRegistry() { throw new AssertionError("No instances"); }
	
	public static void register(String className) {
		register(
			ProxyProviderGUI.Factory.class.cast(
				Reflection.newInstance(Reflection.getConstructor(Reflection.getClass(className)))
			)
		);
	}
	
	public static void register(ProxyProviderGUI.Factory factory) {
		Objects.requireNonNull(factory);
		factories.put(factory.name(), factory);
	}
	
	public static ProxyProviderGUI.Factory factory(String name) {
		return name != null ? factories.get(name) : null;
	}
	
	public static List<ProxyProviderGUI.Factory> factories() {
		return List.copyOf(factories.values());
	}
}
