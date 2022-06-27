package sune.app.mediadown.media.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import sune.app.mediadown.download.segment.RemoteFileSegment;
import sune.app.mediadown.download.segment.RemoteFileSegmentable;
import sune.app.mediadown.download.segment.RemoteFileSegmentsHolder;
import sune.app.mediadown.media.MediaResolution;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.ThrowableFunction;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Web;
import sune.app.mediadown.util.Web.GetRequest;
import sune.app.mediadown.util.Web.Request;
import sune.app.mediadown.util.Web.StreamResponse;

/** @since 00.02.05 */
public final class M3U {
	
	// Forbid anyone to create an instance of this class
	private M3U() {
	}
	
	private static final ThrowableFunction<URI, StreamResponse> streamResolver(Request request) {
		return ((uri) -> Web.requestStream(request.setURL(Utils.url(uri))));
	}
	
	private static final ThrowableFunction<URI, StreamResponse> streamResolver() {
		return ((uri) -> Web.requestStream(new GetRequest(Utils.url(uri))));
	}
	
	public static final List<M3UFile> parse(Request request) throws Exception {
		URI baseURI = Utils.uri(Utils.baseURL(request.url.toString()));
		try(M3UReader reader = new M3UReader(baseURI, Utils.uri(request.url), streamResolver(request), null)) {
			return reader.read();
		}
	}
	
	public static final List<M3UFile> parse(String uri, String content) throws Exception {
		URI baseURI = Utils.uri(Utils.baseURL(uri));
		try(M3UReader reader = new M3UReader(baseURI, Utils.uri(uri), streamResolver(), content)) {
			return reader.read();
		}
	}
	
	public static final class M3USegment {
		
		private final int index;
		private final URI uri;
		private final double duration;
		private final String title;
		
		protected M3USegment(int index, URI uri, double duration, String title) {
			this.index = index;
			this.uri = Objects.requireNonNull(uri);
			this.duration = duration;
			this.title = title; // Title can be null
		}
		
		public final int index() {
			return index;
		}
		
		public final URI uri() {
			return uri;
		}
		
		public final double duration() {
			return duration;
		}
		
		public final String title() {
			return title;
		}
	}
	
	public static final class M3UFile implements RemoteFileSegmentable {
		
		private final URI uri;
		private final List<M3USegment> segments;
		private final double duration;
		private final MediaResolution resolution;
		private final String version;
		private List<RemoteFileSegmentsHolder> segmentsHolders;
		private final M3UKey key;
		
		protected M3UFile(URI uri, List<M3USegment> segments, double duration, MediaResolution resolution,
				String version, M3UKey key) {
			this.uri = Objects.requireNonNull(uri);
			this.segments = Collections.unmodifiableList(Objects.requireNonNull(segments));
			this.duration = duration;
			this.resolution = Objects.requireNonNull(resolution);
			this.version = version;
			this.key = Objects.requireNonNull(key);
		}
		
		public final URI uri() {
			return uri;
		}
		
		public final List<M3USegment> segments() {
			return segments;
		}
		
		public final double duration() {
			return duration;
		}
		
		public final MediaResolution resolution() {
			return resolution;
		}
		
		public final String version() {
			return version;
		}
		
		@Override
		public final List<RemoteFileSegmentsHolder> segmentsHolders() {
			if(segmentsHolders == null) {
				List<RemoteFileSegment> fileSegments = segments.stream()
					.map((seg) -> new RemoteFileSegment(seg.uri(), -1L))
					.collect(Collectors.toList());
				segmentsHolders = List.of(new RemoteFileSegmentsHolder(fileSegments, duration));
			}
			return segmentsHolders;
		}
		
		public M3UKey getKey() {
			return key;
		}
	}
	
	private static final class M3USegmentBuilder {
		
		private int index;
		private URI uri;
		private double duration;
		private String title;
		private boolean dirty;
		
		private final void markDirty() {
			dirty = true;
		}
		
		public final void index(int index) {
			this.index = index;
			markDirty();
		}
		
		public final void uri(URI uri) {
			this.uri = Objects.requireNonNull(uri);
			markDirty();
		}
		
		public final void duration(double duration) {
			this.duration = duration;
			markDirty();
		}
		
		public final void title(String title) {
			this.title = title;
			markDirty();
		}
		
		public final void reset() {
			index = 0;
			uri = null;
			duration = 0.0;
			title = null;
			dirty = false;
		}
		
