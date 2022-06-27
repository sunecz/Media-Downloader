package sune.app.mediadown.media;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import sune.app.mediadown.util.Utils;

/** @since 00.02.05 */
public final class MediaFormat {
	
	private static final Map<String, MediaFormat> registered = new LinkedHashMap<>();
	private static boolean valuesInvalidated;
	private static MediaFormat[] values;
	private static MediaFormat[] outputFormats;
	
	private static final BiFunction<MediaFormat, String, Boolean> PREDICATE_EXTENSIONS
		= ((format, type) -> format.fileExtensions().contains(type));
	private static final BiFunction<MediaFormat, String, Boolean> PREDICATE_NAME
		= ((format, string) -> format.name().toLowerCase().equals(string));
	private static final BiFunction<MediaFormat, String, Boolean> PREDICATE_MIME_TYPE
		= ((format, mimeType) -> format.mimeTypes().contains(mimeType));
	
	// Special formats
	public static final MediaFormat UNKNOWN;
	// Video formats
	public static final MediaFormat MP4;
	public static final MediaFormat FLV;
	public static final MediaFormat AVI;
	public static final MediaFormat MKV;
	public static final MediaFormat WMV;
	public static final MediaFormat WEBM;
	public static final MediaFormat OGG;
	// Audio formats
	public static final MediaFormat MP3;
	public static final MediaFormat WAV;
	public static final MediaFormat WMA;
	public static final MediaFormat M4A;
	// Stream formats
	public static final MediaFormat M3U8;
	public static final MediaFormat DASH;
	// Subtitles formats
	public static final MediaFormat SRT;
	public static final MediaFormat VTT;
	
	static {
		UNKNOWN = new Builder().name("UNKNOWN").build();
		MP4 = new Builder().name("MP4").formatType(MediaFormatType.BOTH).mediaType(MediaType.VIDEO)
			        .fileExtensions("mp4").mimeTypes("video/mp4").string("MP4").build();
		FLV = new Builder().name("FLV").formatType(MediaFormatType.BOTH).mediaType(MediaType.VIDEO)
			        .fileExtensions("flv").mimeTypes("video/x-flv").string("FLV").build();
		AVI = new Builder().name("AVI").formatType(MediaFormatType.BOTH).mediaType(MediaType.VIDEO)
			        .fileExtensions("avi").mimeTypes("video/avi").string("AVI").build();
		MKV = new Builder().name("MKV").formatType(MediaFormatType.BOTH).mediaType(MediaType.VIDEO)
			        .fileExtensions("mkv").mimeTypes("video/x-matroska").string("MKV").build();
		WMV = new Builder().name("WMV").formatType(MediaFormatType.BOTH).mediaType(MediaType.VIDEO)
			        .fileExtensions("wmv").mimeTypes("video/x-ms-wmv").string("WMV").build();
		WEBM = new Builder().name("WEBM").formatType(MediaFormatType.BOTH).mediaType(MediaType.VIDEO)
			        .fileExtensions("webm").mimeTypes("video/webm").string("WEBM").build();
		OGG = new Builder().name("OGG").formatType(MediaFormatType.BOTH).mediaType(MediaType.VIDEO)
			        .fileExtensions("ogg").mimeTypes("video/ogg").string("OGG").build();
		MP3 = new Builder().name("MP3").formatType(MediaFormatType.BOTH).mediaType(MediaType.AUDIO)
			        .fileExtensions("mp3").mimeTypes("audio/mpeg").string("MP3").build();
		WAV = new Builder().name("WAV").formatType(MediaFormatType.BOTH).mediaType(MediaType.AUDIO)
			        .fileExtensions("wav").mimeTypes("audio/wav").string("WAV").build();
		WMA = new Builder().name("WMA").formatType(MediaFormatType.BOTH).mediaType(MediaType.AUDIO)
			        .fileExtensions("wma").mimeTypes("audio/x-ms-wma").string("WMA").build();
		M4A = new Builder().name("M4A").formatType(MediaFormatType.INPUT).mediaType(MediaType.AUDIO)
			        .fileExtensions("m4a").mimeTypes("audio/mp4").string("M4A").build();
		M3U8 = new Builder().name("M3U8").formatType(MediaFormatType.INPUT).mediaType(MediaType.VIDEO)
			        .fileExtensions("m3u8", "ts").mimeTypes("application/x-mpegurl", "application/vnd.apple.mpegurl")
			        .string("M3U8").build();
		DASH = new Builder().name("DASH").formatType(MediaFormatType.INPUT).mediaType(MediaType.VIDEO)
			        .fileExtensions("mpd", "m4s").mimeTypes("application/dash+xml").string("DASH").build();
		SRT = new Builder().name("SRT").formatType(MediaFormatType.INPUT).mediaType(MediaType.SUBTITLES)
					.fileExtensions("srt").mimeTypes("text/srt").string("SRT").build(); // Don't use text/plain as mime type
		VTT = new Builder().name("VTT").formatType(MediaFormatType.INPUT).mediaType(MediaType.SUBTITLES)
					.fileExtensions("vtt").mimeTypes("text/vtt").string("VTT").build(); // Don't use text/plain as mime type
	}
	
