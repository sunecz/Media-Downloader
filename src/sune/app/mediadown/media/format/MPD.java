package sune.app.mediadown.media.format;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import sune.app.mediadown.Shared;
import sune.app.mediadown.download.segment.RemoteFileSegment;
import sune.app.mediadown.download.segment.RemoteFileSegmentsHolder;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaProtection;
import sune.app.mediadown.media.MediaProtectionType;
import sune.app.mediadown.media.MediaResolution;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.net.Web.Response;
import sune.app.mediadown.util.Regex;
import sune.app.mediadown.util.Utils;

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
	
	public static final List<MPDFile> parse(Request request) throws Exception {
		return new MPDReader(request).read();
	}
	
	public static final List<MPDFile> parse(String uri, String content) throws Exception {
		return new MPDReader(Net.uri(uri), content).read();
	}
	
	public static final List<MPDCombinedFile> reduce(List<MPDFile> files) {
		List<MPDFile> videos = files.stream()
				.filter((f) -> f.format().mediaType().is(MediaType.VIDEO))
				.collect(Collectors.toList());
		List<MPDFile> audios = files.stream()
				.filter((f) -> f.format().mediaType().is(MediaType.AUDIO))
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
			
			if(element.hasAttr("t")) {
				time = Long.parseLong(element.attr("t"));
			}
			
			if(element.hasAttr("d")) {
				duration = Long.parseLong(element.attr("d"));
			}
			
			if(element.hasAttr("r")) {
				// Number of repetitions AFTER the first segment,
				// i.e. first segment + 'r' more segments.
				count += Math.max(0, Integer.parseInt(element.attr("r")));
			}
			
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
		private final long timeOffset;
		
		private SegmentTemplate(int timescale, String media, String initialization, int startNumber,
				long timeOffset, SegmentTimeline timeline) {
			this.timescale = requireIntPositive(timescale);
			this.media = media;
			this.initialization = initialization;
			this.startNumber = startNumber;
			this.timeOffset = timeOffset;
			this.timeline = Objects.requireNonNull(timeline);
		}
		
		public static final SegmentTemplate parse(Element element) {
			if(!Objects.requireNonNull(element).nodeName().equalsIgnoreCase(NODE_NAME))
				throw new IllegalArgumentException();
			Element elementTimeline = element.getElementsByTag(SegmentTimeline.NODE_NAME).first();
			if(elementTimeline == null)
				throw new IllegalArgumentException();
			int timescale = Math.max(1, Integer.parseInt(element.attr("timescale")));
			String media = element.attr("media");
			String initialization = element.attr("initialization");
			String startNumberStr = element.attr("startNumber");
			if(startNumberStr.isEmpty()) startNumberStr = "0";
			int startNumber = Math.max(0, Integer.parseInt(startNumberStr));
			String strTimeOffset = element.attr("presentationTimeOffset");
			if(strTimeOffset.isEmpty()) strTimeOffset = "0";
			long timeOffset = Long.parseLong(strTimeOffset);
			SegmentTimeline timeline = SegmentTimeline.parse(elementTimeline);
			return new SegmentTemplate(timescale, media, initialization, startNumber, timeOffset, timeline);
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
		
		public long timeOffset() {
			return timeOffset;
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
		/** @since 00.02.09 */
		private SegmentTemplate template;
		
		private Representation(String id, MediaResolution resolution, Map<String, String> attributes, SegmentTemplate template) {
			this.id = requireNonEmpty(id);
			this.resolution = Objects.requireNonNull(resolution);
			this.attributes = Objects.requireNonNull(attributes);
			this.template = template; // May be null
		}
		
		public static final Representation parse(Element element) {
			if(!Objects.requireNonNull(element).nodeName().equalsIgnoreCase(NODE_NAME)) {
				throw new IllegalArgumentException();
			}
			
			String id = element.attr("id");
			MediaResolution resolution = MediaResolution.UNKNOWN;
			
			if(element.hasAttr("width") && element.hasAttr("height")) {
				int width = Integer.parseInt(element.attr("width"));
				int height = Integer.parseInt(element.attr("height"));
				resolution = new MediaResolution(width, height);
			}
			
			Map<String, String> attributes = elementAttributesToMap(element.attributes());
			
			SegmentTemplate template = null;
			Element elementSegmentTemplate = element.getElementsByTag(SegmentTemplate.NODE_NAME).first();
			
			if(elementSegmentTemplate != null) {
				template = SegmentTemplate.parse(elementSegmentTemplate);
			}
			
			return new Representation(id, resolution, attributes, template);
		}
		
		public final MPDFile apply(URI baseURI, MediaFormat format, ContentProtection protection,
				Map<String, String> attributes) throws Exception {
			if(template == null) {
				throw new IllegalStateException("No SegmentTemplate");
			}
			
			return (new MPDFileConstructor(baseURI, this, template, protection, attributes)).construct(format);
		}
		
		/** @since 00.02.09 */
		public void template(SegmentTemplate template) {
			this.template = Objects.requireNonNull(template);
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
		
		/** @since 00.02.09 */
		public SegmentTemplate template() {
			return template;
		}
	}
	
	private static final class MPDFileConstructor {
		
		/** @since 00.02.09 */
		private static final Regex REGEX_SEGMENT_TEMPLATE = Regex.of("\\$(.*?)\\$");
		
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
			int type = Character.toLowerCase(Utils.codePointAt(formatArg, formatArg.length() - 1));
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
			long duration = segment.duration();
			int count = segment.count();
			for(int i = 0; i < count; ++i) {
				addSegment(templateURI, duration, false);
			}
		}
		
		private final void addSegment(String templateURI, long duration, boolean isInitialization) throws Exception {
			double timescaleMult = 1.0 / template.timescale();
			formatMap.put("Time", String.valueOf(time));
			formatMap.put("Number", String.valueOf(count));
			String uri = REGEX_SEGMENT_TEMPLATE.replaceAll(templateURI, this::format);
			URI uriObj = Net.resolve(baseURI, uri);
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
			addSegment(template.initialization(), 0L, true);
			// Add other segments
			String templateURI = template.media();
			for(Segment segment : timeline.segments()) {
				addSegment(templateURI, segment);
			}
			// Compute total duration (in seconds)
			double duration = (time - template.timeOffset()) / (double) template.timescale();
			MediaResolution resolution = representation.resolution();
			Map<String, String> mergedAttrs = Utils.mergeNew(attributes, representation.attributes());
			return new MPDFile(segments, format, resolution, duration, protection, mergedAttrs);
		}
	}
	
	private static final class AdaptationSet {
		
		public static final String NODE_NAME = "AdaptationSet";
		
		private final MediaFormat format;
		private final List<Representation> representations;
		private final ContentProtection protection;
		private final Map<String, String> attributes;
		
		public AdaptationSet(MediaFormat format, List<Representation> representations, ContentProtection protection,
				Map<String, String> attributes) {
			this.format = Objects.requireNonNull(format);
			this.representations = Objects.requireNonNull(representations);
			this.protection = Objects.requireNonNull(protection);
			this.attributes = Objects.requireNonNull(attributes);
		}
		
		public static final AdaptationSet parse(Element element) {
			if(!Objects.requireNonNull(element).nodeName().equalsIgnoreCase(NODE_NAME)) {
				throw new IllegalArgumentException();
			}
			
			String mimeType = element.attr("mimeType");
			
			// Ignore non-video and non-audio adaptation sets
			if(!mimeType.startsWith("video/") && !mimeType.startsWith("audio/")) {
				return null;
			}
			
			MediaFormat format = MediaFormat.fromMimeType(mimeType);
			
			List<Representation> representations = new ArrayList<>();
			boolean needsOuterSegmentTemplate = false;
			
			for(Element elementRepresentation : element.getElementsByTag(Representation.NODE_NAME)) {
				Representation representation = Representation.parse(elementRepresentation);
				
				if(representation.template() == null) {
					needsOuterSegmentTemplate = true;
				}
				
				representations.add(representation);
			}
			
			if(needsOuterSegmentTemplate) {
				Element elementTemplate = null;
				
				for(Element child : element.children()) {
					if(child.nodeName().equalsIgnoreCase(SegmentTemplate.NODE_NAME)) {
						elementTemplate = child;
						break;
					}
				}
				
				if(elementTemplate == null) {
					throw new IllegalStateException("No SegmentTemplate");
				}
				
				SegmentTemplate template = SegmentTemplate.parse(elementTemplate);
				
				for(Representation representation : representations) {
					if(representation.template() != null) {
						continue;
					}
					
					representation.template(template);
				}
			}
			
			ContentProtection protection = ContentProtection.parse(element.getElementsByTag(ContentProtection.NODE_NAME));
			Map<String, String> attributes = elementAttributesToMap(element.attributes());
			
			return new AdaptationSet(format, representations, protection, attributes);
		}
		
		public final List<MPDFile> process(URI baseURI) throws Exception {
			List<MPDFile> files = new ArrayList<>();
			for(Representation representation : representations) {
				files.add(representation.apply(baseURI, format, protection, attributes));
			}
			return files;
		}
	}
	
	private static final class MPDReader {
		
		private final Request request;
		private final URI uri;
		private final String content;
		
		private MPDReader(Request request) throws Exception {
			this.request = Objects.requireNonNull(request);
			this.uri = null;
			this.content = null;
		}
		
		private MPDReader(URI uri, String content) throws Exception {
			this.request = null;
			this.uri = Objects.requireNonNull(uri);
			this.content = Objects.requireNonNull(content);
		}
		
		private final void throwExceptionInvalid(String message) throws IOException {
			throw new IOException("Invalid MPD file" + (message != null ? ": " + message : ""));
		}
		
		private final Document responseDocument(InputStream stream, URI baseURI)
				throws IOException {
			return Jsoup.parse(stream, Shared.CHARSET.name(), baseURI.toString(), Parser.xmlParser());
		}
		
		private final List<MPDFile> read(URI baseURI, Document document) throws Exception {
			List<MPDFile> files = new ArrayList<>();
			
			// Check if the MPD file has BaseURI tag defined
			Element elBaseURI;
			if((elBaseURI = document.selectFirst("MPD > BaseURL")) != null) {
				// Replace the presumed base URI by the new one
				baseURI = Net.uri(elBaseURI.html());
			}
			
			for(Element elementAdaptationSet : document.getElementsByTag(AdaptationSet.NODE_NAME)) {
				AdaptationSet adaptationSet = AdaptationSet.parse(elementAdaptationSet);
				
				// Currently, AdaptationSet is null for non-video and non-audio sources,
				// if it happens just ignore it.
				if(adaptationSet != null) {
					files.addAll(adaptationSet.process(baseURI));
				}
			}
			
			return files;
		}
		
		public final List<MPDFile> read() throws Exception {
			Document document = null;
			URI baseUri = null;
			
			if(request != null) {
				try(Response.OfStream response = Web.requestStream(request)) {
					document = responseDocument(
						response.stream(),
						baseUri = response.uri()
					);
				}
			} else if(content != null) {
				document = responseDocument(
					new ByteArrayInputStream(content.getBytes(Shared.CHARSET)),
					baseUri = Net.baseURI(uri)
				);
			}
			
			if(document == null) {
				throwExceptionInvalid(null);
			}
			
			return read(baseUri, document);
		}
	}
	
	// Reference: https://dashif-documents.azurewebsites.net/Guidelines-Security/master/Guidelines-Security.html#CPS-mpd-scheme
	public static final class ContentProtection {
		
		protected static final String NODE_NAME = "ContentProtection";
		
		/** @since 00.02.09 */
		private final List<MediaProtection> protections;
		
		private ContentProtection(List<MediaProtection> protections) {
			this.protections = protections;
		}
		
		public static final ContentProtection parse(Elements elements) {
			List<MediaProtection> protections = new ArrayList<>();
			
			for(Element element : elements) {
				Elements children = element.children();
				
				// Ignore empty tags for now
				if(children.isEmpty()) {
					continue;
				}
				
				// Assume Widevine by default
				MediaProtectionType type = MediaProtectionType.DRM_WIDEVINE;
				String schemeIdUri = element.attr("schemeIdUri");
				
				if(schemeIdUri != null && !schemeIdUri.isEmpty()) {
					// Schemes (ref.: https://dashif.org/identifiers/content_protection/)
					// - edef8ba9-79d6-4ace-a3c8-27dcd51d21ed (Widevine)
					// - 9a04f079-9840-4286-ab92-e65be0885f95 (Microsoft PlayReady)
					// - 94ce86fb-07ff-4f43-adb8-93d2fa968ca2 (Apple FairPlay)
					if(schemeIdUri.equalsIgnoreCase("urn:uuid:9a04f079-9840-4286-ab92-e65be0885f95")) {
						type = MediaProtectionType.DRM_PLAYREADY;
					}
				}
				
				for(Element child : children) {
					String tagName = child.tagName().toLowerCase();
					int index = tagName.indexOf(':');
					
					if(index > 0) {
						String scheme = tagName.substring(0, index);
						String contentType = tagName.substring(index + 1);
						String content = child.html();
						protections.add(
							MediaProtection.of(type).scheme(scheme).contentType(contentType).content(content).build()
						);
					}
				}
			}
			
			// Normalize the values
			if(protections.isEmpty()) {
				protections = null;
			}
			
			return new ContentProtection(protections);
		}
		
		/** @since 00.02.09 */
		public List<MediaProtection> protections() {
			return protections;
		}
		
		public boolean isPresent() {
			return protections != null;
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
	
	public static final class MPDFile {
		
		private final List<MPDSegment> segments;
		private final MediaFormat format;
		private final MediaResolution resolution;
		private final double duration;
		private RemoteFileSegmentsHolder segmentsHolder;
		private final ContentProtection protection;
		private final Map<String, String> attributes;
		
		private List<String> codecs;
		private Integer bandwidth;
		
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
			if(codecs == null) {
				codecs = List.of(attributes.getOrDefault("codecs", "").split(","));
			}
			
			return codecs;
		}
		
		public final int bandwidth() {
			if(bandwidth == null) {
				bandwidth = Integer.parseInt(attributes.getOrDefault("bandwidth", "0"));
			}
			
			return bandwidth;
		}
		
		public final RemoteFileSegmentsHolder segmentsHolder() {
			if(segmentsHolder == null) {
				List<RemoteFileSegment> fileSegments = segments.stream()
					.map((seg) -> new RemoteFileSegment(seg.uri()))
					.collect(Collectors.toList());
				segmentsHolder = new RemoteFileSegmentsHolder(fileSegments, duration);
			}
			
			return segmentsHolder;
		}
		
		public ContentProtection protection() {
			return protection;
		}
	}
	
	public static final class MPDCombinedFile {
		
		private final MPDFile video;
		private final MPDFile audio;
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
	}
}