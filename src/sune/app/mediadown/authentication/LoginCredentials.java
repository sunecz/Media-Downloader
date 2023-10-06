package sune.app.mediadown.authentication;

import java.util.Arrays;
import java.util.Objects;

import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.util.JSON.JSONCollection;

/** @since 00.02.09 */
public class LoginCredentials implements Credentials {
	
	private static final VarLoader<LoginCredentials> empty = VarLoader.of(LoginCredentials::new);
	
	private byte[] username;
	private byte[] password;
	private boolean disposed;
	
	public LoginCredentials() {
	}
	
	public LoginCredentials(byte[] username, byte[] password) {
		this.username = Arrays.copyOf(Objects.requireNonNull(username), username.length);
		this.password = Arrays.copyOf(Objects.requireNonNull(password), password.length);
	}
	
	public static final LoginCredentials empty() {
		return empty.value();
	}
	
	// Method to be reused by subclasses
	protected void serialize(JSONCollection json) {
		json.set("username", getUsername());
		json.set("password", getPassword());
	}
	
	// Method to be reused by subclasses
	protected void deserialize(JSONCollection json) {
		username = CredentialsCommon.bytes(json.getString("username"));
		password = CredentialsCommon.bytes(json.getString("password"));
	}
	
	@Override
	public byte[] serialize() {
		if(!isInitialized()) {
			return null;
		}
		
		JSONCollection data = JSONCollection.empty();
		serialize(data);
		return CredentialsCommon.bytes(data.toString(true));
	}
	
	@Override
	public void deserialize(byte[] data) {
		JSONCollection json = CredentialsCommon.json(data);
		deserialize(json);
		json.clear();
		disposed = false;
	}
	
	@Override
	public void dispose() {
		if(!isInitialized()) {
			return;
		}
		
		CredentialsCommon.dispose(username);
		CredentialsCommon.dispose(password);
		username = null;
		password = null;
		disposed = true;
	}
	
	@Override
	public boolean isInitialized() {
		return username != null && password != null;
	}
	
	@Override
	public boolean isDisposed() {
		return disposed;
	}
	
	public String getUsername() {
		return isInitialized() ? CredentialsCommon.string(username) : null;
	}
	
	public String getPassword() {
		return isInitialized() ? CredentialsCommon.string(password) : null;
	}
}