		public final M3USegment build() {
			M3USegment segment = new M3USegment(index, uri, duration, title);
			reset();
			return segment;
		}
		
		public final boolean isDirty() {
			return dirty;
		}
	}
	
	private static final class M3UFileBuilder {
		
		private URI uri;
		private List<M3USegment> segments = new ArrayList<>();
		private double duration;
		private MediaResolution resolution = MediaResolution.UNKNOWN;
		private String version;
		private M3UKey key;
		private boolean dirty;
		
		private final void markDirty() {
			dirty = true;
		}
		
		public final void uri(URI uri) {
			this.uri = Objects.requireNonNull(uri);
			markDirty();
		}
		
		public final void addSegment(M3USegment segment) {
			Objects.requireNonNull(segment);
			segments.add(segment);
			duration += segment.duration();
			markDirty();
		}
		
		public final void resolution(MediaResolution resolution) {
			this.resolution = Objects.requireNonNull(resolution);
			markDirty();
		}
		
		public final void version(String version) {
			this.version = version;
			markDirty();
		}
		
		public void key(M3UKey key) {
			this.key = Objects.requireNonNull(key);
			markDirty();
		}
		
		public final void reset() {
			uri = null;
			segments = null;
			duration = 0.0;
			resolution = null;
			version = null;
			dirty = false;
		}
		
		public final M3UFile build() {
			M3UFile file = new M3UFile(uri, segments, duration, resolution, version, key);
			reset();
			return file;
		}
		
		public final boolean isDirty() {
			return dirty;
		}
		
		public final MediaResolution resolution() {
			return resolution;
		}
	}
	
	private static final class M3UReader implements AutoCloseable {
		
		private static final char CHAR_META                    = '#';
		private static final char CHAR_META_DELIMITER_KEYVALUE = ':';
		private static final char CHAR_META_DELIMITER_INFO     = ',';
		private static final char CHAR_META_DELIMITER_ASSIGN   = '=';
		
		private static final String NAME_HEADER      = "EXTM3U";
		private static final String NAME_VERSION     = "EXT-X-VERSION";
		private static final String NAME_SEQUENCE    = "EXT-X-MEDIA-SEQUENCE";
		private static final String NAME_STREAM_INFO = "EXT-X-STREAM-INF";
		private static final String NAME_KEY         = "EXT-X-KEY";
		private static final String NAME_SEGMENT     = "EXTINF";
		
		private static final Pattern PATTERN_ATTRIBUTE_LIST
			= Pattern.compile("([A-Z0-9\\-]+)=([^,\\x0A\\x0D]+|\"[^\"\\x0A\\x0D]+\")");
		
		private final URI baseURI;
		private final URI uri;
		private final ThrowableFunction<URI, StreamResponse> streamResolver;
		private final StreamResponse response;
		private final BufferedReader reader;
		
		// M3U meta information
		private String version;
		private int sequenceIndex;
		private final List<M3UFile> files = new ArrayList<>();
		private M3USegmentBuilder segmentBuilder;
		private M3UFileBuilder fileBuilder;
		private MediaResolution resolution;
		private M3UKey key;
		
		public M3UReader(URI baseURI, URI uri, ThrowableFunction<URI, StreamResponse> streamResolver,
				String content) throws Exception {
			this(Objects.requireNonNull(baseURI), Objects.requireNonNull(uri),
			     streamResolver, content, MediaResolution.UNKNOWN);
		}
		
		private M3UReader(URI baseURI, URI uri, ThrowableFunction<URI, StreamResponse> streamResolver,
				String content, MediaResolution resolution) throws Exception {
			this.baseURI = Objects.requireNonNull(baseURI);
			this.uri = Objects.requireNonNull(uri);
			this.streamResolver = Objects.requireNonNull(streamResolver);
			StreamResponse sr = null;
			Reader r = null;
			if(content != null) {
				r = new StringReader(content);
			} else {
				sr = streamResolver.apply(uri);
				r = new InputStreamReader(sr.stream);
			}
			this.response = sr;
			this.reader = new BufferedReader(r);
			this.resolution = resolution;
		}
		
		private final void throwExceptionInvalid(String message) throws IOException {
			throw new IOException("Invalid M3U file" + (message != null ? ": " + message : ""));
		}
		
