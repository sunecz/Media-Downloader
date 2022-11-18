package sune.app.mediadown.media.type;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaConstants;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaMetadata;
import sune.app.mediadown.media.MediaQuality;
import sune.app.mediadown.media.MediaResolution;
import sune.app.mediadown.media.MediaSource;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.media.SeparatedMediaContainer;
import sune.app.mediadown.media.VideoMediaBase;
import sune.app.mediadown.media.VideoMediaContainer;

/** @since 00.02.05 */
public class SeparatedVideoMediaContainer extends SeparatedMediaContainer implements VideoMediaContainer {
	
	public static final MediaType MEDIA_TYPE = MediaType.VIDEO;
	
	protected final MediaResolution resolution;
	protected final double duration;
	protected final List<String> codecs;
	protected final int bandwidth;
	protected final double frameRate;
	
	protected SeparatedVideoMediaContainer(MediaSource source, URI uri, MediaType type, MediaFormat format,
			MediaQuality quality, long size, MediaMetadata metadata, Media parent,
			SeparatedChildMediaBuilderContext builderContext, MediaResolution resolution, double duration,
			List<String> codecs, int bandwidth, double frameRate) {
		super(source, uri, MEDIA_TYPE, checkFormat(format), checkQuality(quality), size, metadata, parent,
		      builderContext);
		this.resolution = Objects.requireNonNull(resolution);
		this.duration = duration;
		this.codecs = Objects.requireNonNull(codecs);
		this.bandwidth = bandwidth;
		this.frameRate = frameRate;
	}
	
	private static final MediaFormat checkFormat(MediaFormat format) {
		if(!isValidFormat(format))
			throw new IllegalArgumentException("Invalid video format");
		return format;
	}
	
	private static final MediaQuality checkQuality(MediaQuality quality) {
		if(!isValidQuality(quality))
			throw new IllegalArgumentException("Invalid video quality");
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
	public MediaResolution resolution() {
		return resolution;
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
	public double frameRate() {
		return frameRate;
	}
	
	public static class Builder extends SeparatedMediaContainer.Builder<SeparatedVideoMediaContainer, Builder>
			implements VideoMediaContainer.Builder<SeparatedVideoMediaContainer, Builder> {

		private MediaResolution resolution;
		private double duration;
		private List<String> codecs;
		private int bandwidth;
		private double frameRate;
		
		public Builder() {
			type = MEDIA_TYPE;
			resolution = MediaResolution.UNKNOWN;
			duration = MediaConstants.UNKNOWN_DURATION;
			codecs = List.of();
		}
		
		protected void imprintSelf(Media.Builder<?, ?> media) {
			if(media == null) return; // Nothing to imprint from
			super.imprintSelf(media);
			VideoMediaBase.Builder<?, ?> video = (VideoMediaBase.Builder<?, ?>) media;
			if(resolution == MediaResolution.UNKNOWN && video.resolution() != MediaResolution.UNKNOWN) resolution(video.resolution());
			if(duration == MediaConstants.UNKNOWN_DURATION && video.duration() != MediaConstants.UNKNOWN_DURATION) duration(video.duration());
			if(codecs.isEmpty() && !video.codecs().isEmpty()) codecs(video.codecs());
			if(bandwidth == 0 && video.bandwidth() != 0) bandwidth(video.bandwidth());
			if(frameRate == 0.0 && video.frameRate() != 0.0) frameRate(video.frameRate());
		}
		
		@Override
		public SeparatedVideoMediaContainer build() {
			imprintSelf(media.stream().filter((m) -> m.type().is(type)).findFirst().orElse(null));
			return new SeparatedVideoMediaContainer(
				Objects.requireNonNull(source), uri, Objects.requireNonNull(type),
				Objects.requireNonNull(format), Objects.requireNonNull(quality),
				size, Objects.requireNonNull(metadata), parent,
				new SeparatedChildMediaBuilderContext(media), Objects.requireNonNull(resolution),
				duration, Objects.requireNonNull(codecs), bandwidth, frameRate
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
		public Builder resolution(MediaResolution resolution) {
			this.resolution = Objects.requireNonNull(resolution);
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
		public Builder frameRate(double frameRate) {
			this.frameRate = frameRate;
			return this;
		}
		
		@Override
		public MediaResolution resolution() {
			return resolution;
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
		public double frameRate() {
			return frameRate;
		}
	}
}