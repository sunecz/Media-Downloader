package sune.app.mediadown.media;

import sune.app.mediadown.media.type.SegmentedSubtitlesMedia;
import sune.app.mediadown.media.type.SimpleSubtitlesMedia;

/** @since 00.02.05 */
public interface SubtitlesMedia extends Media, SubtitlesMediaBase {
	
	public static SimpleSubtitlesMedia.Builder simple() {
		return SimpleSubtitlesMedia.builder();
	}
	
	/** @since 00.02.09 */
	public static SegmentedSubtitlesMedia.Builder segmented() {
		return SegmentedSubtitlesMedia.builder();
	}
	
	public static interface Builder<T extends SubtitlesMedia,
	                                B extends Media.Builder<T, B> & SubtitlesMediaBase.Builder<T, B>>
			extends Media.Builder<T, B>, SubtitlesMediaBase.Builder<T, B> {
	}
}