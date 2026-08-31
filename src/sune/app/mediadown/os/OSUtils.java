package sune.app.mediadown.os;

/** @since 00.02.09 */
final class OSUtils {
	
	private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
	private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();
	
	private OSUtils() {
		throw new AssertionError("No instances");
	}
	
	public static final OS.Name currentName() {
		return (
			OS_NAME.indexOf("win") >= 0 ? OS.Name.WINDOWS :
			OS_NAME.indexOf("nux") >= 0 ? OS.Name.LINUX   :
			OS_NAME.indexOf("mac") >= 0 ? OS.Name.MACOS   :
			                              OS.Name.OTHER
		);
	}
	
	public static final OS.Arch currentArch() {
		switch(OS_ARCH) {
			case "amd64":
			case "x86_64":
				return OS.Arch.AMD64;
			case "aarch64":
				return OS.Arch.ARM64;
			default:
				return OS.Arch.OTHER;
		}
	}
}
