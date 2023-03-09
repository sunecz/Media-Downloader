package sune.app.mediadown.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import sune.app.mediadown.concurrent.ValidableValue;
import sune.app.mediadown.util.ComparatorCombiner;
import sune.app.mediadown.util.Regex;
import sune.app.mediadown.util.Utils;

/** @since 00.02.05 */
public final class MediaQuality implements Comparable<MediaQuality> {
	
	private static final Map<String, MediaQuality> registered = new LinkedHashMap<>();
	private static final ValidableValue<MediaQuality[]> values;
	private static final ValidableValue<MediaQuality[]> validQualities;
	
	static {
		values = ValidableValue.of(MediaQuality::newValues);
		validQualities = ValidableValue.of(MediaQuality::newValidQualities);
	}
	
	private static final List<MediaQualityParser> parsers = new ArrayList<>();
	
	// Special qualities
	public static final MediaQuality UNKNOWN;
	// Video qualities (Progressive scan)
	public static final MediaQuality P144;
	public static final MediaQuality P240;
	public static final MediaQuality P360;
	public static final MediaQuality P380;
	public static final MediaQuality P480;
	public static final MediaQuality P720;
	public static final MediaQuality P1080;
	public static final MediaQuality P2160;
	// Common qualities
	public static final MediaQuality VIDEO_MIN;
	public static final MediaQuality VIDEO_LOW;
	public static final MediaQuality VIDEO_MEDIUM;
	public static final MediaQuality VIDEO_HIGH;
	public static final MediaQuality AUDIO_MIN;
	public static final MediaQuality AUDIO_LOW;
	public static final MediaQuality AUDIO_MEDIUM;
	public static final MediaQuality AUDIO_HIGH;
	
	static {
		UNKNOWN = builder(true).name("UNKNOWN").build();
		P144 = builder().name("P144").mediaType(MediaType.VIDEO).value(vqv(144)).build();
		P240 = builder().name("P240").mediaType(MediaType.VIDEO).value(vqv(240)).build();
		P360 = builder().name("P360").mediaType(MediaType.VIDEO).value(vqv(360)).build();
		P380 = builder().name("P380").mediaType(MediaType.VIDEO).value(vqv(380)).build();
		P480 = builder().name("P480").mediaType(MediaType.VIDEO).value(vqv(480)).build();
		P720 = builder().name("P720").mediaType(MediaType.VIDEO).value(vqv(720)).build();
		P1080 = builder().name("P1080").mediaType(MediaType.VIDEO).value(vqv(1080)).build();
		P2160 = builder().name("P2160").mediaType(MediaType.VIDEO).value(vqv(2160)).build();
		VIDEO_MIN = builder().name("VIDEO_MIN").mediaType(MediaType.VIDEO).value(vqv(1)).build();
		VIDEO_LOW = builder().name("VIDEO_LOW").mediaType(MediaType.VIDEO).value(vqv(240)).synonyms("low").build();
		VIDEO_MEDIUM = builder().name("VIDEO_MEDIUM").mediaType(MediaType.VIDEO).value(vqv(480)).synonyms("middle", "medium", "sd").build();
		VIDEO_HIGH = builder().name("VIDEO_HIGH").mediaType(MediaType.VIDEO).value(vqv(720)).synonyms("high", "hd").build();
		AUDIO_MIN = builder().name("AUDIO_MIN").mediaType(MediaType.AUDIO).value(aqv(1)).build();
		AUDIO_LOW = builder().name("AUDIO_LOW").mediaType(MediaType.AUDIO).value(aqv(22050)).synonyms("low").build();
		AUDIO_MEDIUM = builder().name("AUDIO_MEDIUM").mediaType(MediaType.AUDIO).value(aqv(44100)).synonyms("middle", "medium", "sd").build();
		AUDIO_HIGH = builder().name("AUDIO_HIGH").mediaType(MediaType.AUDIO).value(aqv(96000)).synonyms("high", "hd").build();
	}
	
	static {
		addStringParser(new ProgressiveScanStringParser());
		addStringParser(new SynonymStringParser());
	}
	
	private final String name;
	private final MediaType mediaType;
	private final QualityValue value;
	private final List<String> synonyms;
	
	private MediaQuality(boolean isUnknown, String name, MediaType mediaType, QualityValue value, List<String> synonyms) {
		this.name = requireValidName(name);
		this.mediaType = Objects.requireNonNull(mediaType);
		this.value = isUnknown ? value : requireValidValue(value, mediaType);
		this.synonyms = Collections.unmodifiableList(Objects.requireNonNull(Utils.nonNullContent(synonyms)));
		register(this);
	}
	
	private static final String requireValidName(String name) {
		if(name == null || name.isBlank())
			throw new IllegalArgumentException("Name may be neither null nor blank.");
		return name;
	}
	
