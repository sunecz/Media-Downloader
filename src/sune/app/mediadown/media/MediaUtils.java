package sune.app.mediadown.media;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.download.segment.FileSegmentsHolder;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.media.MediaQuality.QualityValue;
import sune.app.mediadown.media.MediaQuality.VideoQualityValue;
import sune.app.mediadown.media.format.M3U;
import sune.app.mediadown.media.format.M3U.M3UCombinedFile;
import sune.app.mediadown.media.format.M3U.M3UFile;
import sune.app.mediadown.media.format.MPD;
import sune.app.mediadown.media.format.MPD.ContentProtection;
import sune.app.mediadown.media.format.MPD.MPDCombinedFile;
import sune.app.mediadown.media.format.MPD.MPDFile;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.net.Web.Response;
import sune.app.mediadown.util.CheckedFunction;
import sune.app.mediadown.util.Opt;
import sune.app.mediadown.util.Opt.OptCondition;
import sune.app.mediadown.util.Opt.OptMapper;
import sune.app.mediadown.util.Property;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.05 */
public final class MediaUtils {
	
	private static Translation mediaTitleTranslation;
	
	// Forbid anyone to create an instance of this class
	private MediaUtils() {
	}
	
	/** @since 00.02.07 */
	private static final Object intOrString(String string, boolean canConvert) {
		return string != null
					? (canConvert && Utils.isInteger(string)
							? Integer.parseInt(string)
							: string)
					: null;
	}
	
	private static final Translation mediaTitleTranslation() {
		if(mediaTitleTranslation == null) {
			mediaTitleTranslation = MediaDownloader.translation().getTranslation("media.naming");
		}
		return mediaTitleTranslation;
	}
	
