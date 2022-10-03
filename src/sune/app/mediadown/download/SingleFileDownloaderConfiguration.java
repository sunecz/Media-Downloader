package sune.app.mediadown.download;

@Deprecated(forRemoval=true)
public class SingleFileDownloaderConfiguration {
	
	private final boolean noSizeCheck;
	private final boolean appendMode;
	
	public SingleFileDownloaderConfiguration(boolean noSizeCheck, boolean appendMode) {
		this.noSizeCheck = noSizeCheck || appendMode; // append mode needs noSizeCheck flag
		this.appendMode  = appendMode;
	}
	
	private static SingleFileDownloaderConfiguration DEFAULT;
	public  static final SingleFileDownloaderConfiguration getDefault() {
		return DEFAULT == null ? DEFAULT = new SingleFileDownloaderConfiguration(false, false) : DEFAULT;
	}
	
	public boolean isNoSizeCheck() {
		return noSizeCheck;
	}
	
	public boolean isAppendMode() {
		return appendMode;
	}
}