package sune.app.mediadown.pipeline;

import java.nio.file.Path;
import java.util.Objects;

import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.media.AudioMediaBase;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaLanguage;
import sune.app.mediadown.media.MediaType;
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
 * @since 00.02.09
 */
public class PipelineStates {
	
	// Forbid anyone to create an instance of this class
	private PipelineStates() {
	}
	
	public static final class Serializator {
		
		private Serializator() {
		}
		
		public static final JSONNode serialize(Media media) {
			// Serialize enough to obtain the media and construct the media filter
			return JSONCollection.ofObject(
				"source_uri", JSONObject.ofString(media.metadata().sourceURI().toString()),
				"format", JSONObject.ofString(media.format().toString()),
				"quality", JSONObject.ofString(media.quality().toString()),
				"language", JSONObject.ofString(
					Opt.of(Media.findOfType(media, MediaType.AUDIO))
					.ifTrue(Objects::nonNull).<AudioMediaBase>cast().map(AudioMediaBase::language)
					.orElse(MediaLanguage.UNKNOWN)
					.toString()
				)
			);
		}
		
		public static final JSONNode serialize(Path path) {
			return JSONObject.ofString(path.toAbsolutePath().toString());
		}
		
		public static final JSONNode serialize(MediaDownloadConfiguration configuration) {
			return JSONCollection.ofObject(
				"output_format", configuration.outputFormat(),
				"selected_media", JSONCollection.ofArray(
					configuration.selectedMedia().entrySet().stream()
						.map((e) -> JSONCollection.ofObject(
							"type", e.getKey().toString(),
							"media", JSONCollection.ofArray(
								e.getValue().stream().map(Serializator::serialize)
							)
						))
						.toArray(JSONNode[]::new)
				)
			);
		}
		
		public static final JSONNode serialize(Range<?> range) {
			return JSONCollection.ofObject("from", range.from(), "to", range.to());
		}
		
		public static final JSONNode serialize(DownloadConfiguration configuration) {
			// Currently, we cannot reconstruct the download configuration fully
			return JSONCollection.ofObject(
				"range_input", serialize(configuration.rangeRequest()),
				"range_output", serialize(configuration.rangeOutput())
			);
		}
		
		public static final JSONNode serialize(PipelineMedia media) {
			return JSONCollection.ofObject(
				"media", serialize(media.media()),
				"path", serialize(media.destination()),
				"media_configuration", serialize(media.mediaConfiguration()),
				"download_configuration", serialize(media.configuration())
			);
		}
	}
}
