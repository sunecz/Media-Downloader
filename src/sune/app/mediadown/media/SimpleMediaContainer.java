package sune.app.mediadown.media;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import sune.app.mediadown.util.Singleton;
import sune.app.mediadown.util.Utils;

/** @since 00.02.05 */
public class SimpleMediaContainer implements MediaContainer {
	
	protected final MediaSource source;
	protected final URI uri;
	protected final MediaType type;
	protected final MediaFormat format;
	protected final MediaQuality quality;
	protected final long size;
	protected final MediaMetadata metadata;
	/** @since 00.02.08 */
	protected final Media parent;
	protected final MediaAccessor accessor;
	
	protected SimpleMediaContainer(MediaSource source, URI uri, MediaType type, MediaFormat format, MediaQuality quality,
			long size, MediaMetadata metadata, Media parent, ChildMediaBuilderContext builderContext) {
		this.source = Objects.requireNonNull(source);
		this.uri = uri;
		this.type = Objects.requireNonNull(type);
		this.format = Objects.requireNonNull(format);
		this.quality = Objects.requireNonNull(quality);
		this.size = size;
		this.metadata = Objects.requireNonNull(metadata);
		this.parent = parent;
		this.accessor = new RecursiveMediaAccessor(builderContext.build(this));
	}
	
	@Override
	public MediaSource source() {
		return source;
	}
	
	@Override
	public URI uri() {
		return uri;
	}
	
	@Override
	public MediaType type() {
		return type;
	}
	
	@Override
	public MediaFormat format() {
		return format;
	}
	
	@Override
	public MediaQuality quality() {
		return quality;
	}
	
	@Override
	public long size() {
		return size;
	}
	
	@Override
	public MediaMetadata metadata() {
		return metadata;
	}
	
	@Override
	public Media parent() {
		return parent;
	}
	
	@Override
	public boolean isContainer() {
		return true;
	}
	
	@Override
	public boolean isSegmented() {
		return false;
	}
	
	@Override
	public boolean isCombined() {
		return false;
	}
	
	@Override
	public boolean isSeparated() {
		return false;
	}
	
	@Override
	public boolean isSingle() {
		return false;
	}
	
	@Override
	public boolean isVirtual() {
		return false;
	}
	
	@Override
	public boolean isPhysical() {
		return true;
	}
	
	@Override
	public MediaAccessor recursive() {
		return this;
	}
	
	@Override
	public MediaAccessor direct() {
		return Singleton.of(this, accessor::direct);
	}
	
	@Override
	public <T extends Media> List<T> allOfType(MediaType type) {
		return accessor.allOfType(type);
	}
	
	@Override
	public <T extends MediaContainer> List<T> allContainersOfType(MediaType type) {
		return accessor.allContainersOfType(type);
	}
	
	@Override
	public <T extends Media> T ofType(MediaType type) {
		return accessor.ofType(type);
	}
	
	@Override
	public <T extends MediaContainer> T containerOfType(MediaType type) {
		return accessor.containerOfType(type);
	}
	
	@Override
	public List<Media> media() {
		return accessor.media();
	}
	
	@Override
	public String toString() {
		return "MediaContainer["
					+ "class=" + getClass().getSimpleName() + ", "
					+ "source=" + source + ", "
					+ "type=" + type + ", "
					+ "format=" + format + ", "
					+ "quality=" + quality + ", "
					+ "uri=" + uri + ", "
					+ "size=" + size + ", "
					+ "metadata=" + metadata
		        + "]";
	}
	
	/** @since 00.02.08 */
	protected static class SimpleChildMediaBuilderContext extends ChildMediaBuilderContext {
		
		public SimpleChildMediaBuilderContext(List<? extends Media.Builder<?, ?>> media) {
			super(media);
		}
		
		@Override
		public List<Media> build(Media parent) {
			return media.stream()
					    .map((m) -> (Media.Builder<?, ?>) m.parent(parent))
					    .map(Media.Builder::build)
					    .collect(Collectors.toList());
		}
	}
	
