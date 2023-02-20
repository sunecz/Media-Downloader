package sune.app.mediadown.net;

import java.net.URI;
import java.net.URL;

import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.08 */
public final class Net {
	
	// Forbid anyone to create an instance of this class
	private Net() {
	}
	
	public static final URI uri(String uri) {
		return URI.create(uri);
	}
	
	public static final URI uri(URL url) {
		return Ignore.defaultValue(url::toURI, null);
	}
	
	public static final URL url(String url) {
		return Ignore.defaultValue(uri(url)::toURL, null);
	}
	
	public static final URL url(URI uri) {
		return Ignore.defaultValue(uri::toURL, null);
	}
}