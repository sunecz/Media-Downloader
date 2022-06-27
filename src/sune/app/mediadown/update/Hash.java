package sune.app.mediadown.update;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Hash {
	
	private static MessageDigest MD_SHA1;
	private static MessageDigest ensureMD_SHA1() {
		if((MD_SHA1 == null)) {
			try {
				return MD_SHA1 = MessageDigest.getInstance("SHA-1");
			} catch(NoSuchAlgorithmException ex) {
				// Should not happen
				throw new IllegalStateException("Unable to instantiate SHA-1 Message Digest");
			}
		}
		return MD_SHA1;
	}
	
	// Forbid anyone to create an instance of this class
	private Hash() {
	}
	
	private static final byte[] sha1raw(Path file) {
		// Ensure the message digest of SHA1
		MessageDigest mdg = ensureMD_SHA1();
		// Open the file's channel so it can be mapped and read
		try(FileChannel fch = FileChannel.open(file, StandardOpenOption.READ)) {
			ByteBuffer  buf = ByteBuffer.allocate(8192);
			while(fch.read(buf) != -1) {
				buf.flip();
				mdg.update(buf);
				buf.clear();
			}
		} catch(IOException ex) {
		}
		return mdg.digest();
	}
	
	public static final String sha1(Path file) {
		return Hex.string(sha1raw(file));
	}
}