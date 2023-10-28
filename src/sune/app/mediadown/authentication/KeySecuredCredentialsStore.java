package sune.app.mediadown.authentication;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import javax.crypto.SecretKey;

/** @since 00.02.09 */
public class KeySecuredCredentialsStore extends FileCredentialsStore {
	
	private final SecretKey key;
	
	protected KeySecuredCredentialsStore(SecretKey key) {
		this.key = Objects.requireNonNull(key);
	}
	
	public static final KeySecuredCredentialsStore of(Path path, SecretKey key) throws IOException {
		KeySecuredCredentialsStore store = new KeySecuredCredentialsStore(key);
		store.open(path);
		return store;
	}
	
	@Override
	protected byte[] modifySerializationData(byte[] data) {
		return Crypto.AES256.encrypt(data, key);
	}
	
	@Override
	protected byte[] modifyDeserializationData(byte[] data) {
		return Crypto.AES256.decrypt(data, key);
	}
}