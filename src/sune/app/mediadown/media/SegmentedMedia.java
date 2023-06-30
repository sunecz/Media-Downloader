package sune.app.mediadown.media;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import sune.app.mediadown.download.segment.FileSegmentsHolder;
import sune.app.mediadown.util.Utils;

/** @since 00.02.05 */
public class SegmentedMedia extends SimpleMedia {
	
	protected final transient List<FileSegmentsHolder<?>> segments;
	
	public SegmentedMedia(MediaSource source, URI uri, MediaType type, MediaFormat format, MediaQuality quality,
			long size, MediaMetadata metadata, Media parent, List<FileSegmentsHolder<?>> segments) {
		super(source, uri, type, format, quality, size, metadata, parent);
		this.segments = Collections.unmodifiableList(Objects.requireNonNull(segments));
	}
	
	public List<FileSegmentsHolder<?>> segments() {
		return segments;
	}
	
	@Override
	public boolean isSegmented() {
		return true;
	}
	
	public static class Builder<T extends SegmentedMedia, B extends Builder<T, B>>
			extends SimpleMedia.Builder<T, B> {
		
		protected List<FileSegmentsHolder<?>> segments;
		
		public Builder() {
			segments = List.of();
		}
		
		@Override
		public T build() {
			return t(new SegmentedMedia(
				Objects.requireNonNull(source), uri, Objects.requireNonNull(type),
				Objects.requireNonNull(format), Objects.requireNonNull(quality),
				size, Objects.requireNonNull(metadata), parent, Objects.requireNonNull(segments)
			));
		}
		
		public B segments(List<FileSegmentsHolder<?>> segments) {
			this.segments = Objects.requireNonNull(Utils.nonNullContent(segments));
			return b(this);
		}
		
		public B segments(FileSegmentsHolder<?>... segments) {
			return segments(List.of(Objects.requireNonNull(segments)));
		}
		
		public List<FileSegmentsHolder<?>> segments() {
			return segments;
		}
	}
}