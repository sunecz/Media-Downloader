package sune.app.mediadown.media;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** @since 00.02.05 */
public class CombinedMediaContainer extends SimpleMediaContainer {
	
	protected CombinedMediaContainer(MediaSource source, URI uri, MediaType type, MediaFormat format, MediaQuality quality,
	        long size, MediaMetadata metadata, Media parent, ChildMediaBuilderContext builderContext) {
		super(source, uri, type, format, quality, size, metadata, parent, builderContext);
	}
	
	@Override
	public boolean isCombined() {
		return true;
	}
	
	/** @since 00.02.08 */
	protected static class CombinedChildMediaBuilderContext extends ChildMediaBuilderContext {
		
		private Builder<?, ?> builder;
		
		public CombinedChildMediaBuilderContext(Builder<?, ?> builder, List<? extends Media.Builder<?, ?>> media) {
			super(media);
			this.builder = Objects.requireNonNull(builder);
		}
		
		// Children of combined media have the same source and URI.
		protected Media.Builder<?, ?> imprintChild(Media.Builder<?, ?> media) {
			media.source(builder.source());
			media.uri(builder.uri());
			return media;
		}
		
		@Override
		public List<Media> build(Media parent) {
			return media.stream()
					    .map((m) -> (Media.Builder<?, ?>) m.parent(parent))
					    .map(this::imprintChild)
					    .map(Media.Builder::build)
					    .collect(Collectors.toList());
		}
	}
	
	public static class Builder<T extends CombinedMediaContainer, B extends Builder<T, B>>
			extends SimpleMediaContainer.Builder<T, B> {
		
		protected Builder() {
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
		public T build() {
			imprintSelf(media.stream().filter((m) -> m.type().is(type)).findFirst().orElse(null));
			return t(new CombinedMediaContainer(
				Objects.requireNonNull(source), uri, Objects.requireNonNull(type),
				Objects.requireNonNull(format), Objects.requireNonNull(quality),
				size, Objects.requireNonNull(metadata), parent,
				new CombinedChildMediaBuilderContext(this, media)
			));
		}
	}
}