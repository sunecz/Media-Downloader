package sune.app.mediadown.tor;

import java.nio.file.Path;

import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;

/** @since 00.02.10 */
public final class Tor {
	
	private static final VarLoader<Path> path = VarLoader.of(Tor::ensureBinary);
	
	// Forbid anyone to create an instance of this class
	private Tor() {
	}
	
	private static final Path ensureBinary() {
		Path path = NIO.localPath("resources/binary/tor", OSUtils.getExecutableName("tor"));
		
		if(!NIO.isRegularFile(path)) {
			throw new IllegalStateException("Tor was not found at " + path.toAbsolutePath().toString());
		}
		
		return path;
	}
	
	public static final Path path() {
		return path.value();
	}
	
	public static final Path path(TorResource resource) {
		return path().resolveSibling(resource.relativePath()).toAbsolutePath();
	}
	
	public static final TorProcess createProcess() {
		return TorProcess.create();
	}
	
	public static final String passwordHash(String password) throws Exception {
		try(TorProcess process = createProcess()) {
			return process.hashPassword(password);
		}
	}
}