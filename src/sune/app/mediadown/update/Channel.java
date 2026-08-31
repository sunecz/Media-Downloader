package sune.app.mediadown.update;

/** @since 00.02.09 */
public enum Channel {
	
	STABLE("stable"), DEV("dev");
	
	private final String registryName;
	
	private Channel(String registryName) {
		this.registryName = registryName;
	}
	
	public String registryName() {
		return registryName;
	}
}
