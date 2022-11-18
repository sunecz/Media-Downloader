package sune.app.mediadown.ffmpeg;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import sune.api.process.Processes;
import sune.api.process.ReadOnlyProcess;
import sune.app.mediadown.convert.ConversionCommand;
import sune.app.mediadown.convert.ConversionCommand.Option;
import sune.app.mediadown.convert.ConversionMedia;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.util.Metadata;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;

/** @since 00.02.08 */
public final class FFmpeg {
	
	private static Path path;
	
	// Forbid anyone to create an instance of this class
	private FFmpeg() {
	}
	
	private static final Path ensureBinary() {
		if(path == null) {
			path = NIO.localPath("resources/binary", OSUtils.getExecutableName("ffmpeg"));
			
			if(!NIO.isRegularFile(path)) {
				throw new IllegalStateException("FFmpeg was not found at " + path.toAbsolutePath().toString());
			}
		}
		
		return path;
	}
	
	public static final Path path() {
		return ensureBinary();
	}
	
	public static final ReadOnlyProcess createSynchronousProcess() {
		return Processes.createSynchronous(path());
	}
	
	public static final ReadOnlyProcess createAsynchronousProcess(Consumer<String> listener) {
		return Processes.createAsynchronous(path(), listener);
	}
	
	public static final class Command extends ConversionCommand {
		
		private String string;
		
		protected Command(List<Input> inputs, List<Output> outputs, List<Option> options, Metadata metadata) {
			super(inputs, outputs, options, metadata);
		}
		
		public static final Builder builder() {
			return new Builder();
		}
		
		public static final Builder builder(Command command) {
			return new Builder(command);
		}
		
		public static final Command of(ConversionMedia output, List<ConversionMedia> inputs) {
			return of(output, inputs, Metadata.empty());
		}
		
		public static final Command of(ConversionMedia output, List<ConversionMedia> inputs, Metadata metadata) {
			return Creator.create(output, inputs, metadata);
		}
		
		private final String construct() {
			return (new Constructor(this)).string();
		}
		
		@Override
		public String string() {
			return string == null ? (string = construct()) : string;
		}
		
		private static final class Creator {
			
			private Creator() {
			}
			
			private static final String audioCodec(MediaFormat format) {
				if(format.is(MediaFormat.MP3)) return "mp3";
				if(format.is(MediaFormat.WAV)) return "pcm_s16le";
				if(format.is(MediaFormat.WMA)) return "wmav2";
				if(format.is(MediaFormat.M4A)) return "libfaac";
				
				return null;
			}
			
			private static final void handleAudioOutput(MediaFormat formatInput, MediaFormat formatOutput,
					Output.Builder output) {
				String acodec = audioCodec(formatOutput);
				
				if(acodec == null) {
					throw new IllegalStateException("Audio codec is null for output format: " + formatOutput);
				}
				
				output.addOptions(
					Options.audioCodec().ofValue(acodec),
					Options.audioBitRate().ofValue("248k")
				);
			}
			
			private static final void handleVideoInput(MediaFormat formatInput, MediaFormat formatOutput,
					Output.Builder output) {
				// Special case for OGG format, since it causes some trouble
				if(formatOutput.is(MediaFormat.OGG)) {
					output.addOptions(
						Options.videoCodec().ofValue("libtheora"),
						Options.videoQuality().ofValue("7")
					);
					
					output.addOptions(
						Options.audioCodec().ofValue("libvorbis"),
						Options.audioQuality().ofValue("6")
					);
				} else {
					if(formatInput.is(MediaFormat.M3U8)) {
						output.addOptions(
							Options.codecCopy(),
							Option.ofShort("bsf:a", "aac_adtstoasc")
						);
					} else if(formatInput.is(MediaFormat.DASH)) {
						output.addOptions(
							Options.videoCodecCopy(),
							Options.audioCodec().ofValue("aac")
						);
					} else if(formatInput.is(MediaFormat.OGG)) {
						output.addOptions(
							Options.videoCodec().ofValue("libx264"),
							Option.ofShort("preset", "fast"),
							Option.ofShort("crf", "22")
						);
						
						output.addOptions(
							Options.audioCodec().ofValue("libmp3lame"),
							Options.audioQuality().ofValue("2"),
							Options.audioChannels().ofValue("2"),
							Options.audioSampleRate().ofValue("44100")
						);
					} else {
						output.addOptions(Options.codecCopy());
					}
				}
			}
			
