package sune.app.mediadown.os;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** @since 00.02.07 */
public interface OS {
	
	void highlight(Path path) throws IOException;
	void browse(URI uri) throws IOException;
	
	/** @since 00.02.09 */
	default String executableFileNameSuffix() {
		return "";
	}
	
	/** @since 00.02.09 */
	default String executableFileName(String fileName) {
		return fileName + executableFileNameSuffix();
	}
	
	public static OS windows() { return Windows.instance(); }
	public static OS linux()   { return Linux.instance(); }
	public static OS macOS()   { return MacOS.instance(); }
	
	public static OS current() {
		switch(Name.CURRENT) {
			case WINDOWS: return windows();
			case MACOS:   return macOS();
			case LINUX:   return linux();
			default: throw new IllegalStateException("Unsupported operating system");
		}
	}
	
	/** @since 00.02.09 */
	public static String name() { return Name.CURRENT.value(); }
	/** @since 00.02.09 */
	public static String shortName() { return Name.CURRENT.shortValue(); }
	/** @since 00.02.09 */
	public static String architecture() { return Architecture.CURRENT.value(); }
	/** @since 00.02.09 */
	public static int bits() { return Architecture.CURRENT.bits(); }
	/** @since 00.02.09 */
	public static String string() { return name() + architecture(); }
	/** @since 00.02.09 */
	public static String shortString() { return shortName() + architecture(); }
	
	/** @since 00.02.09 */
	public static boolean isWindows() { return Name.CURRENT == Name.WINDOWS; }
	/** @since 00.02.09 */
	public static boolean isMacOS() { return Name.CURRENT == Name.MACOS; }
	/** @since 00.02.09 */
	public static boolean isLinux() { return Name.CURRENT == Name.LINUX; }
	/** @since 00.02.09 */
	public static boolean isOther() { return Name.CURRENT == Name.OTHER; }
	
	/** @since 00.02.09 */
	public static boolean isArchitectureX86() { return Architecture.CURRENT == Architecture.X86_64; }
	/** @since 00.02.09 */
	public static boolean isArchitectureARM() { return Architecture.CURRENT == Architecture.ARM_64; }
	/** @since 00.02.09 */
	public static boolean isArchitectureOther() { return Architecture.CURRENT == Architecture.OTHER; }
	
	/** @since 00.02.09 */
	public static boolean is32Bit() { return Architecture.CURRENT.bits() == 32; }
	/** @since 00.02.09 */
	public static boolean is64Bit() { return Architecture.CURRENT.bits() == 64; }
	
	/** @since 00.02.09 */
	public static enum Name {
		
		WINDOWS("windows", "win"),
		MACOS("macos", "mac"),
		LINUX("linux", "unx"/* compatibility value */),
		OTHER("other", "unk"/* compatibility value */);
		
		private static final Name CURRENT = of(System.getProperty("os.name"));
		
		private final String value;
		private final String shortValue;
		
		private Name(String value, String shortValue) {
			this.value = Objects.requireNonNull(value);
			this.shortValue = Objects.requireNonNull(shortValue);
		}
		
		public static final Name of(String value) {
			if(value == null) {
				return OTHER;
			}
			
			value = value.toLowerCase(Locale.ROOT);
			
			if(value.contains("win"))   return WINDOWS;
			if(value.contains("mac"))   return MACOS;
			if(value.contains("linux")) return LINUX;
			
			return OTHER;
		}
		
		public static final Name current() {
			return CURRENT;
		}
		
		public String value() { return value; }
		public String shortValue() { return shortValue; }
		@Override public String toString() { return value(); }
	}
	
	/** @since 00.02.09 */
	public static enum Architecture {
		
		X86_64("amd64", 64),
		ARM_64("arm64", 64),
		OTHER("other", 0);
		
		private static final Architecture CURRENT = of(System.getProperty("os.arch"));
		
		private final String value;
		private final int bits;
		
		private Architecture(String value, int bits) {
			this.value = Objects.requireNonNull(value);
			this.bits = checkBits(bits);
		}
		
		private static final int checkBits(int bits) {
			if(bits < 0) {
				throw new IllegalArgumentException("Invalid bits");
			}
			
			return bits;
		}
		
		public static final Architecture of(String value) {
			if(value == null) {
				return OTHER;
			}
			
			value = value.toLowerCase(Locale.ROOT);
			
			if(value.equals("amd64") || value.equals("x86_64"))  return X86_64;
			if(value.equals("arm64") || value.equals("aarch64")) return ARM_64;
			
			return OTHER;
		}
		
		public static final Architecture current() {
			return CURRENT;
		}
		
		public String value() { return value; }
		public int bits() { return bits; }
		@Override public String toString() { return value(); }
	}
}