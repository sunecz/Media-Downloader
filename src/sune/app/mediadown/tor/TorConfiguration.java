package sune.app.mediadown.tor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sune.app.mediadown.concurrent.VarLoader;

/** @since 00.02.09 */
public final class TorConfiguration {
	
	private static final VarLoader<TorConfiguration> empty = VarLoader.of(TorConfiguration::createEmpty);
	
	private final Map<String, TorConfiguration.Option<?>> options;
	
	private TorConfiguration(Map<String, TorConfiguration.Option<?>> options) {
		this.options = Objects.requireNonNull(options);
	}
	
	private static final TorConfiguration createEmpty() {
		return new TorConfiguration(Map.of());
	}
	
	public static final TorConfiguration empty() {
		return empty.value();
	}
	
	public static final TorConfiguration of(TorConfiguration.Option<?>... options) {
		return (new Builder(options)).build();
	}
	
	public static final TorConfiguration.Builder builder() {
		return new Builder();
	}
	
	public <T> TorConfiguration.Option<T> get(String name) {
		@SuppressWarnings("unchecked")
		TorConfiguration.Option<T> option = (TorConfiguration.Option<T>) options.get(name);
		return option;
	}
	
	public void write(BufferedWriter writer) throws IOException {
		for(TorConfiguration.Option<?> option : options.values()) {
			writer.write(option.toString());
			writer.newLine();
		}
	}
	
	public static final class Builder {
		
		private final Map<String, TorConfiguration.Option<?>> options;
		
		private Builder() {
			this.options = new LinkedHashMap<>();
		}
		
		private Builder(TorConfiguration.Option<?>... options) {
			this.options = Stream.of(options)
				.map((o) -> Map.entry(o.name(), o))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
		}
		
		public TorConfiguration build() {
			return new TorConfiguration(Map.copyOf(options));
		}
		
		public <T> TorConfiguration.Builder set(TorConfiguration.Option<T> option) {
			Objects.requireNonNull(option);
			options.put(option.name(), option);
			return this;
		}
		
		public <T> TorConfiguration.Option<T> get(String name) {
			@SuppressWarnings("unchecked")
			TorConfiguration.Option<T> option = (TorConfiguration.Option<T>) options.get(name);
			return option;
		}
	}
	
	public static final class Options {
		
		// Reference: https://manpages.debian.org/stretch-backports/tor/torrc.5.en.html
		
		public static final Option.OfBoolean ClientOnly = new Option.OfBoolean("ClientOnly", true);
		public static final Option.OfString ControlPort = new Option.OfString("ControlPort", "auto");
		public static final Option.OfString SocksPort = new Option.OfString("SocksPort", "9050");
		public static final Option.OfString HTTPTunnelPort = new Option.OfString("HTTPTunnelPort", "0");
		public static final Option.OfString GeoIPFile = new Option.OfString("GeoIPFile", "geoip");
		public static final Option.OfString GeoIPv6File = new Option.OfString("GeoIPv6File", "geoip6");
		public static final Option.OfString HashedControlPassword = new Option.OfString("HashedControlPassword", "");
		public static final Option.OfString ControlPortWriteToFile = new Option.OfString("ControlPortWriteToFile", "control_port");
		public static final Option.OfString DataDirectory = new Option.OfString("DataDirectory", "");
		public static final Option.OfInteger KeepalivePeriod = new Option.OfInteger("KeepalivePeriod", 300);
		public static final Option.OfBoolean HardwareAccel = new Option.OfBoolean("HardwareAccel", true);
		public static final Option.OfBoolean ExcludeSingleHopRelays = new Option.OfBoolean("ExcludeSingleHopRelays", true);
		public static final Option.OfInteger CircuitBuildTimeout = new Option.OfInteger("CircuitBuildTimeout", 60);
		public static final Option.OfInteger CircuitIdleTimeout = new Option.OfInteger("CircuitIdleTimeout", 3600);
		public static final Option.OfInteger CircuitStreamTimeout = new Option.OfInteger("CircuitStreamTimeout", 0);
		public static final Option.OfNodeList ExcludeNodes = new Option.OfNodeList("ExcludeNodes", List.of(), false);
		public static final Option.OfNodeList ExcludeExitNodes = new Option.OfNodeList("ExcludeExitNodes", List.of(), false);
		public static final Option.OfNodeList ExitNodes = new Option.OfNodeList("ExitNodes", List.of(), false);
		public static final Option.OfNodeList EntryNodes = new Option.OfNodeList("EntryNodes", List.of(), false);
		public static final Option.OfBoolean StrictNodes = new Option.OfBoolean("StrictNodes", false);
		public static final Option.OfInteger NewCircuitPeriod = new Option.OfInteger("NewCircuitPeriod", 30);
		public static final Option.OfBoolean AllowSingleHopCircuits = new Option.OfBoolean("AllowSingleHopCircuits", false);
		public static final Option.OfBoolean ClientUseIPv4 = new Option.OfBoolean("ClientUseIPv4", true);
		public static final Option.OfBoolean ClientUseIPv6 = new Option.OfBoolean("ClientUseIPv6", false);
		