	public static final List<Media.Builder<?, ?>> createMediaBuilders(MediaSource source, URI uri, URI sourceURI,
			String title, MediaLanguage language, MediaMetadata data) throws Exception {
		MediaUtils.Parser parser = MediaUtils.parser();
		
		parser.format(MediaFormat.M3U8, new MediaUtils.Parser.M3U8FormatParser((parserData) -> {
			M3UCombinedFile result = parserData.result();
			M3UFile video = result.video();
			MediaMetadata metadata = parserData.mediaData().add(parserData.data()).title(title).build();
			
			MediaQuality videoQuality = MediaQuality.fromResolution(video.resolution());
			if(videoQuality.is(MediaQuality.UNKNOWN)) {
				String qualityName = video.attributes().get("estimatedQuality");
				
				if(qualityName != null) {
					videoQuality = MediaQuality.ofName(qualityName);
				}
			}
			
			if(result.hasSeparateAudio()) {
				M3UFile audio = result.audio();
				MediaLanguage extractedLanguage = MediaLanguage.ofCode(audio.attributes().getOrDefault("language", ""));
				MediaLanguage audioLanguage = Opt.of(extractedLanguage)
					.ifFalse((l) -> l.is(MediaLanguage.UNKNOWN))
					.orElse(language);
				
				return VideoMediaContainer.separated().format(MediaFormat.M3U8).media(
					VideoMedia.segmented().source(source)
						.uri(video.uri()).format(MediaFormat.MP4)
						.quality(videoQuality)
						.segments(Utils.<List<FileSegmentsHolder<?>>>cast(video.segmentsHolders()))
						.resolution(video.resolution()).duration(video.duration())
						.metadata(metadata),
					AudioMedia.segmented().source(source)
						.uri(audio.uri()).format(MediaFormat.M4A)
						.quality(MediaQuality.UNKNOWN)
						.segments(Utils.<List<FileSegmentsHolder<?>>>cast(audio.segmentsHolders()))
						.language(audioLanguage).duration(audio.duration())
						.metadata(metadata)
				);
			}
			
			return VideoMediaContainer.combined().format(MediaFormat.M3U8).media(
				VideoMedia.segmented().source(source)
					.uri(video.uri()).format(MediaFormat.MP4)
					.quality(videoQuality)
					.segments(Utils.<List<FileSegmentsHolder<?>>>cast(video.segmentsHolders()))
					.resolution(video.resolution()).duration(video.duration())
					.metadata(metadata),
				AudioMedia.simple().source(source)
					.uri(video.uri()).format(MediaFormat.M4A)
					.quality(MediaQuality.UNKNOWN)
					.language(language).duration(video.duration())
					.metadata(metadata)
			);
		}));
		
		parser.format(MediaFormat.DASH, new MediaUtils.Parser.DASHFormatParser((parserData) -> {
			MPDCombinedFile result = parserData.result();
			MPDFile video = result.video();
			MPDFile audio = result.audio();
			
			MediaMetadata.Builder metadataBuilder = parserData.mediaData().add(parserData.data()).title(title);
			MediaMetadata metadataVideo;
			MediaMetadata metadataAudio;
			
			if(metadataBuilder.isProtected()) {
				ContentProtection protectionVideo = video.protection();
				ContentProtection protectionAudio = audio.protection();
				metadataVideo = metadataBuilder.addProtections(protectionVideo.protections()).build();
				metadataAudio = metadataBuilder.addProtections(protectionAudio.protections()).build();
			} else {
				MediaMetadata metadata = metadataBuilder.build();
				metadataVideo = metadata;
				metadataAudio = metadata;
			}
			
			double frameRate = Double.valueOf(video.attributes().getOrDefault("framerate", "0.0"));
			int sampleRate = Integer.valueOf(audio.attributes().getOrDefault("audiosamplingrate", "0"));
			MediaQuality.AudioQualityValue audioValue = new MediaQuality.AudioQualityValue(audio.bandwidth(), sampleRate, 0);
			MediaLanguage extractedLanguage = MediaLanguage.ofCode(audio.attributes().getOrDefault("lang", ""));
			MediaLanguage audioLanguage = Opt.of(extractedLanguage)
				.ifFalse((l) -> l.is(MediaLanguage.UNKNOWN))
				.orElse(language);
			
			MediaQuality videoQuality = MediaQuality.fromResolution(video.resolution());
			if(videoQuality.is(MediaQuality.UNKNOWN)) {
				String qualityName = video.attributes().get("estimatedQuality");
				
				if(qualityName != null) {
					videoQuality = MediaQuality.ofName(qualityName);
				}
			}
			
			return VideoMediaContainer.separated().format(MediaFormat.DASH).media(
				VideoMedia.segmented().source(source)
					.uri(parserData.uri()).format(video.format())
					.quality(videoQuality)
					.segments(Utils.<List<FileSegmentsHolder<?>>>cast(video.segmentsHolders()))
					.resolution(video.resolution()).duration(video.duration())
					.codecs(video.codecs()).bandwidth(video.bandwidth()).frameRate(frameRate)
					.metadata(metadataVideo),
				AudioMedia.segmented().source(source)
					.uri(parserData.uri()).format(audio.format())
					.quality(MediaQuality.fromSampleRate(sampleRate).withValue(audioValue))
					.segments(Utils.<List<FileSegmentsHolder<?>>>cast(audio.segmentsHolders()))
					.language(audioLanguage).duration(audio.duration())
					.codecs(audio.codecs()).bandwidth(audio.bandwidth()).sampleRate(sampleRate)
					.metadata(metadataAudio)
			);
		}));
		
		return parser.parse(uri, sourceURI, data.data());
	}
	
	public static final List<Media> createMedia(MediaSource source, URI uri, URI sourceURI, String title,
			MediaLanguage language, MediaMetadata data) throws Exception {
		return createMediaBuilders(source, uri, sourceURI, title, language, data).stream()
					.map(Media.Builder::build)
					.collect(Collectors.toList());
	}
	
	public static final String mediaTitle(String programName, int numSeason, int numEpisode, String episodeName) {
		return mediaTitle(programName, numSeason, numEpisode, episodeName, false);
	}
	
