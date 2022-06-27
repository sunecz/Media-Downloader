package sune.app.mediadown.media;

import java.util.List;

/** @since 00.02.05 */
public interface VideoMediaBase {
	
	MediaResolution resolution();
	double duration();
	List<String> codecs();
	int bandwidth();
	double frameRate();
	
	public static interface Builder<T extends VideoMediaBase, B extends VideoMediaBase.Builder<T, B>> {
		
		Builder<T, B> resolution(MediaResolution resolution);
		Builder<T, B> duration(double duration);
		Builder<T, B> codecs(List<String> codecs);
		Builder<T, B> bandwidth(int bandwidth);
		Builder<T, B> frameRate(double frameRate);
		
		MediaResolution resolution();
		double duration();
		List<String> codecs();
		int bandwidth();
		double frameRate();
	}
}