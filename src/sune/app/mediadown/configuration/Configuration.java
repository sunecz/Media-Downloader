package sune.app.mediadown.configuration;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import sune.app.mediadown.util.MutablePair;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Reflection;
import sune.app.mediadown.util.Reflection2;
import sune.app.mediadown.util.Reflection2.InstanceCreationException;
import sune.app.mediadown.util.Utils;
import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDNode;
import sune.util.ssdf2.SSDObject;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

public class Configuration implements ConfigurationAccessor {
	
	protected final String name;
	protected final SSDCollection data;
	protected final Map<String, ConfigurationProperty<?>> properties;
	
	/** @since 00.02.05 */
	protected Writer writer;
	
	protected Configuration(String name, SSDCollection data, Map<String, ConfigurationProperty<?>> properties) {
		this.name = Objects.requireNonNull(name);
		this.data = Objects.requireNonNull(data);
		this.properties = Objects.requireNonNull(properties);
	}
	
	private final <T> ConfigurationProperty<T> getPropertyAndCheck(String name, ConfigurationPropertyType type) {
		ConfigurationProperty<?> property = properties.get(name);
		if(property != null && property.type() != type) property = null; // Null, if not the required type
		@SuppressWarnings("unchecked")
		ConfigurationProperty<T> local = (ConfigurationProperty<T>) property;
		return local;
	}
	
	private final <T> T getPropertyValue(ConfigurationProperty<T> property, T defaultValue) {
		return property != null ? property.value() : defaultValue;
	}
	
	// Utility method. This method is used primarly for creating the root SSDCollection (name is null, not empty).
	private static final SSDCollection newRootCollection(boolean isArray) {
		try {
			return Reflection2.newInstance(SSDCollection.class, new Class<?>[] { SSDNode.class, boolean.class }, null, isArray);
		} catch(InstanceCreationException ex) {
			// Convert to an unchecked exception
			throw new AssertionError(ex);
		}
	}
	
	// Utility method
	private static final <T> T resolveNull(SSDObject object, Function<SSDValue, T> transformer, T defaultValue) {
		return object.getType() == SSDType.NULL ? defaultValue : transformer.apply(object.getFormattedValue());
	}
	
	/** @since 00.02.04 */
	public static final Builder of(String name) {
		return new Builder(name);
	}
	
	/** @since 00.02.04 */
	public final <T> ConfigurationProperty<T> property(String name) {
		@SuppressWarnings("unchecked")
		ConfigurationProperty<T> local = (ConfigurationProperty<T>) properties.get(name);
		return local;
	}
	
	/** @since 00.02.04 */
	@Override
	public final boolean booleanValue(String name) {
		return getPropertyValue(getPropertyAndCheck(name, ConfigurationPropertyType.BOOLEAN), false);
	}
	
	/** @since 00.02.04 */
	@Override
	public final long longValue(String name) {
		return getPropertyValue(getPropertyAndCheck(name, ConfigurationPropertyType.INTEGER), 0L);
	}
	
	/** @since 00.02.04 */
	@Override
	public final int intValue(String name) {
		return (int) longValue(name);
	}
	
	/** @since 00.02.04 */
	@Override
	public final short shortValue(String name) {
		return (short) longValue(name);
	}
	
	/** @since 00.02.04 */
	@Override
	public final char charValue(String name) {
		return (char) longValue(name);
	}
	
	/** @since 00.02.04 */
	@Override
	public final byte byteValue(String name) {
		return (byte) longValue(name);
	}
	
	/** @since 00.02.04 */
	@Override
	public final double doubleValue(String name) {
		return getPropertyValue(getPropertyAndCheck(name, ConfigurationPropertyType.DECIMAL), 0.0);
	}
	
	/** @since 00.02.04 */
	@Override
	public final float floatValue(String name) {
		return (float) doubleValue(name);
	}
	
	/** @since 00.02.04 */
	@Override
	public final String stringValue(String name) {
		return getPropertyValue(getPropertyAndCheck(name, ConfigurationPropertyType.STRING), null);
	}
	
	/** @since 00.02.04 */
	@Override
	public final <T> T typeValue(String name) {
		@SuppressWarnings("unchecked")
		TypeConfigurationPropertyBase<T> property
			= (TypeConfigurationPropertyBase<T>) this.<String>getPropertyAndCheck(name, ConfigurationPropertyType.STRING);
		return property != null ? property.inverseTransformer().apply(property.value()) : null;
	}
	
	/** @since 00.02.04 */
	@Override
	public final Map<String, ConfigurationProperty<?>> arrayValue(String name) {
		return getPropertyValue(getPropertyAndCheck(name, ConfigurationPropertyType.ARRAY), null);
	}
	
	/** @since 00.02.04 */
	@Override
	public final Map<String, ConfigurationProperty<?>> objectValue(String name) {
		return getPropertyValue(getPropertyAndCheck(name, ConfigurationPropertyType.OBJECT), null);
	}
	
	/** @since 00.02.04 */
	@Override
	public final Object nullValue(String name) {
		return getPropertyValue(getPropertyAndCheck(name, ConfigurationPropertyType.NULL), null);
	}
	
	@Override
	public SSDCollection data() {
		return data;
	}
	
	/** @since 00.02.04 */
	public String name() {
		return name;
	}
	
	/** @since 00.02.04 */
	public Map<String, ConfigurationProperty<?>> properties() {
		return Collections.unmodifiableMap(properties);
	}
	
