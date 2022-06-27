package sune.app.mediadown.media.format;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import sune.app.mediadown.Shared;
import sune.app.mediadown.download.segment.RemoteFileSegment;
import sune.app.mediadown.download.segment.RemoteFileSegmentable;
import sune.app.mediadown.download.segment.RemoteFileSegmentsHolder;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaResolution;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.util.Singleton;
import sune.app.mediadown.util.ThrowableFunction;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Web;
import sune.app.mediadown.util.Web.Request;
import sune.app.mediadown.util.Web.StreamResponse;

/** @since 00.02.05 */
public final class MPD {
	
	private static final double requireValidTime(double value) {
		if(value < 0.0)
			throw new IllegalArgumentException();
		return value;
	}
	
	private static final int requireValidCount(int value) {
		if(value < 0)
			throw new IllegalArgumentException();
		return value;
	}
	
	private static final long requireValidTime(long value) {
		if(value < 0L)
			throw new IllegalArgumentException();
		return value;
	}
	
	private static final int requireIntPositive(int value) {
		if(value <= 0)
			throw new IllegalArgumentException();
		return value;
	}
	
	private static final String requireNonEmpty(String string) {
		if(Objects.requireNonNull(string).isEmpty())
			throw new IllegalArgumentException();
		return string;
	}
	
	private static final Map<String, String> elementAttributesToMap(Attributes attributes) {
		return Utils.stream(attributes.iterator()).collect(Collectors.toMap(Attribute::getKey, Attribute::getValue));
	}
	
	private static final ThrowableFunction<URI, StreamResponse> streamResolver(Request request) {
		return ((uri) -> Web.requestStream(request.setURL(Utils.url(uri))));
	}
	
	public static final Map<MediaFormat, List<MPDFile>> parse(Request request) throws Exception {
		return new MPDReader(Utils.uri(request.url), streamResolver(request)).read();
	}
	
	public static final Map<MediaFormat, List<MPDFile>> parse(String uri, String content) throws Exception {
		return new MPDReader(Utils.uri(uri), content).read();
	}
	
	public static final List<MPDCombinedFile> reduce(Map<MediaFormat, List<MPDFile>> result) {
		List<MPDFile> videos = result.entrySet().stream()
				.filter((e) -> e.getKey().mediaType().is(MediaType.VIDEO))
				.flatMap((e) -> e.getValue().stream())
				.collect(Collectors.toList());
		List<MPDFile> audios = result.entrySet().stream()
				.filter((e) -> e.getKey().mediaType().is(MediaType.AUDIO))
				.flatMap((e) -> e.getValue().stream())
				.collect(Collectors.toList());
		return videos.stream()
				     .flatMap((v) -> audios.stream().map((a) -> new MPDCombinedFile(v, a)))
				     .collect(Collectors.toList());
	}
	
	private static final class Segment {
		
		public static final String NODE_NAME = "S";
		
		private final long time;
		private final long duration;
		private final int count;
		
		public Segment(long time, long duration, int count) {
			this.time = time;
			this.duration = requireValidTime(duration);
			this.count = requireValidCount(count);
		}
		
		public static final Segment parse(Element element) {
			long time = -1L;
			long duration = -1L;
			int count = 1;
			if(element.hasAttr("t"))
				time = Long.valueOf(element.attr("t"));
			if(element.hasAttr("d"))
				duration = Long.valueOf(element.attr("d"));
			if(element.hasAttr("r"))
				// Number of repetitions AFTER the first segment,
				// i.e. first segment + 'r' more segments.
				count = Math.max(1, Integer.valueOf(element.attr("r")) + 1);
			return new Segment(time, duration, count);
		}
		
		public final long time() {
			return time;
		}
		
		public final long duration() {
			return duration;
		}
		
		public final int count() {
			return count;
		}
	}
	
	private static final class SegmentTimeline {
		
		public static final String NODE_NAME = "SegmentTimeline";
		
		private final List<Segment> segments;
		
		private SegmentTimeline(List<Segment> segments) {
			if(Objects.requireNonNull(segments).isEmpty())
				throw new IllegalArgumentException();
			this.segments = segments;
		}
		
