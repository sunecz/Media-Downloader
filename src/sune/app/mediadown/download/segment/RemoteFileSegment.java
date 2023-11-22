package sune.app.mediadown.download.segment;

import java.net.URI;
import java.util.Objects;

import sune.app.mediadown.media.MediaConstants;

/** @since 00.02.05 */
public final class RemoteFileSegment implements FileSegment {
	
	private final URI uri;
	private final long size;
	private final double duration;
	
	/** @since 00.02.09 */
	public RemoteFileSegment(URI uri) {
		this(uri, MediaConstants.UNKNOWN_SIZE, MediaConstants.UNKNOWN_DURATION);
	}
	
	public RemoteFileSegment(URI uri, long size) {
		this(uri, size, MediaConstants.UNKNOWN_DURATION);
	}
	
	/** @since 00.02.09 */
	public RemoteFileSegment(URI uri, long size, double duration) {
		this.uri = Objects.requireNonNull(uri);
		this.size = size;
		this.duration = duration;
	}
	
	@Override
	public final URI uri() {
		return uri;
	}
	
	@Override
	public final long size() {
		return size;
	}
	
	@Override
	public double duration() {
		return duration;
	}
}