package sune.app.mediadown.media;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** @since 00.02.05 */
public class CombinedMediaContainer extends SimpleMediaContainer {
	
	protected CombinedMediaContainer(MediaSource source, URI uri, MediaType type, MediaFormat format, MediaQuality quality,
	        long size, MediaMetadata metadata, List<Media> media) {
		super(source, uri, type, format, quality, size, metadata, media);
	}
	
	@Override
	public boolean isCombined() {
		return true;
	}
	
	public static class Builder<T extends CombinedMediaContainer, B extends Builder<T, B>>
			extends SimpleMediaContainer.Builder<T, B> {
		
		// Children of combined media have the same source and URI.
		protected Media.Builder<?, ?> imprintChild(Media.Builder<?, ?> media) {
			media.source(source);
			media.uri(uri);
			return media;
		}
		
		protected void imprintSelf(Media.Builder<?, ?> media) {
			if(media == null) return; // Nothing to imprint from
			if(source.isEmpty() && !media.source().isEmpty()) source(media.source());
			if(uri == null && media.uri() != null) uri(media.uri());
			if(format.is(MediaFormat.UNKNOWN) && !media.format().is(MediaFormat.UNKNOWN)) format(media.format());
			if(quality.is(MediaQuality.UNKNOWN) && !media.quality().is(MediaQuality.UNKNOWN)) quality(media.quality());
			if(metadata.isEmpty() && !media.metadata().isEmpty()) metadata(media.metadata());
		}
		
		@Override
		protected List<Media> buildMedia(List<sune.app.mediadown.media.Media.Builder<?, ?>> media) {
			return media.stream().map(this::imprintChild).map(Media.Builder::build).collect(Collectors.toList());
		}
		
		@Override
		public T build() {
			imprintSelf(media.stream().filter((m) -> m.type().is(type)).findFirst().orElse(null));
			return t(new CombinedMediaContainer(Objects.requireNonNull(source), uri, Objects.requireNonNull(type),
			                                    Objects.requireNonNull(format), Objects.requireNonNull(quality),
			                                    size, Objects.requireNonNull(metadata), buildMedia(media)));
		}
	}
}