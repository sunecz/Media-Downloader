package sune.app.mediadown.media;

import sune.app.mediadown.media.type.SegmentedAudioMedia;
import sune.app.mediadown.media.type.SimpleAudioMedia;

/** @since 00.02.05 */
public interface AudioMedia extends Media, AudioMediaBase {
	
	public static SimpleAudioMedia.Builder simple() {
		return SimpleAudioMedia.builder();
	}
	
	public static SegmentedAudioMedia.Builder segmented() {
		return SegmentedAudioMedia.builder();
	}
	
	public static interface Builder<T extends AudioMedia,
	                                B extends Media.Builder<T, B> & AudioMediaBase.Builder<T, B>>
			extends Media.Builder<T, B>, AudioMediaBase.Builder<T, B> {
	}
}