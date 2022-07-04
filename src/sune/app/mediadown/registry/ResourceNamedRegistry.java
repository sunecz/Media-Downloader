package sune.app.mediadown.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import sune.app.mediadown.registry.ResourceNamedRegistry.ResourceRegistryEntry;

/** @since 00.02.07 */
public class ResourceNamedRegistry<T> extends SimpleNamedRegistry<ResourceRegistryEntry<T>> {
	
	public void registerValue(String name, T value) {
		super.register(name, ResourceRegistryEntry.ofValue(value));
	}
	
	public void registerValue(String name, T value, boolean isExtractable) {
		super.register(name, ResourceRegistryEntry.ofValue(value, isExtractable));
	}
	
	public T getValue(String name) {
		return Optional.ofNullable(get(name)).map(ResourceRegistryEntry::value).orElse(null);
	}
	
	public Collection<ResourceRegistryEntry<T>> allEntries() {
		return Collections.unmodifiableCollection(values.values());
	}
	
	public Collection<T> values() {
		return values.values().stream().map(ResourceRegistryEntry::value).collect(Collectors.toList());
	}
	
	public static final class ResourceRegistryEntry<T> {
		
		private final T value;
		private final boolean isExtractable;
		
		private ResourceRegistryEntry(T value, boolean isExtractable) {
			this.value = value;
			this.isExtractable = isExtractable;
		}
		
		public static final <T> ResourceRegistryEntry<T> ofValue(T value) {
			return ofValue(value, true);
		}
		
		public static final <T> ResourceRegistryEntry<T> ofValue(T value, boolean isExtractable) {
			return new ResourceRegistryEntry<>(value, isExtractable);
		}
		
		public T value() {
			return value;
		}
		
		public boolean isExtractable() {
			return isExtractable;
		}
	}
}