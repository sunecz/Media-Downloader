package sune.app.mediadown.media;

import java.util.List;

/** @since 00.02.05 */
public interface MediaContainer extends Media, MediaAccessor {
	
	boolean isSeparated();
	default boolean isCombined() {
		return !isSeparated();
	}
	
	public static interface Builder<T extends Media, B extends Builder<T, B>> extends Media.Builder<T, B> {
		
		B media(List<Media.Builder<?, ?>> media);
		B media(Media.Builder<?, ?>... media);
		
		List<Media.Builder<?, ?>> media();
	}
}