	private static final QualityValue requireValidValue(QualityValue value, MediaType mediaType) {
		if(value != UnknownQualityValue.INSTANCE
				&& !mediaType.is(MediaType.UNKNOWN) && !value.type().is(mediaType))
			throw new IllegalArgumentException("Invalid media type for value.");
		return value;
	}
	
	private static final void register(MediaQuality quality) {
		synchronized(registered) {
			if(registered.putIfAbsent(quality.name.toLowerCase(), quality) != null) {
				throw new IllegalStateException("Media quality \"" + quality.name + "\" already registered.");
			}
			
			values.invalidate();
			validQualities.invalidate();
		}
	}
	
	private static final Stream<MediaQuality> unmodifiedQualities() {
		List<MediaQuality> qualities;
		
		synchronized(registered) {
			qualities = List.copyOf(registered.values());
		}
		
		return qualities.stream().filter((q) -> !isModified(q));
	}
	
	private static final MediaQuality[] newValues() {
		return unmodifiedQualities().toArray(MediaQuality[]::new);
	}
	
	private static final MediaQuality[] newValidQualities() {
		return unmodifiedQualities().filter((q) -> !q.is(UNKNOWN)).toArray(MediaQuality[]::new);
	}
	
	// Shortcut for constructing a video quality
	private static final VideoQualityValue vqv(int height) {
		return new VideoQualityValue(height);
	}
	
	// Shortcut for constructing a audio quality
	private static final AudioQualityValue aqv(int sampleRate) {
		return new AudioQualityValue(0, sampleRate, 0);
	}
	
	private static final String generateValidName(String prefix) {
		Objects.requireNonNull(prefix);
		
		int index;
		if((index = prefix.indexOf('$')) >= 0) {
			prefix = prefix.substring(0, index);
		}
		
		prefix = prefix.trim();
		if(prefix.isEmpty()) {
			prefix = UNKNOWN.name;
		}
		
		String name;
		do {
			name = String.format("%s$%s", prefix, Utils.randomString(16));
		} while(registered.containsKey(name));
		
		return name;
	}
	
	private static final boolean isModified(MediaQuality quality) {
		return quality.name().contains("$");
	}
	
	public static final String removeNameSuffix(String name) {
		int i; return (i = name.indexOf('$')) >= 0 ? name.substring(0, i) : name;
	}
	
	public static final boolean equalNames(MediaQuality a, MediaQuality b) {
		return a.name == b.name || a.name != null && removeNameSuffix(a.name).equals(removeNameSuffix(b.name));
	}
	
	/**
	 * Returns a comparator that sorts the qualities from highest to lowest,
	 * but keeps the same order of qualities with same values as is in the array
	 * returned by {@linkplain #values()}.
	 */
	public static final Comparator<MediaQuality> reversedComparatorKeepOrder() {
		return ComparatorCombiner
					.<MediaQuality>of((a, b) -> {
						int cmp; return (cmp = b.compareTo(a)) != 0 && b.value().compareTo(a.value()) == 0 ? -cmp : cmp;
					})
					.combine((a, b) -> Utils.compareIndex(a, b, values()));
	}
	
	public static final MediaQuality[] values() {
		return values.value();
	}
	
	public static final MediaQuality ofName(String name) {
		return registered.entrySet().stream()
					.filter((e) -> e.getKey().equalsIgnoreCase(name))
					.map(Map.Entry::getValue)
					.findFirst().orElse(UNKNOWN);
	}
	
	public static final MediaQuality fromString(String quality, MediaType mediaType) {
		Objects.requireNonNull(mediaType);
		return quality != null && !quality.isBlank() && !parsers.isEmpty()
					? parsers.stream()
						.map((p) -> p.parse(quality, mediaType))
						.filter(Objects::nonNull)
						.findFirst().orElse(UNKNOWN)
					: UNKNOWN;
	}
	
	public static final MediaQuality fromResolution(MediaResolution resolution) {
		if(resolution == null || resolution == MediaResolution.UNKNOWN)
			return UNKNOWN;
		// Try to match with progressive scan qualities
		QualityValue value = new VideoQualityValue(resolution.height());
		return Stream.of(validQualities())
				     .filter((q) -> q.mediaType().is(MediaType.VIDEO))
				     .sorted(reversedComparatorKeepOrder())
				     .filter((q) -> value.compareTo(q.value()) >= 0)
				     .findFirst().orElse(UNKNOWN);
	}
	
