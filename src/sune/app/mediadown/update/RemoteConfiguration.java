package sune.app.mediadown.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Regex;

public final class RemoteConfiguration {
	
	// Characters
	private static final char CHAR_DELIMITER_NAME  = ':';
	private static final char CHAR_DELIMITER_VALUE = ';';
	private static final char CHAR_DELIMITER_PROP  = '=';
	private static final char CHAR_FLAGS_OPEN      = '[';
	private static final char CHAR_FLAGS_CLOSE     = ']';
	private static final char CHAR_FLAGS_DELIMITER = ',';
	
	// Fields
	private final Map<String, ValueHolder> values;
	
	private RemoteConfiguration(Map<String, ValueHolder> values) {
		this.values = values;
	}
	
	private static final String parsePropName(String name, Set<String> flags) {
		int beg = name.indexOf(CHAR_FLAGS_OPEN);
		if(beg <= 0) return name; // No flags present
		int end = name.indexOf(CHAR_FLAGS_CLOSE);
		if(end <= 0) return name; // Invalid name with flags
		Stream.of(name.substring(beg + 1, end)
		              .split(Regex.quote("" + CHAR_FLAGS_DELIMITER)))
		      .forEach(flags::add);
		return name.substring(0, beg);
	}
	
	private static final ValueHolder parseValue(String value) {
		if(value.indexOf(CHAR_DELIMITER_VALUE) <= 0
				&& value.indexOf(CHAR_DELIMITER_PROP) <= 0) {
			return new ValueHolder(value); // Simple value
		}
		Map<String, Property> map = new LinkedHashMap<>();
		final String regexValue = Regex.quote("" + CHAR_DELIMITER_VALUE);
		final String regexProp  = Regex.quote("" + CHAR_DELIMITER_PROP);
		Set<String> flags = new HashSet<>();
		String[] values = value.split(regexValue);
		for(String val : values) {
			String[] pair = val.split(regexProp, 2);
			String propName = parsePropName(pair[0], flags);
			String propValue = pair.length > 1 ? pair[1] : "";
			Set<String> propFlags = Set.of();
			if(!flags.isEmpty()) propFlags = new HashSet<>(flags);
			map.put(propName, new Property(propValue, propFlags));
			flags.clear();
		}
		return new ValueHolder(value, map);
	}
	
	private static final Pair<String, ValueHolder> parseLine(String line) {
		String name = line, value = "";
		int index = line.indexOf(CHAR_DELIMITER_NAME);
		if(index > 0) {
			name  = line.substring(0, index);
			value = line.substring(index + 1);
		}
		return new Pair<>(name, parseValue(value));
	}
	
	private static final RemoteConfiguration from(BufferedReader reader) throws IOException {
		Map<String, ValueHolder> values = new LinkedHashMap<>();
		for(String line; (line = reader.readLine()) != null;) {
			if(line.isBlank()) continue; // Ignore empty lines
			Pair<String, ValueHolder> pair = parseLine(line);
			values.put(pair.a, pair.b);
		}
		return new RemoteConfiguration(values);
	}
	
	public static final RemoteConfiguration from(InputStream stream) throws IOException {
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
			return from(reader);
		}
	}
	
	public final String value(String name) {
		ValueHolder holder;
		return (holder = values.get(name)) != null ? holder.value() : null;
	}
	
	public final Map<String, Property> properties(String name) {
		ValueHolder holder;
		return (holder = values.get(name)) != null
					? (holder.isSimple()
							? Map.of()
							: Collections.unmodifiableMap(holder.properties()))
					: null;
	}
	
	public final Property property(String name, String propertyName) {
		ValueHolder holder;
		return (holder = values.get(name)) != null && !holder.isSimple()
					? holder.properties().get(propertyName)
					: null;
	}
	
	public static final class Property {
		
		private final String value;
		private final Set<String> flags;
		
		public Property(String value, Set<String> flags) {
			this.value = value;
			this.flags = flags;
		}
		
		public String value() {
			return value;
		}
		
		public Set<String> flags() {
			return flags;
		}
	}
	
	private static final class ValueHolder {
		
		private final String value;
		private final Map<String, Property> props;
		
		public ValueHolder(String value) {
			this(value, null);
		}
		
		public ValueHolder(String value, Map<String, Property> properties) {
			this.value = value;
			this.props = properties;
		}
		
		public boolean isSimple() {
			return props == null;
		}
		
		public String value() {
			return value;
		}
		
		public Map<String, Property> properties() {
			return props;
		}
	}
}