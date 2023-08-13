package sune.app.mediadown.report;

import java.util.Objects;

import sune.app.mediadown.entity.Episode;
import sune.app.mediadown.entity.MediaGetter;
import sune.app.mediadown.entity.Program;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaConversionContext;
import sune.app.mediadown.media.MediaDownloadContext;
import sune.app.mediadown.util.JSON.JSONCollection;

/** @since 00.02.09 */
public abstract class ReportContext {
	
	protected ReportContext() {
	}
	
	public static final ReportContext none() {
		return OfNone.INSTANCE;
	}
	
	public static final ReportContext ofMediaGetter(MediaGetter getter) {
		return new OfMediaGetter(getter);
	}
	
	public static final ReportContext ofProgram(Program program) {
		return new OfProgram(program);
	}
	
	public static final ReportContext ofEpisode(Episode episode) {
		return new OfEpisode(episode);
	}
	
	public static final ReportContext ofMedia(Media media) {
		return new OfMedia(media);
	}
	
	public static final ReportContext ofDownload(MediaDownloadContext context) {
		return new OfDownload(context);
	}
	
	public static final ReportContext ofConversion(MediaConversionContext context) {
		return new OfConversion(context);
	}
	
	public abstract JSONCollection serialize(boolean anonymize);
	
	private static final class OfNone extends ReportContext {
		
		private static final OfNone INSTANCE = new OfNone();
		
		private OfNone() {
		}
		
		@Override
		public JSONCollection serialize(boolean anonymize) {
			return JSONCollection.empty();
		}
	}
	
	private static final class OfMediaGetter extends ReportContext {
		
		private final MediaGetter getter;
		
		protected OfMediaGetter(MediaGetter getter) {
			this.getter = Objects.requireNonNull(getter);
		}
		
		@Override
		public JSONCollection serialize(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("name", getClass().getSimpleName());
			data.set("engine", ReportSerialization.serialize(getter, anonymize));
			return data;
		}
	}
	
	private static final class OfProgram extends ReportContext {
		
		private final Program program;
		
		protected OfProgram(Program program) {
			this.program = Objects.requireNonNull(program);
		}
		
		@Override
		public JSONCollection serialize(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("name", getClass().getSimpleName());
			data.set("program", ReportSerialization.serialize(program, anonymize));
			return data;
		}
	}
	
	private static final class OfEpisode extends ReportContext {
		
		private final Episode episode;
		
		protected OfEpisode(Episode episode) {
			this.episode = Objects.requireNonNull(episode);
		}
		
		@Override
		public JSONCollection serialize(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("name", getClass().getSimpleName());
			data.set("episode", ReportSerialization.serialize(episode, anonymize));
			return data;
		}
	}
	
	private static final class OfMedia extends ReportContext {
		
		private final Media media;
		
		protected OfMedia(Media media) {
			this.media = Objects.requireNonNull(media);
		}
		
		@Override
		public JSONCollection serialize(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("name", getClass().getSimpleName());
			data.set("media", ReportSerialization.serialize(media, anonymize));
			return data;
		}
	}
	
	private static final class OfDownload extends ReportContext {
		
		private final MediaDownloadContext context;
		
		protected OfDownload(MediaDownloadContext context) {
			this.context = Objects.requireNonNull(context);
		}
		
		@Override
		public JSONCollection serialize(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("name", getClass().getSimpleName());
			data.set("task", ReportSerialization.serialize(context, anonymize));
			return data;
		}
	}
	
	private static final class OfConversion extends ReportContext {
		
		private final MediaConversionContext context;
		
		protected OfConversion(MediaConversionContext context) {
			this.context = Objects.requireNonNull(context);
		}
		
		@Override
		public JSONCollection serialize(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("name", getClass().getSimpleName());
			data.set("task", ReportSerialization.serialize(context, anonymize));
			return data;
		}
	}
}