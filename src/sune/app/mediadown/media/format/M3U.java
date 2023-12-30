package sune.app.mediadown.media.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import sune.app.mediadown.download.segment.RemoteFileSegment;
import sune.app.mediadown.download.segment.RemoteFileSegmentsHolder;
import sune.app.mediadown.media.MediaConstants;
import sune.app.mediadown.media.MediaProtection;
import sune.app.mediadown.media.MediaQuality;
import sune.app.mediadown.media.MediaResolution;
import sune.app.mediadown.media.MediaUtils;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.net.Web.Response;
import sune.app.mediadown.util.CheckedFunction;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Regex;
import sune.app.mediadown.util.Utils;

/** @since 00.02.05 */
public final class M3U {
	
	// Forbid anyone to create an instance of this class
	private M3U() {
	}
	
	private static final CheckedFunction<URI, Response.OfStream> streamResolver(Request request) {
		return ((uri) -> Web.requestStream(request.ofURI(uri)));
	}
	
	private static final CheckedFunction<URI, Response.OfStream> streamResolver() {
		return ((uri) -> Web.requestStream(Request.of(uri).GET()));
	}
	
	/** @since 00.02.09 */
	private static final List<M3UCombinedFile> build(M3UReaderResult result) {
		Map<String, List<M3UFileBuilder>> groups = result.groups();
		List<M3UCombinedFile> combinedFiles = new ArrayList<>();
		
		for(M3UFileBuilder file : result.files()) {
			List<M3UCombinedFileBuilder> builders = new ArrayList<>();
			String audioGroup = file.attribute("audio");
			String subtitlesGroup = file.attribute("subtitles");
			
			if(audioGroup != null) {
				List<M3UFileBuilder> groupFiles = groups.get(audioGroup);
				
				if(groupFiles != null) {
					for(M3UFileBuilder audio : groupFiles) {
						builders.add((new M3UCombinedFileBuilder()).video(file).audio(audio));
					}
				}
			}
			
			if(builders.isEmpty()) {
				builders.add((new M3UCombinedFileBuilder()).video(file));
			}
			
			if(subtitlesGroup != null) {
				List<M3UFileBuilder> groupFiles = groups.get(subtitlesGroup);
				
				if(groupFiles != null) {
					for(M3UFileBuilder subtitles : groupFiles) {
						for(M3UCombinedFileBuilder builder : builders) {
							builder.addSubtitles(subtitles);
						}
					}
				}
			}
			
			for(M3UCombinedFileBuilder builder : builders) {
				combinedFiles.add(builder.build());
			}
		}
		
		return combinedFiles;
	}
	
	public static final List<M3UCombinedFile> parse(Request request) throws Exception {
		URI baseURI = Net.baseURI(request.uri());
		
		try(M3UReader reader = new M3UReader(baseURI, request.uri(), streamResolver(request), null)) {
			return build(reader.read());
		}
	}
	
	public static final List<M3UCombinedFile> parse(String uri, String content) throws Exception {
		URI uriObj = Net.uri(uri), baseURI = Net.baseURI(uriObj);
		
		try(M3UReader reader = new M3UReader(baseURI, uriObj, streamResolver(), content)) {
			return build(reader.read());
		}
	}
	
	public static final class M3USegment extends RemoteFileSegment {
		
		private final int index;
		/** @since 00.02.09 */
		private final String dateTime;
		
		protected M3USegment(int index, URI uri, double duration, String dateTime) {
			super(uri, MediaConstants.UNKNOWN_SIZE, duration);
			this.index = index;
			this.dateTime = dateTime;
		}
		
		public int index() {
			return index;
		}
		
		/** @since 00.02.09 */
		public String dateTime() {
			return dateTime;
		}
	}
	
	/** @since 00.02.09 */
	public static enum M3UFileType {
		
		VIDEO, AUDIO, SUBTITLES;
	}
	
	public static final class M3UFile {
		
		/** @since 00.02.09 */
		private final M3UFileType type;
		private final URI uri;
		private final RemoteFileSegmentsHolder segmentsHolder;
		private final double duration;
		private final MediaResolution resolution;
		private final String version;
		private final M3UKey key;
		/** @since 00.02.09 */
		private final Map<String, String> attributes;
		/** @since 00.02.09 */
		private MediaProtection protection;
		
