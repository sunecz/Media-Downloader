package sune.app.mediadown.media;

import java.net.URI;
import java.util.Objects;

import sune.app.mediadown.download.segment.FileSegmentsHolder;

/** @since 00.02.05 */
public class SegmentedMedia extends SimpleMedia {
	
	protected final FileSegmentsHolder segments;
	
	public SegmentedMedia(MediaSource source, URI uri, MediaType type, MediaFormat format, MediaQuality quality,
			long size, MediaMetadata metadata, Media parent, FileSegmentsHolder segments) {
		super(source, uri, type, format, quality, size, metadata, parent);
		this.segments = Objects.requireNonNull(segments);
	}
	
	public FileSegmentsHolder segments() {
		return segments;
	}
	
	@Override
	public boolean isSegmented() {
		return true;
	}
	
	public static class Builder<T extends SegmentedMedia, B extends Builder<T, B>>
			extends SimpleMedia.Builder<T, B> {
		
		protected FileSegmentsHolder segments;
		
		protected Builder() {
		}
		
		@Override
		public T build() {
			return t(new SegmentedMedia(
				Objects.requireNonNull(source), uri, Objects.requireNonNull(type),
				Objects.requireNonNull(format), Objects.requireNonNull(quality),
				size, Objects.requireNonNull(metadata), parent, Objects.requireNonNull(segments)
			));
		}
		
		public B segments(FileSegmentsHolder segments) {
			this.segments = Objects.requireNonNull(segments);
			return b(this);
		}
		
		public FileSegmentsHolder segments() {
			return segments;
		}
	}
}