package sune.app.mediadown.authentication;

import java.io.IOException;
import java.util.Set;

/** @since 00.02.09 */
public interface CredentialsStore extends AutoCloseable {
	
	Credentials get(String path) throws IOException;
	boolean has(String path) throws IOException;
	void set(String path, Credentials credentials) throws IOException;
	void remove(String path) throws IOException;
	Set<String> paths() throws IOException;
}