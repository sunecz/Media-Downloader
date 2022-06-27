package sune.app.mediadown.media;

import java.util.Objects;

/** @since 00.02.05 */
public class MediaSource {
	
	private static final MediaSource NONE = new MediaSource();
	
	private final Object instance;
	
	private MediaSource() {
		this.instance = null;
	}
	
	private MediaSource(Object instance) {
		this.instance = Objects.requireNonNull(instance);
	}
	
	public static final MediaSource of(Object instance) {
		return instance == null ? none() : new MediaSource(instance);
	}
	
	public static final MediaSource none() {
		return NONE;
	}
	
	public Object instance() {
		return instance;
	}
	
	public boolean isEmpty() {
		return instance == null;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(instance);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		MediaSource other = (MediaSource) obj;
		return Objects.equals(instance, other.instance);
	}
	
	@Override
	public String toString() {
		return instance == null ? "NONE" : instance.toString();
	}
}