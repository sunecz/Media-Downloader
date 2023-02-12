package sune.app.mediadown.entity;

import java.net.URI;
import java.util.Objects;

import sune.app.mediadown.util.SimpleDataStorable;
import sune.app.mediadown.util.Utils;

public final class Program extends SimpleDataStorable {
	
	private final URI uri;
	private final String title;
	
	public Program(URI uri, String title) {
		this.uri   = Objects.requireNonNull(uri).normalize();
		this.title = title;
	}
	
	public Program(URI uri, String title, Object... data) {
		super(Utils.toMap(data));
		this.uri   = Objects.requireNonNull(uri).normalize();
		this.title = title;
	}
	
	public final URI uri() {
		return uri;
	}
	
	public final String title() {
		return title;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(title, uri);
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(!super.equals(obj))
			return false;
		if(getClass() != obj.getClass())
			return false;
		Program other = (Program) obj;
		return Objects.equals(title, other.title) && Objects.equals(uri, other.uri);
	}
	
	@Override
	public String toString() {
		return "Program[uri=" + uri + ", title=" + title + "]";
	}
}