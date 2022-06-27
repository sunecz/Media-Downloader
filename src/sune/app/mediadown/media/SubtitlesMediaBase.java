package sune.app.mediadown.media;

/** @since 00.02.05 */
public interface SubtitlesMediaBase {
	
	MediaLanguage language();
	
	public static interface Builder<T extends SubtitlesMediaBase, B extends SubtitlesMediaBase.Builder<T, B>> {
		
		Builder<T, B> language(MediaLanguage language);
		MediaLanguage language();
	}
}