package sune.app.mediadown.conversion;

import java.util.Objects;

import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.util.ClassRegistry;

/** @since 00.02.09 */
public final class Conversions {
	
	// Forbid anyone to create an instance of this class
	private Conversions() {
	}
	
	public static final class Providers {
		
		private static final ClassRegistry<ConversionProvider> registry = new Registry();
		
		// Forbid anyone to create an instance of this class
		private Providers() {
		}
		
		public static final void register(String className) throws ClassNotFoundException {
			registry.register(className);
		}
		
		public static final void unregister(String className) throws ClassNotFoundException {
			registry.unregister(className);
		}
		
		public static final ConversionProvider ofName(String name) {
			return registry.get(name);
		}
		
		public static final ClassRegistry<ConversionProvider> registry() {
			return registry;
		}
		
		private static final class Registry extends ClassRegistry<ConversionProvider> {
			
			@Override
			protected String toName(ConversionProvider value) {
				return value.name();
			}
		}
	}
	
	public static final class Formats {
		
		// Forbid anyone to create an instance of this class
		private Formats() {
		}
		
		public static final ConversionFormat of(MediaFormat format) {
			Objects.requireNonNull(format);
			return Providers.registry().allValues().stream()
						.map((p) -> p.formatOf(format))
						.filter(Objects::nonNull)
						.findFirst().orElse(null);
		}
	}
}