package sune.app.mediadown.entity;

import java.net.URI;
import java.util.Objects;

import sune.app.mediadown.util.SimpleDataStorable;
import sune.app.mediadown.util.Utils;

public class Episode extends SimpleDataStorable implements Comparable<Episode> {
	
	private final Program program;
	private final URI uri;
	private final String title;
	/** @since 00.02.09 */
	private final int number;
	/** @since 00.02.09 */
	private final int season;
	
	public Episode(Program program, URI uri, String title) {
		this(program, uri, title, 0, 0);
	}
	
	public Episode(Program program, URI uri, String title, Object... data) {
		this(program, uri, title, 0, 0, data);
	}
	
	/** @since 00.02.09 */
	public Episode(Program program, URI uri, String title, int number, int season) {
		this.program = Objects.requireNonNull(program);
		this.uri     = Objects.requireNonNull(uri).normalize();
		this.title   = title;
		this.number  = number;
		this.season  = season;
	}
	
	/** @since 00.02.09 */
	public Episode(Program program, URI uri, String title, int number, int season, Object... data) {
		super(Utils.toMap(data));
		this.program = Objects.requireNonNull(program);
		this.uri     = Objects.requireNonNull(uri).normalize();
		this.title   = title;
		this.number  = number;
		this.season  = season;
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
	
	/** @since 00.02.09 */
	public int number() {
		return number;
	}
	
	/** @since 00.02.09 */
	public int season() {
		return season;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(number, program, season, title, uri);
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
		return number == other.number
		        && Objects.equals(program, other.program)
		        && season == other.season
		        && Objects.equals(title, other.title)
		        && Objects.equals(uri, other.uri);
	}
	
	@Override
	public int compareTo(Episode other) {
		int cmp;
		if((cmp = Integer.compare(season, other.season)) != 0
				|| (cmp = Integer.compare(number, other.number)) != 0
				|| (cmp = Utils.compareNatural(title, other.title)) != 0
				|| (cmp = uri.compareTo(other.uri)) != 0) {
			return cmp;
		}
		
		return 0;
	}
	
	@Override
	public String toString() {
		return "Episode["
			+ "uri=" + uri + ", "
			+ "title=" + title + ", "
			+ "number=" + number + ", "
			+ "season=" + season + ", "
			+ "program=" + program
		+ "]";
	}
}