package sune.app.mediadown.report;

import java.net.URI;
import java.util.Objects;

import sune.app.mediadown.entity.Episode;
import sune.app.mediadown.entity.Program;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaConversionContext;
import sune.app.mediadown.media.MediaDownloadContext;
import sune.app.mediadown.util.JSON.JSONCollection;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public abstract class Report {
	
	private final String name;
	private final Reason reason;
	private final ReportContext context;
	private final String note;
	private final ContactInformation contact;
	
	protected Report(String name, Reason reason, ReportContext context, String note, ContactInformation contact) {
		this.name = Objects.requireNonNull(name);
		this.reason = Objects.requireNonNull(reason);
		this.context = Objects.requireNonNull(context);
		this.note = note; // May be null
		this.contact = contact; // May be null
	}
	
	public static final Report ofURI(URI uri, Reason reason, ReportContext context, String note,
			ContactInformation contact) {
		return new OfURI(reason, context, note, contact, uri);
	}
	
	public static final Report ofError(Throwable error, Reason reason, ReportContext context, String note,
			ContactInformation contact) {
		return new OfError(reason, context, note, contact, error);
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
	
	public String note() {
		return note;
	}
	
	public ContactInformation contact() {
		return contact;
	}
	
	private static final class OfURI extends Report {
		
		private static final String NAME = "uri";
		
		private final URI uri;
		
		public OfURI(Reason reason, ReportContext context, String note, ContactInformation contact, URI uri) {
			super(NAME, reason, context, note, contact);
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
				return new OfURI(reason, context, note, contact, uri);
			}
		}
	}
	
	private static final class OfError extends Report {
		
		private static final String NAME = "error";
		
		private final Throwable error;
		
		public OfError(Reason reason, ReportContext context, String note, ContactInformation contact, Throwable error) {
			super(NAME, reason, context, note, contact);
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
				return new OfError(reason, context, note, contact, error);
			}
		}
	}
	
	private static final class OfIssue extends Report {
		
		private static final String NAME = "issue";
		
		public OfIssue(String note, ReportContext context, ContactInformation contact) {
			super(NAME, Reason.ISSUE, context, note, contact);
		}
		
		@Override
		public JSONCollection serializeData(boolean anonymize) {
			return JSONCollection.empty();
		}
		
		public static final class Builder extends Report.Builder {
			
			public Builder(ReportContext context) {
				super(NAME, Reason.ISSUE, context);
			}
			
			@Override
			public Report build() {
				return new OfIssue(note, context, contact);
			}
		}
	}
	
	private static final class OfFeedback extends Report {
		
		private static final String NAME = "feedback";
		
		public OfFeedback(String note, ReportContext context, ContactInformation contact) {
			super(NAME, Reason.FEEDBACK, context, note, contact);
		}
		
		@Override
		public JSONCollection serializeData(boolean anonymize) {
			return JSONCollection.empty();
		}
		
		public static final class Builder extends Report.Builder {
			
			public Builder(ReportContext context) {
				super(NAME, Reason.FEEDBACK, context);
			}
			
			@Override
			public Report build() {
				return new OfFeedback(note, context, contact);
			}
		}
	}
	
	private static final class OfProgram extends Report {
		
		private static final String NAME = "program";
		
		private final Program program;
		
		public OfProgram(Reason reason, ReportContext context, String note, ContactInformation contact,
				Program program) {
			super(NAME, reason, context, note, contact);
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
				return new OfProgram(reason, context, note, contact, program);
			}
		}
	}
	
	private static final class OfEpisode extends Report {
		
		private static final String NAME = "episode";
		
		private final Episode episode;
		
		public OfEpisode(Reason reason, ReportContext context, String note, ContactInformation contact,
				Episode episode) {
			super(NAME, reason, context, note, contact);
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
				return new OfEpisode(reason, context, note, contact, episode);
			}
		}
	}
	
	private static final class OfMedia extends Report {
		
		private static final String NAME = "media";
		
		private final Media media;
		
		public OfMedia(Reason reason, ReportContext context, String note, ContactInformation contact, Media media) {
			super(NAME, reason, context, note, contact);
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
				return new OfMedia(reason, context, note, contact, media);
			}
		}
	}
	
	private static final class OfDownload extends Report {
		
		private static final String NAME = "download";
		
		private final MediaDownloadContext mediaContext;
		
		public OfDownload(Reason reason, ReportContext context, String note, ContactInformation contact,
				MediaDownloadContext mediaContext) {
			super(NAME, reason, context, note, contact);
			this.mediaContext = Objects.requireNonNull(mediaContext);
		}
		
		@Override
		public JSONCollection serializeData(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("task", ReportSerialization.serialize(mediaContext, anonymize));
			return data;
		}
		
		public static final class Builder extends Report.Builder {
			
			private final MediaDownloadContext mediaContext;
			
			public Builder(Reason reason, ReportContext context, MediaDownloadContext mediaContext) {
				super(NAME, reason, context);
				this.mediaContext = Objects.requireNonNull(mediaContext);
			}
			
			@Override
			public Report build() {
				return new OfDownload(reason, context, note, contact, mediaContext);
			}
		}
	}
	
	private static final class OfConversion extends Report {
		
		private static final String NAME = "conversion";
		
		private final MediaConversionContext mediaContext;
		
		public OfConversion(Reason reason, ReportContext context, String note, ContactInformation contact,
				MediaConversionContext mediaContext) {
			super(NAME, reason, context, note, contact);
			this.mediaContext = Objects.requireNonNull(mediaContext);
		}
		
		@Override
		public JSONCollection serializeData(boolean anonymize) {
			JSONCollection data = JSONCollection.empty();
			data.set("task", ReportSerialization.serialize(mediaContext, anonymize));
			return data;
		}
		
		public static final class Builder extends Report.Builder {
			
			private final MediaConversionContext mediaContext;
			
			public Builder(Reason reason, ReportContext context, MediaConversionContext mediaContext) {
				super(NAME, reason, context);
				this.mediaContext = Objects.requireNonNull(mediaContext);
			}
			
			@Override
			public Report build() {
				return new OfConversion(reason, context, note, contact, mediaContext);
			}
		}
	}
	
	public static enum Reason {
		
		ERROR, BROKEN, IMPROVEMENT, ISSUE, FEEDBACK, OTHER;
	}
	
	public static abstract class Builder {
		
		protected String name;
		protected Reason reason;
		protected ReportContext context;
		protected String note;
		protected ContactInformation contact;
		
		protected Builder(String name, Reason reason, ReportContext context) {
			this.name = Objects.requireNonNull(name);
			this.reason = Objects.requireNonNull(reason);
			this.context = Objects.requireNonNull(context);
			// Always set the default contact information
			this.contact = Reporting.defaultContactInformation();
		}
		
		public abstract Report build();
		
		public void reason(Reason reason) {
			this.reason = reason;
		}
		
		public void contact(ContactInformation contact) {
			this.contact = contact;
		}
		
		public void note(String note) {
			this.note = note;
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
		
		public String note() {
			return note;
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
		
		public static final Builder ofIssue(ReportContext context) {
			return new OfIssue.Builder(context);
		}
		
		public static final Builder ofFeedback(ReportContext context) {
			return new OfFeedback.Builder(context);
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
		
		public static final Builder ofDownload(MediaDownloadContext mediaContext, Reason reason, ReportContext context) {
			return new OfDownload.Builder(reason, context, mediaContext);
		}
		
		public static final Builder ofConversion(MediaConversionContext mediaContext, Reason reason, ReportContext context) {
			return new OfConversion.Builder(reason, context, mediaContext);
		}
	}
}