	/** @since 00.02.04 */
	public Map<String, ConfigurationProperty<?>> rootProperties() {
		Map<String, ConfigurationProperty<?>> rootProperties = new LinkedHashMap<>();
		Deque<MutablePair<String, Integer>> stack = new ArrayDeque<>();
		MutablePair<String, Integer> currentObject = null;
		for(Entry<String, ConfigurationProperty<?>> entry : properties.entrySet()) {
			String name = entry.getKey();
			ConfigurationProperty<?> property = entry.getValue();
			if(currentObject == null) {
				rootProperties.put(name, property);
			}
			ConfigurationPropertyType type = property.type();
			if(type == ConfigurationPropertyType.ARRAY
					|| type == ConfigurationPropertyType.OBJECT) {
				int remain = ((ObjectConfigurationPropertyBase) property).value().size();
				if(remain > 0) {
					stack.push(currentObject = new MutablePair<>(name, remain));
				}
			} else if(currentObject != null) {
				int remain = currentObject.b() - 1;
				if(remain <= 0) stack.pop();
				else currentObject.b(remain);
				currentObject = stack.peek();
			}
		}
		return Collections.unmodifiableMap(rootProperties);
	}
	
	/** @since 00.02.04 */
	public boolean isEmpty() {
		return properties.isEmpty();
	}
	
	/** @since 00.02.05 */
	public Writer writer() {
		return writer == null ? (writer = new Writer(data, properties)) : writer;
	}
	
	/** @since 00.02.04 */
	@Override
	public int hashCode() {
		return Objects.hash(name, properties);
	}
	
