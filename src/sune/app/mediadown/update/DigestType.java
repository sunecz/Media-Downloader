package sune.app.mediadown.update;

/** @since 00.02.09 */
public enum DigestType {
	
	SHA256("SHA-256");
	
	private final String algorithm;

	private DigestType(String algorithm) {
		this.algorithm = algorithm;
	}
	
	public String algorithm() {
		return algorithm;
	}
}
