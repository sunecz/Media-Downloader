package sune.app.mediadown.update;

import java.util.Objects;

import sune.app.mediadown.os.OS;
import sune.app.mediadown.os.OS.Arch;
import sune.app.mediadown.os.OS.Name;

/** @since 00.02.09 */
public final class Environment {
	
	private final OS.Name osName;
	private final OS.Arch osArch;
	private final Arguments arguments;
	
	public Environment(Name osName, Arch osArch, Arguments arguments) {
		this.osName = Objects.requireNonNull(osName);
		this.osArch = Objects.requireNonNull(osArch);
		this.arguments = Objects.requireNonNull(arguments);
	}
	
	public static final Environment ofCurrent() {
		OS os = OS.current();
		Arguments arguments = Arguments.ofCurrent();
		return new Environment(os.name(), os.arch(), arguments);
	}
	
	public OS.Name osName() {
		return osName;
	}
	
	public OS.Arch osArch() {
		return osArch;
	}
	
	public Arguments arguments() {
		return arguments;
	}
}
