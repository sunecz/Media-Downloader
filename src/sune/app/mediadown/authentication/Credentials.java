package sune.app.mediadown.authentication;

/** @since 00.02.09 */
public interface Credentials {
	
	byte[] serialize();
	void deserialize(byte[] data);
	void dispose();
	boolean isInitialized();
	boolean isDisposed();
}