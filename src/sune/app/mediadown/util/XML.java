package sune.app.mediadown.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import sune.app.mediadown.Shared;
import sune.app.mediadown.net.Net;

public final class XML {
	
	public static final Document parseURL(String url) {
		try(InputStream stream = new URL(url).openStream()) {
			// pass the stream directly to the Jsoup parses method
			return Jsoup.parse(stream, Shared.CHARSET.name(), Net.baseURI(Net.uri(url)).toString(), Parser.xmlParser());
		} catch(IOException ex) {
		}
		return null;
	}
	
	public static final Document parseContent(String content, String baseURL) {
		return Jsoup.parse(content, baseURL, Parser.xmlParser());
	}
	
	// forbid anyone to create an instance of this class
	private XML() {
	}
}