	public static class Builder<T extends SimpleMediaContainer, B extends Builder<T, B>>
			implements MediaContainer.Builder<T, B> {
		
		protected MediaSource source;
		protected URI uri;
		protected MediaType type;
		protected MediaFormat format;
		protected MediaQuality quality;
		protected long size;
		protected MediaMetadata metadata;
		/** @since 00.02.08 */
		protected Media parent;
		protected List<Media.Builder<?, ?>> media;
		
		protected Builder() {
			source = MediaSource.none();
			type = MediaType.UNKNOWN;
			format = MediaFormat.UNKNOWN;
			quality = MediaQuality.UNKNOWN;
			size = MediaConstants.UNKNOWN_SIZE;
			metadata = MediaMetadata.empty();
			parent = null;
			media = null;
		}
		
		@SuppressWarnings("unchecked")
		protected final T t(Media m) { return (T) m; }
		@SuppressWarnings("unchecked")
		protected final B b(Builder<T, B> b) { return (B) b; }
		
		@Override
		public T build() {
			return t(new SimpleMediaContainer(
				Objects.requireNonNull(source), uri, Objects.requireNonNull(type),
				Objects.requireNonNull(format), Objects.requireNonNull(quality), size,
				Objects.requireNonNull(metadata), parent,
				new SimpleChildMediaBuilderContext(media == null ? List.of() : media)
			));
		}
		
		@Override
		public B source(MediaSource source) {
			this.source = Objects.requireNonNull(source);
			return b(this);
		}
		
		@Override
		public B uri(URI uri) {
			this.uri = uri;
			return b(this);
		}
		
		@Override
		public B type(MediaType type) {
			this.type = Objects.requireNonNull(type);
			return b(this);
		}
		
		@Override
		public B format(MediaFormat format) {
			this.format = Objects.requireNonNull(format);
			return b(this);
		}
		
		@Override
		public B quality(MediaQuality quality) {
			this.quality = Objects.requireNonNull(quality);
			return b(this);
		}
		
		@Override
		public B size(long size) {
			this.size = size;
			return b(this);
		}
		
		@Override
		public B metadata(MediaMetadata metadata) {
			this.metadata = Objects.requireNonNull(metadata);
			return b(this);
		}
		
		@Override
		public B parent(Media parent) {
			if(parent == this) {
				throw new IllegalArgumentException("Invalid parent.");
			}
			
			this.parent = parent;
			return b(this);
		}
		
		@Override
		public B media(List<? extends Media.Builder<?, ?>> media) {
			this.media = Utils.cast(Objects.requireNonNull(Utils.nonNullContent(media)));
			return b(this);
		}
		
		@Override
		public B media(Media.Builder<?, ?>... media) {
			return media(List.of(media));
		}
		
		@Override
		public B addMedia(List<? extends Media.Builder<?, ?>> media) {
			if(this.media == null) {
				this.media = new ArrayList<>();
			}
			
			this.media.addAll(media);
			return b(this);
		}
		
		@Override
		public B addMedia(Media.Builder<?, ?>... media) {
			return addMedia(List.of(media));
		}
		
		@Override
		public B removeMedia(List<? extends Media.Builder<?, ?>> media) {
			if(this.media != null) {
				this.media.removeAll(media);
			}
			
			return b(this);
		}
		
		@Override
		public B removeMedia(Media.Builder<?, ?>... media) {
			return removeMedia(List.of(media));
		}
		
		@Override
		public MediaSource source() {
			return source;
		}
		
		@Override
		public URI uri() {
			return uri;
		}
		
		@Override
		public MediaType type() {
			return type;
		}
		
		@Override
		public MediaFormat format() {
			return format;
		}
		
		@Override
		public MediaQuality quality() {
			return quality;
		}
		
		@Override
		public long size() {
			return size;
		}
		
		@Override
		public MediaMetadata metadata() {
			return metadata;
		}
		
		@Override
		public Media parent() {
			return parent;
		}
		
		@Override
		public List<Media.Builder<?, ?>> media() {
			return media;
		}
	}
}