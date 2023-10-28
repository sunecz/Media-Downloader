package sune.app.mediadown.authentication;

import java.util.Arrays;
import java.util.Objects;

/** @since 00.02.09 */
public class EmailCredentials extends FieldsCredentials {
	
	public EmailCredentials() {
		super(
			"email", null,
			"password", null
		);
	}
	
	public EmailCredentials(byte[] email, byte[] password) {
		super(
			"email", Arrays.copyOf(Objects.requireNonNull(email), email.length),
			"password", Arrays.copyOf(Objects.requireNonNull(password), password.length)
		);
	}
	
	public EmailCredentials(String email, String password) {
		this(
			CredentialsUtils.bytes(email),
			CredentialsUtils.bytes(password)
		);
	}
	
	public String email() {
		return isInitialized() ? CredentialsUtils.string(get("email")) : null;
	}
	
	public String password() {
		return isInitialized() ? CredentialsUtils.string(get("password")) : null;
	}
}