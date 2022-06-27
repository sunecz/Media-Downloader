package sune.app.mediadown.download;

/** @since 00.01.15 */
public class DownloadConfiguration {
	
	private final boolean accelerated;
	private final boolean singleRequest;
	
	public DownloadConfiguration(boolean accelerated, boolean singleRequest) {
		this.accelerated   = accelerated;
		this.singleRequest = singleRequest;
	}
	
	private static DownloadConfiguration DEFAULT_CONFIGURATION;
	public  static final DownloadConfiguration getDefault() {
		if((DEFAULT_CONFIGURATION == null)) {
			DEFAULT_CONFIGURATION = new DownloadConfiguration(true, false);
		}
		return DEFAULT_CONFIGURATION;
	}
	
	public boolean isAccelerated() {
		return accelerated;
	}
	
	public boolean isSingleRequest() {
		return singleRequest;
	}
}