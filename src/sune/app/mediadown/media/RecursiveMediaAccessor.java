package sune.app.mediadown.media;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import sune.app.mediadown.util.Singleton;
import sune.app.mediadown.util.Utils;

/** @since 00.02.05 */
public class RecursiveMediaAccessor implements MediaAccessor {
	
	private final List<Media> media;
	
	public RecursiveMediaAccessor(List<Media> media) {
		this.media = Objects.requireNonNull(media);
	}
	
	private final Stream<Media> streamOfType(MediaType type) {
		return media.stream()
				 .flatMap((m) -> m.isContainer()
				                     ? ((MediaContainer) m).recursive().allOfType(type).stream()
				                     : Stream.of(m))
				 .filter((m) -> m.isSingle() && m.type().is(type));
	}
	
	private final Stream<Media> streamContainersOfType(MediaType type) {
		return media.stream()
				 .flatMap((m) -> m.isContainer()
				                     ? Stream.concat(Stream.of(m), ((MediaContainer) m).recursive()
				                                                                       .allContainersOfType(type)
				                                                                       .stream())
				                     : Stream.of(m))
				 .filter((m) -> m.isContainer() && m.type().is(type));
	}
	
	@Override
	public <T extends Media> List<T> allOfType(MediaType type) {
		return Utils.streamToUnmodifiableList(streamOfType(type));
	}
	
	@Override
	public <T extends MediaContainer> List<T> allContainersOfType(MediaType type) {
		return Utils.streamToUnmodifiableList(streamContainersOfType(type));
	}
	
	@Override
	public <T extends Media> T ofType(MediaType type) {
		@SuppressWarnings("unchecked")
		T result = (T) streamOfType(type).findFirst().orElse(null);
		return result;
	}
	
	@Override
	public <T extends MediaContainer> T containerOfType(MediaType type) {
		@SuppressWarnings("unchecked")
		T result = (T) streamContainersOfType(type).findFirst().orElse(null);
		return result;
	}
	
	@Override
	public MediaAccessor recursive() {
		return this;
	}
	
	@Override
	public MediaAccessor direct() {
		return Singleton.of(this, () -> new DirectMediaAccessor(media));
	}
	
	@Override
	public List<Media> media() {
		return Collections.unmodifiableList(media);
	}
}