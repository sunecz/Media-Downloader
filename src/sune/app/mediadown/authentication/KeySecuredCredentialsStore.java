package sune.app.mediadown.authentication;

import java.io.IOException;
import java.nio.file.Path;

/** @since 00.02.09 */
public class KeySecuredCredentialsStore extends FileCredentialsStore {
	
	protected KeySecuredCredentialsStore() {
	}
	
	public static final KeySecuredCredentialsStore of(Path path) throws IOException {
		KeySecuredCredentialsStore store = new KeySecuredCredentialsStore();
		store.open(path);
		return store;
	}
	
	@Override
	protected byte[] modifySerializationData(byte[] data) {
		// TODO: Implement
		return data;
	}
	
	@Override
	protected byte[] modifyDeserializationData(byte[] data) {
		// TODO: Implement
		return data;
	}
}