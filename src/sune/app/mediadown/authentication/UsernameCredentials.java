package sune.app.mediadown.authentication;

import java.util.Arrays;
import java.util.Objects;

/** @since 00.02.09 */
public class UsernameCredentials extends FieldsCredentials {
	
	public UsernameCredentials() {
		super(
			"username", null,
			"password", null
		);
	}
	
	public UsernameCredentials(byte[] username, byte[] password) {
		super(
			"username", Arrays.copyOf(Objects.requireNonNull(username), username.length),
			"password", Arrays.copyOf(Objects.requireNonNull(password), password.length)
		);
	}
	
	public UsernameCredentials(String username, String password) {
		this(
			CredentialsUtils.bytes(username),
			CredentialsUtils.bytes(password)
		);
	}
	
	public String username() {
		return isInitialized() ? CredentialsUtils.string(get("username")) : null;
	}
	
	public String password() {
		return isInitialized() ? CredentialsUtils.string(get("password")) : null;
	}
}