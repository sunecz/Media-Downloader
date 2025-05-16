package sune.app.mediadown.os;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import sune.app.mediadown.gui.util.FXUtils;

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
		// Delegate to the existing method
		FXUtils.openURI(uri);
	}
}