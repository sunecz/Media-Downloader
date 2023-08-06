package sune.app.mediadown.report;

import java.net.URI;
import java.util.Objects;

import sune.app.mediadown.entity.Episode;
import sune.app.mediadown.entity.Program;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.pipeline.ConversionPipelineTask;
import sune.app.mediadown.pipeline.DownloadPipelineTask;
import sune.app.mediadown.util.JSON.JSONCollection;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public abstract class Report {
	
	private final String name;
	private final Reason reason;
	private final ReportContext context;
	private final ContactInformation contact;
	
	protected Report(String name, Reason reason, ReportContext context, ContactInformation contact) {
		this.name = Objects.requireNonNull(name);
		this.reason = Objects.requireNonNull(reason);
		this.context = Objects.requireNonNull(context);
		this.contact = contact;
	}
	
	public static final Report ofURI(URI uri, Reason reason, ReportContext context, ContactInformation contact) {
		return new OfURI(reason, context, contact, uri);
	}
	
	public static final Report ofError(Throwable error, Reason reason, ReportContext context,
			ContactInformation contact) {
		return new OfError(reason, context, contact, error);
	}
	
	public abstract JSONCollection serializeData(boolean anonymize);
	
	public String name() {
		return name;
	}
	
	public Reason reason() {
		return reason;
	}
	
	public ReportContext context() {
		return context;
	}
	
	public ContactInformation contact() {
		return contact;
	}
	
	private static final class OfURI extends Report {
		
		private static final String NAME = "uri";
		
		private final URI uri;
		
		public OfURI(Reason reason, ReportContext context, ContactInformation contact, URI uri) {
			super(NAME, reason, context, contact);
			this.uri = Objects.requireNonNull(uri);
		}
		
		@Override
		public JSONCollection serializeData(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("uri", uri.toString());
			return data;
		}
		
		public static final class Builder extends Report.Builder {
			
			private URI uri;
			
			public Builder(Reason reason, ReportContext context, URI uri) {
				super(NAME, reason, context);
				this.uri = Objects.requireNonNull(uri);
			}
			
			@Override
			public Report build() {
				return new OfURI(reason, context, contact, uri);
			}
		}
	}
	
	private static final class OfError extends Report {
		
		private static final String NAME = "error";
		
		private final Throwable error;
		
		public OfError(Reason reason, ReportContext context, ContactInformation contact, Throwable error) {
			super(NAME, reason, context, contact);
			this.error = Objects.requireNonNull(error);
		}
		
		@Override
		public JSONCollection serializeData(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("error", Utils.throwableToString(error));
			return data;
		}
		
		public static final class Builder extends Report.Builder {
			
			private final Throwable error;
			
			public Builder(Reason reason, ReportContext context, Throwable error) {
				super(NAME, reason, context);
				this.error = Objects.requireNonNull(error);
			}
			
			@Override
			public Report build() {
				return new OfError(reason, context, contact, error);
			}
		}
	}
	
	private static final class OfProgram extends Report {
		
		private static final String NAME = "program";
		
		private final Program program;
		
		public OfProgram(Reason reason, ReportContext context, ContactInformation contact, Program program) {
			super(NAME, reason, context, contact);
			this.program = Objects.requireNonNull(program);
		}
		
		@Override
		public JSONCollection serializeData(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("program", ReportSerialization.serialize(program, anonymize));
			return data;
		}
		
		public static final class Builder extends Report.Builder {
			
			private final Program program;
			
			public Builder(Reason reason, ReportContext context, Program program) {
				super(NAME, reason, context);
				this.program = Objects.requireNonNull(program);
			}
			
			@Override
			public Report build() {
				return new OfProgram(reason, context, contact, program);
			}
		}
	}
	
	private static final class OfEpisode extends Report {
		
		private static final String NAME = "episode";
		
		private final Episode episode;
		
		public OfEpisode(Reason reason, ReportContext context, ContactInformation contact, Episode episode) {
			super(NAME, reason, context, contact);
			this.episode = Objects.requireNonNull(episode);
		}
		
		@Override
		public JSONCollection serializeData(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("episode", ReportSerialization.serialize(episode, anonymize));
			return data;
		}
		
		public static final class Builder extends Report.Builder {
			
			private final Episode episode;
			
			public Builder(Reason reason, ReportContext context, Episode episode) {
				super(NAME, reason, context);
				this.episode = Objects.requireNonNull(episode);
			}
			
			@Override
			public Report build() {
				return new OfEpisode(reason, context, contact, episode);
			}
		}
	}
	
	private static final class OfMedia extends Report {
		
		private static final String NAME = "media";
		
		private final Media media;
		
		public OfMedia(Reason reason, ReportContext context, ContactInformation contact, Media media) {
			super(NAME, reason, context, contact);
			this.media = Objects.requireNonNull(media);
		}
		
		@Override
		public JSONCollection serializeData(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("media", ReportSerialization.serialize(media, anonymize));
			return data;
		}
		
		public static final class Builder extends Report.Builder {
			
			private final Media media;
			
			public Builder(Reason reason, ReportContext context, Media media) {
				super(NAME, reason, context);
				this.media = Objects.requireNonNull(media);
			}
			
			@Override
			public Report build() {
				return new OfMedia(reason, context, contact, media);
			}
		}
	}
	
	private static final class OfDownload extends Report {
		
		private static final String NAME = "download";
		
		private final DownloadPipelineTask task;
		
		public OfDownload(Reason reason, ReportContext context, ContactInformation contact, DownloadPipelineTask task) {
			super(NAME, reason, context, contact);
			this.task = Objects.requireNonNull(task);
		}
		
		@Override
		public JSONCollection serializeData(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("task", ReportSerialization.serialize(task, anonymize));
			return data;
		}
		
		public static final class Builder extends Report.Builder {
			
			private final DownloadPipelineTask task;
			
			public Builder(Reason reason, ReportContext context, DownloadPipelineTask task) {
				super(NAME, reason, context);
				this.task = Objects.requireNonNull(task);
			}
			
			@Override
			public Report build() {
				return new OfDownload(reason, context, contact, task);
			}
		}
	}
	
	private static final class OfConversion extends Report {
		
		private static final String NAME = "conversion";
		
		private final ConversionPipelineTask task;
		
		public OfConversion(Reason reason, ReportContext context, ContactInformation contact, ConversionPipelineTask task) {
			super(NAME, reason, context, contact);
			this.task = Objects.requireNonNull(task);
		}
		
		@Override
		public JSONCollection serializeData(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("task", ReportSerialization.serialize(task, anonymize));
			return data;
		}
		
		public static final class Builder extends Report.Builder {
			
			private final ConversionPipelineTask task;
			
			public Builder(Reason reason, ReportContext context, ConversionPipelineTask task) {
				super(NAME, reason, context);
				this.task = Objects.requireNonNull(task);
			}
			
			@Override
			public Report build() {
				return new OfConversion(reason, context, contact, task);
			}
		}
	}
	
	public static enum Reason {
		
		ERROR, BROKEN, IMPROVEMENT, OTHER;
	}
	
	public static abstract class Builder {
		
		protected String name;
		protected Reason reason;
		protected ReportContext context;
		protected ContactInformation contact;
		
		protected Builder(String name, Reason reason, ReportContext context) {
			this.name = Objects.requireNonNull(name);
			this.reason = Objects.requireNonNull(reason);
			this.context = Objects.requireNonNull(context);
		}
		
		public abstract Report build();
		
		public void contact(ContactInformation contact) {
			this.contact = contact;
		}
		
		public String name() {
			return name;
		}
		
		public Reason reason() {
			return reason;
		}
		
		public ReportContext context() {
			return context;
		}
		
		public ContactInformation contact() {
			return contact;
		}
	}
	
	public static final class Builders {
		
		// Forbid anyone to create an instance of this class
		private Builders() {
		}
		
		public static final Builder ofURI(URI uri, Reason reason, ReportContext context) {
			return new OfURI.Builder(reason, context, uri);
		}
		
		public static final Builder ofError(Throwable error, Reason reason, ReportContext context) {
			return new OfError.Builder(reason, context, error);
		}
		
		public static final Builder ofProgram(Program program, Reason reason, ReportContext context) {
			return new OfProgram.Builder(reason, context, program);
		}
		
		public static final Builder ofEpisode(Episode episode, Reason reason, ReportContext context) {
			return new OfEpisode.Builder(reason, context, episode);
		}
		
		public static final Builder ofMedia(Media media, Reason reason, ReportContext context) {
			return new OfMedia.Builder(reason, context, media);
		}
		
		public static final Builder ofDownload(DownloadPipelineTask task, Reason reason, ReportContext context) {
			return new OfDownload.Builder(reason, context, task);
		}
		
		public static final Builder ofConversion(ConversionPipelineTask task, Reason reason, ReportContext context) {
			return new OfConversion.Builder(reason, context, task);
		}
	}
}