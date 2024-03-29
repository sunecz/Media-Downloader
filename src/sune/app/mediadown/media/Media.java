package sune.app.mediadown.media;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import sune.app.mediadown.util.Opt;
import sune.app.mediadown.util.Opt.OptMapper;

/** @since 00.02.05 */
public interface Media {
	
	MediaSource source();
	URI uri();
	MediaType type();
	MediaFormat format();
	MediaQuality quality();
	long size();
	MediaMetadata metadata();
	/** @since 00.02.08 */
	Media parent();
	boolean isContainer();
	default boolean isSingle() {
		return !isContainer();
	}
	boolean isSegmented();
	default boolean isSolid() {
		return !isSegmented();
	}
	/** @since 00.02.09 */
	boolean isVirtual();
	/** @since 00.02.09 */
	default boolean isPhysical() {
		return !isVirtual();
	}
	
	/** @since 00.02.09 */
	public static MediaContainer asContainer(Media media) {
		return media.isContainer() || media instanceof MediaContainer ? (MediaContainer) media : null;
	}
	
	/** @since 00.02.08 */
	public static Media root(Media media) {
		if(media == null) {
			return null;
		}
		
		Media root = media;
		for(Media parent; (parent = root.parent()) != null && parent != media; root = parent);
		
		return root;
	}
	
	public static <T extends Media> List<T> findAllOfType(Media media, MediaType mediaType) {
		return Opt.of(media)
				  .ifTrue((v) -> v.isSingle() && v.type().is(mediaType)).map(List::of)
				  .<Media>or((opt) -> opt.ifTrue(Media::isContainer)
				                         .map(OptMapper.of(Media::asContainer)
				                                       .then((v) -> v.allOfType(mediaType))
				                         ))
				  .<List<T>>castAny().orElseGet(List::of);
	}
	
	public static <T extends Media> List<T> findAllContainersOfType(Media media, MediaType mediaType) {
		return Opt.of(media)
				  .ifTrue((v) -> v.isContainer() && v.type().is(mediaType)).map(List::of)
				  .<Media>or((opt) -> opt.ifTrue(Media::isContainer)
				                         .map(OptMapper.of(Media::asContainer)
				                                       .then((v) -> v.allContainersOfType(mediaType).stream()
				                                                     .map((m) -> (Media) m)
				                                                     .collect(Collectors.toList()))
				                         ))
				  .<List<T>>castAny().orElseGet(List::of);
	}
	
	public static <T extends Media> T findOfType(Media media, MediaType mediaType) {
		return Opt.of(media)
				  .ifTrue((v) -> v.isSingle() && v.type().is(mediaType))
				  .<Media>or((opt) -> opt.ifTrue(Media::isContainer)
				                         .map(OptMapper.of(Media::asContainer)
				                                       .then((v) -> v.ofType(mediaType))
				                         ))
				  .<T>cast().get();
	}
	
	public static <T extends Media> T findContainerOfType(Media media, MediaType mediaType) {
		return Opt.of(media)
				  .ifTrue((v) -> v.isContainer() && v.type().is(mediaType))
				  .<Media>or((opt) -> opt.ifTrue(Media::isContainer)
				                         .map(OptMapper.of(Media::asContainer)
				                                       .then((v) -> v.containerOfType(mediaType))
				                         ))
				  .<T>cast().get();
	}
	
	public static interface Builder<T extends Media, B extends Builder<T, B>> {
		
		T build();
		
		B source(MediaSource source);
		B uri(URI uri);
		B type(MediaType type);
		B format(MediaFormat format);
		B quality(MediaQuality quality);
		B size(long size);
		B metadata(MediaMetadata metadata);
		/** @since 00.02.08 */
		B parent(Media parent);
		
		MediaSource source();
		URI uri();
		MediaType type();
		MediaFormat format();
		MediaQuality quality();
		long size();
		MediaMetadata metadata();
		/** @since 00.02.08 */
		Media parent();
	}
}