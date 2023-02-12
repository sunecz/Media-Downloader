package sune.app.mediadown.conversion;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.util.Metadata;
import sune.app.mediadown.util.Utils;

/**
 * Converter command is made of:
 * <ul>
 *     <li>
 *         list of inputs, where each input is made of:
 *         <ul>
 *             <li>path (absolute),</li>
 *             <li>input format, either explicitly stated or obtained
 *             from the path,</li>
 *             <li>list of additional options (may be empty)</li>
 *         </ul>
 *     </li>
 *     <li>
 *         list of outputs, where each output is made of:
 *         <ul>
 *             <li>path (absolute),</li>
 *             <li>output format, either explicitly stated or obtained
 *             from the path,</li>
 *             <li>list of additional options (may be empty)</li>
 *         </ul>
 *     </li>
 *     <li>list of additional options (may be empty)</li>
 *     <li>additional metadata (may be empty)</li>
 * </ul>
 * 
 * <p>
 * Lists are used for options to preserve order since it may
 * be important in some cases. Duplicate options are then removed
 * automatically. Two options are considered duplicate when
 * their names, values and "longness" are all equal.
 * </p>
 * 
 * @since 00.02.08
 */
public abstract class ConversionCommand {
	
	protected final List<Input> inputs;
	protected final List<Output> outputs;
	protected final List<Option> options;
	protected final Metadata metadata;
	
	protected ConversionCommand(List<Input> inputs, List<Output> outputs, List<Option> options, Metadata metadata) {
		this.inputs = List.copyOf(Objects.requireNonNull(inputs));
		this.outputs = List.copyOf(Objects.requireNonNull(outputs));
		this.options = List.copyOf(Objects.requireNonNull(options));
		this.metadata = Objects.requireNonNull(metadata).seal();
	}
	
	public List<Input> inputs() {
		return inputs;
	}
	
	public List<Output> outputs() {
		return outputs;
	}
	
	public List<Option> options() {
		return options;
	}
	
	public Metadata metadata() {
		return metadata;
	}
	
	public abstract String string();
	
	@Override
	public int hashCode() {
		return Objects.hash(inputs, metadata, options, outputs);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		ConversionCommand other = (ConversionCommand) obj;
		return Objects.equals(inputs, other.inputs)
		        && Objects.equals(metadata, other.metadata)
		        && Objects.equals(options, other.options)
		        && Objects.equals(outputs, other.outputs);
	}
	
	@Override
	public String toString() {
		return string();
	}
	
	private static class IOPoint {
		
		protected final Path path;
		protected final MediaFormat format;
		protected final List<Option> options;
		protected final Metadata metadata;
		
		protected IOPoint(Path path, MediaFormat format, List<Option> options, Metadata metadata) {
			this.path = Objects.requireNonNull(path);
			this.format = Objects.requireNonNull(format);
			this.options = List.copyOf(Objects.requireNonNull(options));
			this.metadata = Objects.requireNonNull(metadata).seal();
		}
		
		public Path path() {
			return path;
		}
		
		public MediaFormat format() {
			return format;
		}
		
		public List<Option> options() {
			return options;
		}
		
		public Metadata metadata() {
			return metadata;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(format, metadata, options, path);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			IOPoint other = (IOPoint) obj;
			return Objects.equals(format, other.format)
			        && Objects.equals(metadata, other.metadata)
			        && Objects.equals(options, other.options)
			        && Objects.equals(path, other.path);
		}
		
		protected static abstract class Builder {
			
			protected final Path path;
			protected Set<Option> options;
			protected Metadata metadata;
			
			protected Builder(Path path) {
				this.path = path;
				this.options = new LinkedHashSet<>();
				this.metadata = Metadata.create();
			}
			
			@SafeVarargs
			protected static final <T> List<T> checkItems(T... items) {
				return List.of(Objects.requireNonNull(Utils.nonNullContent(items)));
			}
			
			protected static final <T> List<T> checkItems(List<T> items) {
				return Objects.requireNonNull(Utils.nonNullContent(items));
			}
			
			public abstract IOPoint asFormat(MediaFormat format);
			
			public Builder addOptions(Option... options) {
				this.options.addAll(checkItems(options));
				return this;
			}
			
			public Builder addOptions(List<Option> options) {
				this.options.addAll(checkItems(options));
				return this;
			}
			
			public Builder removeOptions(Option... options) {
				this.options.removeAll(checkItems(options));
				return this;
			}
			
			public Builder removeOptions(List<Option> options) {
				this.options.removeAll(checkItems(options));
				return this;
			}
			
			public Builder addMetadata(Metadata metadata) {
				this.metadata.setAll(metadata);
				return this;
			}
			
