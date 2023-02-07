package sune.app.mediadown.library;

import java.nio.file.Path;
import java.util.Objects;

/** @since 00.02.08 */
public final class Library {
	
	private final String name;
	private final Path path;
	
	public Library(String name, Path path) {
		this.name = Objects.requireNonNull(name);
		this.path = Objects.requireNonNull(path);
	}
	
	public String name() {
		return name;
	}
	
	public Path path() {
		return path;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, path);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Library other = (Library) obj;
		return Objects.equals(name, other.name) && Objects.equals(path, other.path);
	}
}