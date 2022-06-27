package sune.app.mediadown.media;

import java.util.List;

/** @since 00.02.05 */
public interface AudioMediaBase {
	
	MediaLanguage language();
	double duration();
	List<String> codecs();
	int bandwidth();
	int sampleRate();
	
	public static interface Builder<T extends AudioMediaBase, B extends AudioMediaBase.Builder<T, B>> {
		
		Builder<T, B> language(MediaLanguage language);
		Builder<T, B> duration(double duration);
		Builder<T, B> codecs(List<String> codecs);
		Builder<T, B> bandwidth(int bandwidth);
		Builder<T, B> sampleRate(int sampleRate);
		
		MediaLanguage language();
		double duration();
		List<String> codecs();
		int bandwidth();
		int sampleRate();
	}
}