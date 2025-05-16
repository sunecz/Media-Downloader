package sune.app.mediadown.os;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

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
		// See: com.sun.javafx.application.HostServicesDelegate$StandaloneHostService::showDocument
		// method for official implementation as of OpenJDK 24.
		Runtime.getRuntime().exec(new String[] {
			"rundll32", "url.dll,FileProtocolHandler", uri.toString()
		});
	}
	
	@Override
	public String executableFileNameSuffix() {
		return ".exe";
	}
}