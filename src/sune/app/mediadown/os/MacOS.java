package sune.app.mediadown.os;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import sune.app.mediadown.util.FXUtils;

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
		// Delegate to the existing method
		FXUtils.openURI(uri);
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