	public static final String mediaTitle(String programName, int numSeason, int numEpisode, String episodeName,
			boolean splitSeasonAndEpisode) {
		String strNumSeason  = numSeason  >= 0 ? String.format("%02d", numSeason)  : null;
		String strNumEpisode = numEpisode >= 0 ? String.format("%02d", numEpisode) : null;
		return mediaTitle(programName, strNumSeason, strNumEpisode, episodeName, splitSeasonAndEpisode);
	}
	
	public static final String mediaTitle(String programName, String numSeason, String numEpisode, String episodeName) {
		return mediaTitle(programName, numSeason, numEpisode, episodeName, false);
	}
	
	public static final String mediaTitle(String programName, String numSeason, String numEpisode, String episodeName,
			boolean splitSeasonAndEpisode) {
		return mediaTitle(programName, numSeason, numEpisode, episodeName, splitSeasonAndEpisode, true, true);
	}
	
	/** @since 00.02.07 */
	public static final String mediaTitle(String programName, String numSeason, String numEpisode, String episodeName,
			boolean splitSeasonAndEpisode, boolean canConvertSeason, boolean canConvertEpisode) {
		if(programName == null || programName.isBlank())
			throw new IllegalArgumentException("Program name cannot be null or blank");
		
		Map<String, Object> args = new HashMap<>();
		args.put("translation", mediaTitleTranslation());
		args.put("program_name", programName);
		
		if(episodeName != null)
			args.put("episode_name", episodeName);
		
		Object season = intOrString(numSeason, canConvertSeason);
		if(season != null)
			args.put("season", season);
		
		Object episode = intOrString(numEpisode, canConvertEpisode);
		if(episode != null)
			args.put("episode", episode);
		
		if(splitSeasonAndEpisode)
			args.put("split", splitSeasonAndEpisode);
		
		MediaTitleFormat format = MediaDownloader.configuration().mediaTitleFormat();
		return format.format(args);
	}
	
	public static final boolean isSegmentedMedia(Media media) {
		return Opt.of(media).ifTrue(Media::isSingle).filter(Media::isSegmented)
				  .<Media>or((opt) -> opt.ifTrue(Media::isContainer)
				                         .map(Media::mapToContainer)
				                         .filter((m) -> m.media().stream()
				                                                 .filter(MediaUtils::isSegmentedMedia)
				                                                 .findAny().isPresent())
				                         .<Media>castAny())
				  .isPresent();
	}
	
	public static final List<FileSegmentsHolder<?>> segments(Media media) {
		return Opt.of(media).ifTrue(OptCondition.of(Media::isSingle).and(Media::isSegmented))
				            .map((m) -> ((SegmentedMedia) m).segments())
				  .<Media>or((opt) -> opt.ifTrue(Media::isContainer)
				                         .map(OptMapper.of(Media::mapToContainer).join(MediaContainer::media).build())
				                         .map((l) -> l.stream()
				                                      .map(MediaUtils::segments)
				                                      .flatMap(List::stream)
				                                      .collect(Collectors.toList())))
				  .orElse(List.of());
	}
	
	public static final List<Media> solids(Media media) {
		return Opt.of(media).ifTrue(OptCondition.of(Media::isSingle).and(Media::isSolid))
	                        .map((m) -> List.of(m))
				  .<Media>or((opt) -> opt.ifTrue(Media::isContainer)
				                         .map(OptMapper.of(Media::mapToContainer).join(MediaContainer::media).build())
				                         .map((l) -> l.stream()
				                                      .map(MediaUtils::solids)
				                                      .flatMap(List::stream)
				                                      .collect(Collectors.toList())))
				  .orElse(List.of());
	}
	
	public static final MediaContainer.Builder<?, ?> appendMedia(MediaContainer.Builder<?, ?> container,
			Stream<Media.Builder<?, ?>> media) {
		return container.media(Stream.concat(container.media().stream(), media).collect(Collectors.toList()));
	}
	