		public static final SegmentTimeline parse(Element element) {
			if(!Objects.requireNonNull(element).nodeName().equalsIgnoreCase(NODE_NAME))
				throw new IllegalArgumentException();
			List<Segment> segments = new ArrayList<>();
			for(Element elementSegment : element.getElementsByTag(Segment.NODE_NAME)) {
				segments.add(Segment.parse(elementSegment));
			}
			return new SegmentTimeline(segments);
		}
		
		public final List<Segment> segments() {
			return segments;
		}
	}
	
	private static final class SegmentTemplate {
		
		public static final String NODE_NAME = "SegmentTemplate";
		
		private final int timescale;
		private final String media;
		private final String initialization;
		private final SegmentTimeline timeline;
		private final int startNumber;
		
		private SegmentTemplate(int timescale, String media, String initialization, int startNumber, SegmentTimeline timeline) {
			this.timescale = requireIntPositive(timescale);
			this.media = media;
			this.initialization = initialization;
			this.startNumber = startNumber;
			this.timeline = Objects.requireNonNull(timeline);
		}
		
		public static final SegmentTemplate parse(Element element) {
			if(!Objects.requireNonNull(element).nodeName().equalsIgnoreCase(NODE_NAME))
				throw new IllegalArgumentException();
			Element elementTimeline = element.getElementsByTag(SegmentTimeline.NODE_NAME).first();
			if(elementTimeline == null)
				throw new IllegalArgumentException();
			int timescale = Integer.valueOf(element.attr("timescale"));
			String media = element.attr("media");
			String initialization = element.attr("initialization");
			String startNumberStr = element.attr("startNumber");
			if(startNumberStr.isEmpty()) startNumberStr = "0";
			int startNumber = Math.max(0, Integer.valueOf(startNumberStr));
			SegmentTimeline timeline = SegmentTimeline.parse(elementTimeline);
			return new SegmentTemplate(timescale, media, initialization, startNumber, timeline);
		}
		
		public final int timescale() {
			return timescale;
		}
		
		public final String media() {
			return media;
		}
		
		public final String initialization() {
			return initialization;
		}
		
		public final int startNumber() {
			return startNumber;
		}
		
		public final SegmentTimeline timeline() {
			return timeline;
		}
	}
	
	private static final class Representation {
		
		public static final String NODE_NAME = "Representation";
		
		private final String id;
		private final MediaResolution resolution;
		private final Map<String, String> attributes;
		
		private Representation(String id, MediaResolution resolution, Map<String, String> attributes) {
			this.id = requireNonEmpty(id);
			this.resolution = Objects.requireNonNull(resolution);
			this.attributes = Objects.requireNonNull(attributes);
		}
		
		public static final Representation parse(Element element) {
			if(!Objects.requireNonNull(element).nodeName().equalsIgnoreCase(NODE_NAME))
				throw new IllegalArgumentException();
			String id = element.attr("id");
			MediaResolution resolution = MediaResolution.UNKNOWN;
			if(element.hasAttr("width") && element.hasAttr("height")) {
				int width = Integer.valueOf(element.attr("width"));
				int height = Integer.valueOf(element.attr("height"));
				resolution = new MediaResolution(width, height);
			}
			Map<String, String> attributes = elementAttributesToMap(element.attributes());
			return new Representation(id, resolution, attributes);
		}
		
		public final MPDFile apply(URI baseURI, MediaFormat format, SegmentTemplate template,
				ContentProtection protection, Map<String, String> attributes) throws Exception {
			return (new MPDFileConstructor(baseURI, this, template, protection, attributes)).construct(format);
		}
		
		public final String id() {
			return id;
		}
		
		public final MediaResolution resolution() {
			return resolution;
		}
		
		public Map<String, String> attributes() {
			return attributes;
		}
	}
	
	private static final class MPDFileConstructor {
		
		private final URI baseURI;
		private final Representation representation;
		private final SegmentTemplate template;
		private final List<MPDSegment> segments = new ArrayList<>();
		private long time;
		private int count;
		private final ContentProtection protection;
		private final Map<String, String> attributes;
		
		private final Map<String, String> formatMap = new HashMap<>();
		
