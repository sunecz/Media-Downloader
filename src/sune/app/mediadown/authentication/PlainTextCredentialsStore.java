package sune.app.mediadown.authentication;

import java.io.IOException;
import java.nio.file.Path;

/** @since 00.02.09 */
public class PlainTextCredentialsStore extends FileCredentialsStore {
	
	protected PlainTextCredentialsStore() {
	}
	
	public static final PlainTextCredentialsStore of(Path path) throws IOException {
		return new PlainTextCredentialsStore();
	}
}