	public static final MediaQuality fromSampleRate(int sampleRate) {
		if(sampleRate <= 0) return UNKNOWN;
		QualityValue value = new AudioQualityValue(0, sampleRate, 0);
		return Stream.of(validQualities())
				     .filter((q) -> q.mediaType().is(MediaType.AUDIO))
				     .sorted(reversedComparatorKeepOrder())
				     .filter((q) -> value.compareTo(q.value()) >= 0)
				     .findFirst().orElse(UNKNOWN);
	}
	
	public static final MediaQuality[] validQualities() {
		return validQualities.value();
	}
	
	public static final void addStringParser(MediaQualityParser parser) {
		parsers.add(Objects.requireNonNull(parser));
	}
	
	private static final Builder builder(boolean isUnknown) {
		return new Builder(isUnknown);
	}
	
	public static final Builder builder() {
		return builder(false);
	}
	
	public MediaQuality withValue(QualityValue newValue) {
		if(value.equals(Objects.requireNonNull(newValue)))
			return this; // Optimization
		return new MediaQuality(false, generateValidName(name), mediaType, Objects.requireNonNull(newValue), synonyms);
	}
	
	public String name() {
		return name;
	}
	
	public MediaType mediaType() {
		return mediaType;
	}
	
	public QualityValue value() {
		return value;
	}
	
	public List<String> synonyms() {
		return synonyms;
	}
	
	// Equal-like method for more abstract equality check
	public boolean is(MediaQuality quality) {
		if(this == quality)
			return true;
		if(quality == null)
			return false;
		if(getClass() != quality.getClass())
			return false;
		MediaQuality other = (MediaQuality) quality;
		return Objects.equals(mediaType, other.mediaType)
		        && Objects.equals(removeNameSuffix(name), removeNameSuffix(other.name))
		        && Objects.equals(synonyms, other.synonyms);
	}
	
	// Method just for convenience
	public boolean isAnyOf(MediaQuality... qualities) {
		return Stream.of(qualities).anyMatch(this::is);
	}
	
