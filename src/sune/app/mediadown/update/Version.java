package sune.app.mediadown.update;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

import sune.app.mediadown.util.Regex;

public final class Version implements Comparable<Version> {
	
	public static final Version UNKNOWN = new Version();
	/** @since 00.02.07 */
	public static final Version ZERO    = new Version(0, 0, 0, List.of(), "");
	
	/** @since 00.02.07 */
	private final int major;
	/** @since 00.02.07 */
	private final int minor;
	/** @since 00.02.07 */
	private final int patch;
	/** @since 00.02.09 */
	private final List<String> prerelease;
	/** @since 00.02.09 */
	private final String build;
	
	/** @since 00.02.07 */
	private Version() {
		this.major = -1;
		this.minor = -1;
		this.patch = -1;
		this.prerelease = List.of();
		this.build = "";
	}
	
	/** @since 00.02.09 */
	private Version(int major, int minor, int patch, List<String> prerelease, String build) {
		this.major = checkInteger(major);
		this.minor = checkInteger(minor);
		this.patch = checkInteger(patch);
		this.prerelease = List.copyOf(Objects.requireNonNull(prerelease));
		this.build = Objects.requireNonNull(build);
	}
	
	/** @since 00.02.07 */
	private static final int checkInteger(int value) {
		if(value < 0) {
			throw new IllegalArgumentException("Value cannot be < 0");
		}
		
		return value;
	}
	
	/** @since 00.02.09 */
	private static final boolean isNumeric(String string) {
		if(string.isEmpty()) {
			return false;
		}
		
		for(int i = 0, l = string.length(); i < l; ++i) {
			if(!Character.isDigit(string.charAt(i))) {
				return false;
			}
		}
		
		return true;
	}
	
	/** @since 00.02.09 */
	private static final int compareIdentifier(String a, String b) {
		boolean aNumeric = isNumeric(a), bNumeric = isNumeric(b);
		
		if(aNumeric && bNumeric) {
			return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
		}
		
		if(aNumeric != bNumeric) {
			// Numeric identifiers always have lower precedence than alphanumeric ones
			return aNumeric ? -1 : 1;
		}
		
		return a.compareTo(b);
	}
	
	/** @since 00.02.09 */
	private static final int comparePrerelease(List<String> a, List<String> b) {
		boolean aEmpty = a.isEmpty(), bEmpty = b.isEmpty();
		
		if(aEmpty && bEmpty) return 0;
		// A version without a prerelease has a higher precedence than one with a prerelease.
		if(aEmpty) return  1;
		if(bEmpty) return -1;
		
		for(int i = 0, l = Math.min(a.size(), b.size()), cmp; i < l; ++i) {
			if((cmp = compareIdentifier(a.get(i), b.get(i))) != 0) {
				return cmp;
			}
		}
		
		// A larger set has higher precedence than a smaller set, if all identifiers
		// in the smaller set are equal.
		return Integer.compare(a.size(), b.size());
	}
	
	/** @since 00.02.07 */
	public static final Version of(String string) {
		return Parser.instance().parse(string);
	}
	
	/** @since 00.02.09 */
	public static final Version of(int major, int minor, int patch) {
		return builder().major(major).minor(minor).patch(patch).build();
	}
	
	/** @since 00.02.09 */
	public static final Version of(int major, int minor, int patch, String... prerelease) {
		return builder().major(major).minor(minor).patch(patch).prerelease(prerelease).build();
	}
	
	/** @since 00.02.07 */
	public static final Builder builder() {
		return new Builder();
	}
	
