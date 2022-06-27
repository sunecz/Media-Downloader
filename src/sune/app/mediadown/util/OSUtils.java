package sune.app.mediadown.util;

public final class OSUtils {
	
	private static final String OS_NAME_RAW = System.getProperty("os.name").toLowerCase();
	private static final String OS_ARCH_RAW = System.getProperty("os.arch").toLowerCase();
	
	private static final String OS_NAME = normalizeOSName(OS_NAME_RAW);
	private static final String OS_ARCH = normalizeOSArch(OS_ARCH_RAW);
	
	public static final String OS_NAME_WINDOWS = "win";
	public static final String OS_NAME_MACOS   = "mac";
	public static final String OS_NAME_UNIX    = "unx";
	public static final String OS_NAME_UNKNOWN = "unk";
	
	public static final String OS_ARCH_64 = "64";
	public static final String OS_ARCH_32 = "32";
	
	private static final String normalizeOSName(String osName) {
		return // Windows
			   osName.indexOf("win")   >= 0 	? OS_NAME_WINDOWS :
			   // Mac
			   osName.indexOf("mac")   >= 0 	? OS_NAME_MACOS :
			   // Unix or Linux 
			   (
			   osName.indexOf("nix")   >= 0 ||
			   osName.indexOf("nux")   >= 0 ||
			   osName.indexOf("aix")   >  0
			   ) 								? OS_NAME_UNIX :
			   // Solaris
			   osName.indexOf("sunos") >= 0 	? OS_NAME_UNIX :
			   // Other
			   OS_NAME_UNKNOWN;
	}
	
	private static final String normalizeOSArch(String osArch) {
		return // 64-bit
			   (
			   osArch.equals("amd64")  ||
			   osArch.equals("x86_64") ||
			   osArch.equals("sparcv9")
			   ) ?
			   OS_ARCH_64 :
			   // Treat others as 32-bit
			   OS_ARCH_32;
	}
	
	public static final String getSystemName() {
		return OS_NAME;
	}
	
	public static final String getSystemArch() {
		return OS_ARCH;
	}
	
	public static final String getSystemString() {
		return OS_NAME + OS_ARCH;
	}
	
	public static final boolean isWindows() {
		return getSystemName().equals(OS_NAME_WINDOWS);
	}
	
	public static final boolean isMacOS() {
		return getSystemName().equals(OS_NAME_MACOS);
	}
	
	public static final boolean isUnix() {
		return getSystemName().equals(OS_NAME_UNIX);
	}
	
	public static final Pair<String, String> parse(String string) {
		return string != null && string.length() == 5
					? new Pair<>(string.substring(0, 3).toLowerCase(), string.substring(3))
					: null;
	}
	
	public static final String getExecutableName(String name) {
		return isWindows() ? name + ".exe" : name;
	}
	
	// Forbid anyone to create an instance of this class
	private OSUtils() {
	}
}