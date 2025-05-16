package sune.app.mediadown.os;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import sune.app.mediadown.gui.util.FXUtils;

/** @since 00.02.07 */
class Windows implements OS {
	
	private static Windows INSTANCE;
	
	// Forbid anyone to create an instance of this class
	private Windows() {
	}
	
	public static final Windows instance() {
		return INSTANCE == null ? (INSTANCE = new Windows()) : INSTANCE;
	}
	
	@Override
	public void highlight(Path path) throws IOException {
		Runtime.getRuntime().exec(new String[] {
			"explorer.exe",
			"/select,", // Mind the comma at the end!
			path.toAbsolutePath().toString()
		});
	}
	
	@Override
	public void browse(URI uri) throws IOException {
		// Delegate to the existing method
		FXUtils.openURI(uri);
	}
	
	@Override
	public String executableFileNameSuffix() {
		return ".exe";
	}
}