	private final String name;
	private final MediaFormatType formatType;
	private final MediaType mediaType;
	private final List<String> fileExtensions;
	private final List<String> mimeTypes;
	private final String string;
	
	private MediaFormat(String name, MediaFormatType formatType, MediaType mediaType, List<String> fileExtensions,
			List<String> mimeTypes, String string) {
		this.name = requireValidName(name);
		this.formatType = Objects.requireNonNull(formatType);
		this.mediaType = Objects.requireNonNull(mediaType);
		this.fileExtensions = Objects.requireNonNull(Utils.nonNullContent(fileExtensions));
		this.mimeTypes = Objects.requireNonNull(Utils.nonNullContent(mimeTypes));
		this.string = string != null ? string : name;
		register(this);
	}
	
	private static final String requireValidName(String name) {
		if(name == null || name.isBlank())
			throw new IllegalArgumentException("Name may be neither null nor blank.");
		return name;
	}
	
	private static final void register(MediaFormat format) {
		if(registered.putIfAbsent(format.name.toLowerCase(), format) != null)
			throw new IllegalStateException("Media format \"" + format.name + "\" already registered.");
		valuesInvalidated = true;
	}
	
	private static final void ensureValidStaticMembers() {
		if(values == null || valuesInvalidated) {
			Collection<MediaFormat> formats = registered.values();
			values = formats.toArray(MediaFormat[]::new);
			outputFormats = formats.stream().filter(MediaFormat::isOutputFormat).toArray(MediaFormat[]::new);
			valuesInvalidated = false;
		}
	}
	
	private static final <T> MediaFormat filter(BiFunction<MediaFormat, T, Boolean> predicate, T value) {
		return Stream.of(values()).filter((f) -> predicate.apply(f, value)).findFirst().orElse(UNKNOWN);
	}
	
	public static final MediaFormat[] values() {
		ensureValidStaticMembers();
		return values;
	}
	
	public static final MediaFormat ofName(String name) {
		return registered.entrySet().stream()
					.filter((e) -> e.getKey().equalsIgnoreCase(name))
					.map(Map.Entry::getValue)
					.findFirst().orElse(UNKNOWN);
	}
	
	public static final MediaFormat fromPath(Path path) {
		return fromPath(path.getFileName().toString());
	}
	
	public static final MediaFormat fromPath(String path) {
		return filter(PREDICATE_EXTENSIONS, Utils.fileType(path).toLowerCase());
	}
	
	public static final MediaFormat fromName(String string) {
		return filter(PREDICATE_NAME, string.toLowerCase());
	}
	
