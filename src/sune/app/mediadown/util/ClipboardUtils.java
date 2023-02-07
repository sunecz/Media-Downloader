package sune.app.mediadown.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.05 */
public final class ClipboardUtils {
	
	private static Clipboard clipboard;
	
	// Forbid anyone to create an instance of this class
	private ClipboardUtils() {
	}
	
	public static final Clipboard clipboard() {
		return clipboard == null ? (clipboard = Clipboard.getSystemClipboard()) : clipboard;
	}
	
	public static final void copy(String string) {
		ClipboardContent content = new ClipboardContent();
		content.putString(string);
		clipboard().setContent(content);
	}
	
	/** @since 00.02.07 */
	public static final List<URI> uris() {
		List<URI> uris = new ArrayList<>();
		Clipboard clipboard = clipboard();
		Object contents;
		
		if((contents = clipboard.getContent(DataFormat.URL)) != null) {
			String string = (String) contents;
			URI uri = Ignore.call(() -> Utils.uri(string.strip()));
			if(uri != null) uris.add(uri);
		}
		
		if((contents = clipboard.getContent(DataFormat.PLAIN_TEXT)) != null) {
			// May have multiple URIs in the contents
			for(String line : Regex.of("\\r?\\n").split((String) contents)) {
				URI uri = Ignore.call(() -> Utils.uri(line.strip()));
				if(uri != null) uris.add(uri);
			}
		}
		
		if((contents = clipboard.getContent(DataFormat.HTML)) != null) {
			Document document = Utils.parseDocument((String) contents);
			
			// Edge browser copy links from the URL bar as an HTML anchor tag.
			// Probe the content for anchor tags and extract their hrefs.
			for(Element elLink : document.select("a")) {
				String href = elLink.attr("href");
				URI uri = Ignore.call(() -> Utils.uri(href));
				if(uri != null) uris.add(uri);
			}
			
			// Always add the textual content of the document,
			// maybe there's an URL.
			String text = document.text().strip();
			URI uri = Ignore.call(() -> Utils.uri(text));
			if(uri != null) uris.add(uri);
		}
		
		uris = Utils.deduplicate(uris);
		return uris;
	}
}