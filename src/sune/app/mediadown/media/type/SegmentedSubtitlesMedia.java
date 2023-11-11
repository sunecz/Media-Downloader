package sune.app.mediadown.media.type;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import sune.app.mediadown.download.segment.FileSegmentsHolder;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaLanguage;
import sune.app.mediadown.media.MediaMetadata;
import sune.app.mediadown.media.MediaQuality;
import sune.app.mediadown.media.MediaSource;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.media.SegmentedMedia;
import sune.app.mediadown.media.SubtitlesMedia;

/** @since 00.02.09 */
public class SegmentedSubtitlesMedia extends SegmentedMedia implements SubtitlesMedia {
	
	public static final MediaType MEDIA_TYPE = MediaType.SUBTITLES;
	
	protected final MediaLanguage language;
	
	protected SegmentedSubtitlesMedia(MediaSource source, URI uri, MediaFormat format, long size,
			MediaMetadata metadata, Media parent, List<FileSegmentsHolder<?>> segments, MediaLanguage language) {
		super(source, uri, MEDIA_TYPE, checkFormat(format), MediaQuality.UNKNOWN, size, metadata, parent,
		      Objects.requireNonNull(segments));
		this.language = Objects.requireNonNull(language);
	}
	
	private static final MediaFormat checkFormat(MediaFormat format) {
		if(!isValidFormat(format))
			throw new IllegalArgumentException("Invalid subtitles format");
		return format;
	}
	
	public static final boolean isValidFormat(MediaFormat format) {
		return format.is(MediaFormat.UNKNOWN) || format.mediaType().is(MEDIA_TYPE);
	}
	
	public static final Builder builder() {
		return new Builder();
	}
	
	@Override
	public MediaLanguage language() {
		return language;
	}
	
	public static final class Builder extends SegmentedMedia.Builder<SegmentedSubtitlesMedia, Builder>
			implements SubtitlesMedia.Builder<SegmentedSubtitlesMedia, Builder> {
		
		private MediaLanguage language;
		
		public Builder() {
			type = MEDIA_TYPE;
			language = MediaLanguage.UNKNOWN;
		}
		
		@Override
		public SegmentedSubtitlesMedia build() {
			return new SegmentedSubtitlesMedia(
				Objects.requireNonNull(source), uri,
				Objects.requireNonNull(format),
				size, Objects.requireNonNull(metadata), parent,
				Objects.requireNonNull(segments),
				Objects.requireNonNull(language)
			);
		}
		
		@Override
		public Builder type(MediaType type) {
			throw new UnsupportedOperationException("Cannot set media type");
		}
		
		@Override
		public Builder format(MediaFormat format) {
			return super.format(checkFormat(format));
		}
		
		@Override
		public Builder quality(MediaQuality quality) {
			throw new UnsupportedOperationException("Cannot set media quality");
		}
		
		@Override
		public Builder language(MediaLanguage language) {
			this.language = Objects.requireNonNull(language);
			return this;
		}
		
		@Override
		public MediaLanguage language() {
			return language;
		}
	}
}