	/** @since 00.02.07 */
	public Version release() {
		return this == UNKNOWN ? UNKNOWN : new Version(major, minor, patch, List.of(), "");
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
	
	/** @since 00.02.09 */
	public List<String> prerelease() {
		return prerelease;
	}
	
	/** @since 00.02.09 */
	public String build() {
		return build;
	}
	
	/** @since 00.02.08 */
	public final String string() {
		return Formatter.instance().full(this);
	}
	
	/** @since 00.02.08 */
	public final String stringRelease() {
		return Formatter.instance().release(this);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(major, minor, patch, prerelease, build);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if(!(obj instanceof Version)) return false;
		Version other = (Version) obj;
		return (
			   major == other.major
			&& minor == other.minor
			&& patch == other.patch
			&& prerelease.equals(other.prerelease)
			&& build.equals(other.build)
		);
	}
	
	@Override
	public int compareTo(Version other) {
		int cmp;
		if((cmp = Integer.compare(major, other.major)) != 0) return cmp;
		if((cmp = Integer.compare(minor, other.minor)) != 0) return cmp;
		if((cmp = Integer.compare(patch, other.patch)) != 0) return cmp;
		return comparePrerelease(prerelease, other.prerelease);
	}
	
	@Override
	public String toString() {
		return string();
	}
	
	/** @since 00.02.07 */
	private static final class Parser {
		
		private static final Parser INSTANCE = new Parser();
		// Semver-like syntax with optional minor and patch parts
		private static final Regex REGEX = Regex.of(
			       "^(?<major>0|[1-9]\\d*)"
			+ "(?:\\.(?<minor>0|[1-9]\\d*))?"
			+ "(?:\\.(?<patch>0|[1-9]\\d*))?"
			+ "(?:-(?<prerelease>[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
			+ "(?:\\+(?<build>[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?$"
		);
		
		// Forbid anyone to create an instance of this class
		private Parser() {
		}
		
		public static final Parser instance() {
			return INSTANCE;
		}
		
		public Version parse(String string) {
			if(string == null || string.isEmpty()
					|| string.equalsIgnoreCase(UNKNOWN.toString())) {
				return UNKNOWN;
			}
			
			Matcher matcher;
			if(!(matcher = REGEX.matcher(string)).matches()) {
				return UNKNOWN;
			}
			
			int major = Integer.valueOf(matcher.group("major"));
			int minor = Optional.ofNullable(matcher.group("minor")).map(Integer::valueOf).orElse(0);
			int patch = Optional.ofNullable(matcher.group("patch")).map(Integer::valueOf).orElse(0);
			List<String> prerelease = Optional.ofNullable(matcher.group("prerelease"))
				.map((s) -> List.of(s.split("\\.")))
				.orElse(List.of());
			String build = Optional.ofNullable(matcher.group("build")).orElse("");
			return new Version(major, minor, patch, prerelease, build);
		}
	}
	
	/** @since 00.02.07 */
	private static final class Formatter {
		
		private static final Formatter INSTANCE = new Formatter();
		
		// Forbid anyone to create an instance of this class
		private Formatter() {
		}
		
		public static final Formatter instance() {
			return INSTANCE;
		}
		
		/** @since 00.02.09 */
		private static final String formatIdentifier(String identifier) {
			return isNumeric(identifier) ? Long.toString(Long.parseLong(identifier)) : identifier;
		}
		
		private final void release(Version version, StringBuilder builder) {
			if(version == null || version == UNKNOWN) {
				builder.append("UNKNOWN");
			} else {
				builder.append(version.major()).append('.');
				builder.append(version.minor()).append('.');
				builder.append(version.patch());
			}
		}
		
		private final void full(Version version, StringBuilder builder) {
			release(version, builder);
			
			if(version == null || version == UNKNOWN) {
				return; // Nothing else to do
			}
			
			List<String> prerelease = version.prerelease();
			for(int i = 0, l = prerelease.size(); i < l; ++i) {
				builder.append(i == 0 ? '-' : '.');
				builder.append(formatIdentifier(prerelease.get(i)));
			}
			
			String build = version.build();
			if(!build.isEmpty()) {
				builder.append('+');
				builder.append(formatIdentifier(build));
			}
		}
		
		public final String release(Version version) {
			StringBuilder builder = new StringBuilder();
			release(version, builder);
			return builder.toString();
		}
		
		public final String full(Version version) {
			StringBuilder builder = new StringBuilder();
			full(version, builder);
			return builder.toString();
		}
	}
	
	/** @since 00.02.07 */
	public static final class Builder {
		
		private int major;
		private int minor;
		private int patch;
		/** @since 00.02.09 */
		private List<String> prerelease;
		/** @since 00.02.09 */
		private String build;
		
		private Builder() {
			this.major = 0;
			this.minor = 0;
			this.patch = 0;
			this.prerelease = List.of();
			this.build = "";
		}
		
		public Version build() {
			return new Version(major, minor, patch, prerelease, build);
		}
		
		public Builder major(int major) {
			this.major = major;
			return this;
		}
		
		public Builder minor(int minor) {
			this.minor = minor;
			return this;
		}
		
		public Builder patch(int patch) {
			this.patch = patch;
			return this;
		}
		
		/** @since 00.02.09 */
		public Builder prerelease(String... prerelease) {
			this.prerelease = List.of(prerelease);
			return this;
		}
		
		/** @since 00.02.09 */
		public Builder prerelease(List<String> prerelease) {
			this.prerelease = List.copyOf(prerelease);
			return this;
		}
		
		/** @since 00.02.09 */
		public Builder build(String build) {
			this.build = Objects.requireNonNull(build);
			return this;
		}
	}
}
