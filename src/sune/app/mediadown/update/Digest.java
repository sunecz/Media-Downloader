package sune.app.mediadown.update;

import java.util.Objects;

/** @since 00.02.09 */
public final class Digest {
	
	private final DigestType type;
	private final byte[] value;
	
	public Digest(DigestType type, byte[] value) {
		this.type = Objects.requireNonNull(type);
		this.value = Objects.requireNonNull(value);
	}
	
	public DigestType type() {
		return type;
	}
	
	public byte[] value() {
		return value;
	}
}