	public static final MediaContainer.Builder<?, ?> appendMedia(MediaContainer.Builder<?, ?> container,
			List<Media.Builder<?, ?>> media) {
		return appendMedia(container, media.stream());
	}
	
	public static final MediaContainer.Builder<?, ?> appendMedia(MediaContainer.Builder<?, ?> container,
			Media.Builder<?, ?>... media) {
		return appendMedia(container, Stream.of(media));
	}
	
	public static final Parser parser() {
		return new Parser();
	}
	
	/** @since 00.02.09 */
	public static final MediaQuality estimateMediaQualityFromBandwidth(int bandwidth) {
		return MediaQualityEstimator.fromBandwidth(bandwidth);
	}
	
	/** @since 00.02.09 */
	private static final class MediaQualityEstimator {
		
		/*
		 * [1] BitRate approximation table for video with aspect ratio 16:9.
		 * Source: https://www.circlehd.com/blog/how-to-calculate-video-file-size
		 * +---------+----------+
		 * | Quality | BitRate  |
		 * +---------+----------+
		 * | 2160p   |  20 Mbps |
		 * | 1080p   |   5 Mbps |
		 * |  720p   |   1 Mbps |
		 * |  480p   | 0.5 Mbps |
		 * +---------+----------+
		 */
		
		// Forbid anyone to create an instance of this class
		private MediaQualityEstimator() {
		}
		
		private static final double bandwidthToBitRateMbps(int bandwidth) {
			return bandwidth / 1024.0 / 1024.0;
		}
		
		private static final int approximateVideoHeightFromBandwidth(int bandwidth) {
			double bitRate = bandwidthToBitRateMbps(bandwidth);
			
			// Inverse of quadratic regression for the approximation table [1] and some
			// additional values to ensure the positivity of resulting values.
			// Values {x,y} used: {0,0},{1,0.125},{2,0.25},{3,0.375},{4,0.5},{6,1},{9,5},{18,20}.
			double y = 0.142703 + Math.sqrt(0.142703 * 0.142703 - 4.0 * 0.069617 * (0.0745748 - bitRate)) / (2.0 * 0.069617);
			
			// Handle special cases where the monotonicity of the quadratic regression
			// is not preserved, i.e. in the range of <0, 1).
			if(y <= 0.125) {
				y = bitRate / 0.125; // Use linear interpolation
			}
			
			return Math.max(1, (int) (y * 120.0));
		}
		
		public static final MediaQuality fromBandwidth(int bandwidth) {
			final int height = approximateVideoHeightFromBandwidth(bandwidth);
			
			// Try to match with progressive scan qualities
			QualityValue value = new VideoQualityValue(height);
			MediaQuality prev = null;
			MediaQuality last = null;
			
			for(MediaQuality quality : Utils.iterable(
				Stream.of(MediaQuality.validQualities())
					.filter((q) -> q.mediaType().is(MediaType.VIDEO))
					.filter((q) -> q.name().startsWith("P")) // Only progressive scan qualities
					.sorted(MediaQuality.reversedComparatorKeepOrder())
					.iterator()
			)) {
				if(value.compareTo(quality.value()) >= 0) {
					last = quality;
					break;
				}
				
				prev = quality;
			}
			
			// Now we have prev.height >= height >= last.height, so we just select
			// either the prev or last based on the difference of their heights.
			VideoQualityValue vqvPrev = (VideoQualityValue) prev.value();
			VideoQualityValue vqvLast = (VideoQualityValue) last.value();
			int diffPrev = Math.abs(vqvPrev.height() - height);
			int diffLast = Math.abs(vqvLast.height() - height);
			return diffPrev <= diffLast ? prev : last;
		}
	}
	
	public static final class Parser {
		
		private final Map<MediaFormat, FormatParser> parsers;
		
		private Parser() {
			parsers = new HashMap<>();
		}
		
