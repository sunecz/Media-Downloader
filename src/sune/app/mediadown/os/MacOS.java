package sune.app.mediadown.os;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/** @since 00.02.07 */
class MacOS implements OS {
	
	private static MacOS INSTANCE;
	
	// Forbid anyone to create an instance of this class
	private MacOS() {
	}
	
	public static final MacOS instance() {
		return INSTANCE == null ? (INSTANCE = new MacOS()) : INSTANCE;
	}
	
	@Override
	public void highlight(Path path) throws IOException {
		Runtime.getRuntime().exec(new String[] {
			"open", "-R", path.toAbsolutePath().toString()
		});
	}
	
	@Override
	public void browse(URI uri) throws IOException {
		// Should be equivalent to the Desktop::browse(URI) method as of OpenJDK 24 (2025-05-16).
		// `open` executable should call the LSOpenURLsWithRole function (or similar) as the native
		// method `_lsOpenURI` does in the OpenJDK source code.
		// See: sun.lwawt.macosx.CDesktopPeer::browse (lsOpen) method for official implementation
		// as of OpenJDK 24.
		Runtime.getRuntime().exec(new String[] {
			"open", uri.toString()
		});
	}
}