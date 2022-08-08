package sune.app.mediadown.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import sune.app.mediadown.util.PathSystem;

public final class Resource {
	
	private static final String PATH_RESOURCES = PathSystem.getFullPath("resources/");
	
	public static final InputStream stream(String path) {
		try {
			return Files.newInputStream(Path.of(resolve(path)));
		} catch(IOException ex) {
		}
		return null;
	}
	
	public static final String resolve(String path) {
		return PATH_RESOURCES + path;
	}
	
	// Forbid anyone to create an instance of this class
	private Resource() {
	}
}