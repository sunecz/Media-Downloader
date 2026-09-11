package sune.app.mediadown.os;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/** @since 00.02.07 */
final class MacOS implements OS {
	
	static MacOS INSTANCE = new MacOS();
	
	// Forbid anyone to create an instance of this class
	private MacOS() {
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
	
	@Override
	public String executableFileName(String name) {
		return name;
	}
	
	@Override
	public Name name() {
		return Name.MACOS;
	}
}