		// Forbid anyone to create an instance of this class
		private Options() {
		}
	}
	
	public static abstract class Option<T> {
		
		protected final String name;
		protected final T value;
		
		protected Option(String name, T value) {
			this.name = checkName(name);
			this.value = value;
		}
		
		private static final String checkName(String name) {
			if(name == null || name.isBlank()) {
				throw new IllegalArgumentException("Invalid option name");
			}
			
			return name;
		}
		
		protected abstract String valueString();
		public abstract TorConfiguration.Option<T> ofValue(T newValue);
		
		public String name() {
			return name;
		}
		
		public T value() {
			return value;
		}
		
		@Override
		public String toString() {
			return name + ' ' + valueString();
		}
		
		public static final class OfBoolean extends TorConfiguration.Option<Boolean> {
			
			private OfBoolean(String name, Boolean value) {
				super(name, value);
			}
			
			@Override
			protected String valueString() {
				return value ? "1" : "0";
			}
			
			@Override
			public Option.OfBoolean ofValue(Boolean newValue) {
				return new OfBoolean(name, Objects.requireNonNull(newValue));
			}
		}
		
		public static final class OfInteger extends TorConfiguration.Option<Integer> {
			
			private OfInteger(String name, Integer value) {
				super(name, value);
			}
			
			@Override
			protected String valueString() {
				return String.valueOf(value);
			}
			
			@Override
			public Option.OfInteger ofValue(Integer newValue) {
				return new OfInteger(name, Objects.requireNonNull(newValue));
			}
		}

		public static final class OfString extends TorConfiguration.Option<String> {
			
			private OfString(String name, String value) {
				super(name, value);
			}
			
			@Override
			protected String valueString() {
				return value;
			}
			
			@Override
			public Option.OfString ofValue(String newValue) {
				return new OfString(name, Objects.requireNonNull(newValue));
			}
		}
		
		public static final class OfList extends TorConfiguration.Option<List<?>> {
			
			private OfList(String name, List<?> value) {
				super(name, value);
			}
			
			@Override
			protected String valueString() {
				StringJoiner joiner = new StringJoiner(",");
				for(Object o : value) joiner.add(String.valueOf(o));
				return joiner.toString();
			}
			
			@Override
			public Option.OfList ofValue(List<?> newValue) {
				return new OfList(name, Objects.requireNonNull(newValue));
			}
		}
		
		public static final class OfNodeList extends TorConfiguration.Option<List<String>> {
			
			private final boolean strict;
			
			private OfNodeList(String name, List<String> value, boolean strict) {
				super(name, value);
				this.strict = strict;
			}
			
			private static final List<String> countryList(List<TorCountry> countries) {
				return countries.stream().map((c) -> '{' + c.code() + '}').collect(Collectors.toList());
			}
			
			@Override
			protected String valueString() {
				StringBuilder builder = new StringBuilder();
				StringJoiner joiner = new StringJoiner(",");
				for(Object o : value) joiner.add(String.valueOf(o));
				builder.append(joiner);
				if(strict) builder.append(" StrictNodes 1");
				return builder.toString();
			}
			
			@Override
			public Option.OfNodeList ofValue(List<String> newValue) {
				return ofValue(newValue, strict);
			}
			
			public Option.OfNodeList ofValue(List<String> newValue, boolean strict) {
				return new OfNodeList(name, Objects.requireNonNull(newValue), strict);
			}
			
			public Option.OfNodeList ofCountries(List<TorCountry> newValue) {
				return ofCountries(newValue, strict);
			}
			
			public Option.OfNodeList ofCountries(List<TorCountry> newValue, boolean strict) {
				return new OfNodeList(name, countryList(Objects.requireNonNull(newValue)), strict);
			}
		}
	}
}