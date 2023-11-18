package sune.app.mediadown.media.type;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import sune.app.mediadown.download.segment.FileSegmentsHolder;
import sune.app.mediadown.media.AudioMedia;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaConstants;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaLanguage;
import sune.app.mediadown.media.MediaMetadata;
import sune.app.mediadown.media.MediaQuality;
import sune.app.mediadown.media.MediaSource;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.media.SegmentedMedia;

/** @since 00.02.05 */
public class SegmentedAudioMedia extends SegmentedMedia implements AudioMedia {
	
	public static final MediaType MEDIA_TYPE = MediaType.AUDIO;
	
	protected final MediaLanguage language;
	protected final double duration;
	protected final List<String> codecs;
	protected final int bandwidth;
	protected final int sampleRate;
	
	protected SegmentedAudioMedia(MediaSource source, URI uri, MediaType type, MediaFormat format, MediaQuality quality,
			long size, MediaMetadata metadata, Media parent, List<FileSegmentsHolder<?>> segments,
			MediaLanguage language, double duration, List<String> codecs, int bandwidth, int sampleRate) {
		super(source, uri, MEDIA_TYPE, checkFormat(format), checkQuality(quality), size, metadata, parent,
		      Objects.requireNonNull(segments));
		this.language = Objects.requireNonNull(language);
		this.duration = duration;
		this.codecs = Objects.requireNonNull(codecs);
		this.bandwidth = bandwidth;
		this.sampleRate = sampleRate;
	}
	
	private static final MediaFormat checkFormat(MediaFormat format) {
		if(!isValidFormat(format))
			throw new IllegalArgumentException("Invalid audio format");
		return format;
	}
	
	private static final MediaQuality checkQuality(MediaQuality quality) {
		if(!isValidQuality(quality))
			throw new IllegalArgumentException("Invalid audio quality");
		return quality;
	}
	
	protected static final boolean isValidFormat(MediaFormat format) {
		return format.is(MediaFormat.UNKNOWN) || format.mediaType().is(MEDIA_TYPE);
	}
	
	protected static final boolean isValidQuality(MediaQuality quality) {
		return quality.is(MediaQuality.UNKNOWN) || quality.mediaType().is(MEDIA_TYPE);
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	@Override
	public MediaLanguage language() {
		return language;
	}
	
	@Override
	public double duration() {
		return duration;
	}
	
	@Override
	public List<String> codecs() {
		return codecs;
	}
	
	@Override
	public int bandwidth() {
		return bandwidth;
	}
	
	@Override
	public int sampleRate() {
		return sampleRate;
	}
	
	public static class Builder extends SegmentedMedia.Builder<SegmentedAudioMedia, Builder>
			implements AudioMedia.Builder<SegmentedAudioMedia, Builder> {
		
		protected MediaLanguage language;
		protected double duration;
		protected List<String> codecs;
		protected int bandwidth;
		protected int sampleRate;
		
		protected Builder() {
			type = MEDIA_TYPE;
			language = MediaLanguage.UNKNOWN;
			duration = MediaConstants.UNKNOWN_DURATION;
			codecs = List.of();
		}
		
		@Override
		public SegmentedAudioMedia build() {
			return new SegmentedAudioMedia(
				Objects.requireNonNull(source), uri, MEDIA_TYPE,
				Objects.requireNonNull(format), Objects.requireNonNull(quality),
				size, Objects.requireNonNull(metadata), parent,
				Objects.requireNonNull(segments), Objects.requireNonNull(language),
				duration, codecs, bandwidth, sampleRate
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
		public Builder duration(double duration) {
			this.duration = duration;
			return this;
		}
		
		@Override
		public Builder codecs(List<String> codecs) {
			this.codecs = Objects.requireNonNull(codecs);
			return this;
		}
		
		@Override
		public Builder bandwidth(int bandwidth) {
			this.bandwidth = bandwidth;
			return this;
		}
		
		@Override
		public Builder sampleRate(int sampleRate) {
			this.sampleRate = sampleRate;
			return this;
		}
		
		@Override
		public MediaLanguage language() {
			return language;
		}
		
		@Override
		public double duration() {
			return duration;
		}
		
		@Override
		public List<String> codecs() {
			return codecs;
		}
		
		@Override
		public int bandwidth() {
			return bandwidth;
		}
		
		@Override
		public int sampleRate() {
			return sampleRate;
		}
	}
}