package sune.app.mediadown.media.type;

import java.net.URI;
import java.util.Objects;

import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaLanguage;
import sune.app.mediadown.media.MediaMetadata;
import sune.app.mediadown.media.MediaQuality;
import sune.app.mediadown.media.MediaSource;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.media.SimpleMedia;
import sune.app.mediadown.media.SubtitlesMedia;

/** @since 00.02.05 */
public class SimpleSubtitlesMedia extends SimpleMedia implements SubtitlesMedia {
	
	public static final MediaType MEDIA_TYPE = MediaType.SUBTITLES;
	
	protected final MediaLanguage language;
	
	protected SimpleSubtitlesMedia(MediaSource source, URI uri, MediaType type, MediaFormat format, MediaQuality quality,
			long size, MediaMetadata metadata, Media parent, MediaLanguage language) {
		super(source, uri, MEDIA_TYPE, checkFormat(format), checkQuality(quality), size, metadata, parent);
		this.language = Objects.requireNonNull(language);
	}
	
	private static final MediaFormat checkFormat(MediaFormat format) {
		if(!isValidFormat(format))
			throw new IllegalArgumentException("Invalid subtitles format");
		return format;
	}
	
	private static final MediaQuality checkQuality(MediaQuality quality) {
		if(!isValidQuality(quality))
			throw new IllegalArgumentException("Invalid subtitles quality");
		return quality;
	}
	
	public static final boolean isValidFormat(MediaFormat format) {
		return format.is(MediaFormat.UNKNOWN) || format.mediaType().is(MEDIA_TYPE);
	}
	
	public static final boolean isValidQuality(MediaQuality quality) {
		return quality.is(MediaQuality.UNKNOWN) || quality.mediaType().is(MEDIA_TYPE);
	}
	
	public static final Builder builder() {
		return new Builder();
	}
	
	@Override
	public MediaLanguage language() {
		return language;
	}
	
	public static final class Builder extends SimpleMedia.Builder<SimpleSubtitlesMedia, Builder>
			implements SubtitlesMedia.Builder<SimpleSubtitlesMedia, Builder> {
		
		private MediaLanguage language;
		
		public Builder() {
			type = MEDIA_TYPE;
			language = MediaLanguage.UNKNOWN;
		}
		
		@Override
		public SimpleSubtitlesMedia build() {
			return new SimpleSubtitlesMedia(
				Objects.requireNonNull(source), uri, MEDIA_TYPE,
				Objects.requireNonNull(format), Objects.requireNonNull(quality),
				size, Objects.requireNonNull(metadata), parent,
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
			return super.quality(checkQuality(quality));
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