		protected M3UFile(M3UFileType type, URI uri, RemoteFileSegmentsHolder segmentsHolder, double duration,
				MediaResolution resolution, String version, M3UKey key, Map<String, String> attributes) {
			this.type = Objects.requireNonNull(type);
			this.uri = Objects.requireNonNull(uri);
			this.segmentsHolder = Objects.requireNonNull(segmentsHolder);
			this.duration = duration;
			this.resolution = Objects.requireNonNull(resolution);
			this.version = version; // May be null
			this.key = Objects.requireNonNull(key);
			this.attributes = attributes; // May be null
		}
		
		/** @since 00.02.09 */
		private static final String emptyIfNull(String value) {
			return value != null ? value : "";
		}
		
		public M3UFileType type() {
			return type;
		}
		
		public URI uri() {
			return uri;
		}
		
		public double duration() {
			return duration;
		}
		
		public MediaResolution resolution() {
			return resolution;
		}
		
		public String version() {
			return version;
		}
		
		public RemoteFileSegmentsHolder segmentsHolder() {
			return segmentsHolder;
		}
		
		public M3UKey key() {
			return key;
		}
		
		/** @since 00.02.09 */
		public Map<String, String> attributes() {
			return attributes;
		}
		
		/** @since 00.02.09 */
		public String attribute(String name) {
			return attributes == null ? null : attributes.get(name);
		}
		
		/** @since 00.02.09 */
		public String attribute(String name, String defaultValue) {
			return attributes == null ? defaultValue : attributes.getOrDefault(name, defaultValue);
		}
		
		/** @since 00.02.09 */
		public MediaProtection protection() {
			if(protection == null && key.isPresent()) {
				protection = MediaProtection.ofUnknown()
					.content(emptyIfNull(key.uri()))
					.contentType(emptyIfNull(key.keyFormat()))
					.scheme(emptyIfNull(key.method()))
					.build();
			}
			
			return protection;
		}
	}
	
	/** @since 00.02.09 */
	public static final class M3UCombinedFile {
		
		private final M3UFile video;
		private final M3UFile audio;
		private final List<M3UFile> subtitles;
		
		private M3UCombinedFile(M3UFile video, M3UFile audio, List<M3UFile> subtitles) {
			this.video = video;
			this.audio = audio;
			this.subtitles = subtitles;
		}
		
		public static final M3UCombinedFile ofCombined(M3UFile videoAndAudio) {
			return new M3UCombinedFile(Objects.requireNonNull(videoAndAudio), null, null);
		}
		
		public static final M3UCombinedFile ofCombined(M3UFile videoAndAudio, List<M3UFile> subtitles) {
			return new M3UCombinedFile(Objects.requireNonNull(videoAndAudio), null, Objects.requireNonNull(subtitles));
		}
		
		public static final M3UCombinedFile ofSeparate(M3UFile video, M3UFile audio) {
			return new M3UCombinedFile(Objects.requireNonNull(video), Objects.requireNonNull(audio), null);
		}
		
		public static final M3UCombinedFile ofSeparate(M3UFile video, M3UFile audio, List<M3UFile> subtitles) {
			return new M3UCombinedFile(
				Objects.requireNonNull(video),
				Objects.requireNonNull(audio),
				Objects.requireNonNull(subtitles)
			);
		}
		
		public M3UFile video() {
			return video;
		}
		
		public M3UFile audio() {
			return audio;
		}
		
		public List<M3UFile> subtitles() {
			return subtitles;
		}
		
		public boolean hasSeparateAudio() {
			return audio != null;
		}
		
		public boolean hasSubtitles() {
			return subtitles != null && !subtitles.isEmpty();
		}
	}
	
	public static final class M3UKey {
		
		private final String method;
		private final String uri;
		private final String iv;
		private final String keyFormat;
		private final String keyFormatVersions;
		
		public M3UKey(String method, String uri, String iv, String keyFormat, String keyFormatVersions) {
			this.method = Objects.requireNonNull(method);
			this.uri = uri;
			this.iv = iv;
			this.keyFormat = keyFormat;
			this.keyFormatVersions = keyFormatVersions;
		}
		
		public static final M3UKey none() {
			return new M3UKey("NONE", null, null, null, null);
		}
		
		public String method() {
			return method;
		}
		
		public String uri() {
			return uri;
		}
		
		public String iv() {
			return iv;
		}
		
		public String keyFormat() {
			return keyFormat;
		}
		
		public String keyFormatVersions() {
			return keyFormatVersions;
		}
		