	/** @since 00.02.04 */
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Configuration other = (Configuration) obj;
		return Objects.equals(name, other.name) && Objects.equals(properties, other.properties);
	}
	
	/** @since 00.02.04 */
	public static enum ConfigurationPropertyType {
		
		// SSDF-storable types, should be sufficient
		BOOLEAN, INTEGER, DECIMAL, STRING, ARRAY, OBJECT, NULL;
	}
	
	/** @since 00.02.04 */
	public static abstract class ConfigurationProperty<V> {
		
		protected final String name;
		protected final ConfigurationPropertyType type;
		protected V value;
		protected final boolean isHidden;
		/** @since 00.02.07 */
		protected final String group;
		
		protected ConfigurationProperty(String name, ConfigurationPropertyType type, V value, boolean isHidden,
				String group) {
			this.name = Objects.requireNonNull(name);
			this.type = Objects.requireNonNull(type);
			this.value = value;
			this.isHidden = isHidden;
			this.group = group;
		}
		
		public static final BooleanConfigurationProperty.Builder ofBoolean(String name) {
			return new BooleanConfigurationProperty.Builder(name);
		}
		
		public static final IntegerConfigurationProperty.Builder ofInteger(String name) {
			return new IntegerConfigurationProperty.Builder(name);
		}
		
		public static final DecimalConfigurationProperty.Builder ofDecimal(String name) {
			return new DecimalConfigurationProperty.Builder(name);
		}
		
		public static final StringConfigurationProperty.Builder ofString(String name) {
			return new StringConfigurationProperty.Builder(name);
		}
		
		public static final <T> TypeConfigurationProperty.Builder<T> ofType(String name, Class<? extends T> typeClass) {
			return new TypeConfigurationProperty.Builder<>(name, typeClass);
		}
		
		public static final ArrayConfigurationProperty.Builder ofArray(String name) {
			return new ArrayConfigurationProperty.Builder(name);
		}
		
		public static final ObjectConfigurationProperty.Builder ofObject(String name) {
			return new ObjectConfigurationProperty.Builder(name);
		}
		
		public static final NullConfigurationProperty.Builder ofNull(String name) {
			return new NullConfigurationProperty.Builder(name);
		}
		
		/** @since 00.02.05 */
		protected final void value(V value) {
			this.value = value;
		}
		
		public String name() {
			return name;
		}
		
		public ConfigurationPropertyType type() {
			return type;
		}
		
		public V value() {
			return value;
		}
		
		public boolean isHidden() {
			return isHidden;
		}
		
		/** @since 00.02.07 */
		public String group() {
			return group;
		}
		
		public abstract SSDNode toNode(Map<String, ConfigurationProperty<?>> builtProperties);
		
		public SSDNode toNode() {
			return toNode(null);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(isHidden, name, type, value);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			ConfigurationProperty<?> other = (ConfigurationProperty<?>) obj;
			return isHidden == other.isHidden
			        && Objects.equals(name, other.name)
			        && type == other.type
			        && Objects.equals(value, other.value);
		}
		
		public static abstract class BuilderBase<V, T extends ConfigurationProperty<V>> {
			
			protected final String name;
			protected final ConfigurationPropertyType type;
			protected boolean isHidden;
			/** @since 00.02.07 */
			protected String group;
			
			public BuilderBase(String name, ConfigurationPropertyType type) {
				this.name = Objects.requireNonNull(name);
				this.type = Objects.requireNonNull(type);
				this.isHidden = false;
				this.group = null;
			}
			
			public BuilderBase<V, T> asHidden(boolean isHidden) {
				this.isHidden = isHidden;
				return this;
			}
			
			/** @since 00.02.07 */
			public BuilderBase<V, T> inGroup(String group) {
				this.group = Objects.requireNonNull(group);
				return this;
			}
			
			public abstract V value();
			public abstract BuilderBase<V, T> loadData(SSDNode node);
			public abstract T build();
			
			public String name() {
				return name;
			}
			
			public ConfigurationPropertyType type() {
				return type;
			}
			
			/** @since 00.02.07 */
			public boolean isHidden() {
				return isHidden;
			}
			
			/** @since 00.02.07 */
			public String group() {
				return group;
			}
		}
		
		public static abstract class Builder<V, T extends ConfigurationProperty<V>> extends BuilderBase<V, T> {
			
			protected V value;
			protected boolean useValue;
			protected V defaultValue;
			
			public Builder(String name, ConfigurationPropertyType type) {
				super(name, type);
				this.value = null;
				this.useValue = false;
				this.defaultValue = null;
			}
			
			@Override
			public V value() {
				return useValue ? value : defaultValue;
			}
			
			protected Builder<V, T> withValue(V value) {
				this.value = value;
				this.useValue = true;
				return this;
			}
			
			protected Builder<V, T> withDefaultValue(V defaultValue) {
				this.defaultValue = defaultValue;
				return this;
			}
		}
		
		public static abstract class ScalarBuilder<V, T extends ConfigurationProperty<V>> extends Builder<V, T> {
			
			public ScalarBuilder(String name, ConfigurationPropertyType type) {
				super(name, type);
			}
			
			@Override
			public ScalarBuilder<V, T> withValue(V value) {
				return (ScalarBuilder<V, T>) super.withValue(value);
			}
			
			@Override
			public ScalarBuilder<V, T> withDefaultValue(V defaultValue) {
				return (ScalarBuilder<V, T>) super.withDefaultValue(defaultValue);
			}
			
			@Override
			public ScalarBuilder<V, T> asHidden(boolean isHidden) {
				return (ScalarBuilder<V, T>) super.asHidden(isHidden);
			}
			
			/** @since 00.02.07 */
			@Override
			public ScalarBuilder<V, T> inGroup(String group) {
				return (ScalarBuilder<V, T>) super.inGroup(group);
			}
		}
	}
	
	/** @since 00.02.04 */
	public static final class BooleanConfigurationProperty extends ConfigurationProperty<Boolean> {
		
		private BooleanConfigurationProperty(String name, Boolean value, boolean isHidden, String group) {
			super(name, ConfigurationPropertyType.BOOLEAN, value != null && value, isHidden, group);
		}
		
		@Override
		public SSDNode toNode(Map<String, ConfigurationProperty<?>> builtProperties) {
			SSDNode node = SSDObject.of(name, value);
			if(builtProperties != null)
				builtProperties.put(name, this);
			return node;
		}
		
		public static final class Builder extends ConfigurationProperty.ScalarBuilder<Boolean, BooleanConfigurationProperty> {
			
			public Builder(String name) {
				super(name, ConfigurationPropertyType.BOOLEAN);
				defaultValue = false;
			}
			
			/** @since 00.02.07 */
			@Override
			public Builder asHidden(boolean isHidden) {
				return (Builder) super.asHidden(isHidden);
			}
			
			/** @since 00.02.07 */
			@Override
			public Builder inGroup(String group) {
				return (Builder) super.inGroup(group);
			}
			
			@Override
			public Builder loadData(SSDNode node) {
				if(!node.isObject()) return this;
				return (Builder) withValue(resolveNull((SSDObject) node, SSDValue::booleanValue, defaultValue));
			}
			
			@Override
			public BooleanConfigurationProperty build() {
				return new BooleanConfigurationProperty(name, value(), isHidden, group);
			}
		}
	}
	
	/** @since 00.02.04 */
	public static final class IntegerConfigurationProperty extends ConfigurationProperty<Long> {
		
		private IntegerConfigurationProperty(String name, Long value, boolean isHidden, String group) {
			super(name, ConfigurationPropertyType.INTEGER, value != null ? value : 0L, isHidden, group);
		}
		
		private IntegerConfigurationProperty(String name, Integer value, boolean isHidden, String group) {
			this(name, value != null ? Long.valueOf(value) : 0L, isHidden, group);
		}
		
		private IntegerConfigurationProperty(String name, Short value, boolean isHidden, String group) {
			this(name, value != null ? Long.valueOf(value) : 0L, isHidden, group);
		}
		
		private IntegerConfigurationProperty(String name, Character value, boolean isHidden, String group) {
			this(name, value != null ? Long.valueOf(value) : 0L, isHidden, group);
		}
		
		private IntegerConfigurationProperty(String name, Byte value, boolean isHidden, String group) {
			this(name, value != null ? Long.valueOf(value) : 0L, isHidden, group);
		}
		
		@Override
		public SSDNode toNode(Map<String, ConfigurationProperty<?>> builtProperties) {
			SSDNode node = SSDObject.of(name, value);
			if(builtProperties != null)
				builtProperties.put(name, this);
			return node;
		}
		
		public static final class Builder extends ConfigurationProperty.ScalarBuilder<Long, IntegerConfigurationProperty> {
			
			public Builder(String name) {
				super(name, ConfigurationPropertyType.INTEGER);
				defaultValue = 0L;
			}
			
			/** @since 00.02.07 */
			@Override
			public Builder asHidden(boolean isHidden) {
				return (Builder) super.asHidden(isHidden);
			}
			
			/** @since 00.02.07 */
			@Override
			public Builder inGroup(String group) {
				return (Builder) super.inGroup(group);
			}
			
			public Builder withDefaultValue(Integer defaultValue) {
				return (Builder) super.withDefaultValue(Long.valueOf(defaultValue));
			}
			
			public Builder withDefaultValue(Short defaultValue) {
				return (Builder) super.withDefaultValue(Long.valueOf(defaultValue));
			}
			
			public Builder withDefaultValue(Character defaultValue) {
				return (Builder) super.withDefaultValue(Long.valueOf(defaultValue));
			}
			
			public Builder withDefaultValue(Byte defaultValue) {
				return (Builder) super.withDefaultValue(Long.valueOf(defaultValue));
			}
			
			public Builder withValue(Integer value) {
				return (Builder) super.withValue(Long.valueOf(value));
			}
			
			public Builder withValue(Short value) {
				return (Builder) super.withValue(Long.valueOf(value));
			}
			
			public Builder withValue(Character value) {
				return (Builder) super.withValue(Long.valueOf(value));
			}
			
			public Builder withValue(Byte value) {
				return (Builder) super.withValue(Long.valueOf(value));
			}
			
			@Override
			public Builder loadData(SSDNode node) {
				if(!node.isObject()) return this;
				return (Builder) withValue(resolveNull((SSDObject) node, SSDValue::longValue, defaultValue));
			}
			
			@Override
			public IntegerConfigurationProperty build() {
				return new IntegerConfigurationProperty(name, value(), isHidden, group);
			}
		}
	}
	
	/** @since 00.02.04 */
	public static final class DecimalConfigurationProperty extends ConfigurationProperty<Double> {
		
		private DecimalConfigurationProperty(String name, Double value, boolean isHidden, String group) {
			super(name, ConfigurationPropertyType.DECIMAL, value != null ? value : 0.0, isHidden, group);
		}
		
		private DecimalConfigurationProperty(String name, Float value, boolean isHidden, String group) {
			this(name, value != null ? Double.valueOf(value) : 0.0, isHidden, group);
		}
		
		@Override
		public SSDNode toNode(Map<String, ConfigurationProperty<?>> builtProperties) {
			SSDNode node = SSDObject.of(name, value);
			if(builtProperties != null)
				builtProperties.put(name, this);
			return node;
		}
		
		public static final class Builder extends ConfigurationProperty.ScalarBuilder<Double, DecimalConfigurationProperty> {
			
			public Builder(String name) {
				super(name, ConfigurationPropertyType.DECIMAL);
				defaultValue = 0.0;
			}
			
			/** @since 00.02.07 */
			@Override
			public Builder asHidden(boolean isHidden) {
				return (Builder) super.asHidden(isHidden);
			}
			
			/** @since 00.02.07 */
			@Override
			public Builder inGroup(String group) {
				return (Builder) super.inGroup(group);
			}
			
			public Builder withDefaultValue(Float defaultValue) {
				return (Builder) super.withDefaultValue(Double.valueOf(defaultValue));
			}
			
			public Builder withValue(Float value) {
				return (Builder) super.withValue(Double.valueOf(value));
			}
			
			@Override
			public Builder loadData(SSDNode node) {
				if(!node.isObject()) return this;
				return (Builder) withValue(resolveNull((SSDObject) node, SSDValue::doubleValue, defaultValue));
			}
			
			@Override
			public DecimalConfigurationProperty build() {
				return new DecimalConfigurationProperty(name, value(), isHidden, group);
			}
		}
	}
	
	/** @since 00.02.04 */
	public static final class StringConfigurationProperty extends ConfigurationProperty<String> {
		
		private StringConfigurationProperty(String name, String value, boolean isHidden, String group) {
			super(name, ConfigurationPropertyType.STRING, value, isHidden, group);
		}
		
		@Override
		public SSDNode toNode(Map<String, ConfigurationProperty<?>> builtProperties) {
			String escapedValue = value != null ? value.replaceAll("\"", "\\\"") : null;
			SSDNode node = SSDObject.of(name, escapedValue);
			if(builtProperties != null)
				builtProperties.put(name, this);
			return node;
		}
		
		public static final class Builder extends ConfigurationProperty.ScalarBuilder<String, StringConfigurationProperty> {
			
			public Builder(String name) {
				super(name, ConfigurationPropertyType.STRING);
				defaultValue = "";
			}
			
			/** @since 00.02.07 */
			@Override
			public Builder asHidden(boolean isHidden) {
				return (Builder) super.asHidden(isHidden);
			}
			
			/** @since 00.02.07 */
			@Override
			public Builder inGroup(String group) {
				return (Builder) super.inGroup(group);
			}
			
			@Override
			public Builder loadData(SSDNode node) {
				if(!node.isObject()) return this;
				return (Builder) withValue(resolveNull((SSDObject) node, SSDValue::stringValue, defaultValue));
			}
			
			@Override
			public StringConfigurationProperty build() {
				return new StringConfigurationProperty(name, value(), isHidden, group);
			}
		}
	}
	
	protected static abstract class TypeConfigurationPropertyBase<T> extends ConfigurationProperty<String> {
		
		protected final Class<? extends T> typeClass;
		protected final Supplier<Collection<String>> factory;
		protected final Function<T, String> transformer;
		protected final Function<String, T> inverseTransformer;
		
		private TypeConfigurationPropertyBase(String name, Class<? extends T> typeClass, String value,
				boolean isHidden, Supplier<Collection<String>> factory, Function<T, String> transformer,
				Function<String, T> inverseTransformer, String group) {
			super(name, ConfigurationPropertyType.STRING, value, isHidden, group);
			this.typeClass = Objects.requireNonNull(typeClass);
			this.factory = factory;
			this.transformer = transformer;
			this.inverseTransformer = inverseTransformer;
		}
		
		public Class<?> typeClass() {
			return typeClass;
		}
		
		public Collection<String> values() {
			return factory != null ? factory.get() : List.of();
		}
		
		public Supplier<Collection<String>> factory() {
			return factory;
		}
		
		public Function<T, String> transformer() {
			return transformer;
		}
		
		public Function<String, T> inverseTransformer() {
			return inverseTransformer;
		}
		
		public static abstract class Builder<T> extends ConfigurationProperty.BuilderBase<String, TypeConfigurationPropertyBase<T>> {
			
			protected final Class<? extends T> typeClass;
			
			protected String value;
			protected boolean useValue;
			protected String defaultValue;
			
			protected Supplier<Collection<String>> factory;
			protected Function<T, String> transformer;
			protected Function<String, T> inverseTransformer;
			/** @since 00.02.07 */
			protected Function<String, String> rawValueTransformer;
			
			public Builder(String name, Class<? extends T> typeClass) {
				super(name, ConfigurationPropertyType.STRING);
				this.typeClass = typeClass;
				this.value = null;
				this.useValue = false;
				this.defaultValue = null;
			}
			
			@Override
			public Builder<T> asHidden(boolean isHidden) {
				return (Builder<T>) super.asHidden(isHidden);
			}
			
			/** @since 00.02.07 */
			public Builder<T> inGroup(String group) {
				return (Builder<T>) super.inGroup(group);
			}
			
			public Builder<T> withTransformer(Function<T, String> transformer, Function<String, T> inverseTransformer) {
				this.transformer = transformer;
				this.inverseTransformer = inverseTransformer;
				return this;
			}
			
			public Builder<T> withFactory(Supplier<Collection<String>> factory) {
				this.factory = factory;
				return this;
			}
			
			/** @since 00.02.07 */
			public Builder<T> withRawValueTransformer(Function<String, String> rawValueTransformer) {
				this.rawValueTransformer = rawValueTransformer;
				return this;
			}
			
			public Builder<T> withValue(String value) {
				this.value = value;
				this.useValue = true;
				return this;
			}
			
			public Builder<T> withDefaultValue(String defaultValue) {
				this.defaultValue = defaultValue;
				return this;
			}
			
			protected Collection<String> values() {
				return factory != null ? factory.get() : List.of();
			}
		}
	}
	
	/** @since 00.02.04 */
	public static final class TypeConfigurationProperty<T> extends TypeConfigurationPropertyBase<T> {
		
		private TypeConfigurationProperty(String name, Class<? extends T> typeClass, String value,
				boolean isHidden, Supplier<Collection<String>> factory, Function<T, String> transformer,
				Function<String, T> inverseTransformer, String group) {
			super(name, typeClass, value, isHidden, factory, transformer, inverseTransformer, group);
		}
		
		@Override
		public SSDNode toNode(Map<String, ConfigurationProperty<?>> builtProperties) {
			String escapedValue = value != null ? value.replaceAll("\"", "\\\"") : null;
			SSDNode node = SSDObject.of(name, escapedValue);
			if(builtProperties != null)
				builtProperties.put(name, this);
			return node;
		}
		
		public static final class Builder<T> extends TypeConfigurationPropertyBase.Builder<T> {
			
			public Builder(String name, Class<? extends T> typeClass) {
				super(name, typeClass);
			}
			
			/** @since 00.02.07 */
			@Override
			public Builder<T> asHidden(boolean isHidden) {
				return (Builder<T>) super.asHidden(isHidden);
			}
			
			/** @since 00.02.07 */
			@Override
			public Builder<T> inGroup(String group) {
				return (Builder<T>) super.inGroup(group);
			}
			
			@Override
			public String value() {
				return useValue ? value : defaultValue;
			}
			
			@Override
			public Builder<T> loadData(SSDNode node) {
				if(!node.isObject()) return this;
				return (Builder<T>) withValue(resolveNull((SSDObject) node, SSDValue::stringValue, defaultValue));
			}
			
			@Override
			public TypeConfigurationPropertyBase<T> build() {
				String transformedValue = value();
				
				if(rawValueTransformer != null) {
					transformedValue = rawValueTransformer.apply(transformedValue);
				}
				
				return factory == null || values().contains(transformedValue)
							? new TypeConfigurationProperty<T>(name, typeClass, transformedValue, isHidden,
									factory, transformer, inverseTransformer, group)
							: new NullTypeConfigurationProperty<>(name, typeClass, isHidden, group);
			}
		}
	}
	
	/** @since 00.02.04 */
	public static final class NullTypeConfigurationProperty<T> extends TypeConfigurationPropertyBase<T> {
		
		private NullTypeConfigurationProperty(String name, Class<? extends T> typeClass, boolean isHidden,
				String group) {
			super(name, typeClass, null, isHidden, null, null, null, group);
		}
		
		@Override
		public SSDNode toNode(Map<String, ConfigurationProperty<?>> builtProperties) {
			SSDNode node = SSDObject.of(name, null);
			if(builtProperties != null)
				builtProperties.put(name, this);
			return node;
		}
	}
	
	protected static abstract class ObjectConfigurationPropertyBase
			extends ConfigurationProperty<Map<String, ConfigurationProperty<?>>> {
		
		private ObjectConfigurationPropertyBase(String name, ConfigurationPropertyType type,
				Map<String, ConfigurationProperty<?>> value, boolean isHidden, String group) {
			super(name, type, value, isHidden, group);
		}
		
		/** @since 00.02.07 */
		protected abstract SSDCollection createCollection();
		/** @since 00.02.07 */
		protected abstract void addNode(SSDCollection collection, ConfigurationProperty<?> property, SSDNode node);
		
		@Override
		public SSDNode toNode(Map<String, ConfigurationProperty<?>> builtProperties) {
			Map<String, ConfigurationProperty<?>> localBuiltProperties
				= builtProperties != null ? new LinkedHashMap<>() : null;
			SSDCollection collection = createCollection();
			if(builtProperties != null)
				builtProperties.put(name, this);
			for(ConfigurationProperty<?> property : value().values()) {
				addNode(collection, property, property.toNode(localBuiltProperties));
			}
			// Add all locally built properties to the given collection of built properties
			// prefixed with the name of the current property.
			if(localBuiltProperties != null) {
				builtProperties.putAll(localBuiltProperties.entrySet().stream()
				                            .map((e) -> Map.entry(name + "." + e.getKey(), e.getValue()))
				                            .collect(Collectors.toMap(Map.Entry::getKey,
				                                                      Map.Entry::getValue)));
			}
			return collection;
		}
		
		protected static abstract class Builder<T extends ConfigurationProperty<Map<String, ConfigurationProperty<?>>>>
				extends ConfigurationProperty.BuilderBase<Map<String, ConfigurationProperty<?>>, T> {
			
			protected Map<String, ConfigurationProperty.BuilderBase<?, ?>> value;
			protected boolean useValue;
			protected Map<String, ConfigurationProperty.BuilderBase<?, ?>> defaultValue;
			
			public Builder(String name, ConfigurationPropertyType type) {
				super(name, type);
				this.value = new LinkedHashMap<>();
				this.useValue = false;
				this.defaultValue = Map.of();
			}
			
			@Override
			public final Builder<T> asHidden(boolean isHidden) {
				return (Builder<T>) super.asHidden(isHidden);
			}
			
			/** @since 00.02.07 */
			public Builder<T> inGroup(String group) {
				return (Builder<T>) super.inGroup(group);
			}
			
			protected Map<String, ConfigurationProperty.BuilderBase<?, ?>> valueOfBuilders() {
				return useValue ? value : defaultValue;
			}
			
			public final Builder<T> addProperty(ConfigurationProperty.BuilderBase<?, ?> property) {
				value.put(property.name(), property);
				return this;
			}
			
			public final Builder<T> removeProperty(String propertyName) {
				value.remove(propertyName);
				return this;
			}
			
			public final Builder<T> removeProperty(ConfigurationProperty.BuilderBase<?, ?> property) {
				value.remove(property.name(), property);
				return this;
			}
			
			public final Builder<T> withProperties(ConfigurationProperty.BuilderBase<?, ?>... properties) {
				return withProperties(List.of(properties));
			}
			
			public final Builder<T> withProperties(List<ConfigurationProperty.BuilderBase<?, ?>> properties) {
				properties.forEach((p) -> value.put(p.name(), p));
				useValue = true;
				return this;
			}
			
			public final Builder<T> withDefaultProperties(ConfigurationProperty.BuilderBase<?, ?>... properties) {
				return withDefaultProperties(List.of(properties));
			}
			
			public final Builder<T> withDefaultProperties(List<ConfigurationProperty.BuilderBase<?, ?>> properties) {
				defaultValue = properties.stream()
						.collect(Collectors.toMap(ConfigurationProperty.BuilderBase::name,
												  Function.identity(),
												  (a, b) -> a, LinkedHashMap::new));
				return this;
			}
			
			/** @since 00.02.07 */
			@Override
			public Map<String, ConfigurationProperty<?>> value() {
				Map<String, ConfigurationProperty<?>> builtProperites = new LinkedHashMap<>();
				for(ConfigurationProperty.BuilderBase<?, ?> builder : valueOfBuilders().values()) {
					ConfigurationProperty<?> property = builder.build();
					builtProperites.put(property.name(), property);
				}
				return builtProperites;
			}
			
			/** @since 00.02.07 */
			@Override
			public Builder<T> loadData(SSDNode node) {
				if(!node.isCollection()) return this;
				Map<String, ConfigurationProperty.BuilderBase<?, ?>> properties = valueOfBuilders();
				ConfigurationProperty.BuilderBase<?, ?> builder;
				for(SSDNode subNode : ((SSDCollection) node).nodes()) {
					if((builder = properties.get(subNode.getName())) != null) {
						builder.loadData(subNode);
					}
				}
				return this;
			}
		}
	}
	
	/** @since 00.02.04 */
	public static final class ArrayConfigurationProperty extends ObjectConfigurationPropertyBase {
		
		private ArrayConfigurationProperty(String name, Map<String, ConfigurationProperty<?>> value,
				boolean isHidden, String group) {
			super(name, ConfigurationPropertyType.ARRAY, value, isHidden, group);
		}
		
		/** @since 00.02.07 */
		@Override
		protected SSDCollection createCollection() {
			return SSDCollection.emptyArray();
		}
		
		/** @since 00.02.07 */
		@Override
		protected void addNode(SSDCollection collection, ConfigurationProperty<?> property, SSDNode node) {
			// Unfortunatelly, there is no add(SSDNode) method
			if(node.isObject()) collection.add((SSDObject)     node);
			else                collection.add((SSDCollection) node);
		}
		
		public static final class Builder extends ObjectConfigurationPropertyBase.Builder<ArrayConfigurationProperty> {
			
			public Builder(String name) {
				super(name, ConfigurationPropertyType.ARRAY);
			}
			
			@Override
			public ArrayConfigurationProperty build() {
				return new ArrayConfigurationProperty(name, value(), isHidden, group);
			}
		}
	}
	
	/** @since 00.02.04 */
	public static final class ObjectConfigurationProperty extends ObjectConfigurationPropertyBase {
		
		private ObjectConfigurationProperty(String name, Map<String, ConfigurationProperty<?>> value,
				boolean isHidden, String group) {
			super(name, ConfigurationPropertyType.OBJECT, value, isHidden, group);
		}
		
		/** @since 00.02.07 */
		@Override
		protected SSDCollection createCollection() {
			return SSDCollection.empty();
		}
		
		/** @since 00.02.07 */
		@Override
		protected void addNode(SSDCollection collection, ConfigurationProperty<?> property, SSDNode node) {
			// Unfortunatelly, there is no set(String, SSDNode) method
			if(node.isObject()) collection.set(property.name(), (SSDObject)     node);
			else                collection.set(property.name(), (SSDCollection) node);
		}
		
		public static final class Builder extends ObjectConfigurationPropertyBase.Builder<ObjectConfigurationProperty> {
			
			public Builder(String name) {
				super(name, ConfigurationPropertyType.OBJECT);
			}
			
			@Override
			public ObjectConfigurationProperty build() {
				return new ObjectConfigurationProperty(name, value(), isHidden, group);
			}
		}
	}
	
	/** @since 00.02.04 */
	public static final class NullConfigurationProperty extends ConfigurationProperty<Object> {
		
		private NullConfigurationProperty(String name, boolean isHidden, String group) {
			super(name, ConfigurationPropertyType.NULL, null, isHidden, group);
		}
		
		@Override
		public SSDNode toNode(Map<String, ConfigurationProperty<?>> builtProperties) {
			SSDNode node = SSDObject.of(name, null);
			if(builtProperties != null)
				builtProperties.put(name, this);
			return node;
		}
		
		public static final class Builder extends ConfigurationProperty.Builder<Object, NullConfigurationProperty> {
			
			public Builder(String name) {
				super(name, ConfigurationPropertyType.NULL);
			}
			
			/** @since 00.02.07 */
			@Override
			public Builder asHidden(boolean isHidden) {
				return (Builder) super.asHidden(isHidden);
			}
			
			/** @since 00.02.07 */
			@Override
			public Builder inGroup(String group) {
				return (Builder) super.inGroup(group);
			}
			
			@Override
			public Builder loadData(SSDNode node) {
				// Do nothing, must be null either way
				return this;
			}
			
			@Override
			public NullConfigurationProperty build() {
				return new NullConfigurationProperty(name, isHidden, group);
			}
		}
	}
	
	/** @since 00.02.04 */
	public static class Builder {
		
		protected final String name;
		protected final Map<String, ConfigurationProperty.BuilderBase<?, ?>> properties = new LinkedHashMap<>();
		protected Accessor accessor;
		
		protected Builder(String name) {
			this.name = Objects.requireNonNull(name);
		}
		
		/** @since 00.02.07 */
		private static final void setIfNonExistent(SSDCollection parent, String name, SSDCollection collection) {
			if(!parent.hasCollection(name)) parent.set(name, collection);
		}
		
		/** @since 00.02.07 */
		private static final void setIfNonExistent(SSDCollection parent, int index, SSDCollection collection) {
			if(!parent.hasCollection(index)) parent.set(index, collection);
		}
		
		/** @since 00.02.07 */
		private static final void ensureDataParents(SSDCollection parent, String fullName) {
			int pos;
			if((pos = fullName.indexOf('.')) <= 0)
				return; // Last name, no need to create parents
			
			String parentName = fullName.substring(0, pos);
			
			int end = pos + 1;
			if((pos = fullName.indexOf('.', end)) <= 0)
				pos = fullName.length();
			
			String nextName = fullName.substring(end, pos);
			boolean isArray = nextName.matches("\\d+");
			SSDCollection collection = isArray ? SSDCollection.emptyArray() : SSDCollection.empty();
			
			switch(parent.getType()) {
				case ARRAY:  setIfNonExistent(parent, Integer.valueOf(parentName), collection); break;
				case OBJECT: setIfNonExistent(parent, parentName,                  collection); break;
			}
			
			ensureDataParents(collection, fullName.substring(end));
		}
		
		public Builder addProperty(ConfigurationProperty.BuilderBase<?, ?> property) {
			Objects.requireNonNull(property);
			properties.put(property.name(), property);
			return this;
		}
		
		public Builder removeProperty(String propertyName) {
			Objects.requireNonNull(propertyName);
			properties.remove(propertyName);
			return this;
		}
		
		public Builder removeProperty(ConfigurationProperty.BuilderBase<?, ?> property) {
			Objects.requireNonNull(property);
			properties.remove(property.name(), property);
			return this;
		}
		
		public Builder loadData(SSDCollection data) {
			// Loop through properties rather than the given data, since some properties
			// can have complex names that would require recursion or more complex access.
			for(Entry<String, ConfigurationProperty.BuilderBase<?, ?>> entry : properties.entrySet()) {
				String name = entry.getKey();
				
				if(data.has(name)) {
					ConfigurationProperty.BuilderBase<?, ?> builder = entry.getValue();
					builder.loadData(data.get(name));
				}
			}
			
			return this;
		}
		
		public SSDCollection data(Map<String, ConfigurationProperty<?>> builtProperties) {
			SSDCollection data = newRootCollection(false);
			for(ConfigurationProperty.BuilderBase<?, ?> property : properties.values()) {
				SSDNode node = property.build().toNode(builtProperties);
				ensureDataParents(data, property.name());
				// Unfortunatelly, there is no set(String, SSDNode) method
				if(node.isObject()) data.set(property.name(), (SSDObject)     node);
				else                data.set(property.name(), (SSDCollection) node);
			}
			return data;
		}
		
		public Configuration build() {
			Map<String, ConfigurationProperty<?>> builtProperties = new LinkedHashMap<>();
			SSDCollection data = data(builtProperties);
			return new Configuration(name, data, builtProperties);
		}
		
		/** @since 00.02.07 */
		public String name() {
			return name;
		}
		
		public Accessor accessor() {
			return accessor == null ? (accessor = new Accessor()) : accessor;
		}
		
		public class Accessor implements ConfigurationAccessor {
			
			private final <V, T extends ConfigurationProperty<V>> ConfigurationProperty.BuilderBase<V, T>
					getPropertyAndCheck(String name, ConfigurationPropertyType type) {
				ConfigurationProperty.BuilderBase<?, ?> property = properties.get(name);
				if(property != null && property.type() != type) property = null; // Null, if not the required type
				@SuppressWarnings("unchecked")
				ConfigurationProperty.BuilderBase<V, T> local
					= (ConfigurationProperty.BuilderBase<V, T>) property;
				return local;
			}
			
			private final <V, T extends ConfigurationProperty<V>> V
					getPropertyValue(ConfigurationProperty.BuilderBase<V, T> property, V defaultValue) {
				return property != null ? property.value() : defaultValue;
			}
			
			@Override
			public final boolean booleanValue(String name) {
				return getPropertyValue(getPropertyAndCheck(name, ConfigurationPropertyType.BOOLEAN), false);
			}
			
			@Override
			public final long longValue(String name) {
				return getPropertyValue(getPropertyAndCheck(name, ConfigurationPropertyType.INTEGER), 0L);
			}

			@Override
			public final int intValue(String name) {
				return (int) longValue(name);
			}

			@Override
			public final short shortValue(String name) {
				return (short) longValue(name);
			}

			@Override
			public final char charValue(String name) {
				return (char) longValue(name);
			}

			@Override
			public final byte byteValue(String name) {
				return (byte) longValue(name);
			}

			@Override
			public final double doubleValue(String name) {
				return getPropertyValue(getPropertyAndCheck(name, ConfigurationPropertyType.DECIMAL), 0.0);
			}

			@Override
			public final float floatValue(String name) {
				return (float) doubleValue(name);
			}

			@Override
			public final String stringValue(String name) {
				return getPropertyValue(getPropertyAndCheck(name, ConfigurationPropertyType.STRING), null);
			}

			@Override
			public final <T> T typeValue(String name) {
				TypeConfigurationPropertyBase.Builder<T> property
					= (TypeConfigurationPropertyBase.Builder<T>) this.<String, TypeConfigurationPropertyBase<T>>getPropertyAndCheck
					(name, ConfigurationPropertyType.STRING);
				return property != null ? property.inverseTransformer.apply(property.value()) : null;
			}

			@Override
			public final Map<String, ConfigurationProperty<?>> arrayValue(String name) {
				return getPropertyValue(getPropertyAndCheck(name, ConfigurationPropertyType.ARRAY), null);
			}

			@Override
			public final Map<String, ConfigurationProperty<?>> objectValue(String name) {
				return getPropertyValue(getPropertyAndCheck(name, ConfigurationPropertyType.OBJECT), null);
			}

			@Override
			public final Object nullValue(String name) {
				return getPropertyValue(getPropertyAndCheck(name, ConfigurationPropertyType.NULL), null);
			}
			
			@Override
			public SSDCollection data() {
				Map<String, ConfigurationProperty<?>> builtProperties = new LinkedHashMap<>();
				SSDCollection data = Builder.this.data(builtProperties);
				return data;
			}
		}
	}
	
	/** @since 00.02.05 */
	public static class Writer {
		
		private static final MethodHandle mh_SSDCollection_set;
		
		static {
			try {
				Method method = SSDCollection.class.getDeclaredMethod("set", String.class, SSDType.class, Object.class);
				Reflection.setAccessible(method, true);
				mh_SSDCollection_set = MethodHandles.lookup().unreflect(method);
			} catch(Exception ex) {
				throw new ExceptionInInitializerError(ex);
			}
		}
		
		protected final SSDCollection refData;
		protected final Map<String, ConfigurationProperty<?>> refProperties;
		
		protected Writer(SSDCollection refData, Map<String, ConfigurationProperty<?>> refProperties) {
			this.refData = Objects.requireNonNull(refData);
			this.refProperties = Objects.requireNonNull(refProperties);
		}
		
		// Unsafe, must check whether property::type == ConfigurationPropertyType::from(V)
		private final <T, V> void setValue(ConfigurationProperty<T> property, V value) {
			property.value(Utils.cast(value));
		}
		
		private final void setData(String name, Object value) {
			try {
				mh_SSDCollection_set.invoke(refData, name, SSDType.recognize(value), value);
			} catch(Throwable ex) {
				throw new AssertionError(ex);
			}
		}
		
		public final Writer set(String name, Object value) {
			ConfigurationProperty<?> existing;
			if((existing = refProperties.get(name)) != null) {
				setValue(existing, value);
				setData(name, value);
			}
			return this;
		}
		
		public final Writer save(Path destination) throws IOException {
			NIO.save(destination, refData.toString());
			return this;
		}
	}
}