			public static final Command create(ConversionMedia output, List<ConversionMedia> inputs,
					Metadata metadata) {
				if(output == null) {
					throw new IllegalArgumentException("Output cannot be null.");
				}
				
				if(inputs == null || inputs.isEmpty()) {
					throw new IllegalArgumentException("Inputs cannot be neither null nor empty.");
				}
				
				if(metadata == null) {
					throw new IllegalArgumentException("Metadata cannot be null.");
				}
				
				Command.Builder command = Command.builder();
				MediaFormat formatInput = Media.root(inputs.get(0).media()).format();
				MediaFormat formatOutput = MediaFormat.fromPath(output.path());
				Output.Builder out = Output.ofMutable(output.path());
				
				command.addOptions(
					Options.yes(),
					Options.hideBanner(),
					Options.logWarning(),
					Options.stats()
				);
				
				Metadata metadataInput
					= metadata.has("noExplicitInputFormat")
						? Metadata.of("noExplicitFormat", true).seal()
						: Metadata.empty();
				
				for(ConversionMedia input : inputs) {
					command.addInputs(Input.of(input.path(), input.media().format(), metadataInput));
				}
				
				if(inputs.size() == 1 && inputs.get(0).media().format().is(formatOutput)) {
					command.addOptions(Options.codecCopy());
				} else if(formatOutput.mediaType().is(MediaType.AUDIO)) {
					handleAudioOutput(formatInput, formatOutput, out);
				} else if(formatInput.mediaType().is(MediaType.VIDEO)) {
					handleVideoInput(formatInput, formatOutput, out);
				} else {
					throw new IllegalStateException(String.format(
						"Unable to create FFmpeg command: input=%s, output=%s",
						formatInput, formatOutput
					));
				}
				
				command.addOutputs(out.asFormat(formatOutput));
				command.addMetadata(metadata);
				
				return command.build();
			}
		}
		
		private static final class Constructor {
			
			private static final Pattern REGEX_NEEDS_QUOTES = Pattern.compile("[\\s\"']");
			
			private final Command command;
			
			private Constructor(Command command) {
				this.command = command;
			}
			
			private final boolean needsQuotes(String value) {
				return REGEX_NEEDS_QUOTES.matcher(value).find();
			}
			
			private final String escape(String value) {
				return value.replaceAll("\"", "\\\"");
			}
			
			private final void handle(StringBuilder builder, boolean isLong, String name, String value,
					boolean forceQuotes) {
				if(name != null) {
					builder.append(' ');
					builder.append('-');
					
					if(isLong) {
						builder.append('-');
					}
					
					builder.append(name);
				}
				
				if(value != null) {
					builder.append(' ');
					
					if(forceQuotes || needsQuotes(value)) {
						builder.append('"').append(escape(value)).append('"');
					} else {
						builder.append(value);
					}
				}
			}
			
			private final void handle(StringBuilder builder, Path path, MediaFormat format, List<Option> options,
					String argName, boolean includeFormat) {
				for(Option option : options) handle(builder, option);
				
				if(includeFormat && !format.is(MediaFormat.UNKNOWN) && !MediaFormat.fromPath(path).is(format)) {
					handle(builder, false, "f", format.toString().toLowerCase(), false);
				}
				
				handle(builder, false, argName, path.toAbsolutePath().toString(), true);
			}
			
			private final void handle(StringBuilder builder, Option option) {
				handle(builder, option.isLong(), option.name(), option.value(), false);
			}
			
			private final void handle(StringBuilder builder, Input input) {
				boolean includeFormat = !input.metadata().has("noExplicitFormat");
				handle(builder, input.path(), input.format(), input.options(), "i", includeFormat);
			}
			
			private final void handle(StringBuilder builder, Output output) {
				boolean includeFormat = !output.metadata().has("noExplicitFormat");
				handle(builder, output.path(), output.format(), output.options(), null, includeFormat);
			}
			
			public String string() {
				StringBuilder builder = new StringBuilder();
				
				for(Option option : command.options()) handle(builder, option);
				for(Input input   : command.inputs())  handle(builder, input);
				for(Output output : command.outputs()) handle(builder, output);
				
				return builder.toString().stripLeading();
			}
		}
		
		public static final class Builder extends ConversionCommand.Builder {
			
