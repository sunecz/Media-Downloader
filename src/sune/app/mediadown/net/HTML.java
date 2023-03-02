package sune.app.mediadown.net;

import java.net.URI;
import java.nio.charset.Charset;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import sune.app.mediadown.Shared;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.net.Web.Response;

/** @since 00.02.08 */
public final class HTML {
	
	private static final Charset CHARSET = Shared.CHARSET;
	
	// Forbid anyone to create an instance of this class
	private HTML() {
	}
	
	private static final String jsoupBaseUri(URI baseUri) {
		return baseUri == null ? "" : baseUri.normalize().toString();
	}
	
	public static final Document from(URI uri) throws Exception {
		return from(Request.of(uri).GET());
	}
	
	public static final Document from(Request request) throws Exception {
		try(Response.OfStream response = Web.requestStream(request)) {
			return Jsoup.parse(response.stream(), CHARSET.name(), jsoupBaseUri(request.uri()));
		}
	}
	
	public static final Document parse(String content) {
		return parse(content, null);
	}
	
	public static final Document parse(String content, URI baseUri) {
		return Jsoup.parse(content, jsoupBaseUri(baseUri));
	}
}