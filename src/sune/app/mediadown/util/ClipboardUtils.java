package sune.app.mediadown.util;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

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
}