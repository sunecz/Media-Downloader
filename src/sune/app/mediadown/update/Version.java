package sune.app.mediadown.update;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sune.app.mediadown.Shared;
import sune.app.mediadown.util.Utils;

public final class Version implements Comparable<Version> {
	
	public static final Version UNKNOWN = new Version();
	
	private static Comparator<Version> comparator;
	
	private final VersionType type;
	/** @since 00.02.07 */
	private final int major;
	/** @since 00.02.07 */
	private final int minor;
	/** @since 00.02.07 */
	private final int patch;
	/** @since 00.02.07 */
	private final int value;
	
	/** @since 00.02.07 */
	private Version() {
		this.type = VersionType.UNKNOWN;
		this.major = -1;
		this.minor = -1;
		this.patch = -1;
		this.value = -1;
	}
	
	/** @since 00.02.07 */
	private Version(VersionType type, int major, int minor, int patch, int value) {
		this.type = Objects.requireNonNull(type);
		this.major = checkInteger(major);
		this.minor = checkInteger(minor);
		this.patch = checkInteger(patch);
		this.value = checkInteger(value);
	}
	
	/** @since 00.02.07 */
	private static final int checkInteger(int value) {
		if(value < 0)
			throw new IllegalArgumentException("Value cannot be < 0");
		return value;
	}
	
	public static final Version fromString(String string) {
		return Parser.instance().parse(string);
	}
	
	public static final Version fromURL(String url, int timeout) {
		return Utils.ignore(() -> {
			URLConnection connection = new URL(url).openConnection();
			connection.setConnectTimeout(timeout);
			try(InputStream stream = connection.getInputStream()) {
				return fromString(new String(stream.readAllBytes(), Shared.CHARSET));
			}
		}, UNKNOWN);
	}
	
	/** @since 00.02.07 */
	private final String string(boolean isCompact) {
		return Formatter.instance().full(this, isCompact);
	}
	
	/** @since 00.02.07 */
	private final String stringRelease(boolean isCompact) {
		return Formatter.instance().release(this, isCompact);
	}
	
	public VersionType type() {
		return type;
	}
	
	/** @since 00.02.07 */
	public int major() {
		return major;
	}
	
	/** @since 00.02.07 */
	public int minor() {
		return minor;
	}
	
	/** @since 00.02.07 */
	public int patch() {
		return patch;
	}
	
	public int value() {
		return value;
	}
	
	/** @since 00.02.07 */
	public String string() {
		return string(false);
	}
	
	/** @since 00.02.07 */
	public String stringRelease() {
		return stringRelease(false);
	}
	
	/** @since 00.02.07 */
	public String compactString() {
		return string(true);
	}
	
	/** @since 00.02.07 */
	public String compactStringRelease() {
		return stringRelease(true);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(major, minor, patch, type, value);
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
		return major == other.major
		        && minor == other.minor
		        && patch == other.patch
		        && type == other.type
		        && value == other.value;
	}
	
	@Override
	public int compareTo(Version other) {
		if(comparator == null) {
			comparator = Comparator.nullsLast(
				Comparator.    comparing(Version::major)
				          .thenComparing(Version::minor)
				          .thenComparing(Version::patch)
				          .thenComparing(Version::type)
				          .thenComparing(Version::value)
			);
		}
		
		return comparator.compare(this, other);
	}
	
	@Override
	public String toString() {
		return string();
	}
	
	/** @since 00.02.07 */
	private static final class Parser {
		
		private static final Pattern REGEX = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([a-z]+)(?:\\.(\\d+))?)?$");
		private static Parser INSTANCE;
		
		// Forbid anyone to create an instance of this class
		private Parser() {
		}
		
		public static final Parser instance() {
			return INSTANCE == null ? (INSTANCE = new Parser()) : INSTANCE;
		}
		
		public Version parse(String string) {
			if(string == null || string.isEmpty()
					|| string.equalsIgnoreCase(UNKNOWN.toString())) {
				return UNKNOWN;
			}
			
			Matcher matcher;
			if(!(matcher = REGEX.matcher(string)).matches())
				return UNKNOWN;
			
			int major = Integer.valueOf(matcher.group(1));
			int minor = Integer.valueOf(matcher.group(2));
			int patch = Integer.valueOf(matcher.group(3));
			VersionType type = Optional.ofNullable(matcher.group(4)).map(VersionType::from).orElse(VersionType.RELEASE);
			int value = Optional.ofNullable(matcher.group(5)).map(Integer::valueOf).orElse(0);
			return new Version(type, major, minor, patch, value);
		}
	}
	
	/** @since 00.02.07 */
	private static final class Formatter {
		
		private static Formatter INSTANCE;
		
		// Forbid anyone to create an instance of this class
		private Formatter() {
		}
		
		public static final Formatter instance() {
			return INSTANCE == null ? (INSTANCE = new Formatter()) : INSTANCE;
		}
		
		private final String format(int value, boolean isCompact) {
			return isCompact ? Integer.toString(value) : String.format("%02d", value);
		}
		
		private final void release(Version version, StringBuilder builder, boolean isCompact) {
			if(version == null || version.type() == VersionType.UNKNOWN) {
				builder.append("UNKNOWN");
			} else {
				builder.append(format(version.major(), isCompact)).append('.');
				builder.append(format(version.minor(), isCompact)).append('.');
				builder.append(format(version.patch(), isCompact));
			}
		}
		
		private final void full(Version version, StringBuilder builder, boolean isCompact) {
			release(version, builder, isCompact);
			
			VersionType type = version.type();
			switch(type) {
				case UNKNOWN:
				case RELEASE:
					// Do nothing
					break;
				default:
					builder.append('-').append(type.string());
					
					if(version.value() > 0) {
						builder.append('.').append(version.value());
					}
					break;
			}
		}
		
		public final String release(Version version, boolean isCompact) {
			StringBuilder builder = new StringBuilder();
			release(version, builder, isCompact);
			return builder.toString();
		}
		
		public final String full(Version version, boolean isCompact) {
			StringBuilder builder = new StringBuilder();
			full(version, builder, isCompact);
			return builder.toString();
		}
	}
}