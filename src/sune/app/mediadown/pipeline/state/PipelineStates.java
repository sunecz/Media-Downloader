package sune.app.mediadown.pipeline.state;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.entity.MediaGetter;
import sune.app.mediadown.entity.MediaGetters;
import sune.app.mediadown.media.AudioMediaBase;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaFilter;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaLanguage;
import sune.app.mediadown.media.MediaQuality;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.media.ResolvedMedia;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineMedia;
import sune.app.mediadown.pipeline.PipelineResult;
import sune.app.mediadown.pipeline.PipelineTask;
import sune.app.mediadown.util.JSON.JSONCollection;
import sune.app.mediadown.util.JSON.JSONNode;
import sune.app.mediadown.util.JSON.JSONObject;
import sune.app.mediadown.util.Opt;
import sune.app.mediadown.util.Range;

/**
 * <p>
 * Helper class for handling pipeline states. This class is especially
 * used for serializing and deserializing pipeline states.
 * </p>
 * 
 * <p>
 * A class that implements {@link PipelineState} can be easily serialized
 * by calling its {@link PipelineState#serialize()} method. Its implementation
 * can be just taking its internal state and outputing it as serialized data.
 * This data will be then used for deserialization. Care should be taken
 * to output such data that are required for the deserialization process.
 * For serialization there is the {@link #serialize(PipelineState)} method.
 * This method, apart from calling the {@link PipelineState#serialize()} method,
 * also checks for the presence of the <code>type</code> property in the data
 * that is used during the deserialization process.
 * </p>
 * 
 * <p>
 * Deserialization process is done using a specific {@link Deserializator}.
 * Given a type string in a serialized data, {@link Deserializator} is chosen
 * and is then used to construct a {@link DeserializationResult} by calling
 * its {@link Deserializator#deserialize(JSONCollection)} method.
 * For deserialization there is the {@link #deserialize(PipelineState)} method.
 * </p>
 * 
 * <p>
 * For convenience there are many methods in the {@link Serialization} class
 * for serialization and in the {@link Deserialization} class for deserialization.
 * </p>
 * 
 * @since 00.02.09
 */
public final class PipelineStates {
	
	private static final Map<String, List<Deserializator>> deserializators = new HashMap<>();
	
	// Forbid anyone to create an instance of this class
	private PipelineStates() {
	}
	
