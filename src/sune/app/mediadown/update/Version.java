package sune.app.mediadown.update;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

import sune.app.mediadown.util.Regex;

public final class Version implements Comparable<Version> {
	
	public static final Version UNKNOWN = new Version();
	/** @since 00.02.07 */
	public static final Version ZERO    = new Version(VersionType.UNKNOWN, 0, 0, 0, 0, 0);
	
	private final VersionType type;
	/** @since 00.02.07 */
	private final int major;
	/** @since 00.02.07 */
	private final int minor;
	/** @since 00.02.07 */
	private final int patch;
	/** @since 00.02.07 */
	private final int value;
	/** @since 00.02.09 */
	private final int buildNumber;
	
	/** @since 00.02.07 */
	private Version() {
		this.type = VersionType.UNKNOWN;
		this.major = -1;
		this.minor = -1;
		this.patch = -1;
		this.value = -1;
		this.buildNumber = -1;
	}
	
	/** @since 00.02.07 */
	private Version(VersionType type, int major, int minor, int patch, int value, int buildNumber) {
		this.type = Objects.requireNonNull(type);
		this.major = checkInteger(major);
		this.minor = checkInteger(minor);
		this.patch = checkInteger(patch);
		this.value = checkInteger(value);
		this.buildNumber = checkInteger(buildNumber);
	}
	
	/** @since 00.02.07 */
	private static final int checkInteger(int value) {
		if(value < 0) {
			throw new IllegalArgumentException("Value cannot be < 0");
		}
		
		return value;
	}
	
	/** @since 00.02.08 */
	private static final FormatterSettings formatterSettings(boolean isCompact) {
		return isCompact ? FormatterSettings.ofCompact() : FormatterSettings.ofDefault();
	}
	
	/** @since 00.02.07 */
	public static final Version of(String string) {
		return Parser.instance().parse(string);
	}
	
	/** @since 00.02.07 */
	public static final Version of(VersionType type, int major, int minor, int patch, int value) {
		return builder().type(type).major(major).minor(minor).patch(patch).value(value).build();
	}
	
	/** @since 00.02.09 */
	public static final Version of(VersionType type, int major, int minor, int patch, int value, int buildNumber) {
		return builder().type(type).major(major).minor(minor).patch(patch).value(value).buildNumber(buildNumber).build();
	}
	
	/** @since 00.02.07 */
	public static final Builder builder() {
		return new Builder();
	}
	
	/** @since 00.02.07 */
	private final String string(boolean isCompact) {
		return string(formatterSettings(isCompact));
	}
	
	/** @since 00.02.07 */
	private final String stringRelease(boolean isCompact) {
		return stringRelease(formatterSettings(isCompact));
	}
	
