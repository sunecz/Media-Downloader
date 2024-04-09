package sune.app.mediadown.update;

import java.util.Objects;

import sune.app.mediadown.util.OSUtils;
import sune.app.mediadown.util.Regex;

/** @since 00.02.07 */
public final class Requirements {
	
	private static final String STRING_DELIMITER   = ";";
	private static final String STRING_ANY_OS_NAME = "***";
	private static final String STRING_ANY_OS_ARCH = "**";
	
	public static final Requirements CURRENT = new Requirements(OSUtils.getSystemName(), OSUtils.getSystemArch());
	public static final Requirements ANY     = new Requirements(null, null);
	public static final Requirements WIN_64  = new Requirements(OSUtils.OS_NAME_WINDOWS, OSUtils.OS_ARCH_64);
	public static final Requirements UNX_64  = new Requirements(OSUtils.OS_NAME_UNIX, OSUtils.OS_ARCH_64);
	public static final Requirements MAC_64  = new Requirements(OSUtils.OS_NAME_MACOS, OSUtils.OS_ARCH_64);
	
	private final String osName;
	private final String osArch;
	
	private Requirements(String osName, String osArch) {
		this.osName = osName;
		this.osArch = osArch;
	}
	
	private static final String nonEmptyString(String string) {
		if(string == null || string.isEmpty())
			throw new IllegalArgumentException();
		return string;
	}
	
	public static final Requirements create(String osName, String osArch) {
		return new Requirements(nonEmptyString(osName), nonEmptyString(osArch));
	}
	
	public static final Requirements parse(String value) {
		if(value == null || value.isEmpty())
			throw new IllegalArgumentException("Invalid Requirement string");
		String[] parts = value.split(Regex.quote(STRING_DELIMITER));
		if(parts.length != 2)
			throw new IllegalArgumentException("Invalid Requirement string");
		String osName = parts[0];
		String osArch = parts[1];
		// Special value
		if(osName.equals(STRING_ANY_OS_NAME)
				&& osArch.equals(STRING_ANY_OS_ARCH))
			return ANY;
		// Normal value
		return create(osName, osArch);
	}
	
	public static final Requirements parseNoDelimiter(String value) {
		if(value == null || value.isEmpty())
			throw new IllegalArgumentException("Invalid Requirement string");
		if(value.length() != 5)
			throw new IllegalArgumentException("Invalid Requirement string");
		String osName = value.substring(0, 3);
		String osArch = value.substring(3, 5);
		// Special value
		if(osName.equals(STRING_ANY_OS_NAME)
				&& osArch.equals(STRING_ANY_OS_ARCH))
			return ANY;
		// Normal value
		return create(osName, osArch);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(osArch, osName);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Requirements other = (Requirements) obj;
		return Objects.equals(osArch, other.osArch) && Objects.equals(osName, other.osName);
	}
	
	@Override
	public String toString() {
		return String.format(
			"%s%s%s",
			osName == null ? STRING_ANY_OS_NAME : osName,
			STRING_DELIMITER,
			osArch == null ? STRING_ANY_OS_ARCH : osArch
		);
	}
	
	/** @since 00.02.10 */
	public String toCompactString() {
		return String.format(
			"%s%s",
			osName == null ? STRING_ANY_OS_NAME : osName,
			osArch == null ? STRING_ANY_OS_ARCH : osArch
		);
	}
}