			private Builder() {
			}
			
			private Builder(Command command) {
				inputs.addAll(command.inputs);
				outputs.addAll(command.outputs);
				options.addAll(command.options);
				metadata.setAll(command.metadata);
			}
			
			@Override
			public Command build() {
				if(inputs.isEmpty()) {
					throw new IllegalArgumentException("No inputs");
				}
				
				if(outputs.isEmpty()) {
					throw new IllegalArgumentException("No outputs");
				}
				
				return new Command(List.copyOf(inputs), List.copyOf(outputs), List.copyOf(options), metadata.seal());
			}
		}
	}
	
	public static final class Options {
		
		// ----- Immutable options
		private static Option YES;
		private static Option HIDE_BANNER;
		private static Option LOG_WARNING;
		private static Option STATS;
		private static Option CODEC_COPY;
		private static Option VIDEO_CODEC_COPY;
		private static Option AUDIO_CODEC_COPY;
		// -----
		
		// ----- Mutable options
		private static Option.Builder VIDEO_CODEC;
		private static Option.Builder VIDEO_QUALITY;
		private static Option.Builder AUDIO_CODEC;
		private static Option.Builder AUDIO_BIT_RATE;
		private static Option.Builder AUDIO_QUALITY;
		private static Option.Builder AUDIO_CHANNELS;
		private static Option.Builder AUDIO_SAMPLE_RATE;
		// -----
		
		// Forbid anyone to create an instance of this class
		private Options() {
		}
		
		public static final Option yes() {
			return YES == null
						? YES = Option.ofShort("y")
						: YES;
		}
		
		public static final Option hideBanner() {
			return HIDE_BANNER == null
						? HIDE_BANNER = Option.ofShort("hide_banner")
						: HIDE_BANNER;
		}
		
		public static final Option logWarning() {
			return LOG_WARNING == null
						? LOG_WARNING = Option.ofShort("loglevel", "warning")
						: LOG_WARNING;
		}
		
		public static final Option stats() {
			return STATS == null
						? STATS = Option.ofShort("stats")
						: STATS;
		}
		
		public static final Option codecCopy() {
			return CODEC_COPY == null
						? CODEC_COPY = Option.ofShort("c", "copy")
						: CODEC_COPY;
		}
		
		public static final Option videoCodecCopy() {
			return VIDEO_CODEC_COPY == null
						? VIDEO_CODEC_COPY = Option.ofShort("c:v", "copy")
						: VIDEO_CODEC_COPY;
		}
		
		public static final Option audioCodecCopy() {
			return AUDIO_CODEC_COPY == null
						? AUDIO_CODEC_COPY = Option.ofShort("c:a", "copy")
						: AUDIO_CODEC_COPY;
		}
		
		public static final Option.Builder videoCodec() {
			return (VIDEO_CODEC == null
						? VIDEO_CODEC = Option.ofMutableShort("c:v")
						: VIDEO_CODEC)
					.copy();
		}
		
		public static final Option.Builder videoQuality() {
			return (VIDEO_QUALITY == null
						? VIDEO_QUALITY = Option.ofMutableShort("q:v")
						: VIDEO_QUALITY)
					.copy();
		}
		
		public static final Option.Builder audioCodec() {
			return (AUDIO_CODEC == null
						? AUDIO_CODEC = Option.ofMutableShort("c:a")
						: AUDIO_CODEC)
					.copy();
		}
		
		public static final Option.Builder audioBitRate() {
			return (AUDIO_BIT_RATE == null
						? AUDIO_BIT_RATE = Option.ofMutableShort("ab")
						: AUDIO_BIT_RATE)
					.copy();
		}
		
		public static final Option.Builder audioQuality() {
			return (AUDIO_QUALITY == null
						? AUDIO_QUALITY = Option.ofMutableShort("q:a")
						: AUDIO_QUALITY)
					.copy();
		}
		
		public static final Option.Builder audioChannels() {
			return (AUDIO_CHANNELS == null
						? AUDIO_CHANNELS = Option.ofMutableShort("ac")
						: AUDIO_CHANNELS)
					.copy();
		}
		
		public static final Option.Builder audioSampleRate() {
			return (AUDIO_SAMPLE_RATE == null
						? AUDIO_SAMPLE_RATE = Option.ofMutableShort("ar")
						: AUDIO_SAMPLE_RATE)
					.copy();
		}
	}
}