	/** @since 00.02.07 */
	public Version release() {
		return this == UNKNOWN ? UNKNOWN : new Version(VersionType.RELEASE, major, minor, patch, 0, 0);
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
	
	/** @since 00.02.09 */
	public int buildNumber() {
		return buildNumber;
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
	
	/** @since 00.02.08 */
	public final String string(FormatterSettings settings) {
		return Formatter.instance().full(this, Objects.requireNonNull(settings));
	}
	
	/** @since 00.02.08 */
	public final String stringRelease(FormatterSettings settings) {
		return Formatter.instance().release(this, Objects.requireNonNull(settings));
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(major, minor, patch, type, value, buildNumber);
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
		        && value == other.value
		        && buildNumber == other.buildNumber;
	}
	
	@Override
	public int compareTo(Version other) {
		int cmp;
		if((cmp = Integer.compare(major, other.major))             != 0) return cmp;
		if((cmp = Integer.compare(minor, other.minor))             != 0) return cmp;
		if((cmp = Integer.compare(patch, other.patch))             != 0) return cmp;
		if((cmp = type.compareTo(other.type))                      != 0) return cmp;
		if((cmp = Integer.compare(value, other.value))             != 0) return cmp;
		if((cmp = Integer.compare(buildNumber, other.buildNumber)) != 0) return cmp;
		return 0;
	}
	
	@Override
	public String toString() {
		return string();
	}
	
	/** @since 00.02.07 */
	private static final class Parser {
		
		private static final Parser INSTANCE = new Parser();
		private static final Regex REGEX = Regex.of(
			"^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:-(?:([a-z]+)\\.)?(\\d+))?)?)?(?:\\+(\\d+))?$"
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
			
			int major = Integer.valueOf(matcher.group(1));
			int minor = Optional.ofNullable(matcher.group(2)).map(Integer::valueOf).orElse(0);
			int patch = Optional.ofNullable(matcher.group(3)).map(Integer::valueOf).orElse(0);
			VersionType type = Optional.ofNullable(matcher.group(4)).map(VersionType::from).orElse(VersionType.RELEASE);
			int value = Optional.ofNullable(matcher.group(5)).map(Integer::valueOf).orElse(0);
			int buildNumber = Optional.ofNullable(matcher.group(6)).map(Integer::valueOf).orElse(0);
			return new Version(type, major, minor, patch, value, buildNumber);
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
		
		/** @since 00.02.08 */
		private static final String format(int value, int numberOfDigits) {
			return numberOfDigits <= 1 ? Integer.toString(value) : String.format("%0" + numberOfDigits + "d", value);
		}
		
		private final void release(Version version, StringBuilder builder, FormatterSettings settings) {
			if(version == null || version.type() == VersionType.UNKNOWN) {
				builder.append("UNKNOWN");
			} else {
				builder.append(format(version.major(), settings.numberOfDigits(FormatterDigitsType.MAJOR))).append('.');
				builder.append(format(version.minor(), settings.numberOfDigits(FormatterDigitsType.MINOR))).append('.');
				builder.append(format(version.patch(), settings.numberOfDigits(FormatterDigitsType.PATCH)));
			}
		}
		
		private final void full(Version version, StringBuilder builder, FormatterSettings settings) {
			release(version, builder, settings);
			
			VersionType type;
			if((type = version.type()) == VersionType.UNKNOWN) {
				return; // Nothing else to do
			}
			
			boolean isTypePresent = !type.string().isEmpty();
			
			if(isTypePresent) {
				builder.append('-').append(type.string());
			}
			
			if(version.value() > 0) {
				if(isTypePresent) {
					builder.append('.');
				} else {
					builder.append('-');
				}
				
				builder.append(format(
					version.value(),
					settings.numberOfDigits(FormatterDigitsType.VALUE)
				));
			}
			
			if(version.buildNumber() > 0) {
				builder.append('+');
				builder.append(format(
					version.buildNumber(),
					settings.numberOfDigits(FormatterDigitsType.BUILD_NUMBER)
				));
			}
		}
		
		public final String release(Version version, FormatterSettings settings) {
			StringBuilder builder = new StringBuilder();
			release(version, builder, settings);
			return builder.toString();
		}
		
		public final String full(Version version, FormatterSettings settings) {
			StringBuilder builder = new StringBuilder();
			full(version, builder, settings);
			return builder.toString();
		}
	}
	
	/** @since 00.02.08 */
	public static final class FormatterSettings {
		
		private static final FormatterSettings DEFAULT = of();
		private static final FormatterSettings COMPACT = of(1, 1, 1, 1, 1);
		
		private final int[] numberOfDigits;
		
		private FormatterSettings(int... numberOfDigits) {
			this.numberOfDigits = Objects.requireNonNull(numberOfDigits);
		}
		
		public static final FormatterSettings of(int... numberOfDigits) {
			Objects.requireNonNull(numberOfDigits);
			FormatterDigitsType[] digitsTypes = FormatterDigitsType.values();
			
			for(int i = 0, l = numberOfDigits.length; i < l; ++i) {
				numberOfDigits[i] = Math.max(numberOfDigits[i], FormatterDigitsType.MIN_DIGITS);
			}
			
			if(numberOfDigits.length < digitsTypes.length) {
				int[] complete = new int[digitsTypes.length];
				System.arraycopy(numberOfDigits, 0, complete, 0, numberOfDigits.length);
				
				for(int i = numberOfDigits.length, l = digitsTypes.length; i < l; ++i) {
					complete[i] = digitsTypes[i].defaultNumberOfDigits();
				}
				
				numberOfDigits = complete;
			}
			
			return new FormatterSettings(numberOfDigits);
		}
		
		public static final FormatterSettings ofDefault() {
			return DEFAULT;
		}
		
		public static final FormatterSettings ofCompact() {
			return COMPACT;
		}
		
		public int numberOfDigits(FormatterDigitsType digitsType) {
			return numberOfDigits[Objects.requireNonNull(digitsType).ordinal()];
		}
	}
	
	/** @since 00.02.08 */
	public static enum FormatterDigitsType {
		
		MAJOR(2),
		MINOR(2),
		PATCH(2),
		VALUE(1),
		/** @since 00.02.09 */
		BUILD_NUMBER(1);
		
		public static final int MIN_DIGITS = 1;
		
		private final int defaultNumberOfDigits;
		
		private FormatterDigitsType(int defaultNumberOfDigits) {
			this.defaultNumberOfDigits = defaultNumberOfDigits;
		}
		
		public int defaultNumberOfDigits() {
			return defaultNumberOfDigits;
		}
	}
	
	/** @since 00.02.07 */
	public static final class Builder {
		
		private VersionType type;
		private int major;
		private int minor;
		private int patch;
		private int value;
		/** @since 00.02.09 */
		private int buildNumber;
		
		private Builder() {
			this.type = VersionType.UNKNOWN;
			this.major = 0;
			this.minor = 0;
			this.patch = 0;
			this.value = 0;
			this.buildNumber = 0;
		}
		
		public Version build() {
			return new Version(type, major, minor, patch, value, buildNumber);
		}
		
		public Builder type(VersionType type) {
			this.type = type;
			return this;
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
		
		public Builder value(int value) {
			this.value = value;
			return this;
		}
		
		/** @since 00.02.09 */
		public Builder buildNumber(int buildNumber) {
			this.buildNumber = buildNumber;
			return this;
		}
		
		public VersionType type() {
			return type;
		}
		
		public int major() {
			return major;
		}
		
		public int minor() {
			return minor;
		}
		
		public int patch() {
			return patch;
		}
		
		public int value() {
			return value;
		}
		
		/** @since 00.02.09 */
		public int buildNumber() {
			return buildNumber;
		}
	}
}