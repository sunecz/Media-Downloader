package sune.app.mediadown.update;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

import sune.app.mediadown.util.StreamUtils;

public final class Version implements Comparable<Version> {
	
	public static final Version UNKNOWN = new Version("UNKNOWN", -1);
	public static final Version ZERO    = new Version("0000", 0);
	
	private final VersionType type;
	private final String      string;
	private final int         value;
	
	// Forbid anyone to create an instance of this class
	private Version(VersionType type, String string, int value) {
		if((type == null))
			throw new IllegalArgumentException("Version type cannot be null");
		if((string == null || string.isEmpty()))
			throw new IllegalArgumentException("Version string cannot be null or empty");
		if((value < 0))
			throw new IllegalArgumentException("Version value cannot be < 0");
		this.type   = type;
		this.string = string;
		this.value  = value;
	}
	
	// Used only internally for special version instances
	private Version(String string, int value) {
		this.type   = VersionType.DEVELOPMENT;
		this.string = string;
		this.value  = value;
	}
	
	private Version(String string) {
		this(VersionType.from(string), string);
	}
	
	private Version(VersionType type, String string) {
		this(type, string, string2int(VersionType.remove(string, type)));
	}
	
	private static final int string2int(String string) {
		if((string == null || string.isEmpty()))
			// Return zero rather than -1
			return 0;
		// The output value
		int val = 0;
		// The actual conversion
		for(int i = string.length()-1, m = 1, c; i >= 0; --i) {
			c = string.charAt(i);
			// Only convert digits
			if((Character.isDigit(c))) {
				val += Character.digit(c, 10) * m;
				m   *= 10;
			}
		}
		return val;
	}
	
	public static final Version fromString(String string) {
		return string != null && !string.isEmpty() ? new Version(string) : UNKNOWN;
	}
	
	public static final Version fromURL(String url, int timeout) {
		try {
			URLConnection connection = new URL(url).openConnection();
			connection.setConnectTimeout(timeout);
			try(InputStream stream = connection.getInputStream()) {
				byte[] bytes = StreamUtils.readAllBytes(stream);
				return fromString(new String(bytes));
			}
		} catch(IOException ex) {
			// Ignore
		}
		return UNKNOWN;
	}
	
	public VersionType getType() {
		return type;
	}
	
	public int getValue() {
		return value;
	}
	
	@Override
	public int compareTo(Version other) {
		// If null, return that the current version is newer
		if((other == null)) return 1;
		int ordN = other.getType().ordinal();
		int ordC = getType().ordinal();
		int valN = other.getValue();
		int valC = getValue();
		return ordN == ordC ? valC - valN : ordC - ordN;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(type, value);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Version other = (Version) obj;
		return type == other.type && value == other.value;
	}
	
	@Override
	public String toString() {
		return string;
	}
}