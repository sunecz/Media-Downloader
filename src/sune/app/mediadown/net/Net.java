package sune.app.mediadown.net;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Path;

import sune.app.mediadown.Shared;
import sune.app.mediadown.util.Regex;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.08 */
public final class Net {
	
	// Forbid anyone to create an instance of this class
	private Net() {
	}
	
	private static final String join(String separator, String... strings) {
		StringBuilder builder = new StringBuilder();
		
		for(String string : strings) {
			builder.append(string).append(separator);
		}
		
		int length;
		if((length = builder.length()) > 0) {
			builder.setLength(length - separator.length());
		}
		
		return builder.toString();
	}
	
	public static final URI uri(String uri) {
		return URI.create(uri);
	}
	
	public static final URI uri(URL url) {
		return Ignore.defaultValue(url::toURI, null);
	}
	
	public static final URI uri(Path path) {
		StringBuilder builder = new StringBuilder();
		
		Path root;
		if((root = path.getRoot()) != null) {
			builder.append(root.toString().replace('\\', '/'));
		}
		
		boolean first = true;
		for(Path part : path) {
			if(first) {
				first = false;
			} else {
				builder.append('/');
			}
			
			builder.append(encodeURL(part.toString()).replace("+", "%20"));
		}
		
		return uri("file:///" + builder.toString());
	}
	
	public static final URL url(String url) {
		return Ignore.defaultValue(uri(url)::toURL, null);
	}
	
	public static final URL url(URI uri) {
		return Ignore.defaultValue(uri::toURL, null);
	}
	
	public static final String encodeURL(String url) {
		return URLEncoder.encode(url, Shared.CHARSET);
	}
	
	public static final String decodeURL(String url) {
		return URLDecoder.decode(url, Shared.CHARSET);
	}
	
	// Source: https://stackoverflow.com/a/10159309
	public static final URI baseURI(URI uri) {
		return uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
	}
	
	public static final boolean isValidURI(String uri) {
		return Ignore.callAndCheck(() -> uri(uri));
	}
	
	public static final boolean isRelativeURI(String uri) {
		return !uri(uri).isAbsolute();
	}
	
	public static final String uriConcat(String... parts) {
		return Regex.of("([^:])//+").replaceAll(join("/", parts), "$1/");
	}
}