package sune.app.mediadown.media;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import sune.app.mediadown.util.Utils;

/** @since 00.02.05 */
public final class MediaMetadata {
	
	private static final MediaMetadata EMPTY = new MediaMetadata(Map.of());
	
	private final Map<String, Object> data;
	
	private MediaMetadata(Map<String, Object> data) {
		this.data = Objects.requireNonNull(data);
	}
	
	public static final Builder builder() {
		return new Builder();
	}
	
	public static final MediaMetadata empty() {
		return EMPTY;
	}
	
	public static final MediaMetadata of(Object... data) {
		return Objects.requireNonNull(data).length == 0 ? empty() : new MediaMetadata(Utils.stringKeyMap(data));
	}
	
	public static final MediaMetadata of(Map<String, Object> data) {
		return Objects.requireNonNull(data).isEmpty() ? empty() : new MediaMetadata(new LinkedHashMap<>(data));
	}
	
	public final boolean has(String name) {
		return data.containsKey(name);
	}
	
	public final boolean hasValue(Object value) {
		return data.containsValue(value);
	}
	
	public final <T> T get(String name) {
		@SuppressWarnings("unchecked")
		T value = (T) data.get(name);
		return value;
	}
	
	public final <T> T get(String name, T defaultValue) {
		@SuppressWarnings("unchecked")
		T value = (T) data.getOrDefault(name, defaultValue);
		return value;
	}
	
	public final <T> T getOrSupply(String name, Supplier<T> supplier) {
		@SuppressWarnings("unchecked")
		T value = (T) data.get(name);
		return value == null && !data.containsKey(name) ? supplier.get() : value;
	}
	
	public final boolean isEmpty() {
		return data.isEmpty();
	}
	
	public final Map<String, Object> data() {
		return Collections.unmodifiableMap(data);
	}
	
	public final URI sourceURI() {
		return get(Properties.sourceURI);
	}
	
	public final String title() {
		return get(Properties.title);
	}
	
	public final boolean isProtected() {
		return get(Properties.isProtected, false);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(data);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		MediaMetadata other = (MediaMetadata) obj;
		return Objects.equals(data, other.data);
	}
	
	@Override
	public String toString() {
		return data.isEmpty() ? "EMPTY" : "[" + data + "]";
	}
	
	public static final class Properties {
		
		public static final String sourceURI = "sourceURI";
		public static final String title = "title";
		public static final String isProtected = "isProtected";
	}
	
	public static final class Builder {
		
		private final Map<String, Object> data;
		
		private Builder() {
			data = new LinkedHashMap<>();
		}
		
		public final MediaMetadata build() {
			return of(data);
		}
		
		public final Builder add(Map<String, Object> data) {
			this.data.putAll(Objects.requireNonNull(data));
			return this;
		}
		
		public final Builder add(Object... data) {
			return add(Utils.stringKeyMap(Objects.requireNonNull(data)));
		}
		
		public final Builder remove(List<String> names) {
			Objects.requireNonNull(names).forEach(this.data::remove);
			return this;
		}
		
		public final Builder remove(String... names) {
			return remove(List.of(Objects.requireNonNull(names)));
		}
		
		public final Builder sourceURI(URI sourceURI) {
			this.data.put(Properties.sourceURI, Objects.requireNonNull(sourceURI));
			return this;
		}
		
		public final Builder sourceURI(URL sourceURL) {
			return sourceURI(Utils.uri(sourceURL));
		}
		
		public final Builder sourceURI(String sourceURL) {
			return sourceURI(Utils.uri(sourceURL));
		}
		
		public final Builder title(String title) {
			this.data.put(Properties.title, Objects.requireNonNull(title));
			return this;
		}
		
		public final Builder isProtected(boolean isProtected) {
			this.data.put(Properties.isProtected, isProtected);
			return this;
		}
		
		public final <T> T get(String name) {
			@SuppressWarnings("unchecked")
			T value = (T) this.data.get(name);
			return value;
		}
		
		public final URI sourceURI() {
			return get(Properties.sourceURI);
		}
		
		public final String title() {
			return get(Properties.title);
		}
		
		public final boolean isProtected() {
			return get(Properties.isProtected);
		}
	}
}