		public MPDFileConstructor(URI baseURI, Representation representation, SegmentTemplate template,
				ContentProtection protection, Map<String, String> attributes) {
			this.baseURI = Objects.requireNonNull(baseURI);
			this.representation = Objects.requireNonNull(representation);
			this.template = Objects.requireNonNull(template);
			this.protection = Objects.requireNonNull(protection);
			this.attributes = Objects.requireNonNull(attributes);
			prepareFormatMap();
		}
		
		private final void prepareFormatMap() {
			formatMap.put("RepresentationID", representation.id());
		}
		
		private static final Object castToFormatArg(String formatArg, String value) {
			int type = Character.toLowerCase(formatArg.codePointAt(formatArg.length() - 1));
			switch(type) {
				case 'b':
					return Boolean.valueOf(value);
				case 'h':
				case 'c':
				case 'd':
				case 'o':
				case 'x':
					return Long.valueOf(value);
				case 'e':
				case 'f':
				case 'g':
				case 'a':
					return Double.valueOf(value);
				default:
					return value;
			}
		}
		
		private final String format(MatchResult result) {
			String text = result.group(0);
			String name = result.group(1);
			int index;
			// Simple name without additional formatting arguments or % as a variable name.
			if((index = name.indexOf("%")) < 1)
				return formatMap.getOrDefault(name, text);
			String varName = name.substring(0, index);
			String args = name.substring(index + 1);
			String value = formatMap.getOrDefault(varName, text);
			value = String.format("%1$" + args, castToFormatArg(args, value));
			return value;
		}
		
		private final void addSegment(String templateURI, Segment segment) throws Exception {
			long startTime = segment.time();
			if(startTime >= 0L) time = startTime;
			double duration = segment.duration();
			int count = segment.count();
			for(int i = 0; i < count; ++i) {
				addSegment(templateURI, duration, false);
			}
		}
		
		private final void addSegment(String templateURI, double duration, boolean isInitialization) throws Exception {
			double timescaleMult = 1.0 / template.timescale();
			formatMap.put("Time", String.valueOf(time));
			formatMap.put("Number", String.valueOf(count));
			String uri = Utils.replaceAll("\\$(.*?)\\$", templateURI, this::format);
			URI uriObj = baseURI.resolve(uri);
			segments.add(new MPDSegment(uriObj, duration * timescaleMult, time * timescaleMult));
			time += duration;
			if(!isInitialization) ++count; // For the Number variable
		}
		
		public final MPDFile construct(MediaFormat format) throws Exception {
			SegmentTimeline timeline = template.timeline();
			Map<String, String> args = new HashMap<>();
			args.put("RepresentationID", representation.id());
			time = 0L; // Muset reset the time first
			count = template.startNumber(); // Muset reset the count first for the Number variable
			// Add initialization segment
			addSegment(template.initialization(), 0.0, true);
			// Add other segments
			String templateURI = template.media();
			for(Segment segment : timeline.segments()) {
				addSegment(templateURI, segment);
			}
			// Compute total duration (in seconds)
			double duration = time / (double) template.timescale();
			MediaResolution resolution = representation.resolution();
			Map<String, String> mergedAttrs = Utils.mergeNew(attributes, representation.attributes());
			return new MPDFile(segments, format, resolution, duration, protection, mergedAttrs);
		}
	}
	
	private static final class AdaptationSet {
		
		public static final String NODE_NAME = "AdaptationSet";
		
		private final MediaFormat format;
		private final List<Representation> representations;
		private final SegmentTemplate template;
		private final ContentProtection protection;
		private final Map<String, String> attributes;
		
		public AdaptationSet(MediaFormat format, List<Representation> representations, SegmentTemplate template,
				ContentProtection protection, Map<String, String> attributes) {
			this.format = Objects.requireNonNull(format);
			this.representations = Objects.requireNonNull(representations);
			this.template = Objects.requireNonNull(template);
			this.protection = Objects.requireNonNull(protection);
			this.attributes = Objects.requireNonNull(attributes);
		}
		
