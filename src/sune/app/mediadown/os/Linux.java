package sune.app.mediadown.os;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import sune.app.mediadown.util.FXUtils;

/** @since 00.02.07 */
class Linux implements OS {
	
	private static Linux INSTANCE;
	
	// Forbid anyone to create an instance of this class
	protected Linux() {
	}
	
	public static final Linux instance() {
		return INSTANCE == null ? (INSTANCE = new Linux()) : INSTANCE;
	}
	
	@Override
	public void highlight(Path path) throws IOException {
		// Only GNOME desktop supported for now
		String command = String.format("nautilus -s \"%s\"", path.toAbsolutePath().toString());
		Runtime.getRuntime().exec(command);
	}
	
	@Override
	public void browse(URI uri) throws IOException {
		// Delegate to the existing method
		FXUtils.openURI(uri);
	}
}