		private static final List<String> contentType(HttpHeaders headers) {
			return headers.allValues("content-type");
		}
		
		private static final FormatParser defaultFormatParser(MediaFormat format) {
			if(format == MediaFormat.M3U8) return new DefaultM3U8FormatParser(); else
			if(format == MediaFormat.DASH) return new DefaultDASHFormatParser();
			else                           return new DefaultFormatParser();
		}
		
		private final List<Media.Builder<?, ?>> parseFormat(URI uri, MediaFormat format, Request request,
				URI sourceURI, Map<String, Object> data, long size) throws Exception {
			Property<Exception> ex = new Property<>();
			Exception exception;
			List<Media.Builder<?, ?>> media = Opt.of(parsers.get(format)).ifTrue(Objects::nonNull)
					  .or(() -> Opt.of(defaultFormatParser(format)))
					  .map((p) -> Ignore.defaultValue(() -> p.parse(uri, format, request, sourceURI, data, size), null, ex::setValue))
					  .orElseGet(List::of);
			if((exception = ex.getValue()) != null) throw exception;
			return media;
		}
		
		public final List<Media.Builder<?, ?>> parse(URI uri, URI sourceURI, Map<String, Object> data)
				throws Exception {
			Request request = Request.of(uri).GET();
			try(Response.OfStream response = Web.peek(request)) {
				MediaFormat format = Opt.of(contentType(response.headers()))
					.ifTrue(Objects::nonNull).map(List::stream).orElseGet(Stream::empty)
					.map(MediaFormat::fromMimeType).filter(Objects::nonNull).findFirst()
					.orElse(MediaFormat.UNKNOWN);
				return parseFormat(uri, format, request, sourceURI, data, Web.size(response));
			}
		}
		
		public final <T> Parser format(MediaFormat format, FormatParser parser) {
			parsers.put(Objects.requireNonNull(format), Objects.requireNonNull(parser));
			return this;
		}
		
		@FunctionalInterface
		public static interface FormatParser {
			
			List<Media.Builder<?, ?>> parse(URI uri, MediaFormat format, Request request, URI sourceURI,
					Map<String, Object> data, long size) throws Exception;
		}
		
		public static final class FormatParserData<T> {
			
			private final URI uri;
			private final MediaFormat format;
			private final Request request;
			private final URI sourceURI;
			private final Map<String, Object> data;
			private final long size;
			private T result;
			private MediaMetadata.Builder mediaData;
			
			public FormatParserData(URI uri, MediaFormat format, Request request, URI sourceURI,
			        Map<String, Object> data, long size) {
				this.uri = uri;
				this.format = format;
				this.request = request;
				this.sourceURI = sourceURI;
				this.data = data;
				this.size = size;
			}
			
			FormatParserData<T> result(T result) {
				this.result = result;
				return this;
			}
			
			FormatParserData<T> mediaData(MediaMetadata.Builder mediaData) {
				this.mediaData = mediaData;
				return this;
			}
			
			public URI uri() {
				return uri;
			}
			
			public MediaFormat format() {
				return format;
			}
			
			public Request request() {
				return request;
			}
			
			public URI sourceURI() {
				return sourceURI;
			}
			
			public Map<String, Object> data() {
				return data;
			}
			
			public long size() {
				return size;
			}
			
			public T result() {
				return result;
			}
			
			public MediaMetadata.Builder mediaData() {
				return mediaData;
			}
		}
		
		public static class M3U8FormatParser implements FormatParser {
			
			private final CheckedFunction<FormatParserData<M3UCombinedFile>, Media.Builder<?, ?>> mapper;
			
			public M3U8FormatParser(CheckedFunction<FormatParserData<M3UCombinedFile>, Media.Builder<?, ?>> mapper) {
				this.mapper = Objects.requireNonNull(mapper);
			}
			
