package sune.app.mediadown.media;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import sune.app.mediadown.util.Singleton;
import sune.app.mediadown.util.Utils;

/** @since 00.02.05 */
public class DirectMediaAccessor implements MediaAccessor {
	
	private final List<Media> media;
	
	public DirectMediaAccessor(List<Media> media) {
		this.media = Objects.requireNonNull(media);
	}
	
	private final Stream<Media> streamOfType(MediaType type) {
		return media.stream().filter((m) -> m.isSingle() && m.type().is(type));
	}
	
	private final Stream<Media> streamContainersOfType(MediaType type) {
		return media.stream().filter((m) -> m.isContainer() && m.type().is(type));
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
		return Singleton.of(this, () -> new RecursiveMediaAccessor(media));
	}
	
	@Override
	public MediaAccessor direct() {
		return this;
	}
	
	@Override
	public List<Media> media() {
		return Collections.unmodifiableList(media);
	}
}