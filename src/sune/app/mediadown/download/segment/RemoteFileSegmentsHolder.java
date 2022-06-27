package sune.app.mediadown.download.segment;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import sune.app.mediadown.media.MediaConstants;

/** @since 00.02.05 */
public final class RemoteFileSegmentsHolder implements FileSegmentsHolder<RemoteFileSegment> {
	
	private static final RemoteFileSegmentsHolder EMPTY = new RemoteFileSegmentsHolder();
	
	private final List<RemoteFileSegment> segments;
	private final double duration;
	
	private RemoteFileSegmentsHolder() {
		this.segments = List.of();
		this.duration = MediaConstants.UNKNOWN_DURATION;
	}
	
	public RemoteFileSegmentsHolder(List<RemoteFileSegment> segments, double duration) {
		this.segments = new ArrayList<>(Objects.requireNonNull(segments)); // List copy
		this.duration = duration;
	}
	
	public static final RemoteFileSegmentsHolder empty() {
		return EMPTY;
	}
	
	public static final RemoteFileSegmentsHolder ofSingle(URI uri, long size, double duration) {
		return new RemoteFileSegmentsHolder(List.of(new RemoteFileSegment(uri, size)), duration);
	}
	
	@Override
	public List<RemoteFileSegment> segments() {
		return Collections.unmodifiableList(segments);
	}
	
	@Override
	public int count() {
		return segments.size();
	}
	
	@Override
	public double duration() {
		return duration;
	}
}