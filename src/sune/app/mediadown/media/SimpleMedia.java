package sune.app.mediadown.media;

import java.net.URI;
import java.util.Objects;

/** @since 00.02.05 */
public class SimpleMedia implements Media {
	
	protected final MediaSource source;
	protected final URI uri;
	protected final MediaType type;
	protected final MediaFormat format;
	protected final MediaQuality quality;
	protected final long size;
	protected final MediaMetadata metadata;
	/** @since 00.02.08 */
	protected final Media parent;
	
	protected SimpleMedia(MediaSource source, URI uri, MediaType type, MediaFormat format, MediaQuality quality,
			long size, MediaMetadata metadata, Media parent) {
		this.source = Objects.requireNonNull(source);
		this.uri = uri;
		this.type = Objects.requireNonNull(type);
		this.format = Objects.requireNonNull(format);
		this.quality = Objects.requireNonNull(quality);
		this.size = size;
		this.metadata = Objects.requireNonNull(metadata);
		this.parent = parent;
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
		return false;
	}
	
	@Override
	public boolean isSegmented() {
		return false;
	}
	
	@Override
	public boolean isSingle() {
		return true;
	}
	
	@Override
	public String toString() {
		return "Media["
					+ "class=" + getClass().getSimpleName() + ", "
					+ "source=" + source + ", "
					+ "type=" + type + ", "
					+ "format=" + format + ", "
					+ "quality=" + quality + ", "
					+ "uri=" + uri + ", "
					+ "size=" + size + ", "
					+ "metadata=" + metadata + ", "
					+ "parent=" + parent
		        + "]";
	}
	
	public static class Builder<T extends SimpleMedia, B extends Builder<T, B>> implements Media.Builder<T, B> {
		
		protected MediaSource source;
		protected URI uri;
		protected MediaType type;
		protected MediaFormat format;
		protected MediaQuality quality;
		protected long size;
		protected MediaMetadata metadata;
		/** @since 00.02.08 */
		protected Media parent;
		
		public Builder() {
			source = MediaSource.none();
			type = MediaType.UNKNOWN;
			format = MediaFormat.UNKNOWN;
			quality = MediaQuality.UNKNOWN;
			size = MediaConstants.UNKNOWN_SIZE;
			metadata = MediaMetadata.empty();
			parent = null;
		}
		
		@SuppressWarnings("unchecked")
		protected final T t(Media m) { return (T) m; }
		@SuppressWarnings("unchecked")
		protected final B b(Builder<T, B> b) { return (B) b; }
		
		@Override
		public T build() {
			return t(new SimpleMedia(
				Objects.requireNonNull(source), uri, Objects.requireNonNull(type),
				Objects.requireNonNull(format), Objects.requireNonNull(quality), size,
				Objects.requireNonNull(metadata), parent
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
	}
}