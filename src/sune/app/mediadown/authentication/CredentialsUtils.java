package sune.app.mediadown.authentication;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import sune.app.mediadown.Shared;
import sune.app.mediadown.util.JSON;
import sune.app.mediadown.util.JSON.JSONCollection;

/** @since 00.02.09 */
public final class CredentialsUtils {
	
	// Forbid anyone to create an instance of this class
	private CredentialsUtils() {
	}
	
	public static final byte[] bytes(String data) {
		return Objects.requireNonNull(data).getBytes(Shared.CHARSET);
	}
	
	public static final String string(byte[] data) {
		return new String(Objects.requireNonNull(data), Shared.CHARSET);
	}
	
	public static final JSONCollection json(byte[] data) {
		Objects.requireNonNull(data);
		
		try(ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
			return JSON.read(stream, Shared.CHARSET);
		} catch(IOException ex) {
			// Should not happen
		}
		
		return null;
	}
	
	public static final void dispose(byte[] data) {
		Objects.requireNonNull(data);
		Arrays.fill(data, (byte) 0x00);
	}
	
	public static final void dispose(ByteBuffer buf) {
		Objects.requireNonNull(buf);
		
		int cap;
		if((cap = buf.capacity()) <= 0) {
			return;
		}
		
		buf.clear();
		
		int len = Math.min(cap, 8192);
		byte[] fill = new byte[len];
		Arrays.fill(fill, (byte) 0x00);
		
		for(int pos = 0; pos < cap; pos += len) {
			buf.put(fill, 0, Math.min(cap - pos, len));
		}
	}
}