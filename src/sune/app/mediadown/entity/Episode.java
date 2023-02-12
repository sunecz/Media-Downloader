package sune.app.mediadown.entity;

import java.net.URI;
import java.util.Objects;

import sune.app.mediadown.util.SimpleDataStorable;
import sune.app.mediadown.util.Utils;

public class Episode extends SimpleDataStorable {
	
	private final Program program;
	private final URI uri;
	private final String title;
	
	public Episode(Program program, URI uri, String title) {
		this.program = Objects.requireNonNull(program);
		this.uri     = Objects.requireNonNull(uri).normalize();
		this.title   = title;
	}
	
	public Episode(Program program, URI uri, String title, Object... data) {
		super(Utils.toMap(data));
		this.program = Objects.requireNonNull(program);
		this.uri     = Objects.requireNonNull(uri).normalize();
		this.title   = title;
	}
	
	public Program program() {
		return program;
	}
	
	public URI uri() {
		return uri;
	}
	
	public String title() {
		return title;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(program, title, uri);
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
		Episode other = (Episode) obj;
		return Objects.equals(program, other.program)
		        && Objects.equals(title, other.title)
		        && Objects.equals(uri, other.uri);
	}
	
	@Override
	public String toString() {
		return "Episode[uri=" + uri + ", title=" + title + ", program=" + program + "]";
	}
}