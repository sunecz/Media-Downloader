package sune.app.mediadown.library;

import java.nio.file.Path;

public final class NativeLibrary {
	
	private final Path path;
	private final String name;
	private final String osName;
	private final String osArch;
	private final String version;
	
	private static final String checkString(String string, String message) {
		if((string == null || string.isEmpty()))
			throw new IllegalArgumentException(message);
		return string;
	}
	
	private static final Path checkPath(Path path, String message) {
		if((path == null))
			throw new IllegalArgumentException(message);
		return path;
	}
	
	public NativeLibrary(Path path, String name, String osName, String osArch, String version) {
		this.path = checkPath(path, "Native library's path cannot be null");
		this.name = checkString(name, "Native library's name cannot be null");
		this.osName = checkString(osName, "OS name cannot be null");
		this.osArch = checkString(osArch, "OS architecture cannot be null");
		this.version = checkString(version, "Native library's version cannot be null");
	}
	
	public Path getPath() {
		return path;
	}
	
	public String getName() {
		return name;
	}
	
	public String getOSName() {
		return osName;
	}
	
	public String getOSArch() {
		return osArch;
	}
	
	public String getVersion() {
		return version;
	}
}