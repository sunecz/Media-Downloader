package sune.app.mediadown.report;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sune.app.mediadown.conversion.ConversionMedia;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.entity.Episode;
import sune.app.mediadown.entity.MediaGetter;
import sune.app.mediadown.entity.Program;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaConversionContext;
import sune.app.mediadown.media.MediaDownloadContext;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.util.JSON.JSONCollection;
import sune.app.mediadown.util.JSON.JSONNode;
import sune.app.mediadown.util.JSON.JSONObject;
import sune.app.mediadown.util.Metadata;
import sune.app.mediadown.util.Range;

/** @since 00.02.09 */
public final class ReportSerialization {
	
	// Forbid anyone to create an instance of this class
	private ReportSerialization() {
	}
	
	public static final String prepare(String string) {
		return string.replace("\\", "\\\\").replace("\"", "\\\"");
	}
	
	public static final JSONNode serialize(Map<String, Object> metadata) {
		JSONCollection parent = JSONCollection.empty();
		
		for(Entry<String, Object> pair : metadata.entrySet()) {
			parent.set(pair.getKey(), JSONObject.of(pair.getValue()));
		}
		
		return parent;
	}
	
	public static final JSONNode serialize(Range<Long> range) {
		JSONCollection parent = JSONCollection.empty();
		parent.set("from", range.from());
		parent.set("to", range.to());
		return parent;
	}
	
	public static final JSONNode serialize(MediaDownloadConfiguration configuration, boolean anonymize) {
		JSONCollection parent = JSONCollection.empty();
		parent.set("outputFormat", configuration.outputFormat().name());
		
		for(Entry<MediaType, List<Media>> entry : configuration.selectedMedia().entrySet()) {
			JSONCollection array = JSONCollection.emptyArray();
			entry.getValue().stream().map((m) -> serialize(m, anonymize)).forEach(array::add);
			parent.set(entry.getKey().name(), array);
		}
		
		return parent;
	}
	
	public static final JSONCollection serialize(MediaGetter getter, boolean anonymize) {
		JSONCollection data = JSONCollection.empty();
		data.set("title", getter.title());
		data.set("version", getter.version());
		data.set("url", getter.url());
		data.set("author", getter.author());
		return data;
	}
	
	public static final JSONCollection serialize(Program program, boolean anonymize) {
		JSONCollection data = JSONCollection.empty();
		data.set("uri", program.uri().toString());
		data.set("title", program.title());
		data.set("metadata", ReportSerialization.serialize(program.data()));
		return data;
	}
	
	public static final JSONCollection serialize(Episode episode, boolean anonymize) {
		JSONCollection data = JSONCollection.empty();
		data.set("program", serialize(episode.program(), anonymize));
		data.set("uri", episode.uri().toString());
		data.set("title", episode.title());
		data.set("metadata", ReportSerialization.serialize(episode.data()));
		return data;
	}
	
	public static final JSONCollection serialize(Media media, boolean anonymize) {
		JSONCollection data = JSONCollection.empty();
		data.set("type", media.type().toString());
		data.set("source", media.source().toString());
		data.set("uri", media.uri().toString());
		data.set("format", media.format().toString());
		data.set("quality", media.quality().toString());
		data.set("size", media.size());
		data.set("isContainer", media.isContainer());
		data.set("isSegmented", media.isSegmented());
		data.set("isSingle", media.isSingle());
		data.set("isSolid", media.isSolid());
		data.set("metadata", ReportSerialization.serialize(media.metadata().data()));
		
		Media parent;
		if((parent = media.parent()) != null) {
			data.set("parent", serialize(parent, anonymize));
		} else {
			data.setNull("parent");
		}
		
		return data;
	}
	
	public static final JSONCollection serialize(MediaDownloadContext context, boolean anonymize) {
		JSONCollection data = JSONCollection.empty();
		
		data.set("media", serialize(context.media(), anonymize));
		
		Path destination = context.destination().toAbsolutePath();
		if(!anonymize) {
			data.set("destination", ReportSerialization.prepare(destination.toString()));
		}
		data.set("mediaConfiguration", ReportSerialization.serialize(context.mediaConfiguration(), anonymize));
		
		DownloadConfiguration configuration = context.configuration();
		JSONCollection dataConfiguration = JSONCollection.empty();
		dataConfiguration.set("rangeOutput", ReportSerialization.serialize(configuration.rangeOutput()));
		dataConfiguration.set("rangeRequest", ReportSerialization.serialize(configuration.rangeRequest()));
		dataConfiguration.set("totalBytes", configuration.totalBytes());
		data.set("configuration", dataConfiguration);
		
		return data;
	}
	
	public static final JSONCollection serialize(MediaConversionContext context, boolean anonymize) {
		JSONCollection data = JSONCollection.empty();
		
		ResolvedMedia output = context.output();
		JSONCollection dataOutput = JSONCollection.empty();
		dataOutput.set("media", serialize(output.media(), anonymize));
		dataOutput.set("configuration", ReportSerialization.serialize(output.configuration(), anonymize));
		if(!anonymize) {
			dataOutput.set("path", ReportSerialization.prepare(output.path().toAbsolutePath().toString()));
		}
		data.set("output", dataOutput);
		
		List<ConversionMedia> inputs = context.inputs();
		JSONCollection dataInputs = JSONCollection.emptyArray();
		
		for(ConversionMedia media : inputs) {
			JSONCollection item = JSONCollection.empty();
			item.set("media", serialize(media.media(), anonymize));
			item.set("format", media.format().name());
			if(!anonymize) {
				item.set("path", ReportSerialization.prepare(media.path().toAbsolutePath().toString()));
			}
			item.set("duration", media.duration());
			dataInputs.add(item);
		}
		
		Metadata metadata = context.metadata();
		data.set("metadata", ReportSerialization.serialize(metadata.data()));
		
		return data;
	}
}