package sune.app.mediadown.media;

import sune.app.mediadown.media.type.SegmentedVideoMedia;
import sune.app.mediadown.media.type.SimpleVideoMedia;

/** @since 00.02.05 */
public interface VideoMedia extends Media, VideoMediaBase {
	
	public static SimpleVideoMedia.Builder simple() {
		return SimpleVideoMedia.builder();
	}
	
	public static SegmentedVideoMedia.Builder segmented() {
		return SegmentedVideoMedia.builder();
	}
	
	public static interface Builder<T extends VideoMedia,
	                                B extends Media.Builder<T, B> & VideoMediaBase.Builder<T, B>>
			extends Media.Builder<T, B>, VideoMediaBase.Builder<T, B> {
	}
}