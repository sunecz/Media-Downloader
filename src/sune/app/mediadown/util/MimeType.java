package sune.app.mediadown.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MimeType {
	
	private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
	
	public static final String guess(Path file) {
		try {
			return Files.probeContentType(file);
		} catch(IOException ex) {
			// Ignore
		}
		return DEFAULT_MIME_TYPE;
	}
	
	public static final boolean isImage(Path file) {
		String mimeType = guess(file);
		return mimeType != null && mimeType.startsWith("image/");
	}
	
	// Forbid anyone to create an instance of this class
	private MimeType() {
	}
}