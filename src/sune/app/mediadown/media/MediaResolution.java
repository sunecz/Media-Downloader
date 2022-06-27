package sune.app.mediadown.media;

import java.util.Objects;

/** @since 00.02.05 */
public final class MediaResolution implements Comparable<MediaResolution> {
	
	public static final MediaResolution UNKNOWN = new MediaResolution(false, -1, -1);
	
	private final int width;
	private final int height;
	
	// Used only for the UNKNOWN resolution
	private MediaResolution(boolean unused, int width, int height) {
		this.width  = width;
		this.height = height;
	}
	
	public MediaResolution(int width, int height) {
		if(width < 0 || height < 0)
			throw new IllegalArgumentException();
		this.width  = width;
		this.height = height;
	}
	
	public static final MediaResolution fromString(String string) {
		if(string == null)
			return UNKNOWN;
		int index = string.indexOf('x');
		if(index < 0)
			return UNKNOWN;
		int w = Integer.valueOf(string.substring(0, index));
		int h = Integer.valueOf(string.substring(index + 1));
		return new MediaResolution(w, h);
	}
	
	public int width() {
		return width;
	}
	
	public int height() {
		return height;
	}
	
	public int area() {
		return width < 0 || height < 0 ? -1 : width * height;
	}
	
	@Override
	public int compareTo(MediaResolution other) {
		return this == other ? 0 : Integer.compare(area(), other.area());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(height, width);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		MediaResolution other = (MediaResolution) obj;
		return height == other.height && width == other.width;
	}
	
	@Override
	public String toString() {
		return this == UNKNOWN ? "UNKNOWN" : width + "x" + height;
	}
}