		public boolean isPresent() {
			return !method.equals("NONE");
		}
	}
	
	/** @since 00.02.09 */
	protected static final class M3UCombinedFileBuilder {
		
		private M3UFileBuilder video;
		private M3UFileBuilder audio;
		private List<M3UFileBuilder> subtitles;
		
		public M3UCombinedFileBuilder() {
		}
		
		public M3UCombinedFile build() {
			if(video == null && audio == null && (subtitles == null || subtitles.isEmpty())) {
				throw new IllegalArgumentException("All files are null or empty");
			}
			
			return new M3UCombinedFile(
				video == null ? null : video.build(),
				audio == null ? null : audio.build(),
				subtitles == null ? null : subtitles.stream().map(M3UFileBuilder::build).collect(Collectors.toList())
			);
		}
		
		public M3UCombinedFileBuilder video(M3UFileBuilder video) {
			this.video = video;
			return this;
		}
		
		public M3UCombinedFileBuilder audio(M3UFileBuilder audio) {
			this.audio = audio;
			return this;
		}
		
		public M3UCombinedFileBuilder addSubtitles(List<M3UFileBuilder> subtitles) {
			if(subtitles == null) {
				return this;
			}
			
			if(this.subtitles == null) {
				this.subtitles = new ArrayList<>(subtitles.size());
			}
			
			this.subtitles.addAll(subtitles);
			return this;
		}
		
		public M3UCombinedFileBuilder addSubtitles(M3UFileBuilder... subtitles) {
			return subtitles.length == 0 ? this : addSubtitles(List.of(subtitles));
		}
	}
	
	protected static final class M3USegmentBuilder {
		
		private int index;
		private URI uri;
		private double duration;
		/** @since 00.02.09 */
		private String dateTime;
		private boolean dirty;
		
		private void markDirty() {
			dirty = true;
		}
		
		public void index(int index) {
			this.index = index;
			markDirty();
		}
		
		public void uri(URI uri) {
			this.uri = uri;
			markDirty();
		}
		
		public void duration(double duration) {
			this.duration = duration;
			markDirty();
		}
		
		/** @since 00.02.09 */
		public void dateTime(String dateTime) {
			this.dateTime = dateTime;
			markDirty();
		}
		
		public void reset() {
			index = 0;
			uri = null;
			duration = 0.0;
			dateTime = null;
			dirty = false;
		}
		
		public M3USegment build() {
			return new M3USegment(index, uri, duration, dateTime);
		}
		
		public boolean isDirty() {
			return dirty;
		}
	}
	
	protected static final class M3UFileBuilder {
		
		private M3UFileType type = M3UFileType.VIDEO;
		private URI uri;
		private List<M3USegment> segments;
		private double duration;
		private MediaResolution resolution = MediaResolution.UNKNOWN;
		private String version;
		private M3UKey key;
		/** @since 00.02.09 */
		private Map<String, String> attributes;
		private boolean dirty;
		
		private void markDirty() {
			dirty = true;
		}
		
		public void type(M3UFileType type) {
			this.type = type;
			markDirty();
		}
		
		public void uri(URI uri) {
			this.uri = uri;
			markDirty();
		}
		
		public void addSegment(M3USegment segment) {
			Objects.requireNonNull(segment);
			
			if(segments == null) {
				segments = new ArrayList<>();
			}
			
			segments.add(segment);
			duration += segment.duration();
			markDirty();
		}
		
		public void resolution(MediaResolution resolution) {
			this.resolution = resolution;
			markDirty();
		}
		
		public void version(String version) {
			this.version = version;
			markDirty();
		}
		
		public void key(M3UKey key) {
			this.key = key;
			markDirty();
		}
		
		/** @since 00.02.09 */
		public void addAttribute(String name, String value) {
			if(attributes == null) {
				attributes = new LinkedHashMap<>();
			}
			
			attributes.put(name, value);
			markDirty();
		}
		
		/** @since 00.02.09 */
		public void attributes(Map<String, String> attributes) {
			this.attributes = attributes;
			markDirty();
		}
		
		public void reset() {
			// Do not reset global attributes: key, version
			type = M3UFileType.VIDEO;
			uri = null;
			segments = null;
			duration = 0.0;
			resolution = MediaResolution.UNKNOWN;
			attributes = null;
			dirty = false;
		}
		