		private final String readLine() throws IOException {
			return reader.readLine();
		}
		
		private final boolean isMetaLine(String line) {
			return line.charAt(0) == CHAR_META;
		}
		
		private final Pair<String, String> parseMetaLine(String line) {
			String name = line, value = null;
			int index = line.indexOf(CHAR_META_DELIMITER_KEYVALUE);
			if(index > 0) {
				name = line.substring(1, index);
				value = line.substring(index + 1);
			}
			return new Pair<>(name, value);
		}
		
		private final void parseSegmentMetaData(String value) throws IOException {
			int index = value.indexOf(CHAR_META_DELIMITER_INFO);
			String duration = value, title = null;
			if(index > 0) {
				duration = value.substring(0, index);
				title = value.substring(index + 1);
			}
			segmentBuilder.duration(Double.valueOf(duration));
			segmentBuilder.title(title);
			segmentBuilder.index(sequenceIndex++);
		}
		
		private final void parseStreamInfoMetaData(String value) throws IOException {
			String[] values = value.split("" + CHAR_META_DELIMITER_INFO);
			if(values.length <= 0)
				throwExceptionInvalid("Invalid stream info meta data");
			for(String val : values) {
				String partName = val, partValue = null;
				int index = val.indexOf(CHAR_META_DELIMITER_ASSIGN);
				if(index > 0) {
					partName = val.substring(0, index);
					partValue = val.substring(index + 1);
				}
				switch(partName.toLowerCase()) {
					case "resolution": fileBuilder.resolution(MediaResolution.fromString(partValue)); break;
					default:
						// Do nothing
						break;
				}
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
		
		private final void updateMetaData(String name, String value) throws IOException {
			switch(name) {
				case NAME_VERSION: version = value; break;
				case NAME_SEQUENCE: sequenceIndex = Integer.valueOf(value); break;
				case NAME_SEGMENT: parseSegmentMetaData(value); break;
				case NAME_STREAM_INFO: parseStreamInfoMetaData(value); break;
				case NAME_KEY: parseKey(value); break;
				default:
					// Do nothing
					break;
			}
		}
		
		private final void addSegment(M3USegment segment) throws IOException {
			if(segment == null)
				throwExceptionInvalid("Invalid segment");
			fileBuilder.addSegment(segment);
		}
		
		private final URI resolveURI(String uri) {
			return Utils.isRelativeURL(uri) ? baseURI.resolve(uri) : Utils.uri(uri);
		}
		
		private final List<M3UFile> readStreamInfo(String uri) throws Exception {
			URI resolvedURI = resolveURI(uri);
			URI resolvedBaseURI = Utils.isRelativeURL(uri) ? resolveURI(uri) : Utils.uri(Utils.baseURL(uri));
			MediaResolution resolution = fileBuilder.resolution();
			try(M3UReader reader = new M3UReader(resolvedBaseURI, resolvedURI, streamResolver, null, resolution)) {
				return reader.read();
			}
		}
		
		public final List<M3UFile> read() throws Exception {
			String first = readLine();
			if(first == null // EOF
					|| !isMetaLine(first))
				throwExceptionInvalid("No header");
			// Check if the header line is present
			if(!first.substring(1).equals(NAME_HEADER))
				throwExceptionInvalid("No header");
			segmentBuilder = new M3USegmentBuilder();
			fileBuilder = new M3UFileBuilder();
			// Parse the file
			for(String line; (line = readLine()) != null;) {
				if(isMetaLine(line)) {
					if(segmentBuilder.isDirty())
						throwExceptionInvalid("Segment URI is not present");
					Pair<String, String> data = parseMetaLine(line);
					updateMetaData(data.a, data.b);
				} else {
					if(segmentBuilder.isDirty()) { // Segment URI
						segmentBuilder.uri(resolveURI(line));
						addSegment(segmentBuilder.build());
					} else if(fileBuilder.isDirty()) {
						files.addAll(readStreamInfo(line));
					}
				}
			}
			// If there were only segments in this file
			if(files.isEmpty()) {
				fileBuilder.uri(uri);
				fileBuilder.resolution(resolution);
				fileBuilder.version(version);
				if(key == null) key = M3UKey.none();
				fileBuilder.key(key);
				files.add(fileBuilder.build());
			}
			return files;
		}
		
		@Override
		public final void close() throws Exception {
			reader.close();
			if(response != null)
				response.close();
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
}