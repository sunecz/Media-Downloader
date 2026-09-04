package sune.app.mediadown.os;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/** @since 00.02.07 */
public interface OS {
	
	void highlight(Path path) throws IOException;
	void browse(URI uri) throws IOException;
	/** @since 00.02.09 */
	String executableFileName(String name);
	
	/** @since 00.02.09 */
	default Name name() { return OSUtils.currentName(); }
	/** @since 00.02.09 */
	default Arch arch() { return OSUtils.currentArch(); }
	
	static OS windows() { return Windows.INSTANCE; }
	static OS linux()   { return Linux.INSTANCE; }
	static OS macOS()   { return MacOS.INSTANCE; }
	
	static OS current() {
		switch(OSUtils.currentName()) {
			case WINDOWS: return windows();
			case LINUX:   return linux();
			case MACOS:   return macOS();
			default: throw new IllegalStateException("Unsupported operating system");
		}
	}
	
	/** @since 00.02.09 */
	static enum Name { WINDOWS, LINUX, MACOS, OTHER; }
	/** @since 00.02.09 */
	static enum Arch { AMD64, ARM64, OTHER; }
}
