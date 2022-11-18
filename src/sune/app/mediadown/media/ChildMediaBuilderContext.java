package sune.app.mediadown.media;

import java.util.List;
import java.util.Objects;

/** @since 00.02.08 */
// Package-private
abstract class ChildMediaBuilderContext {
	
	protected final List<? extends Media.Builder<?, ?>> media;
	
	public ChildMediaBuilderContext(List<? extends Media.Builder<?, ?>> media) {
		this.media = Objects.requireNonNull(media);
	}
	
	public abstract List<Media> build(Media parent);
}