			public Builder removeMetadata(Metadata metadata) {
				this.metadata.removeAll(metadata);
				return this;
			}
			
			public Path path() {
				return path;
			}
			
			public List<Option> options() {
				return List.copyOf(options);
			}
			
			public Metadata metadata() {
				return metadata.seal();
			}
		}
	}
	
	public static final class Input extends IOPoint {
		
		private Input(Path path, MediaFormat format, List<Option> options, Metadata metadata) {
			super(path, checkFormat(format), options, metadata);
		}
		
		private static final MediaFormat checkFormat(MediaFormat format) {
			if(!format.isInputFormat()) {
				throw new IllegalArgumentException("Not an input format: " + format);
			}
			
			return format;
		}
		
		public static final Input of(Path path, MediaFormat format) {
			return of(path, format, List.of(), Metadata.create());
		}
		
		public static final Input of(Path path, MediaFormat format, List<Option> options) {
			return of(path, format, options, Metadata.create());
		}
		
		public static final Input of(Path path, MediaFormat format, Metadata metadata) {
			return new Input(path, format, List.of(), metadata);
		}
		
		public static final Input of(Path path, MediaFormat format, List<Option> options, Metadata metadata) {
			return new Input(path, format, options, metadata);
		}
		
		public static final Builder ofMutable(Input input) {
			return ofMutable(input.path(), input.options(), input.metadata());
		}
		
		public static final Builder ofMutable(Path path) {
			return new Builder(path);
		}
		
		public static final Builder ofMutable(Path path, List<Option> options) {
			return (Builder) (new Builder(path)).addOptions(options);
		}
		
		public static final Builder ofMutable(Path path, Metadata metadata) {
			return (Builder) (new Builder(path)).addMetadata(metadata);
		}
		
		public static final Builder ofMutable(Path path, List<Option> options, Metadata metadata) {
			return (Builder) (new Builder(path)).addOptions(options).addMetadata(metadata);
		}
		
		@Override
		public int hashCode() {
			return super.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(!super.equals(obj))
				return false;
			if(getClass() != obj.getClass())
				return false;
			return true;
		}
		
		public static final class Builder extends IOPoint.Builder {
			
			private Builder(Path path) {
				super(path);
			}
			
			@Override
			public Input asFormat(MediaFormat format) {
				return new Input(path, checkFormat(format), List.copyOf(options), metadata);
			}
		}
	}
	
	public static final class Output extends IOPoint {
		
		private Output(Path path, MediaFormat format, List<Option> options, Metadata metadata) {
			super(path, checkFormat(format), options, metadata);
		}
		
		private static final MediaFormat checkFormat(MediaFormat format) {
			if(!format.isOutputFormat()) {
				throw new IllegalArgumentException("Not an output format: " + format);
			}
			
			return format;
		}
		
		public static final Output of(Path path, MediaFormat format) {
			return of(path, format, List.of(), Metadata.create());
		}
		
		public static final Output of(Path path, MediaFormat format, List<Option> options) {
			return of(path, format, options, Metadata.create());
		}
		
		public static final Output of(Path path, MediaFormat format, Metadata metadata) {
			return new Output(path, format, List.of(), metadata);
		}
		
		public static final Output of(Path path, MediaFormat format, List<Option> options, Metadata metadata) {
			return new Output(path, format, options, metadata);
		}
		
		public static final Builder ofMutable(Output output) {
			return ofMutable(output.path(), output.options(), output.metadata());
		}
		
		public static final Builder ofMutable(Path path) {
			return new Builder(path);
		}
		
		public static final Builder ofMutable(Path path, List<Option> options) {
			return (Builder) (new Builder(path)).addOptions(options);
		}
		
		public static final Builder ofMutable(Path path, Metadata metadata) {
			return (Builder) (new Builder(path)).addMetadata(metadata);
		}
		
		public static final Builder ofMutable(Path path, List<Option> options, Metadata metadata) {
			return (Builder) (new Builder(path)).addOptions(options).addMetadata(metadata);
		}
		
		@Override
		public int hashCode() {
			return super.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(!super.equals(obj))
				return false;
			if(getClass() != obj.getClass())
				return false;
			return true;
		}
		
		public static final class Builder extends IOPoint.Builder {
			
			private Builder(Path path) {
				super(path);
			}
			
			@Override
			public Output asFormat(MediaFormat format) {
				return new Output(path, checkFormat(format), List.copyOf(options), metadata);
			}
		}
	}
	
	public static final class Option {
		
		private static final String NO_VALUE = null;
		
		private final boolean isLong;
		private final String name;
		private final String value;
		
		private Option(boolean isLong, String name, String value) {
			this.isLong = isLong;
			this.name = checkName(name);
			this.value = value;
		}
		