	public static final void register(String type, Deserializator deserializator) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(deserializator);
		deserializators.compute(type, (n, v) -> v == null ? new ArrayList<>() : v).add(deserializator);
	}
	
	public static final JSONCollection serialize(PipelineState state) {
		Objects.requireNonNull(state);
		JSONCollection serialized = state.serialize();
		
		if(!serialized.hasString("type")) {
			throw new IllegalArgumentException("Serialized data does not specify its type");
		}
		
		return serialized;
	}
	
	public static final DeserializationResult deserialize(JSONCollection data) throws Exception {
		if(data == null) {
			return null;
		}
		
		String type = data.getString("type");
		
		if(type == null) {
			return null;
		}
		
		List<Deserializator> list = deserializators.get(type);
		
		if(list == null) {
			return null;
		}
		
		for(Deserializator deserializator : list) {
			DeserializationResult result = deserializator.deserialize(data);
			
			if(result != null) {
				return result;
			}
		}
		
		return null;
	}
	
	public static final class DeserializationResult {
		
		private final PipelineMedia media;
		private final PipelineTask task;
		private final boolean isTerminating;
		
		private ResolvedMedia resolvedMedia;
		private PipelineResult pipelineResult;
		
		public DeserializationResult(
			PipelineMedia media,
			PipelineTask task,
			boolean isTerminating
		) {
			this.media = Objects.requireNonNull(media);
			this.task = Objects.requireNonNull(task);
			this.isTerminating = isTerminating;
		}
		
		public PipelineMedia pipelineMedia() {
			return media;
		}
		
		public PipelineTask pipelineTask() {
			return task;
		}
		
		public boolean isTerminating() {
			return isTerminating;
		}
		
		public ResolvedMedia resolvedMedia() {
			if(resolvedMedia == null) {
				resolvedMedia = new ResolvedMedia(
					media.media(),
					media.destination(),
					media.mediaConfiguration()
				);
			}
			
			return resolvedMedia;
		}
		
		public PipelineResult pipelineResult() {
			if(pipelineResult == null) {
				pipelineResult = new DeserializationPipelineResult(task, isTerminating);
			}
			
			return pipelineResult;
		}
		
		private static final class DeserializationPipelineResult implements PipelineResult {
			
			private final PipelineTask task;
			private final boolean isTerminating;
			
			public DeserializationPipelineResult(PipelineTask task, boolean isTerminating) {
				this.task = task;
				this.isTerminating = isTerminating;
			}
			
			@Override
			public PipelineTask process(Pipeline pipeline) throws Exception {
				return task;
			}
			
			@Override
			public boolean isTerminating() {
				return isTerminating;
			}
		}
	}
	
	public static interface Deserializator {
		
		DeserializationResult deserialize(JSONCollection data) throws Exception;
	}
	
	public static final class Serialization {
		
		private Serialization() {
		}
		
		public static final JSONNode serialize(Media media) {
			if(media == null) {
				throw new IllegalArgumentException("Invalid media");
			}
			
			// Serialize enough to obtain the media and construct the media filter
			return JSONCollection.ofObject(
				"source_uri", JSONObject.ofString(media.metadata().sourceURI().toString()),
				"format", JSONObject.ofString(media.format().name()),
				"quality", JSONObject.ofString(media.quality().name()),
				"language", JSONObject.ofString(
					Opt.of(Media.findOfType(media, MediaType.AUDIO))
					.ifTrue(Objects::nonNull).<AudioMediaBase>cast().map(AudioMediaBase::language)
					.orElse(MediaLanguage.UNKNOWN)
					.name()
				)
			);
		}
		
		public static final JSONNode serialize(Path path) {
			if(path == null) {
				throw new IllegalArgumentException("Invalid path");
			}
			
			return JSONObject.ofString(path.toAbsolutePath().toString());
		}
		
		public static final JSONNode serialize(MediaDownloadConfiguration configuration) {
			if(configuration == null) {
				throw new IllegalArgumentException("Invalid media configuration");
			}
			
			Map<MediaType, List<Media>> selectedMedia = configuration.selectedMedia();
			
			if(selectedMedia == null || selectedMedia.isEmpty()) {
				return JSONCollection.ofObject(
					"output_format", configuration.outputFormat()
				);
			}
			
			return JSONCollection.ofObject(
				"output_format", configuration.outputFormat(),
				"selected_media", JSONCollection.ofArray(
					configuration.selectedMedia().entrySet().stream()
						.map((e) -> JSONCollection.ofObject(
							"type", e.getKey().toString(),
							"media", JSONCollection.ofArray(
								e.getValue().stream().map(Serialization::serialize)
							)
						))
						.toArray(JSONNode[]::new)
				)
			);
		}
		
		public static final JSONNode serialize(Range<?> range) {
			if(range == null) {
				return JSONObject.ofNull();
			}
			
			return JSONCollection.ofObject("from", range.from(), "to", range.to());
		}
		
		public static final JSONNode serialize(DownloadConfiguration configuration) {
			if(configuration == null) {
				return JSONObject.ofNull();
			}
			
			// Currently, we cannot reconstruct the download configuration fully
			return JSONCollection.ofObject(
				"range_input", serialize(configuration.rangeRequest()),
				"range_output", serialize(configuration.rangeOutput())
			);
		}
		
		public static final JSONNode serialize(PipelineMedia media) {
			if(media == null) {
				throw new IllegalArgumentException("Invalid pipeline media");
			}
			
			return JSONCollection.ofObject(
				"media", serialize(media.media()),
				"path", serialize(media.destination()),
				"media_configuration", serialize(media.mediaConfiguration()),
				"download_configuration", serialize(media.configuration())
			);
		}
	}
	
	public static final class Deserialization {
		
		private static final Range<Long> RANGE_UNSET = new Range<>(-1L, -1L);
		
		private Deserialization() {
		}
		
		public static final Media media(JSONCollection data) throws Exception {
			if(data == null) {
				throw new IllegalArgumentException("Invalid media data");
			}
			
			String strSourceUri = data.getString("source_uri");
			
			if(strSourceUri == null) {
				throw new IllegalStateException("Invalid source URI");
			}
			
			URI sourceUri = Net.uri(strSourceUri);
			MediaFormat format = MediaFormat.ofName(data.getString("format"));
			MediaQuality quality = MediaQuality.ofName(data.getString("quality"));
			MediaLanguage language = MediaLanguage.ofName(data.getString("language"));
			
			MediaFilter.Builder builder = MediaFilter.builder();
			
			if(!format.is(MediaFormat.UNKNOWN)) {
				builder.formatPriority(format);
			}
			
			if(!quality.is(MediaQuality.UNKNOWN)) {
				builder.qualityPriority(quality);
			}
			
			if(!language.is(MediaLanguage.UNKNOWN)) {
				builder.audioLanguage(language);
			}
			
			MediaFilter filter = builder.build();
			MediaGetter getter = MediaGetters.fromURI(sourceUri);
			
			if(getter == null) {
				throw new IllegalStateException("Unable to obtain media getter");
			}
			
			List<Media> allMedia = getter.getMedia(sourceUri).startAndGet();
			Media media = filter.filter(allMedia);
			
			if(media == null) {
				throw new IllegalStateException("No media found");
			}
			
			return media;
		}
		
		public static final Path path(JSONObject data) {
			if(data == null) {
				throw new IllegalArgumentException("Invalid path");
			}
			
			return Path.of(data.stringValue());
		}
		
		public static final MediaDownloadConfiguration mediaConfiguration(
			JSONCollection data,
			Media rootMedia
		) {
			if(data == null) {
				throw new IllegalArgumentException("Invalid media configuration");
			}
			
			MediaFormat outputFormat = MediaFormat.ofName(data.getString("output_format"));
			JSONCollection selectedMediaArray = data.getCollection("selected_media");
			Map<MediaType, List<Media>> selectedMedia = null;
			
			if(selectedMediaArray != null && selectedMediaArray.length() > 0) {
				selectedMedia = new HashMap<>();
				
				for(JSONCollection item : selectedMediaArray.collectionsIterable()) {
					MediaType mediaType = MediaType.ofName(item.getString("type"));
					List<Media> filteredMedia = Media.findAllOfType(rootMedia, mediaType);
					List<Media> selectedOfType = new ArrayList<>();
					JSONCollection mediaArray = item.getCollection("media");
					
					if(mediaArray != null) {
						for(JSONCollection mediaItem : mediaArray.collectionsIterable()) {
							MediaFormat format = MediaFormat.ofName(mediaItem.getString("format"));
							Media subMedia = filteredMedia.stream()
								.filter((m) -> m.format().is(format))
								.findFirst().orElse(null);
							
							// Silently fail when a selected media cannot be obtained. This allows
							// to continue the reconstruction even for incomplete data.
							// The check for incompleteness should be done outside of this method.
							if(subMedia != null) {
								selectedOfType.add(subMedia);
							}
						}
					}
					
					if(!selectedOfType.isEmpty()) {
						selectedMedia.put(mediaType, selectedOfType);
					}
				}
			}
			
			if(selectedMedia == null || selectedMedia.isEmpty()) {
				selectedMedia = Map.of();
			}
			
			return MediaDownloadConfiguration.of(outputFormat, selectedMedia);
		}
		
		public static final Range<Long> rangeOfLong(JSONCollection data) {
			if(data == null) {
				return RANGE_UNSET;
			}
			
			return new Range<>(data.getLong("from", -1L), data.getLong("to", -1L));
		}
		
		public static final DownloadConfiguration downloadConfiguration(JSONCollection data) {
			if(data == null) {
				return DownloadConfiguration.ofDefault();
			}
			
			Range<Long> rangeOutput = rangeOfLong(data.getCollection("range_output"));
			Range<Long> rangeRequest = rangeOfLong(data.getCollection("range_input"));
			
			if(rangeOutput.from() < 0L && rangeOutput.to() < 0L
					&& rangeRequest.from() < 0L && rangeRequest.to() < 0L) {
				return DownloadConfiguration.ofDefault();
			}
			
			return DownloadConfiguration.ofRanges(rangeOutput, rangeRequest);
		}
		
		public static final PipelineMedia pipelineMedia(JSONCollection data) throws Exception {
			if(data == null) {
				throw new IllegalArgumentException("Invalid pipeline media data");
			}
			
			Media media = media(data.getCollection("media"));
			Path path = path(data.getObject("path"));
			
			MediaDownloadConfiguration mediaConfiguration = mediaConfiguration(
				data.getCollection("media_configuration"),
				media
			);
			
			DownloadConfiguration downloadConfiguration = downloadConfiguration(
				data.getCollection("download_configuration")
			);
			
			return PipelineMedia.of(media, path, mediaConfiguration, downloadConfiguration);
		}
	}
}
