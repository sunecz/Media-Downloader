package sune.app.mediadown.ffmpeg;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import sune.api.process.Processes;
import sune.api.process.ReadOnlyProcess;
import sune.app.mediadown.conversion.AbstractConversionProvider;
import sune.app.mediadown.conversion.ConversionCommand;
import sune.app.mediadown.conversion.ConversionCommand.Input;
import sune.app.mediadown.conversion.ConversionCommand.Option;
import sune.app.mediadown.conversion.ConversionCommand.Output;
import sune.app.mediadown.conversion.ConversionFormat;
import sune.app.mediadown.conversion.ConversionMedia;
import sune.app.mediadown.entity.Converter;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.media.AudioMedia;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.media.VideoMedia;
import sune.app.mediadown.util.Metadata;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Regex;

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
	
	private static final class Formats {
		
		private static final String DEFAULT_AUDIO_BIT_RATE = "320k";
		private static final String DEFAULT_AUDIO_CHANNELS = "2";
		private static final String DEFAULT_AUDIO_SAMPLE_RATE = "48000";
		
		private static final int RESULT_NONE = 0;
		private static final int RESULT_COPY = 1;
		private static final int RESULT_REENCODE = 2;
		
		// Forbid anyone to create an instance of this class
		private Formats() {
		}
		
		private static final <T> T value(Metadata metadata, String name, T defaultValue) {
			return Optional.ofNullable(metadata.<T>get(name)).orElse(defaultValue);
		}
		
		private static final String audioBitRate(Metadata metadata) {
			return value(metadata, "audio.bitRate", DEFAULT_AUDIO_BIT_RATE);
		}
		
		private static final String audioSampleRate(Metadata metadata) {
			return value(metadata, "audio.sampleRate", DEFAULT_AUDIO_SAMPLE_RATE);
		}
		
		private static final String audioChannels(Metadata metadata) {
			return value(metadata, "audio.channels", DEFAULT_AUDIO_CHANNELS);
		}
		
		private static final boolean isAudioSeparated(Media root) {
			return !root.format().is(MediaFormat.M3U8) && Media.findOfType(root, MediaType.AUDIO) != null;
		}
		
		private static abstract class VideoConversionFormat extends ConversionFormat {
			
			public VideoConversionFormat(MediaFormat format) {
				super(format);
			}
			
			protected abstract int ensureVideo(Media media, int index, Input.Builder input, Output.Builder output,
					boolean force);
			protected abstract int ensureAudio(Media media, int index, Input.Builder input, Output.Builder output,
					boolean force);
			
			@Override
			public void from(Media media, int index, Input.Builder input, Output.Builder output) {
				if(media.parent() == null && media.format().is(format)) {
					output.addOptions(Options.streamCodecCopy(index));
					return;
				}
				
				MediaType type = media.type();
				
				if(type.is(MediaType.VIDEO)) {
					ensureVideo(media, index, input, output, false);
					
					Media root = Media.root(media);
					int result = ensureAudio(root, index, input, output, false);
					
					if(result == RESULT_NONE
							// Check whether the root media also has an audio media (e.g. inputFormat=DASH)
							&& !isAudioSeparated(root)) {
						// If an audio media is not present separately, force the audio from the video
						ensureAudio(root, index, input, output, true);
					}
				} else if(type.is(MediaType.AUDIO)) {
					int result = ensureAudio(media, index, input, output, false);
					
					if(result == RESULT_COPY) {
						output.removeOptions(Options.streamAudioCodecCopy(index));
						output.addOptions(Options.streamCodecCopy(index));
					}
				} else {
					throw new IllegalStateException("Type not supported: " + type);
				}
			}
			
			private static abstract class MP4Compatible extends VideoConversionFormat {
				
				protected static final String DEFAULT_VIDEO_PRESET = "fast";
				protected static final String DEFAULT_VIDEO_CRF = "20";
				
				protected MP4Compatible(MediaFormat format) {
					super(format);
				}
				
				@Override
				protected int ensureVideo(Media media, int index, Input.Builder input, Output.Builder output,
						boolean force) {
					VideoMedia video = Media.findOfType(media, MediaType.VIDEO);
					
					if(video == null && !force) {
						return RESULT_NONE;
					}
					
					if(video != null && video.format().is(MediaFormat.MP4)) {
						output.addOptions(Options.streamVideoCodecCopy(index));
						return RESULT_COPY;
					}
					
					Metadata inputMetadata = input.metadata();
					
					output.addOptions(
						Options.videoCodec().ofValue("libx264"),
						Option.ofShort("preset", value(inputMetadata, "video.preset", DEFAULT_VIDEO_PRESET)),
						Option.ofShort("crf", value(inputMetadata, "video.crf", DEFAULT_VIDEO_CRF))
					);
					
					return RESULT_REENCODE;
				}
				
				@Override
				protected int ensureAudio(Media media, int index, Input.Builder input, Output.Builder output,
						boolean force) {
					if(!force) {
						AudioMedia audio = Media.findOfType(media, MediaType.AUDIO);
						
						if(audio == null) {
							return RESULT_NONE;
						}
						
						if(audio.format().isAnyOf(MediaFormat.M4A, MediaFormat.AAC)) {
							output.addOptions(Options.streamAudioCodecCopy(index));
							return RESULT_COPY;
						}
					} else {
						Media root = Media.root(media);
						
						if(root.format().is(MediaFormat.M3U8)) {
							output.addOptions(
								Options.streamAudioCodecCopy(index),
								Option.ofShort("bsf:a", "aac_adtstoasc")
							);
							
							return RESULT_COPY;
						}
						
						if(root.format().isAnyOf(MediaFormat.MP4, MediaFormat.M4A, MediaFormat.AAC)) {
							output.addOptions(Options.streamAudioCodecCopy(index));
							return RESULT_COPY;
						}
					}
					
					Metadata inputMetadata = input.metadata();
					
					output.addOptions(
						Options.audioCodec().ofValue("aac"),
						Options.audioChannels().ofValue(audioChannels(inputMetadata)),
						Options.audioSampleRate().ofValue(audioSampleRate(inputMetadata)),
						Options.audioBitRate().ofValue(audioBitRate(inputMetadata))
					);
					
					return RESULT_REENCODE;
				}
			}
			
			public static final class MP4 extends MP4Compatible {
				
				public MP4() {
					super(MediaFormat.MP4);
				}
			}
			
			public static final class FLV extends MP4Compatible {
				
				public FLV() {
					super(MediaFormat.FLV);
				}
			}
			
			public static final class AVI extends MP4Compatible {
				
				public AVI() {
					super(MediaFormat.AVI);
				}
			}
			
			public static final class MKV extends VideoConversionFormat {
				
				public MKV() {
					super(MediaFormat.MKV);
				}
				
				@Override
				protected final int ensureVideo(Media media, int index, Input.Builder input, Output.Builder output,
						boolean force) {
					output.addOptions(Options.streamVideoCodecCopy(index));
					return RESULT_COPY;
				}
				
				@Override
				protected final int ensureAudio(Media media, int index, Input.Builder input, Output.Builder output,
						boolean force) {
					output.addOptions(Options.streamAudioCodecCopy(index));
					return RESULT_COPY;
				}
			}
			
			public static final class WMV extends MP4Compatible {
				
				public WMV() {
					super(MediaFormat.WMV);
				}
			}
			
			public static final class WEBMV extends VideoConversionFormat {
				
				private static final String DEFAULT_VIDEO_CRF = "31";
				private static final String DEFAULT_VIDEO_CPU_USED = "5";
				
				public WEBMV() {
					super(MediaFormat.WEBMV);
				}
				
				@Override
				protected final int ensureVideo(Media media, int index, Input.Builder input, Output.Builder output,
						boolean force) {
					VideoMedia video = Media.findOfType(media, MediaType.VIDEO);
					
					if(video == null && !force) {
						return RESULT_NONE;
					}
					
					if(video != null && video.format().is(MediaFormat.WEBMV)) {
						output.addOptions(Options.streamVideoCodecCopy(index));
						return RESULT_COPY;
					}
					
					Metadata inputMetadata = input.metadata();
					
					output.addOptions(
						Options.videoCodec().ofValue("libvpx-vp9"),
						Option.ofShort("b:v", "0"), // Must be equal to 0 to allow setting of CRF
						Option.ofShort("crf", value(inputMetadata, "video.crf", DEFAULT_VIDEO_CRF)),
						Option.ofShort("row-mt", "1"), // Enable Row based multithreading
						Option.ofShort("deadline", "realtime"),
						Option.ofShort("cpu-used", value(inputMetadata, "video.cpu_used", DEFAULT_VIDEO_CPU_USED))
					);
					
					return RESULT_REENCODE;
				}
				
				@Override
				protected final int ensureAudio(Media media, int index, Input.Builder input, Output.Builder output,
						boolean force) {
					if(!force) {
						AudioMedia audio = Media.findOfType(media, MediaType.AUDIO);
						
						if(audio == null) {
							return RESULT_NONE;
						}
						
						if(audio.format().isAnyOf(MediaFormat.WEBMA, MediaFormat.OGGA)) {
							output.addOptions(Options.streamAudioCodecCopy(index));
							return RESULT_COPY;
						}
					} else {
						Media root = Media.root(media);
						
						if(root.format().isAnyOf(MediaFormat.WEBM, MediaFormat.WEBMA, MediaFormat.OGG,
								MediaFormat.OGGA)) {
							output.addOptions(Options.streamAudioCodecCopy(index));
							return RESULT_COPY;
						}
					}
					
					Metadata inputMetadata = input.metadata();
					
					output.addOptions(
						Options.audioCodec().ofValue("libopus"),
						Options.audioChannels().ofValue(audioChannels(inputMetadata)),
						Options.audioSampleRate().ofValue(audioSampleRate(inputMetadata)),
						Options.audioBitRate().ofValue(audioBitRate(inputMetadata))
					);
					
					return RESULT_REENCODE;
				}
			}
			
			public static final class OGGV extends VideoConversionFormat {
				
				private static final String DEFAULT_VIDEO_QUALITY = "7";
				
				public OGGV() {
					super(MediaFormat.OGGV);
				}
				
				@Override
				protected final int ensureVideo(Media media, int index, Input.Builder input, Output.Builder output,
						boolean force) {
					VideoMedia video = Media.findOfType(media, MediaType.VIDEO);
					
					if(video == null && !force) {
						return RESULT_NONE;
					}
					
					if(video != null && video.format().is(MediaFormat.OGGV)) {
						output.addOptions(Options.streamVideoCodecCopy(index));
						return RESULT_COPY;
					}
					
					Metadata inputMetadata = input.metadata();
					
					output.addOptions(
						Options.videoCodec().ofValue("libtheora"),
						Options.videoQuality().ofValue(value(inputMetadata, "video.quality", DEFAULT_VIDEO_QUALITY))
					);
					
					return RESULT_REENCODE;
				}
				
				@Override
				protected final int ensureAudio(Media media, int index, Input.Builder input, Output.Builder output,
						boolean force) {
					if(!force) {
						AudioMedia audio = Media.findOfType(media, MediaType.AUDIO);
						
						if(audio == null) {
							return RESULT_NONE;
						}
						
						if(audio.format().is(MediaFormat.OGGA)) {
							output.addOptions(Options.streamAudioCodecCopy(index));
							return RESULT_COPY;
						}
					} else {
						Media root = Media.root(media);
						
						if(root.format().isAnyOf(MediaFormat.WEBM, MediaFormat.WEBMA, MediaFormat.OGG,
								MediaFormat.OGGA)) {
							output.addOptions(Options.streamAudioCodecCopy(index));
							return RESULT_COPY;
						}
					}
					
					Metadata inputMetadata = input.metadata();
					
					output.addOptions(
						Options.audioCodec().ofValue("libopus"),
						Options.audioChannels().ofValue(audioChannels(inputMetadata)),
						Options.audioSampleRate().ofValue(audioSampleRate(inputMetadata)),
						Options.audioBitRate().ofValue(audioBitRate(inputMetadata))
					);
					
					return RESULT_REENCODE;
				}
			}
			
			/** @since 00.02.09 */
			public static final class TS extends MP4Compatible {
				
				protected TS() {
					super(MediaFormat.TS);
				}
				
				@Override
				public boolean isConversionNeeded(List<ConversionMedia> inputs, ResolvedMedia output) {
					// If the source is HLS (M3U8), just rename the input file
					return !Media.root(inputs.get(0).media()).format().is(MediaFormat.M3U8);
				}
			}
		}
		
		private static final class AudioConversionFormat extends ConversionFormat {
			
			private final String codec;
			
			public AudioConversionFormat(MediaFormat format, String codec) {
				super(format);
				this.codec = Objects.requireNonNull(codec);
			}
			
			protected int ensureAudio(Media media, int index, Input.Builder input, Output.Builder output,
					boolean force) {
				if(!force) {
					AudioMedia audio = Media.findOfType(media, MediaType.AUDIO);
					
					if(audio == null) {
						return RESULT_NONE;
					}
					
					if(audio.format().is(format)) {
						output.addOptions(Options.streamAudioCodecCopy(index));
						return RESULT_COPY;
					}
				} else {
					Media root = Media.root(media);
					
					if(root.format().is(format)) {
						output.addOptions(Options.streamAudioCodecCopy(index));
						return RESULT_COPY;
					}
				}
				
				Metadata inputMetadata = input.metadata();
				
				output.addOptions(
					Options.audioCodec().ofValue(codec),
					Options.audioBitRate().ofValue(audioBitRate(inputMetadata))
				);
				
				return RESULT_REENCODE;
			}
			
			@Override
			public void from(Media media, int index, Input.Builder input, Output.Builder output) {
				if(media.parent() == null && media.format().is(format)) {
					output.addOptions(
						Options.streamCodecCopy(index),
						Options.noVideo()
					);
					
					return;
				}
				
				MediaType type = media.type();
				
				if(type.is(MediaType.VIDEO)) {
					Media root = Media.root(media);
					int result = ensureAudio(root, index, input, output, false);
					
					if(result == RESULT_NONE
							// Check whether the root media also has an audio media (e.g. inputFormat=DASH)
							&& !isAudioSeparated(root)) {
						// If an audio media is not present separately, force the audio from the video
						ensureAudio(root, index, input, output, true);
					}
				} else if(type.is(MediaType.AUDIO)) {
					int result = ensureAudio(media, index, input, output, false);
					
					if(result == RESULT_COPY) {
						output.removeOptions(Options.streamAudioCodecCopy(index));
						output.addOptions(Options.streamCodecCopy(index));
					}
				} else {
					throw new IllegalStateException("Type not supported: " + type);
				}
				
				output.addOptions(Options.noVideo());
			}
		}
	}
	
	/** @since 00.02.09 */
	public static final class Provider extends AbstractConversionProvider {
		
		public static final String NAME = "ffmpeg";
		
		// Allow instantiation outside of this class
		Provider() {
			registerDefaultFormats();
		}
		
		private final void registerDefaultFormats() {
			// Video formats
			register(new Formats.VideoConversionFormat.MP4());
			register(new Formats.VideoConversionFormat.FLV());
			register(new Formats.VideoConversionFormat.AVI());
			register(new Formats.VideoConversionFormat.MKV());
			register(new Formats.VideoConversionFormat.WMV());
			register(new Formats.VideoConversionFormat.WEBMV());
			register(new Formats.VideoConversionFormat.OGGV());
			register(new Formats.VideoConversionFormat.TS());
			// Audio formats
			register(new Formats.AudioConversionFormat(MediaFormat.MP3, "mp3"));
			register(new Formats.AudioConversionFormat(MediaFormat.WAV, "pcm_s16le"));
			register(new Formats.AudioConversionFormat(MediaFormat.WMA, "wmav2"));
		}
		
		@Override
		public Converter createConverter(TrackerManager trackerManager) {
			return new FFmpegConverter(trackerManager);
		}
		
		@Override
		public ConversionCommand createCommand(List<ConversionMedia> inputs, ResolvedMedia output) {
			if(output == null) {
				throw new IllegalArgumentException("Output cannot be null.");
			}
			
			if(inputs == null || inputs.isEmpty()) {
				throw new IllegalArgumentException("Inputs cannot be neither null nor empty.");
			}
			
			Command.Builder command = Command.builder();
			MediaFormat formatInput = Media.root(inputs.get(0).media()).format();
			MediaFormat formatOutput = output.configuration().outputFormat();
			Output.Builder out = Output.ofMutable(output.path());
			
			command.addOptions(
				Options.yes(),
				Options.hideBanner(),
				Options.logWarning(),
				Options.stats()
			);
			
			Metadata metadataInput = Metadata.of("noExplicitFormat", true).seal();
			
			List<Pair<Media, Input.Builder>> mutableInputs = inputs.stream()
				.map((i) -> new Pair<>(i.media(), Input.ofMutable(i.path(), List.of(), metadataInput)))
				.collect(Collectors.toList());
			
			ConversionFormat format = formatOf(formatOutput);
			
			if(format == null) {
				throw new IllegalStateException(String.format(
					"Unable to create FFmpeg command: input=%s, output=%s",
					formatInput, formatOutput
				));
			}
			
			if(!format.isConversionNeeded(inputs, output)) {
				return ConversionCommand.Constants.RENAME;
			}
			
			for(int i = 0, l = mutableInputs.size(); i < l; ++i) {
				Pair<Media, Input.Builder> pair = mutableInputs.get(i);
				format.from(pair.a, i, pair.b, out);
			}
			
			for(Pair<Media, Input.Builder> pair : mutableInputs) {
				command.addInputs(pair.b.asFormat(pair.a.format()));
			}
			
			CommandOptimizer.optimizeOutput(out, mutableInputs.size());
			command.addOutputs(out.asFormat(formatOutput));
			
			return command.build();
		}
		
		@Override
		public String name() {
			return NAME;
		}
		
		/** @since 00.02.09 */
		private static final class CommandOptimizer {
			
			private static final Regex REGEX_CODEC_COPY = Regex.of("^c:(?:([va]):)?(\\d+)$");
			private static final int VALUE_TYPE_VIDEO = 0b1 << 0;
			private static final int VALUE_TYPE_AUDIO = 0b1 << 1;
			private static final int VALUE_TYPE_ALL = VALUE_TYPE_VIDEO | VALUE_TYPE_AUDIO;
			
			// Forbid anyone to create an instance of this class
			private CommandOptimizer() {
			}
			
			// Optimizes the output options in such a way that when the codec of both video and
			// audio should be copied it replaces these two options by a single one. This will
			// actually speed up the whole conversion process, since no stream selection will take
			// place.
			public static final void optimizeOutput(Output.Builder output, int numOfInputs) {
				int[] codecCopy = new int[numOfInputs];
				
				for(Option option : output.options()) {
					Matcher matcher = REGEX_CODEC_COPY.matcher(option.name());
					
					if(!matcher.matches()) {
						continue;
					}
					
					int index = Integer.valueOf(matcher.group(2));
					int value = 0;
					
					if(matcher.group(1) == null) {
						value = VALUE_TYPE_ALL;
					} else {
						switch(matcher.group(1)) {
							case "v": value = VALUE_TYPE_VIDEO; break;
							case "a": value = VALUE_TYPE_AUDIO; break;
						}
					}
					
					codecCopy[index] |= value;
				}
				
				boolean allCopy = true;
				for(int i = 0; i < numOfInputs; ++i) {
					if(codecCopy[i] != VALUE_TYPE_ALL) {
						allCopy = false;
						break;
					}
				}
				
				if(allCopy) {
					for(int i = 0; i < numOfInputs; ++i) {
						output.removeOptions(
							Options.streamVideoCodecCopy(i),
							Options.streamAudioCodecCopy(i),
							Options.streamCodecCopy(i)
						);
					}
					
					output.addOptions(Options.codecCopy());
					return;
				}
				
				for(int i = 0; i < numOfInputs; ++i) {
					if(codecCopy[i] != VALUE_TYPE_ALL) {
						continue;
					}
					
					output.removeOptions(
						Options.streamVideoCodecCopy(i),
						Options.streamAudioCodecCopy(i)
					);
					
					output.addOptions(Options.streamCodecCopy(i));
				}
			}
		}
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
		
		private final String construct() {
			return (new Constructor(this)).string();
		}
		
		@Override
		public String string() {
			return string == null ? (string = construct()) : string;
		}
		
		private static final class Constructor {
			
			private static final Regex REGEX_NEEDS_QUOTES = Regex.of("[\\s\"']");
			/** @since 00.02.09 */
			private static final Regex REGEX_ESCAPE = Regex.of("\"");
			
			private final Command command;
			
			private Constructor(Command command) {
				this.command = command;
			}
			
			private final boolean needsQuotes(String value) {
				return REGEX_NEEDS_QUOTES.matcher(value).find();
			}
			
			private final String escape(String value) {
				return REGEX_ESCAPE.replaceAll(value, "\\\"");
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
		private static Option NO_VIDEO;
		private static Option NO_AUDIO;
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
		
		public static final Option noVideo() {
			return NO_VIDEO == null
						? NO_VIDEO = Option.ofShort("vn")
						: NO_VIDEO;
		}
		
		public static final Option noAudio() {
			return NO_AUDIO == null
						? NO_AUDIO = Option.ofShort("an")
						: NO_AUDIO;
		}
		
		public static final Option streamCodecCopy(int index) {
			return Option.ofMutableShort("c:" + index).ofValue("copy");
		}
		
		public static final Option streamVideoCodecCopy(int index) {
			return Option.ofMutableShort("c:v:" + index).ofValue("copy");
		}
		
		public static final Option streamAudioCodecCopy(int index) {
			return Option.ofMutableShort("c:a:" + index).ofValue("copy");
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
						? AUDIO_BIT_RATE = Option.ofMutableShort("b:a")
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