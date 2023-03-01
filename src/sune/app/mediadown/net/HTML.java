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
	
	public static final Document from(URI uri) {
		try(Response.OfStream response = Web.requestStream(Request.of(uri).GET())) {
			return Jsoup.parse(response.stream(), CHARSET.name(), jsoupBaseUri(uri));
		} catch(Exception ex) {
			// Ignore
		}
		
		return null;
	}
	
	public static final Document parse(String content) {
		return parse(content, null);
	}
	
	public static final Document parse(String content, URI baseUri) {
		return Jsoup.parse(content, jsoupBaseUri(baseUri));
	}
}