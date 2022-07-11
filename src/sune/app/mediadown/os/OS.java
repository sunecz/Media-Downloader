package sune.app.mediadown.os;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import sune.app.mediadown.util.OSUtils;

/** @since 00.02.07 */
public interface OS {
	
	void highlight(Path path) throws IOException;
	void browse(URI uri) throws IOException;
	
	public static OS windows() { return Windows.instance(); }
	public static OS linux()   { return Linux.instance(); }
	public static OS macOS()   { return MacOS.instance(); }
	
	public static OS current() {
		switch(OSUtils.getSystemName()) {
			case OSUtils.OS_NAME_WINDOWS: return windows();
			case OSUtils.OS_NAME_UNIX:    return linux();
			case OSUtils.OS_NAME_MACOS:   return macOS();
			default: throw new IllegalStateException("Unsupported operating system");
		}
	}
}