		private static final String checkName(String name) {
			if(name == null || name.isBlank()) {
				throw new IllegalArgumentException("Name is null or blank");
			}
			
			return name;
		}
		
		public static final Option ofShort(String name) {
			return new Option(false, name, NO_VALUE);
		}
		
		public static final Option ofShort(String name, String value) {
			return new Option(false, name, Objects.requireNonNull(value));
		}
		
		public static final Option ofLong(String name) {
			return new Option(true, name, NO_VALUE);
		}
		
		public static final Option ofLong(String name, String value) {
			return new Option(true, name, Objects.requireNonNull(value));
		}
		
		public static final Builder ofMutableShort(String name) {
			return new Builder(name, false);
		}
		
		public static final Builder ofMutableLong(String name) {
			return new Builder(name, true);
		}
		
		public boolean isShort() {
			return !isLong;
		}
		
		public boolean isLong() {
			return isLong;
		}
		
		public boolean hasValue() {
			return value != NO_VALUE;
		}
		
		public String name() {
			return name;
		}
		
		public String value() {
			return value;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(isLong, name, value);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			Option other = (Option) obj;
			return isLong == other.isLong && Objects.equals(name, other.name) && Objects.equals(value, other.value);
		}
		
		public static final class Builder {
			
			private final String name;
			private final boolean isLong;
			
			private Builder(String name, boolean isLong) {
				this.name = checkName(name);
				this.isLong = isLong;
			}
			
			public Option noValue() {
				return new Option(isLong, name, NO_VALUE);
			}
			
			public Option ofValue(String value) {
				return new Option(isLong, name, value);
			}
			
			public Builder copy() {
				return new Builder(name, isLong);
			}
			
			public String name() {
				return name;
			}
			
			public boolean isLong() {
				return isLong;
			}
		}
	}
	
	public static abstract class Builder {
		
		protected final Set<Input> inputs;
		protected final Set<Output> outputs;
		protected final Set<Option> options;
		protected final Metadata metadata;
		
		protected Builder() {
			inputs = new LinkedHashSet<>();
			outputs = new LinkedHashSet<>();
			options = new LinkedHashSet<>();
			metadata = Metadata.create();
		}
		
		@SafeVarargs
		protected static final <T> List<T> checkItems(T... items) {
			return List.of(Objects.requireNonNull(Utils.nonNullContent(items)));
		}
		
		protected static final <T> List<T> checkItems(List<T> items) {
			return Objects.requireNonNull(Utils.nonNullContent(items));
		}
		
		public abstract ConversionCommand build();
		
		public Builder addInputs(Input... inputs) {
			this.inputs.addAll(checkItems(inputs));
			return this;
		}
		
		public Builder addInputs(List<Input> inputs) {
			this.inputs.addAll(checkItems(inputs));
			return this;
		}
		
		public Builder addOutputs(Output... outputs) {
			this.outputs.addAll(checkItems(outputs));
			return this;
		}
		
		public Builder addOutputs(List<Output> outputs) {
			this.outputs.addAll(checkItems(outputs));
			return this;
		}
		
		public Builder addOptions(Option... options) {
			this.options.addAll(checkItems(options));
			return this;
		}
		
		public Builder addOptions(List<Option> options) {
			this.options.addAll(checkItems(options));
			return this;
		}
		
		public Builder removeInputs(Input... inputs) {
			this.inputs.removeAll(checkItems(inputs));
			return this;
		}
		
		public Builder removeInputs(List<Input> inputs) {
			this.inputs.removeAll(checkItems(inputs));
			return this;
		}
		
		public Builder removeOutputs(Output... outputs) {
			this.outputs.removeAll(checkItems(outputs));
			return this;
		}
		
		public Builder removeOutputs(List<Output> outputs) {
			this.outputs.removeAll(checkItems(outputs));
			return this;
		}
		
		public Builder removeOptions(Option... options) {
			this.options.removeAll(checkItems(options));
			return this;
		}
		
		public Builder removeOptions(List<Option> options) {
			this.options.removeAll(checkItems(options));
			return this;
		}
		
		public Builder addMetadata(Metadata metadata) {
			this.metadata.setAll(metadata);
			return this;
		}
		
		public Builder removeMetadata(Metadata metadata) {
			this.metadata.removeAll(metadata);
			return this;
		}
		
		public List<Input> inputs() {
			return List.copyOf(inputs);
		}
		
		public List<Output> outputs() {
			return List.copyOf(outputs);
		}
		
		public List<Option> options() {
			return List.copyOf(options);
		}
		
		public Metadata metadata() {
			return metadata.seal();
		}
	}
}