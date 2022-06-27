package sune.app.mediadown.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

public final class DesktopUtils {
	
	private static final void highlightFile_windows(Path file) {
		String cmd = String.format("explorer /select, \"%s\"", file.toAbsolutePath().toString());
		executeCommand(cmd);
	}
	
	private static final void highlightFile_macOS(Path file) {
		String cmd = String.format("open -R \"%s\"", file.toAbsolutePath().toString());
		executeCommand(cmd);
	}
	
	private static final void highlightFile_unix(Path file) {
		// only GNOME desktop supported
		String cmd = String.format("nautilus -s \"%s\"", file.toAbsolutePath().toString());
		executeCommand(cmd);
	}
	
	private static final void executeCommand(String cmd) {
		try {
			Runtime.getRuntime().exec(cmd);
		} catch(IOException ex) {
		}
	}
	
	public static final void highlightFile(Path file) {
		String osName = OSUtils.getSystemName();
		switch(osName) {
			// Windows
			case "win":
				highlightFile_windows(file);
				break;
			// Mac OS
			case "mac":
				highlightFile_macOS(file);
				break;
			// Unix-like
			case "lin":
				highlightFile_unix(file);
				break;
			default:
				throw new IllegalStateException("Unable to highlight file. OS not supported.");
		}
	}
	
	/** @since 00.02.02 */
	public static final void browse(String url) throws IOException, URISyntaxException {
		browse(Utils.url(url));
	}
	
	/** @since 00.02.02 */
	public static final void browse(URL url) throws IOException, URISyntaxException {
		browse(url.toURI());
	}
	
	/** @since 00.02.02 */
	public static final void browse(URI uri) throws IOException {
		Desktop.getDesktop().browse(uri);
	}
	
	// Forbid anyone to create an instance of this class
	private DesktopUtils() {
	}
}