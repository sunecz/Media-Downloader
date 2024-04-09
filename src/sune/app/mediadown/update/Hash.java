package sune.app.mediadown.update;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import sune.app.mediadown.concurrent.VarLoader;

public final class Hash {
	
	/** @since 00.02.10 */
	private static final VarLoader<MessageDigest> sha1 = VarLoader.ofChecked(Hash::initSha1MessageDigest);
	
	// Forbid anyone to create an instance of this class
	private Hash() {
	}
	
	/** @since 00.02.10 */
	private static final MessageDigest initSha1MessageDigest() throws NoSuchAlgorithmException {
		return MessageDigest.getInstance("SHA-1");
	}
	
	/** @since 00.02.10 */
	private static final MessageDigest sha1MessageDigest() throws IOException {
		try {
			return sha1.valueChecked();
		} catch(Exception ex) {
			throw new IOException(ex);
		}
	}
	
	/** @since 00.02.10 */
	public static final byte[] sha1rawChecked(Path file) throws IOException {
		MessageDigest digest = sha1MessageDigest();
		
		try(FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
			ByteBuffer buffer = ByteBuffer.allocate(8192);
			
			while(channel.read(buffer) != -1) {
				buffer.flip();
				digest.update(buffer);
				buffer.clear();
			}
		}
		
		return digest.digest();
	}
	
	/** @since 00.02.10 */
	public static final byte[] sha1raw(Path file) throws IOException {
		try {
			return sha1rawChecked(file);
		} catch(IOException ex) {
			// Ignore
		}
		
		return null;
	}
	
	/** @since 00.02.10 */
	public static final String sha1Checked(Path file) throws IOException {
		return Hex.string(sha1raw(file));
	}
	
	public static final String sha1(Path file) {
		try {
			return sha1Checked(file);
		} catch(IOException ex) {
			// Ignore
		}
		
		return null;
	}
}