	@Override
	public int compareTo(MediaQuality other) {
		if(this == other) return 0;
		
		if(mediaType.is(other.mediaType)) {
			boolean deferThis = value.deferComparison();
			boolean deferOther = other.value.deferComparison();
			
			if(deferThis && deferOther) {
				int cmp = Utils.compareIndex(this, other, values(), MediaQuality::equalNames);
				return cmp == 0
							? value.compareTo(other.value)
							: cmp;
			} else {
				int cmp = value.compareTo(other.value);
				return cmp == 0
							? Utils.compareIndex(this, other, values(), MediaQuality::equalNames)
							: cmp;
			}
		}
		
		if(mediaType.is(MediaType.UNKNOWN))
			return other.mediaType.is(MediaType.UNKNOWN) ? 0 : -1;
		else if(other.mediaType.is(MediaType.UNKNOWN))
			return 1;
		
		return Utils.compareIndex(mediaType, other.mediaType, MediaType.values());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(mediaType, name, synonyms, value);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		MediaQuality other = (MediaQuality) obj;
		return Objects.equals(mediaType, other.mediaType)
		        && Objects.equals(removeNameSuffix(name), removeNameSuffix(other.name))
		        && Objects.equals(synonyms, other.synonyms)
		        && Objects.equals(value, other.value);
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public static final class Builder {
		
		private static final QualityValue VALUE_UNKNOWN = UnknownQualityValue.INSTANCE;
		private static final List<String> EMPTY_SYNONYMS = List.of();
		
		private final boolean isUnknown;
		private String name;
		private MediaType mediaType;
		private QualityValue value;
		private List<String> synonyms;
		
		// Special constructor for UNKNOWN quality
		private Builder(boolean isUnknown) {
			this.isUnknown = isUnknown;
			mediaType = MediaType.UNKNOWN;
			value = VALUE_UNKNOWN;
			synonyms = EMPTY_SYNONYMS;
		}
		
		public MediaQuality build() {
			return new MediaQuality(isUnknown, Objects.requireNonNull(name),
			                        Objects.requireNonNull(mediaType),
			                        isUnknown ? value : requireValidValue(value, mediaType),
			                        Objects.requireNonNull(Utils.nonNullContent(synonyms)));
		}
		
		public Builder name(String name) {
			this.name = Objects.requireNonNull(name);
			return this;
		}
		
		public Builder mediaType(MediaType mediaType) {
			this.mediaType = Objects.requireNonNull(mediaType);
			return this;
		}
		
		public Builder value(QualityValue value) {
			this.value = requireValidValue(value, mediaType);
			return this;
		}
		
		public Builder synonyms(String... synonyms) {
			this.synonyms = List.of(Objects.requireNonNull(Utils.nonNullContent(synonyms)));
			return this;
		}
		
		public String name() {
			return name;
		}
		
		public MediaType mediaType() {
			return mediaType;
		}
		
		public QualityValue value() {
			return value;
		}
		
		public List<String> synonyms() {
			return synonyms;
		}
	}
	
	public static abstract class QualityValue implements Comparable<QualityValue> {
		
		public abstract MediaType type();
		public abstract boolean deferComparison();
		public abstract boolean equals(Object obj);
		public abstract int hashCode();
	}
	
	private static final class UnknownQualityValue extends QualityValue {
		
		public static final QualityValue INSTANCE = new UnknownQualityValue();
		
		private UnknownQualityValue() {
		}
		
		@Override
		public MediaType type() {
			return MediaType.UNKNOWN;
		}
		
		@Override
		public boolean deferComparison() {
			return false;
		}
		
		@Override
		public int compareTo(QualityValue qv) {
			return this == qv ? 0 : -1;
		}
		
		@Override
		public int hashCode() {
			return 0;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			return true;
		}
	}
	
	public static class VideoQualityValue extends QualityValue {
		
		private final int height;
		private final boolean deferComparison;
		
		public VideoQualityValue(int height) {
			this(height, false);
		}
		
		public VideoQualityValue(int height, boolean deferComparison) {
			this.height = height;
			this.deferComparison = deferComparison;
		}
		
		@Override
		public MediaType type() {
			return MediaType.VIDEO;
		}
		
		@Override
		public boolean deferComparison() {
			return deferComparison;
		}
		
		public int height() {
			return height;
		}
		
		@Override
		public int compareTo(QualityValue qv) {
			if(this == qv || !(qv instanceof VideoQualityValue))
				return 0;
			VideoQualityValue other = (VideoQualityValue) qv;
			return Integer.compare(height, other.height);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(height);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			VideoQualityValue other = (VideoQualityValue) obj;
			return height == other.height;
		}
	}
	
	public static class AudioQualityValue extends QualityValue {
		
		private final int bandwidth;
		private final int sampleRate;
		private final int bitRate;
		private final boolean deferComparison;
		
		public AudioQualityValue(int bandwidth, int sampleRate, int bitRate) {
			this(bandwidth, sampleRate, bitRate, false);
		}
		
		public AudioQualityValue(int bandwidth, int sampleRate, int bitRate, boolean deferComparison) {
			this.bandwidth = bandwidth;
			this.sampleRate = sampleRate;
			this.bitRate = bitRate;
			this.deferComparison = deferComparison;
		}
		
		@Override
		public MediaType type() {
			return MediaType.AUDIO;
		}
		
		@Override
		public boolean deferComparison() {
			return deferComparison;
		}
		
		public int bandwidth() {
			return bandwidth;
		}
		
		public int sampleRate() {
			return sampleRate;
		}
		
		public int bitRate() {
			return bitRate;
		}
		
		@Override
		public int compareTo(QualityValue qv) {
			if(this == qv || !(qv instanceof AudioQualityValue))
				return 0;
			AudioQualityValue other = (AudioQualityValue) qv;
			return Utils.compare(bandwidth, other.bandwidth, sampleRate, other.sampleRate,
			                     bitRate, other.bitRate);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(bandwidth, bitRate, sampleRate);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			AudioQualityValue other = (AudioQualityValue) obj;
			return bandwidth == other.bandwidth
			        && bitRate == other.bitRate
			        && sampleRate == other.sampleRate;
		}
	}
	
	public static interface MediaQualityParser {
		
		MediaQuality parse(String string, MediaType mediaType);
	}
	
	private static final class ProgressiveScanStringParser implements MediaQualityParser {
		
		private static final Regex REGEX = Regex.of("^(\\d+)p?$");
		
		@Override
		public MediaQuality parse(String string, MediaType mediaType) {
			if(!Objects.requireNonNull(mediaType).is(MediaType.VIDEO))
				return null;
			MediaQuality parsed = null;
			Matcher matcher = REGEX.matcher(string);
			if(matcher.matches()) {
				QualityValue value = new VideoQualityValue(Integer.valueOf(matcher.group(1)));
				parsed = Stream.of(validQualities())
					.filter((q) -> q.mediaType().is(MediaType.VIDEO)
					                    && q.name().startsWith("P")
					                    && q.value().compareTo(value) == 0)
					.findFirst().orElse(null); // Do not return UNKNOWN, if not parsed
			}
			return parsed;
		}
	}
	
	private static final class SynonymStringParser implements MediaQualityParser {
		
		@Override
		public MediaQuality parse(String string, MediaType mediaType) {
			Objects.requireNonNull(mediaType);
			String lcString = string.toLowerCase();
			return Stream.of(validQualities())
				.filter((q) -> q.mediaType().is(mediaType)
				                   && Utils.contains(q.synonyms(), lcString, String::toLowerCase))
				.findFirst().orElse(null); // Do not return UNKNOWN, if not parsed
		}
	}
}