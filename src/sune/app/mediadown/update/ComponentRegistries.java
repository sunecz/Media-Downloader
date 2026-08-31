package sune.app.mediadown.update;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/** @since 00.02.09 */
public final class ComponentRegistries {
	
	private static final Map<String, ComponentRegistry> registries = new LinkedHashMap<>();
	
	private ComponentRegistries() {
		throw new AssertionError("No instances");
	}
	
	public static final void add(ComponentRegistry registry) {
		Objects.requireNonNull(registry);
		registries.put(registry.endpointUri(), registry);
	}
	
	public static final void remove(ComponentRegistry registry) {
		Objects.requireNonNull(registry);
		registries.remove(registry.endpointUri());
	}
	
	public static final Stream<ComponentRegistry> stream() {
		return registries.values().stream();
	}
	
	public static final List<ComponentRegistry> all() {
		return List.copyOf(registries.values());
	}
}
