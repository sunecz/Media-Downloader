package sune.app.mediadown.os;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import sune.app.mediadown.util.FXUtils;

/** @since 00.02.07 */
final class Windows implements OS {
	
	static Windows INSTANCE = new Windows();
	
	// Forbid anyone to create an instance of this class
	private Windows() {
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
	public String executableFileName(String name) {
		return name + ".exe";
	}
	
	@Override
	public Name name() {
		return Name.WINDOWS;
	}
}
