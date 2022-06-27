package sune.app.mediadown.download.segment;

import java.net.URI;
import java.util.Objects;

/** @since 00.02.05 */
public final class RemoteFileSegment implements FileSegment {
	
	private final URI uri;
	private long size;
	
	public RemoteFileSegment(URI uri, long size) {
		this.uri = Objects.requireNonNull(uri);
		this.size = size;
	}
	
	@Override
	public final void size(long size) {
		this.size = size;
	}
	
	@Override
	public final URI uri() {
		return uri;
	}
	
	@Override
	public final long size() {
		return size;
	}
}