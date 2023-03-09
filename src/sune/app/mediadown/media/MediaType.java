package sune.app.mediadown.media;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import sune.app.mediadown.concurrent.ValidableValue;

/** @since 00.02.05 */
public final class MediaType {
	
	private static final Map<String, MediaType> registered = new LinkedHashMap<>();
	private static final ValidableValue<MediaType[]> values;
	
	static {
		values = ValidableValue.of(MediaType::newValues);
	}
	
	// Special media types
	public static final MediaType UNKNOWN;
	// Common media types
	public static final MediaType VIDEO;
	public static final MediaType AUDIO;
	public static final MediaType SUBTITLES;
	
	static {
		UNKNOWN   = new Builder().name("UNKNOWN").build();
		VIDEO     = new Builder().name("VIDEO").build();
		AUDIO     = new Builder().name("AUDIO").build();
		SUBTITLES = new Builder().name("SUBTITLES").build();
	}
	
	private final String name;
	
	private MediaType(String name) {
		this.name = requireValidName(name);
		register(this);
	}
	
	private static final String requireValidName(String name) {
		if(name == null || name.isBlank())
			throw new IllegalArgumentException("Name may be neither null nor blank.");
		return name;
	}
	
	private static final void register(MediaType type) {
		synchronized(registered) {
			if(registered.putIfAbsent(type.name.toLowerCase(), type) != null) {
				throw new IllegalStateException("Media type \"" + type.name + "\" already registered.");
			}
			
			values.invalidate();
		}
	}
	
	private static final Stream<MediaType> allTypes() {
		List<MediaType> types;
		
		synchronized(registered) {
			types = List.copyOf(registered.values());
		}
		
		return types.stream();
	}
	
	private static final MediaType[] newValues() {
		return allTypes().toArray(MediaType[]::new);
	}
	
	public static final MediaType[] values() {
		return values.value();
	}
	
	public static final MediaType ofName(String name) {
		return registered.entrySet().stream()
					.filter((e) -> e.getKey().equalsIgnoreCase(name))
					.map(Map.Entry::getValue)
					.findFirst().orElse(UNKNOWN);
	}
	
	public String name() {
		return name;
	}
	
	// Method just for convenience
	public boolean is(MediaType type) {
		return equals(type);
	}
	
	// Method just for convenience
	public boolean isAnyOf(MediaType... types) {
		return Stream.of(types).anyMatch(this::is);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		MediaType other = (MediaType) obj;
		return Objects.equals(name, other.name);
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public static final class Builder {
		
		private String name;
		
		public MediaType build() {
			return new MediaType(requireValidName(name));
		}
		
		public Builder name(String name) {
			this.name = requireValidName(name);
			return this;
		}
		
		public String name() {
			return name;
		}
	}
}