package sune.app.mediadown.media;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import sune.app.mediadown.util.Utils;

/** @since 00.02.05 */
public class SeparatedMediaContainer extends SimpleMediaContainer {
	
	protected SeparatedMediaContainer(MediaSource source, URI uri, MediaType type, MediaFormat format, MediaQuality quality,
	        long size, MediaMetadata metadata, List<Media> media) {
		super(source, uri, type, format, quality, size, metadata, media);
	}
	
	@Override
	public boolean isSeparated() {
		return true;
	}
	
	public static class Builder<T extends SeparatedMediaContainer, B extends Builder<T, B>>
			extends SimpleMediaContainer.Builder<T, B> {
		
		protected void imprintSelf(Media.Builder<?, ?> media) {
			if(media == null) return; // Nothing to imprint from
			if(source.isEmpty() && !media.source().isEmpty()) source(media.source());
			if(uri == null && media.uri() != null) uri(media.uri());
			if(format.is(MediaFormat.UNKNOWN) && !media.format().is(MediaFormat.UNKNOWN)) format(media.format());
			if(quality.is(MediaQuality.UNKNOWN) && !media.quality().is(MediaQuality.UNKNOWN)) quality(media.quality());
			if(metadata.isEmpty() && !media.metadata().isEmpty()) metadata(media.metadata());
		}
		
		@Override
		public T build() {
			imprintSelf(media.stream().filter((m) -> m.type().is(type)).findFirst().orElse(null));
			return t(new SeparatedMediaContainer(Objects.requireNonNull(source), uri, Objects.requireNonNull(type),
			                                     Objects.requireNonNull(format), Objects.requireNonNull(quality),
			                                     size, Objects.requireNonNull(metadata), buildMedia(media)));
		}
		
		@Override
		public B media(Media.Builder<?, ?>... media) {
			if(Objects.requireNonNull(Utils.nonNullContent(media)).length <= 1)
				throw new IllegalArgumentException("Separated media must contain at least two non-null media instances");
			return super.media(media);
		}
	}
}