		public static final AdaptationSet parse(Element element) {
			if(!Objects.requireNonNull(element).nodeName().equalsIgnoreCase(NODE_NAME))
				throw new IllegalArgumentException();
			MediaFormat format = MediaFormat.fromMimeType(element.attr("mimeType"));
			List<Representation> representations = new ArrayList<>();
			for(Element elementRepresentation : element.getElementsByTag(Representation.NODE_NAME)) {
				Representation representation = Representation.parse(elementRepresentation);
				representations.add(representation);
			}
			Element elementTemplate = element.getElementsByTag(SegmentTemplate.NODE_NAME).first();
			if(elementTemplate == null)
				throw new IllegalArgumentException();
			SegmentTemplate template = SegmentTemplate.parse(elementTemplate);
			ContentProtection protection = ContentProtection.parse(element.getElementsByTag(ContentProtection.NODE_NAME));
			Map<String, String> attributes = elementAttributesToMap(element.attributes());
			return new AdaptationSet(format, representations, template, protection, attributes);
		}
		
		public final List<MPDFile> process(URI baseURI) throws Exception {
			List<MPDFile> files = new ArrayList<>();
			for(Representation representation : representations) {
				files.add(representation.apply(baseURI, format, template, protection, attributes));
			}
			return files;
		}
		
		public final MediaFormat format() {
			return format;
		}
	}
	
	private static final class MPDReader {
		
		private final URI uri;
		private final ThrowableFunction<URI, StreamResponse> streamResolver;
		private final String content;
		
		private MPDReader(URI uri, ThrowableFunction<URI, StreamResponse> streamResolver) throws Exception {
			this.uri = Objects.requireNonNull(uri);
			this.streamResolver = Objects.requireNonNull(streamResolver);
			this.content = null;
		}
		
		private MPDReader(URI uri, String content) throws Exception {
			this.uri = Objects.requireNonNull(uri);
			this.streamResolver = null;
			this.content = Objects.requireNonNull(content);
		}
		
		private final void throwExceptionInvalid(String message) throws IOException {
			throw new IOException("Invalid MPD file" + (message != null ? ": " + message : ""));
		}
		
		private final Document responseDocument(InputStream stream, URI baseURI)
				throws IOException {
			return Jsoup.parse(stream, Shared.CHARSET.name(), baseURI.toString());
		}
		
		private final Map<MediaFormat, List<MPDFile>> read(URI baseURI, Document document) throws Exception {
			// Check if the MPD file has BaseURI tag defined
			Element elBaseURI;
			if((elBaseURI = document.selectFirst("MPD > BaseURL")) != null) {
				// Replace the presumed base URI by the new one
				baseURI = Utils.uri(elBaseURI.html());
			}
			Map<MediaFormat, List<MPDFile>> map = new LinkedHashMap<>();
			for(Element elementAdaptationSet : document.getElementsByTag(AdaptationSet.NODE_NAME)) {
				AdaptationSet adaptationSet = AdaptationSet.parse(elementAdaptationSet);
				map.put(adaptationSet.format(), adaptationSet.process(baseURI));
			}
			return map;
		}
		
		public final Map<MediaFormat, List<MPDFile>> read() throws Exception {
			URI baseURI = Utils.uri(Utils.baseURL(uri.toString()));
			Document document = null;
			if(streamResolver != null) {
				try(StreamResponse response = streamResolver.apply(uri)) {
					document = responseDocument(response.stream, baseURI);
				}
			} else if(content != null) {
				document = responseDocument(new ByteArrayInputStream(content.getBytes(Shared.CHARSET)), baseURI);
			}
			if(document == null)
				throwExceptionInvalid(null);
			return read(baseURI, document);
		}
	}
	
	// Reference: https://dashif-documents.azurewebsites.net/Guidelines-Security/master/Guidelines-Security.html#CPS-mpd-scheme
	public static final class ContentProtection {
		
		protected static final String NODE_NAME = "ContentProtection";
		
		private final String scheme;
		private final Map<String, String> keys;
		
		public ContentProtection(String scheme, Map<String, String> keys) {
			this.scheme = scheme;
			this.keys = keys;
		}
		
