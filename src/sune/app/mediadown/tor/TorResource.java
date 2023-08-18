package sune.app.mediadown.tor;

import java.util.Objects;

/** @since 00.02.09 */
public enum TorResource {
	
	GEO_IPV4("data/geoip"),
	GEO_IPV6("data/geoip6");
	
	private final String relativePath;
	
	private TorResource(String relativePath) {
		this.relativePath = Objects.requireNonNull(relativePath);
	}
	
	public String relativePath() {
		return relativePath;
	}
}