		/** @since 00.02.09 */
		private RemoteFileSegmentsHolder segmentsHolder() {
			return new RemoteFileSegmentsHolder(segments, duration);
		}
		
		/** @since 00.02.09 */
		private Map<String, String> attributes() {
			return attributes != null ? new LinkedHashMap<>(attributes) : null;
		}
		
		public M3UFile build() {
			return new M3UFile(type, uri, segmentsHolder(), duration, resolution, version, key, attributes());
		}
		
		/** @since 00.02.09 */
		public M3UFileBuilder copy() {
			M3UFileBuilder copy = new M3UFileBuilder();
			copy.type = type;
			copy.uri = uri;
			
			if(segments != null) {
				copy.segments = new ArrayList<>(segments);
			}
			
			copy.duration = duration;
			copy.resolution = resolution;
			copy.version = version;
			copy.key = key;
			
			if(attributes != null) {
				copy.attributes = new LinkedHashMap<>(attributes);
			}
			
			copy.dirty = true;
			return copy;
		}
		
		public boolean isDirty() {
			return dirty;
		}
		
		public MediaResolution resolution() {
			return resolution;
		}
		
		/** @since 00.02.09 */
		public String attribute(String name) {
			return attributes != null ? attributes.get(name) : null;
		}
	}
	
	/** @since 00.02.09 */
	protected static final class M3UReaderResult {
		
		private final List<M3UFileBuilder> files;
		private final Map<String, List<M3UFileBuilder>> groups;
		
		public M3UReaderResult(List<M3UFileBuilder> files, Map<String, List<M3UFileBuilder>> groups) {
			this.files = Objects.requireNonNull(files);
			this.groups = Objects.requireNonNull(groups);
		}
		
		public List<M3UFileBuilder> files() {
			return files;
		}
		
		public Map<String, List<M3UFileBuilder>> groups() {
			return groups;
		}
	}
	
	protected static final class M3UReader implements AutoCloseable {
		
		private static final char CHAR_META                    = '#';
		private static final char CHAR_META_DELIMITER_KEYVALUE = ':';
		private static final char CHAR_META_DELIMITER_INFO     = ',';
		private static final char CHAR_META_DELIMITER_ASSIGN   = '=';
		
		private static final String NAME_HEADER           = "EXTM3U";
		private static final String NAME_VERSION          = "EXT-X-VERSION";
		private static final String NAME_SEQUENCE         = "EXT-X-MEDIA-SEQUENCE";
		private static final String NAME_STREAM_INFO      = "EXT-X-STREAM-INF";
		private static final String NAME_KEY              = "EXT-X-KEY";
		/** @since 00.02.09 */
		private static final String NAME_MEDIA            = "EXT-X-MEDIA";
		/** @since 00.02.09 */
		private static final String NAME_SEGMENT_INFO     = "EXTINF";
		/** @since 00.02.09 */
		private static final String NAME_SEGMENT_DATETIME = "EXT-X-PROGRAM-DATE-TIME";
		
		private static final Regex PATTERN_ATTRIBUTE_LIST
			= Regex.of("([A-Z0-9\\-]+)=(\"[^\"\\x0A\\x0D]+\"|[^,\\x0A\\x0D]+)");
		
		private final URI baseURI;
		private final URI uri;
		private final CheckedFunction<URI, Response.OfStream> streamResolver;
		private final Response.OfStream response;
		private final BufferedReader reader;
		
		private String version;
		private int sequenceIndex;
		private final List<M3UFileBuilder> files = new ArrayList<>();
		private M3USegmentBuilder segmentBuilder;
		private M3UFileBuilder fileBuilder;
		private M3UKey key;
		private final Map<String, List<M3UFileBuilder>> groups = new LinkedHashMap<>();
		
		private M3UReader(URI baseURI, URI uri, CheckedFunction<URI, Response.OfStream> streamResolver, String content)
				throws Exception {
			this.baseURI = Objects.requireNonNull(baseURI);
			this.uri = Objects.requireNonNull(uri);
			this.streamResolver = Objects.requireNonNull(streamResolver);
			
			Response.OfStream sr = null;
			Reader r = null;
			if(content != null) {
				r = new StringReader(content);
			} else {
				sr = streamResolver.apply(uri);
				r = new InputStreamReader(sr.stream());
			}
			
			this.response = sr;
			this.reader = new BufferedReader(r);
		}
		