	public static final MediaFormat fromMimeType(String mimeType) {
		return filter(PREDICATE_MIME_TYPE, mimeType.toLowerCase());
	}
	
	public static final MediaFormat[] outputFormats() {
		ensureValidStaticMembers();
		return outputFormats;
	}
	
	public String name() {
		return name;
	}
	
	public MediaFormatType formatType() {
		return formatType;
	}
	
	public boolean isInputFormat() {
		return formatType == MediaFormatType.INPUT || formatType == MediaFormatType.BOTH;
	}
	
	public boolean isOutputFormat() {
		return formatType == MediaFormatType.OUTPUT || formatType == MediaFormatType.BOTH;
	}
	
	public MediaType mediaType() {
		return mediaType;
	}
	
	public List<String> fileExtensions() {
		return Collections.unmodifiableList(fileExtensions);
	}
	
	public List<String> mimeTypes() {
		return Collections.unmodifiableList(mimeTypes);
	}
	
	// Method just for convenience
	public boolean is(MediaFormat format) {
		return equals(format);
	}
	
	// Method just for convenience
	public boolean isAnyOf(MediaFormat... formats) {
		return Stream.of(formats).anyMatch(this::is);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(fileExtensions, formatType, mediaType, mimeTypes, name);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		MediaFormat other = (MediaFormat) obj;
		return Objects.equals(fileExtensions, other.fileExtensions)
		        && formatType == other.formatType
		        && Objects.equals(mediaType, other.mediaType)
		        && Objects.equals(mimeTypes, other.mimeTypes)
		        && Objects.equals(name, other.name);
	}
	
	@Override
	public String toString() {
		return string;
	}
	
	public static enum MediaFormatType {
		
		NONE, INPUT, OUTPUT, BOTH;
	}
	
	public static final class Builder {
		
		private static final List<String> EMPTY_FILE_EXTENSIONS = List.of();
		private static final List<String> EMPTY_MIME_TYPES = List.of();
		
		private String name;
		private MediaFormatType formatType;
		private MediaType mediaType;
		private List<String> fileExtensions;
		private List<String> mimeTypes;
		private String string;
		
		public Builder() {
			formatType = MediaFormatType.NONE;
			mediaType = MediaType.UNKNOWN;
			fileExtensions = EMPTY_FILE_EXTENSIONS;
			mimeTypes = EMPTY_MIME_TYPES;
		}
		
		public MediaFormat build() {
			return new MediaFormat(requireValidName(name), Objects.requireNonNull(formatType),
			                       Objects.requireNonNull(mediaType),
			                       Objects.requireNonNull(Utils.nonNullContent(fileExtensions)),
			                       Objects.requireNonNull(Utils.nonNullContent(mimeTypes)),
			                       string);
		}
		
		public Builder name(String name) {
			this.name = requireValidName(name);
			return this;
		}
		
		public Builder formatType(MediaFormatType formatType) {
			this.formatType = Objects.requireNonNull(formatType);
			return this;
		}
		
		public Builder mediaType(MediaType mediaType) {
			this.mediaType = Objects.requireNonNull(mediaType);
			return this;
		}
		
		public Builder fileExtensions(String... fileExtensions) {
			this.fileExtensions = List.of(Objects.requireNonNull(Utils.nonNullContent(fileExtensions)));
			return this;
		}
		
		public Builder mimeTypes(String... mimeTypes) {
			this.mimeTypes = List.of(Objects.requireNonNull(Utils.nonNullContent(mimeTypes)));
			return this;
		}
		
		public Builder string(String string) {
			this.string = string;
			return this;
		}
		
		public String name() {
			return name;
		}
		
		public MediaFormatType formatType() {
			return formatType;
		}
		
		public MediaType mediaType() {
			return mediaType;
		}
		
		public List<String> fileExtensions() {
			return fileExtensions;
		}
		
		public List<String> mimeTypes() {
			return mimeTypes;
		}
		
		public String string() {
			return string;
		}
	}
}