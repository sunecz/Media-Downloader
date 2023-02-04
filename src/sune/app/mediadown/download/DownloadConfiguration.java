package sune.app.mediadown.download;

import java.util.Objects;
import java.util.function.Predicate;

import sune.app.mediadown.util.Range;
import sune.app.mediadown.util.Web.Response;

/** @since 00.02.08 */
public class DownloadConfiguration {
	
	private static DownloadConfiguration DEFAULT;
	
	private final Range<Long> rangeOutput;
	private final Range<Long> rangeRequest;
	private final long totalBytes;
	private final Predicate<Response> responseFilter;
	
	private DownloadConfiguration(Range<Long> rangeOutput, Range<Long> rangeRequest, long totalBytes,
			Predicate<Response> responseFilter) {
		this.rangeOutput = Objects.requireNonNull(rangeOutput);
		this.rangeRequest = Objects.requireNonNull(rangeRequest);
		this.totalBytes = totalBytes;
		this.responseFilter = responseFilter; // May be null
	}
	
	public static final Builder builder() {
		return new Builder();
	}
	
	public static final DownloadConfiguration ofDefault() {
		return DEFAULT == null
					? DEFAULT = builder().build()
					: DEFAULT;
	}
	
	public static final DownloadConfiguration ofTotalBytes(long totalBytes) {
		return builder().totalBytes(totalBytes).build();
	}
	
	public static final DownloadConfiguration ofRanges(Range<Long> rangeOutput, Range<Long> rangeRequest) {
		return builder().rangeOutput(rangeOutput).rangeRequest(rangeRequest).build();
	}
	
	public static final DownloadConfiguration ofRanges(Range<Long> rangeOutput, Range<Long> rangeRequest,
			long totalBytes) {
		return builder().rangeOutput(rangeOutput).rangeRequest(rangeRequest).totalBytes(totalBytes).build();
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
	
	public Predicate<Response> responseFilter() {
		return responseFilter;
	}
	
	public static final class Builder {
		
		private static final Range<Long> DEFAULT_RANGE = new Range<>(-1L, -1L);
		private static final long        DEFAULT_TOTAL = -1L;
		
		private Range<Long> rangeOutput;
		private Range<Long> rangeRequest;
		private long totalBytes;
		private Predicate<Response> responseFilter;
		
		private Builder() {
			rangeOutput = DEFAULT_RANGE;
			rangeRequest = DEFAULT_RANGE;
			totalBytes = DEFAULT_TOTAL;
			responseFilter = null;
		}
		
		public DownloadConfiguration build() {
			return new DownloadConfiguration(rangeOutput, rangeRequest, totalBytes, responseFilter);
		}
		
		public Builder rangeOutput(Range<Long> rangeOutput) {
			this.rangeOutput = rangeOutput;
			return this;
		}
		
		public Builder rangeRequest(Range<Long> rangeRequest) {
			this.rangeRequest = rangeRequest;
			return this;
		}
		
		public Builder totalBytes(long totalBytes) {
			this.totalBytes = totalBytes;
			return this;
		}
		
		public Builder responseFilter(Predicate<Response> responseFilter) {
			this.responseFilter = responseFilter;
			return this;
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
		
		public Predicate<Response> responseFilter() {
			return responseFilter;
		}
	}
}