		private final void throwExceptionInvalid(String message) throws IOException {
			throw new IOException("Invalid M3U file" + (message != null ? ": " + message : ""));
		}
		
		/** @since 00.02.09 */
		private final String nextLine() throws IOException {
			String line;
			while((line = reader.readLine()) != null && line.isBlank());
			return line;
		}
		
		private final boolean isMetaLine(String line) {
			return !line.isEmpty() && line.charAt(0) == CHAR_META;
		}
		
		private final Pair<String, String> parseMetaLine(String line) {
			String name = line, value = null;
			
			int index;
			if((index = line.indexOf(CHAR_META_DELIMITER_KEYVALUE)) > 0) {
				name = line.substring(1, index);
				value = line.substring(index + 1);
			}
			
			return new Pair<>(name, value);
		}
		
		/** @since 00.02.09 */
		private final void parseSegmentInfo(String value) throws IOException {
			String duration = value;
			
			int index;
			if((index = value.indexOf(CHAR_META_DELIMITER_INFO)) > 0) {
				duration = value.substring(0, index);
			}
			
			segmentBuilder.duration(Double.valueOf(duration));
			segmentBuilder.index(sequenceIndex++);
		}
		
		/** @since 00.02.09 */
		private final void parseSegmentDateTime(String value) throws IOException {
			segmentBuilder.dateTime(value);
		}
		
		/** @since 00.02.09 */
		private final void parseStreamInfo(String value) throws IOException {
			String[] values = value.split("" + CHAR_META_DELIMITER_INFO);
			
			if(values.length <= 0) {
				throwExceptionInvalid("Invalid stream info meta data");
			}
			
			for(String val : values) {
				String partName = val, partValue = null;
				
				int index;
				if((index = val.indexOf(CHAR_META_DELIMITER_ASSIGN)) > 0) {
					partName = val.substring(0, index);
					partValue = val.substring(index + 1);
				}
				
				switch(partName.toLowerCase()) {
					case "resolution":
						fileBuilder.resolution(MediaResolution.fromString(partValue));
						break;
					case "audio":
						fileBuilder.addAttribute("audio", Utils.unquote(partValue));
						break;
					case "subtitles":
						fileBuilder.addAttribute("subtitles", Utils.unquote(partValue));
						break;
					case "bandwidth":
						int bandwidth = Integer.valueOf(partValue);
						MediaQuality quality = MediaUtils.estimateMediaQualityFromBandwidth(bandwidth);
						fileBuilder.addAttribute("estimatedQuality", quality.name());
						break;
					default:
						// Do nothing
						break;
				}
			}
			
			// Ensure that the builder is dirty to notify the parser
			if(!fileBuilder.isDirty()) {
				fileBuilder.markDirty();
			}
		}
		
		// Reference: https://datatracker.ietf.org/doc/html/rfc8216#section-4.2
		private final Map<String, String> parseAttributeList(String value) {
			Map<String, String> attrs = new LinkedHashMap<>();
			Matcher matcher = PATTERN_ATTRIBUTE_LIST.matcher(value);
			
			while(matcher.find()) {
				String attrName = matcher.group(1).toUpperCase();
				String attrValue = matcher.group(2).replaceAll("(?:^\"|\"$)", "");
				attrs.put(attrName, attrValue);
			}
			
			return attrs;
		}
		
		// Reference: https://datatracker.ietf.org/doc/html/rfc8216#section-4.3.2.4
		private final void parseKey(String value) {
			Map<String, String> attrs = parseAttributeList(value);
			String method = attrs.get("METHOD");
			String uri = attrs.get("URI");
			String iv = attrs.get("IV");
			String keyFormat = attrs.get("KEYFORMAT");
			String keyFormatVersions = attrs.get("KEYFORMATVERSIONS");
			if(method == null) method = "NONE";
			key = new M3UKey(method, uri, iv, keyFormat, keyFormatVersions);
		}
		