			@Override
			public List<Media.Builder<?, ?>> parse(URI uri, MediaFormat format, Request request, URI sourceURI,
					Map<String, Object> data, long size) throws Exception {
				FormatParserData<M3UCombinedFile> parserData = new FormatParserData<>(uri, format, request, sourceURI, data, size);
				List<Media.Builder<?, ?>> media = new ArrayList<>();
				for(M3UCombinedFile result : M3U.parse(request)) {
					boolean isProtected = result.video().key().isPresent();
					MediaMetadata.Builder mediaData = MediaMetadata.builder().isProtected(isProtected).sourceURI(sourceURI);
					Media.Builder<?, ?> m = mapper.apply(parserData.result(result).mediaData(mediaData));
					if(m != null) media.add(m);
				}
				return media;
			}
		}
		
		public static class DASHFormatParser implements FormatParser {
			
			private final CheckedFunction<FormatParserData<MPDCombinedFile>, Media.Builder<?, ?>> mapper;
			
			public DASHFormatParser(CheckedFunction<FormatParserData<MPDCombinedFile>, Media.Builder<?, ?>> mapper) {
				this.mapper = Objects.requireNonNull(mapper);
			}
			
			@Override
			public List<Media.Builder<?, ?>> parse(URI uri, MediaFormat format, Request request, URI sourceURI,
					Map<String, Object> data, long size) throws Exception {
				FormatParserData<MPDCombinedFile> parserData = new FormatParserData<>(uri, format, request, sourceURI, data, size);
				List<Media.Builder<?, ?>> media = new ArrayList<>();
				for(MPDCombinedFile result : MPD.reduce(MPD.parse(request))) {
					boolean isProtected = result.files().stream().filter((f) -> f.protection().isPresent()).findFirst().isPresent();
					MediaMetadata.Builder mediaData = MediaMetadata.builder().isProtected(isProtected).sourceURI(sourceURI);
					Media.Builder<?, ?> m = mapper.apply(parserData.result(result).mediaData(mediaData));
					if(m != null) media.add(m);
				}
				return media;
			}
		}
		
		private static final class DefaultM3U8FormatParser extends M3U8FormatParser {
			
			public DefaultM3U8FormatParser() {
				super(DefaultM3U8FormatParser::mapper);
			}
			
			private static final Media.Builder<?, ?> mapper(FormatParserData<M3UCombinedFile> parserData) {
				return VideoMedia.segmented()
							.uri(parserData.uri()).format(parserData.format())
							.quality(MediaQuality.fromResolution(parserData.result().video().resolution()))
							.segments(Utils.<List<FileSegmentsHolder<?>>>cast(parserData.result().video().segmentsHolders()))
							.metadata(parserData.mediaData().add(parserData.data()).build());
			}
		}
		
		
		private static final class DefaultDASHFormatParser extends DASHFormatParser {
			
			public DefaultDASHFormatParser() {
				super(DefaultDASHFormatParser::mapper);
			}
			
			private static final Media.Builder<?, ?> mapper(FormatParserData<MPDCombinedFile> parserData) {
				return VideoMedia.segmented()
							.uri(parserData.uri()).format(parserData.format())
							.quality(MediaQuality.fromResolution(parserData.result().video().resolution()))
							.segments(Utils.<List<FileSegmentsHolder<?>>>cast(parserData.result().segmentsHolders()))
							.metadata(parserData.mediaData().add(parserData.data()).build());
			}
		}
		
		private static final class DefaultFormatParser implements FormatParser {
			
			@Override
			public List<Media.Builder<?, ?>> parse(URI uri, MediaFormat format, Request request, URI sourceURI,
					Map<String, Object> data, long size) throws Exception {
				MediaMetadata.Builder mediaData = MediaMetadata.builder().isProtected(false).sourceURI(sourceURI);
				return List.of(VideoMedia.simple()
							.uri(uri).format(format)
							.quality(MediaQuality.UNKNOWN)
							.metadata(mediaData.add(data).build()));
			}
		}
	}
}