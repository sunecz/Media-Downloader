package sune.app.mediadown.media;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import sune.app.mediadown.net.Net;
import sune.app.mediadown.util.Utils;

/** @since 00.02.05 */
public final class MediaMetadata {
	
	private static final MediaMetadata EMPTY = new MediaMetadata(Map.of());
	
	private final Map<String, Object> data;
	
	private MediaMetadata(Map<String, Object> data) {
		this.data = Objects.requireNonNull(data);
	}
	
	/** @since 00.02.09 */
	private static final MediaMetadata ofRaw(Map<String, Object> data) {
		return data.isEmpty() ? empty() : new MediaMetadata(data);
	}
	
	public static final Builder builder() {
		return new Builder();
	}
	
	/** @since 00.02.09 */
	public static final Builder builder(Builder... builders) {
		Objects.requireNonNull(builders);
		Builder builder = new Builder();
		for(Builder b : builders) builder.add(b);
		return builder;
	}
	
	/** @since 00.02.09 */
	public static final Builder builder(MediaMetadata... metadata) {
		Objects.requireNonNull(metadata);
		Builder builder = new Builder();
		for(MediaMetadata m : metadata) builder.add(m);
		return builder;
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
	
	/** @since 00.02.09 */
	public static final MediaMetadata of(Builder... builders) {
		return Objects.requireNonNull(builders).length == 0 ? empty() : builder(builders).build();
	}
	
	/** @since 00.02.09 */
	public static final MediaMetadata of(MediaMetadata... metadata) {
		return Objects.requireNonNull(metadata).length == 0 ? empty() : builder(metadata).build();
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
		return has(Properties.protections);
	}
	
	/** @since 00.02.09 */
	public final List<MediaProtection> protections() {
		return get(Properties.protections, List.of());
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
		/** @since 00.02.09 */
		public static final String protections = "protection";
	}
	
	public static final class Builder {
		
		private final Map<String, Object> data;
		
		private Builder() {
			this(new LinkedHashMap<>());
		}
		
		/** @since 00.02.09 */
		private Builder(Map<String, Object> data) {
			this.data = data;
		}
		
		public final MediaMetadata build() {
			return ofRaw(data);
		}
		
		public final Builder add(Map<String, Object> data) {
			this.data.putAll(Objects.requireNonNull(data));
			return this;
		}
		
		public final Builder add(Object... data) {
			return add(Utils.stringKeyMap(Objects.requireNonNull(data)));
		}
		
		/** @since 00.02.09 */
		public final Builder add(Builder builder) {
			return add(Objects.requireNonNull(builder).data);
		}
		
		/** @since 00.02.09 */
		public final Builder add(MediaMetadata metadata) {
			return add(Objects.requireNonNull(metadata).data);
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
			return sourceURI(Net.uri(sourceURL));
		}
		
		public final Builder sourceURI(String sourceURL) {
			return sourceURI(Net.uri(sourceURL));
		}
		
		public final Builder title(String title) {
			this.data.put(Properties.title, Objects.requireNonNull(title));
			return this;
		}
		
		/** @since 00.02.09 */
		public final Builder addProtections(MediaProtection... protections) {
			return addProtections(List.of(protections));
		}
		
		/** @since 00.02.09 */
		public final Builder addProtections(List<MediaProtection> protections) {
			this.data.compute(Properties.protections, (key, list) -> {
				if(list == null) {
					list = new ArrayList<>();
				}
				
				Utils.<List<MediaProtection>>cast(list).addAll(protections);
				return list;
			});
			return this;
		}
		
		/** @since 00.02.09 */
		public final Builder removeProtections(MediaProtection... protections) {
			return removeProtections(List.of(protections));
		}
		
		/** @since 00.02.09 */
		public final Builder removeProtections(List<MediaProtection> protections) {
			this.data.computeIfPresent(Properties.protections, (key, list) -> {
				Utils.<List<MediaProtection>>cast(list).removeAll(protections);
				return list;
			});
			return this;
		}
		
		/** @since 00.02.09 */
		public final Builder copy() {
			return new Builder(new LinkedHashMap<>(data));
		}
		
		/** @since 00.02.09 */
		public final boolean has(String name) {
			return this.data.containsKey(name);
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
			return has(Properties.protections);
		}
		
		/** @since 00.02.09 */
		public final List<MediaProtection> protections() {
			return get(Properties.protections);
		}
	}
}