		/** @since 00.02.09 */
		private final void parseMedia(String value) throws Exception {
			Map<String, String> attrs = parseAttributeList(value);
			String type = attrs.get("TYPE");
			String groupId = attrs.get("GROUP-ID");
			List<M3UFileBuilder> files = null;
			
			switch(type.toLowerCase()) {
				case "audio": {
					String language = attrs.get("LANGUAGE");
					int channels = Integer.valueOf(attrs.getOrDefault("CHANNELS", "-1"));
					String uri = attrs.get("URI");
					
					URI resolvedURI = resolveURI(uri);
					URI resolvedBaseURI = Net.isRelativeURI(uri) ? resolveURI(uri) : Net.baseURI(Net.uri(uri));
					
					try(M3UReader reader = new M3UReader(resolvedBaseURI, resolvedURI, streamResolver, null)) {
						files = reader.read().files();
					}
					
					for(M3UFileBuilder file : files) {
						file.type(M3UFileType.AUDIO);
						file.addAttribute("channels", String.valueOf(channels));
						file.addAttribute("language", String.valueOf(language));
					}
					
					break;
				}
				case "subtitles": {
					String language = attrs.get("LANGUAGE");
					String uri = attrs.get("URI");
					URI resolvedURI = resolveURI(uri);
					URI resolvedBaseURI = Net.isRelativeURI(uri) ? resolveURI(uri) : Net.baseURI(Net.uri(uri));
					
					try(M3UReader reader = new M3UReader(resolvedBaseURI, resolvedURI, streamResolver, null)) {
						files = reader.read().files();
					}
					
					for(M3UFileBuilder file : files) {
						file.type(M3UFileType.SUBTITLES);
						file.addAttribute("language", String.valueOf(language));
					}
					
					break;
				}
			}
			
			if(files == null) {
				return;
			}
			
			final var list = files;
			groups.compute(groupId, (k, v) -> {
				if(v == null) {
					return list;
				}
				
				v.addAll(list);
				return v;
			});
		}
		
		private final void updateMetaData(String name, String value) throws Exception {
			switch(name) {
				case NAME_VERSION: version = value; break;
				case NAME_SEQUENCE: sequenceIndex = Integer.valueOf(value); break;
				case NAME_SEGMENT_INFO: parseSegmentInfo(value); break;
				case NAME_SEGMENT_DATETIME: parseSegmentDateTime(value); break;
				case NAME_STREAM_INFO: parseStreamInfo(value); break;
				case NAME_MEDIA: parseMedia(value); break;
				case NAME_KEY: parseKey(value); break;
				default: /* Do nothing */ break;
			}
		}
		
		private final URI resolveURI(String uri) {
			return Net.isRelativeURI(uri) ? Net.resolve(baseURI, uri) : Net.uri(uri);
		}
		
		private final List<M3UFileBuilder> readStreamInfo(String uri) throws Exception {
			URI resolvedURI = resolveURI(uri);
			URI resolvedBaseURI = Net.isRelativeURI(uri) ? resolveURI(uri) : Net.baseURI(Net.uri(uri));
			
			try(M3UReader reader = new M3UReader(resolvedBaseURI, resolvedURI, streamResolver, null)) {
				return reader.read().files();
			}
		}
		
		public final M3UReaderResult read() throws Exception {
			String first = nextLine();
			
			if(first == null // EOF
					|| !isMetaLine(first) || !first.substring(1).equals(NAME_HEADER)) { // Check if the header line is present
				throwExceptionInvalid("No header");
			}
			
			segmentBuilder = new M3USegmentBuilder();
			fileBuilder = new M3UFileBuilder();
			
			// Parse the content
			for(String line; (line = nextLine()) != null;) {
				if(isMetaLine(line)) {
					Pair<String, String> data = parseMetaLine(line);
					updateMetaData(data.a, data.b);
				} else if(segmentBuilder.isDirty()) { // Segment URI
					segmentBuilder.uri(resolveURI(line));
					fileBuilder.addSegment(segmentBuilder.build());
					segmentBuilder.reset();
				} else if(fileBuilder.isDirty()) { // File URI
					List<M3UFileBuilder> builders = readStreamInfo(line);
					
					MediaResolution resolution = fileBuilder.resolution();
					Map<String, String> attributes = fileBuilder.attributes();
					
					for(M3UFileBuilder builder : builders) {
						builder.resolution(resolution);
						builder.attributes(attributes);
					}
					
					files.addAll(builders);
					fileBuilder.reset();
				}
			}
			
			// If there were only segments in this file
			if(files.isEmpty()) {
				fileBuilder.uri(uri);
				fileBuilder.version(version);
				if(key == null) key = M3UKey.none();
				fileBuilder.key(key);
				files.add(fileBuilder);
			}
			
			return new M3UReaderResult(files, groups);
		}
		
		@Override
		public final void close() throws Exception {
			reader.close();
			
			if(response != null) {
				response.close();
			}
		}
	}
}