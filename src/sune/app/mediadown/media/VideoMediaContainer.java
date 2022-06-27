package sune.app.mediadown.media;

import sune.app.mediadown.media.type.CombinedVideoMediaContainer;
import sune.app.mediadown.media.type.SeparatedVideoMediaContainer;

/** @since 00.02.05 */
public interface VideoMediaContainer extends MediaContainer, VideoMediaBase {
	
	public static CombinedVideoMediaContainer.Builder combined() {
		return CombinedVideoMediaContainer.builder();
	}
	
	public static SeparatedVideoMediaContainer.Builder separated() {
		return SeparatedVideoMediaContainer.builder();
	}
	
	public static interface Builder<T extends VideoMediaContainer,
	                                B extends MediaContainer.Builder<T, B> & VideoMediaBase.Builder<T, B>>
			extends MediaContainer.Builder<T, B>, VideoMediaBase.Builder<T, B> {
	}
}