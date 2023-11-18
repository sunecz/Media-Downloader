package sune.app.mediadown.media;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.download.segment.FileSegment;
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
		Parser parser = new Parser();
		parser.format(MediaFormat.M3U8, new SimpleM3U8FormatParser(source, title, language));
		parser.format(MediaFormat.DASH, new SimpleDASHFormatParser(source, title, language));
		parser.defaultParser(new DefaultFormatParser());
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
		return streamFilterRecursive(media, OptCondition.of(Media::isSegmented))
					.anyMatch(OptCondition.ofTrue());
	}
	
	public static final List<Media> solids(Media media) {
		return filterRecursive(media, OptCondition.ofAll(Media::isSolid));
	}
	
	/** @since 00.02.09 */
	public static final Stream<Media> streamFilterDirect(Media media, OptCondition<Media> condition) {
		return streamFilter(media, MediaContainer::direct, condition);
	}
	
	/** @since 00.02.09 */
	public static final Stream<Media> streamFilterRecursive(Media media, OptCondition<Media> condition) {
		return streamFilter(media, MediaContainer::recursive, condition);
	}
	
	/** @since 00.02.09 */
	public static final Stream<Media> streamFilter(
			Media media, Function<MediaContainer, MediaAccessor> accessorMapper,
			OptCondition<Media> condition
	) {
		Objects.requireNonNull(accessorMapper);
		Objects.requireNonNull(condition);
		return Opt.of(media)
						.ifTrue(Media::isSingle)
						.filter(condition)
						.map(Stream::of)
				  .<Media>or((opt) -> opt
						.ifTrue(Media::isContainer)
						.map(OptMapper.of(Media::asContainer)
							          .then(accessorMapper)
							          .then(MediaAccessor::media)
							          .then(List::stream))
						.map((s) -> s.filter(condition)))
				  .orElseGet(Stream::empty);
	}
	
	/** @since 00.02.09 */
	public static final List<Media> filterDirect(Media media, OptCondition<Media> condition) {
		return filter(media, MediaContainer::direct, condition);
	}
	
	/** @since 00.02.09 */
	public static final List<Media> filterRecursive(Media media, OptCondition<Media> condition) {
		return filter(media, MediaContainer::recursive, condition);
	}
	
	/** @since 00.02.09 */
	public static final List<Media> filter(
			Media media, Function<MediaContainer, MediaAccessor> accessorMapper,
			OptCondition<Media> condition
	) {
		return streamFilter(media, accessorMapper, condition).collect(Collectors.toList());
	}
	
	/** @since 00.02.09 */
	public static final MediaQuality estimateMediaQualityFromBandwidth(int bandwidth) {
		return MediaQualityEstimator.fromBandwidth(bandwidth);
	}
	
	/** @since 00.02.09 */
	public static final double estimateTotalSize(Media media) {
		return TotalSizeEstimator.estimate(media);
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
	
	/** @since 00.02.09 */
	private static final class TotalSizeEstimator {
		
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
		 * 
		 * [2] BitRate approximation table for audio with 2 channels and bit depth of 16.
		 * Source: https://www.omnicalculator.com/other/audio-file-size
		 * +-------------+-------------+
		 * | Sample rate | BitRate     |
		 * +-------------+-------------+
		 * | 96.00 kHz   | 3072.0 kbps |
		 * | 48.00 kHz   | 1536.0 kbps |
		 * | 44.10 kHz   | 1411.2 kbps |
		 * | 22.05 kHz   |  705.6 kbps |
		 * +-------------+-------------+
		 */
		
		// Forbid anyone to create an instance of this class
		private TotalSizeEstimator() {
		}
		
		// Returns values in bps
		private static final int bitRateMbpsToBandwidth(double bitRate) {
			return Math.max(0, (int) Math.ceil(bitRate * 1024.0 * 1024.0));
		}
		
		// Returns values in Mbps
		private static final int approximateBandwidthFromVideoHeight(int height) {
			final double x = height / 120.0;
			// Handle special cases where the monotonicity of quadratic regression
			// is not preserved, i.e. in the range of <0, 1).
			if(height <= 120) return bitRateMbpsToBandwidth(0.125 * x); // Use linear interpolation
			// Quadratic regression for the approximation table [1] and some additional values
			// to ensure the positivity of resulting values.
			// Values {x,y} used: {0,0},{1,0.125},{2,0.25},{3,0.375},{4,0.5},{6,1},{9,5},{18,20}.
			return bitRateMbpsToBandwidth(0.069617 * x * x - 0.142703 * x + 0.0745748);
		}
		
		// Returns values in Mbps
		private static final int approximateBandwidthFromAudioSampleRate(int sampleRate) {
			// Approximation based on the approximation table [2].
			return bitRateMbpsToBandwidth(sampleRate / 1000.0 * 32.0 / 1024.0);
		}
		
		private static final int approximateBandwidth(VideoMediaBase video) {
			return approximateBandwidthFromVideoHeight(video.resolution().height());
		}
		
		private static final int approximateBandwidth(AudioMediaBase audio) {
			return approximateBandwidthFromAudioSampleRate(audio.sampleRate());
		}
		
		private static final double fromBandwidth(int bandwidth, double duration) {
			return bandwidth / 8 * duration;
		}
		
		public static final double estimate(VideoMediaBase video) {
			return video.bandwidth() <= 0
						? fromBandwidth(approximateBandwidth(video), video.duration())
						: fromBandwidth(video.bandwidth(), video.duration());
		}
		
		public static final double estimate(AudioMediaBase audio) {
			return audio.bandwidth() <= 0
						? fromBandwidth(approximateBandwidth(audio), audio.duration())
						: fromBandwidth(audio.bandwidth(), audio.duration());
		}
		
		public static final double estimate(Media media) {
			MediaType type = media.type();
			if(type.is(MediaType.VIDEO)) return estimate((VideoMediaBase) media);
			if(type.is(MediaType.AUDIO)) return estimate((AudioMediaBase) media);
			return MediaConstants.UNKNOWN_SIZE;
		}
	}
	
	/** @since 00.02.09 */
	private static final class SimpleM3U8FormatParser extends Parser.M3U8FormatParser {
		
		private final MediaSource source;
		private final String title;
		private final MediaLanguage language;
		
		public SimpleM3U8FormatParser(MediaSource source, String title, MediaLanguage language) {
			this.source = source;
			this.title = title;
			this.language = language;
		}
		
		@Override
		protected final Media.Builder<?, ?> map(Parser.FormatParserData<M3UCombinedFile> parserData) {
			MediaContainer.Builder<?, ?> builder;
			MediaMetadata.Builder metadataBuilder = parserData.mediaData().add(parserData.data()).title(title);
			M3UCombinedFile result = parserData.result();
			
			M3UFile video = result.video();
			MediaQuality videoQuality = MediaQuality.fromResolution(video.resolution());
			MediaProtection protectionVideo = video.protection();
			MediaMetadata metadataVideo = protectionVideo != null
				? metadataBuilder.addProtections(protectionVideo).build()
				: metadataBuilder.build();
			
			if(videoQuality.is(MediaQuality.UNKNOWN)) {
				String qualityName = video.attribute("estimatedQuality");
				
				if(qualityName != null) {
					videoQuality = MediaQuality.ofName(qualityName);
				}
			}
			
			if(result.hasSeparateAudio()) {
				M3UFile audio = result.audio();
				MediaLanguage extractedLanguage = MediaLanguage.ofCode(audio.attribute("language", ""));
				MediaLanguage audioLanguage = Opt.of(extractedLanguage)
					.ifFalse((l) -> l.is(MediaLanguage.UNKNOWN))
					.orElse(language);
				MediaProtection protectionAudio = audio.protection();
				MediaMetadata metadataAudio = protectionAudio != null
					? metadataBuilder.addProtections(protectionAudio).build()
					: metadataBuilder.build();
				
				builder = VideoMediaContainer.separated().format(MediaFormat.M3U8).media(
					VideoMedia.segmented().source(source)
						.uri(video.uri()).format(MediaFormat.MP4)
						.quality(videoQuality)
						.segments(video.segmentsHolder())
						.resolution(video.resolution()).duration(video.duration())
						.metadata(metadataVideo),
					AudioMedia.segmented().source(source)
						.uri(audio.uri()).format(MediaFormat.M4A)
						.quality(MediaQuality.UNKNOWN)
						.segments(audio.segmentsHolder())
						.language(audioLanguage).duration(audio.duration())
						.metadata(metadataAudio)
				);
			} else {
				builder = VideoMediaContainer.combined().format(MediaFormat.M3U8).media(
					VideoMedia.segmented().source(source)
						.uri(video.uri()).format(MediaFormat.MP4)
						.quality(videoQuality)
						.segments(video.segmentsHolder())
						.resolution(video.resolution()).duration(video.duration())
						.metadata(metadataVideo),
					AudioMedia.virtual().source(source)
						.uri(video.uri()).format(MediaFormat.M4A)
						.quality(MediaQuality.UNKNOWN)
						.language(language).duration(video.duration())
						.metadata(metadataVideo)
				);
			}
			
			if(result.hasSubtitles()) {
				List<M3UFile> m3uSubtitles = result.subtitles();
				List<SubtitlesMedia.Builder<?, ?>> subtitlesBuilders = new ArrayList<>(m3uSubtitles.size());
				
				for(M3UFile file : m3uSubtitles) {
					URI uri = file.uri();
					MediaFormat format = MediaFormat.fromPath(uri.getPath());
					
					if(!format.mediaType().is(MediaType.SUBTITLES)) {
						FileSegment segment = file.segmentsHolder().segments().get(0);
						format = MediaFormat.fromPath(segment.uri().getPath());
						
						if(!format.mediaType().is(MediaType.SUBTITLES)) {
							format = MediaFormat.UNKNOWN;
						}
					}
					
					MediaLanguage language = Opt.of(file.attribute("language"))
						.ifTrue(Objects::nonNull)
						.map(MediaLanguage::ofCode)
						.orElse(MediaLanguage.UNKNOWN);
					
					SubtitlesMedia.Builder<?, ?> subtitles = SubtitlesMedia.segmented()
						.source(source)
						.uri(uri)
						.format(format)
						.segments(file.segmentsHolder())
						.language(language);
					
					subtitlesBuilders.add(subtitles);
				}
				
				builder.addMedia(subtitlesBuilders);
			}
			
			return builder;
		}
	}
	
	/** @since 00.02.09 */
	private static final class SimpleDASHFormatParser extends Parser.DASHFormatParser {
		
		private final MediaSource source;
		private final String title;
		private final MediaLanguage language;
		
		public SimpleDASHFormatParser(MediaSource source, String title, MediaLanguage language) {
			this.source = source;
			this.title = title;
			this.language = language;
		}
		
		@Override
		protected final Media.Builder<?, ?> map(Parser.FormatParserData<MPDCombinedFile> parserData) {
			MPDCombinedFile result = parserData.result();
			MPDFile video = result.video();
			MPDFile audio = result.audio();
			
			ContentProtection protectionVideo = video.protection();
			ContentProtection protectionAudio = audio.protection();
			
			MediaMetadata.Builder metadataBuilder = parserData.mediaData().add(parserData.data()).title(title);
			MediaMetadata metadataVideo = protectionVideo.isPresent()
				? metadataBuilder.addProtections(protectionVideo.protections()).build()
				: metadataBuilder.build();
			MediaMetadata metadataAudio = protectionAudio.isPresent()
				? metadataBuilder.addProtections(protectionAudio.protections()).build()
				: metadataBuilder.build();
			
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
					.segments(video.segmentsHolder())
					.resolution(video.resolution()).duration(video.duration())
					.codecs(video.codecs()).bandwidth(video.bandwidth()).frameRate(frameRate)
					.metadata(metadataVideo),
				AudioMedia.segmented().source(source)
					.uri(parserData.uri()).format(audio.format())
					.quality(MediaQuality.fromSampleRate(sampleRate).withValue(audioValue))
					.segments(audio.segmentsHolder())
					.language(audioLanguage).duration(audio.duration())
					.codecs(audio.codecs()).bandwidth(audio.bandwidth()).sampleRate(sampleRate)
					.metadata(metadataAudio)
			);
		}
	}
	
	/** @since 00.02.09 */
	private static final class DefaultFormatParser implements Parser.FormatParser {
		
		@Override
		public List<Media.Builder<?, ?>> parse(
				URI uri, MediaFormat format, Request request, URI sourceURI, Map<String, Object> data, long size
		) throws Exception {
			MediaMetadata.Builder mediaData = MediaMetadata.builder().sourceURI(sourceURI);
			
			return List.of(
				VideoMedia.simple()
					.uri(uri).format(format).quality(MediaQuality.UNKNOWN)
					.metadata(mediaData.add(data).build())
			);
		}
	}
	
	private static final class Parser {
		
		private final Map<MediaFormat, FormatParser> parsers;
		/** @since 00.02.09 */
		private FormatParser defaultParser;
		
		private Parser() {
			parsers = new HashMap<>();
		}
		
		/** @since 00.02.09 */
		private static final boolean isNotUnknownFormat(MediaFormat format) {
			return format != null && !format.is(MediaFormat.UNKNOWN);
		}
		
		private static final MediaFormat contentType(HttpHeaders headers) {
			return Optional.ofNullable(headers.allValues("content-type"))
						.map(List::stream)
						.orElseGet(Stream::empty)
						.map(MediaFormat::fromMimeType)
						.findFirst()
						.orElse(MediaFormat.UNKNOWN);
		}
		
		/** @since 00.02.09 */
		private static final MediaFormat contentTypeFromUri(URI uri) {
			return MediaFormat.fromPath(uri.getPath());
		}
		
		private final List<Media.Builder<?, ?>> parseFormat(URI uri, MediaFormat format, Request request,
				URI sourceURI, Map<String, Object> data, long size) throws Exception {
			Property<Exception> ex = new Property<>();
			Exception exception;
			List<Media.Builder<?, ?>> media = Opt.of(parsers.get(format)).ifTrue(Objects::nonNull)
					  .or(() -> Opt.of(defaultParser))
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
					.ifTrue(Parser::isNotUnknownFormat)
					.or((o) -> Opt.of(contentTypeFromUri(uri)))
					.ifTrue(Parser::isNotUnknownFormat)
					.orElse(MediaFormat.UNKNOWN);
				return parseFormat(uri, format, request, sourceURI, data, Web.size(response));
			}
		}
		
		public final <T> Parser format(MediaFormat format, FormatParser parser) {
			parsers.put(Objects.requireNonNull(format), Objects.requireNonNull(parser));
			return this;
		}
		
		/** @since 00.02.09 */
		public final Parser defaultParser(FormatParser parser) {
			defaultParser = Objects.requireNonNull(parser);
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
			
			@SuppressWarnings("unused")
			public MediaFormat format() {
				return format;
			}
			
			@SuppressWarnings("unused")
			public Request request() {
				return request;
			}
			
			@SuppressWarnings("unused")
			public URI sourceURI() {
				return sourceURI;
			}
			
			public Map<String, Object> data() {
				return data;
			}
			
			@SuppressWarnings("unused")
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
		
		public static abstract class M3U8FormatParser implements FormatParser {
			
			protected M3U8FormatParser() {
			}
			
			/** @since 00.02.09 */
			protected abstract Media.Builder<?, ?> map(FormatParserData<M3UCombinedFile> parserData) throws Exception;
			
			@Override
			public List<Media.Builder<?, ?>> parse(URI uri, MediaFormat format, Request request, URI sourceURI,
					Map<String, Object> data, long size) throws Exception {
				FormatParserData<M3UCombinedFile> parserData = new FormatParserData<>(uri, format, request, sourceURI, data, size);
				List<Media.Builder<?, ?>> media = new ArrayList<>();
				for(M3UCombinedFile result : M3U.parse(request)) {
					MediaMetadata.Builder mediaData = MediaMetadata.builder().sourceURI(sourceURI);
					Media.Builder<?, ?> m = map(parserData.result(result).mediaData(mediaData));
					if(m != null) media.add(m);
				}
				return media;
			}
		}
		
		public static abstract class DASHFormatParser implements FormatParser {
			
			protected DASHFormatParser() {
			}
			
			/** @since 00.02.09 */
			protected abstract Media.Builder<?, ?> map(FormatParserData<MPDCombinedFile> parserData) throws Exception;
			
			@Override
			public List<Media.Builder<?, ?>> parse(URI uri, MediaFormat format, Request request, URI sourceURI,
					Map<String, Object> data, long size) throws Exception {
				FormatParserData<MPDCombinedFile> parserData = new FormatParserData<>(uri, format, request, sourceURI, data, size);
				List<Media.Builder<?, ?>> media = new ArrayList<>();
				for(MPDCombinedFile result : MPD.reduce(MPD.parse(request))) {
					MediaMetadata.Builder mediaData = MediaMetadata.builder().sourceURI(sourceURI);
					Media.Builder<?, ?> m = map(parserData.result(result).mediaData(mediaData));
					if(m != null) media.add(m);
				}
				return media;
			}
		}
	}
}