package sune.app.mediadown.download;

import java.util.Objects;

import sune.app.mediadown.util.Range;

/** @since 00.02.08 */
public class DownloadConfiguration {
	
	private static final Range<Long> DEFAULT_RANGE = new Range<>(-1L, -1L);
	private static final long        DEFAULT_TOTAL = -1L;
	
	private static DownloadConfiguration DEFAULT;
	
	private final Range<Long> rangeOutput;
	private final Range<Long> rangeRequest;
	private final long totalBytes;
	
	public DownloadConfiguration(long totalBytes) {
		this(DEFAULT_RANGE, DEFAULT_RANGE, totalBytes);
	}
	
	public DownloadConfiguration(Range<Long> rangeOutput, Range<Long> rangeRequest) {
		this(rangeOutput, rangeRequest, DEFAULT_TOTAL);
	}
	
	public DownloadConfiguration(Range<Long> rangeOutput, Range<Long> rangeRequest, long totalBytes) {
		this.rangeOutput = Objects.requireNonNull(rangeOutput);
		this.rangeRequest = Objects.requireNonNull(rangeRequest);
		this.totalBytes = totalBytes;
	}
	
	public static final DownloadConfiguration ofDefault() {
		return DEFAULT == null
					? DEFAULT = new DownloadConfiguration(DEFAULT_RANGE, DEFAULT_RANGE, DEFAULT_TOTAL)
					: DEFAULT;
	}
	
	public Range<Long> rangeOutput() {
		return rangeOutput;
	}
	
	public Range<Long> rangeRequest() {
		return rangeRequest;
	}
	
	public long totalBytes() {
		return totalBytes;
	}
}