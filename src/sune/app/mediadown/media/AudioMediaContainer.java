package sune.app.mediadown.media;

import sune.app.mediadown.media.type.CombinedAudioMediaContainer;
import sune.app.mediadown.media.type.SeparatedAudioMediaContainer;

/** @since 00.02.05 */
public interface AudioMediaContainer extends MediaContainer, AudioMediaBase {
	
	public static CombinedAudioMediaContainer.Builder combined() {
		return CombinedAudioMediaContainer.builder();
	}
	
	public static SeparatedAudioMediaContainer.Builder separated() {
		return SeparatedAudioMediaContainer.builder();
	}
	
	public static interface Builder<T extends AudioMediaContainer,
	                                B extends MediaContainer.Builder<T, B> & AudioMediaBase.Builder<T, B>>
			extends MediaContainer.Builder<T, B>, AudioMediaBase.Builder<T, B> {
	}
}