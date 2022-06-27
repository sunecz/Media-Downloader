package sune.app.mediadown.search;

/** @since 00.01.17 */
public class SearchOptions {
	
	private static SearchOptions DEFAULT_OPTIONS;
	public  static SearchOptions getDefault() {
		return DEFAULT_OPTIONS == null ? DEFAULT_OPTIONS = new SearchOptions() : DEFAULT_OPTIONS;
	}
}