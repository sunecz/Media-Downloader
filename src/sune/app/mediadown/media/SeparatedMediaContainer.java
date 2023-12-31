package sune.app.mediadown.media;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import sune.app.mediadown.util.Utils;

/** @since 00.02.05 */
public class SeparatedMediaContainer extends SimpleMediaContainer {
	
	protected SeparatedMediaContainer(MediaSource source, URI uri, MediaType type, MediaFormat format, MediaQuality quality,
	        long size, MediaMetadata metadata, Media parent, ChildMediaBuilderContext builderContext) {
		super(source, uri, type, format, quality, size, metadata, parent, builderContext);
	}
	
	@Override
	public boolean isSeparated() {
		return true;
	}
	
	/** @since 00.02.08 */
	protected static class SeparatedChildMediaBuilderContext extends SimpleChildMediaBuilderContext {
		
		public SeparatedChildMediaBuilderContext(List<? extends Media.Builder<?, ?>> media) {
			super(media);
		}
	}
	
	public static class Builder<T extends SeparatedMediaContainer, B extends Builder<T, B>>
			extends SimpleMediaContainer.Builder<T, B> {
		
		protected Builder() {
		}
		
		protected void imprintSelf(Media.Builder<?, ?> media) {
			if(source.isEmpty() && !media.source().isEmpty()) source(media.source());
			if(uri == null && media.uri() != null) uri(media.uri());
			
			if(media.type().is(type)) {
				if(format.is(MediaFormat.UNKNOWN) && !media.format().is(MediaFormat.UNKNOWN)) format(media.format());
				if(quality.is(MediaQuality.UNKNOWN) && !media.quality().is(MediaQuality.UNKNOWN)) quality(media.quality());
			}
			
			metadata(MediaUtils.mergeMetadata(metadata, media.metadata()));
		}
		
		@Override
		public T build() {
			media.stream().forEach(this::imprintSelf);
			return t(new SeparatedMediaContainer(
				Objects.requireNonNull(source), uri, Objects.requireNonNull(type),
				Objects.requireNonNull(format), Objects.requireNonNull(quality), size,
				Objects.requireNonNull(metadata), parent,
				new SeparatedChildMediaBuilderContext(media)
			));
		}
		
		@Override
		public B media(Media.Builder<?, ?>... media) {
			if(Objects.requireNonNull(Utils.nonNullContent(media)).length <= 1)
				throw new IllegalArgumentException("Separated media must contain at least two non-null media instances");
			return super.media(media);
		}
	}
}