		public static final ContentProtection parse(Elements elements) {
			String scheme = null;
			Map<String, String> keys = new LinkedHashMap<>();
			for(Element element : elements) {
				Elements children = element.children();
				String attrValue = element.attr("value");
				// Element with the scheme information (e.g. cenc) has no children
				if(children.isEmpty()) {
					// Check for value attribute indicating element for specifying the scheme
					if(!attrValue.isEmpty()) {
						scheme = attrValue.toLowerCase();
					}
				} else {
					// Loop through children and find the key content
					for(Element child : children) {
						String tagName = child.tagName().toLowerCase();
						// Check only for scheme properties specifying tags
						if(tagName.startsWith(scheme)) {
							int index = tagName.indexOf(':');
							if(index > 0) {
								String propName = tagName.substring(index + 1);
								switch(propName) {
									// Tag specifying the content key
									case "pssh":
										keys.put(attrValue, child.html());
										break;
									// Ignore other tags
								}
							}
						}
					}
				}
			}
			// Normalize, so that all variables are null or neither
			if(scheme == null || keys.isEmpty()) {
				scheme = null;
				keys = null;
			}
			return new ContentProtection(scheme, keys);
		}
		
		public String scheme() {
			return scheme;
		}
		
		public Map<String, String> keys() {
			return keys;
		}
		
		public boolean isPresent() {
			return scheme != null && keys != null;
		}
	}
	
	public static final class MPDSegment {
		
		private final URI uri;
		private final double duration;
		private final double startTime;
		
		protected MPDSegment(URI uri, double duration, double startTime) {
			this.uri = Objects.requireNonNull(uri);
			this.duration = requireValidTime(duration);
			this.startTime = requireValidTime(startTime);
		}
		
		public final URI uri() {
			return uri;
		}
		
		public final double duration() {
			return duration;
		}
		
		public final double startTime() {
			return startTime;
		}
	}
	
	public static final class MPDFile implements RemoteFileSegmentable {
		
		private final List<MPDSegment> segments;
		private final MediaFormat format;
		private final MediaResolution resolution;
		private final double duration;
		private List<RemoteFileSegmentsHolder> segmentsHolders;
		private final ContentProtection protection;
		private final Map<String, String> attributes;
		
		protected MPDFile(List<MPDSegment> segments, MediaFormat format, MediaResolution resolution, double duration,
				ContentProtection protection, Map<String, String> attributes) {
			this.segments = Objects.requireNonNull(segments);
			this.format = Objects.requireNonNull(format);
			this.resolution = Objects.requireNonNull(resolution);
			this.duration = requireValidTime(duration);
			this.protection = Objects.requireNonNull(protection);
			this.attributes = Collections.unmodifiableMap(Objects.requireNonNull(attributes));
		}
		
		public final List<MPDSegment> segments() {
			return segments;
		}
		
		public final MediaFormat format() {
			return format;
		}
		
		public final MediaResolution resolution() {
			return resolution;
		}
		
		public final double duration() {
			return duration;
		}
		
		public final Map<String, String> attributes() {
			return attributes;
		}
		
		public final List<String> codecs() {
			return Singleton.of(this, () -> List.of(attributes.getOrDefault("codecs", "").split(",")));
		}
		
		public final int bandwidth() {
			return Singleton.of(this, () -> Integer.valueOf(attributes.getOrDefault("bandwidth", "0")));
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
		
		public ContentProtection protection() {
			return protection;
		}
	}
	
	public static final class MPDCombinedFile implements RemoteFileSegmentable {
		
		private final MPDFile video;
		private final MPDFile audio;
		private List<RemoteFileSegmentsHolder> segmentsHolders;
		private List<MPDFile> files;
		
		protected MPDCombinedFile(MPDFile video, MPDFile audio) {
			this.video = Objects.requireNonNull(video);
			this.audio = Objects.requireNonNull(audio);
		}
		
		public final MPDFile video() {
			return video;
		}
		
		public final MPDFile audio() {
			return audio;
		}
		
		public final List<MPDFile> files() {
			if(files == null) {
				files = List.of(video, audio);
			}
			return files;
		}
		
		@Override
		public final List<RemoteFileSegmentsHolder> segmentsHolders() {
			if(segmentsHolders == null) {
				segmentsHolders = Stream.of(video, audio)
					.flatMap((f) -> f.segmentsHolders().stream())
					.collect(Collectors.toList());
			}
			return segmentsHolders;
		}
	}
}