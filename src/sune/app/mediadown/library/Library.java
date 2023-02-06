package sune.app.mediadown.library;

import java.nio.file.Path;
import java.util.Objects;

public final class Library {
	
	private final String name;
	private final Path   path;
	
	public Library(String name, Path path) {
		if((name == null || path == null))
			throw new IllegalArgumentException("Name and path cannot be null");
		this.name = name;
		this.path = path;
	}
	
	public String getName() {
		return name;
	}
	
	public Path getPath() {
		return path;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, path);
	}
	
	@Override
	public boolean equals(Object obj) {
		if((this == obj))
			return true;
		if((obj == null || getClass() != obj.getClass()))
			return false;
		Library other = (Library) obj;
		return Objects.equals(name, other.name) && Objects.equals(path, other.path);
	}
}