package sune.app.mediadown.update;

/** @since 00.02.09 */
public enum Encoding {
	
	GZIP("gzip");
	
	private final String registryName;

	private Encoding(String registryName) {
		this.registryName = registryName;
	}
	
	public String getEncoding() {
		return registryName;
	}
}
