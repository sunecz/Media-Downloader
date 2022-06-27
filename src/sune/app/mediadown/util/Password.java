package sune.app.mediadown.util;

import java.util.Objects;

/**
 * Used as a non-modifiable wrapper for storing a password in string form
 * in a configuration. No special operations are currently available.
 * @since 00.02.04
 */
public final class Password {
	
	private final String value;
	
	public Password(String value) {
		this.value = Objects.requireNonNull(value);
	}
	
	public String value() {
		return value;
	}
}