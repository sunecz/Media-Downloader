package sune.app.mediadown.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.stream.Collectors;

import sune.app.mediadown.Shared;
import sune.app.mediadown.util.Regex;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.08 */
public final class Net {
	
	// Forbid anyone to create an instance of this class
	private Net() {
	}
	
	private static final String uriSanitize(String uri) {
		return uri.replace("|", "%7C");
	}
	
	private static final void queryConstruct(StringBuilder builder, QueryArgument argument, String namePrefix) {
		String name = encodeURL(argument.name());
		
		if(namePrefix != null) {
			name = namePrefix + '[' + name + ']';
		}
		
		if(!argument.isArray()) {
			builder.append(name).append('=').append(encodeURL(argument.value()));
			return;
		}
		
		if(argument.count() > 0) {
			for(QueryArgument arg : argument.arguments()) {
				queryConstruct(builder, arg, name);
				builder.append('&');
			}
			
			// Remove the last ampersand
			builder.setLength(builder.length() - 1);
		}
	}
	
	private static final void queryMap(Map<String, Object> map, QueryArgument argument, String namePrefix) {
		String name = argument.name();
		
		if(namePrefix != null) {
			name = namePrefix + '[' + name + ']';
		}
		
		if(!argument.isArray()) {
			map.put(name, argument.value());
			return;
		}
		
		if(argument.count() > 0) {
			for(QueryArgument arg : argument.arguments()) {
				queryMap(map, arg, name);
			}
		}
	}
	
	public static final URI uri(String uri) {
		return URI.create(uriSanitize(uri));
	}
	
	public static final URI uri(URL url) {
		return Ignore.defaultValue(url::toURI, null);
	}
	
	public static final URI uri(Path path) {
		StringBuilder builder = new StringBuilder();
		
		Path root;
		if((root = path.getRoot()) != null) {
			builder.append(root.toString().replace('\\', '/'));
		}
		
		if(path.getNameCount() > 0) {
			for(Path part : path) {
				builder.append(encodeURL(part.toString()).replace("+", "%20")).append('/');
			}
			
			builder.setLength(builder.length() - 1);
		}
		
		return uri("file:///" + builder.toString());
	}
	
	public static final URL url(String url) {
		return url(uri(url));
	}
	
	public static final URL url(URI uri) {
		return Ignore.defaultValue(uri::toURL, null);
	}
	
	public static final String encodeURL(String url) {
		return url == null ? null : URLEncoder.encode(url, Shared.CHARSET);
	}
	
	public static final String decodeURL(String url) {
		return url == null ? null : URLDecoder.decode(url, Shared.CHARSET);
	}
	
	public static final URI baseURI(URI uri) {
		return uriDirname(uri);
	}
	
	public static final boolean isValidURI(String uri) {
		return Ignore.callAndCheck(() -> uri(uri));
	}
	
	public static final boolean isRelativeURI(String uri) {
		return !uri(uri).isAbsolute();
	}
	
	public static final String uriConcat(String... parts) {
		return Regex.of("([^:])//+").replaceAll(Utils.join("/", parts), "$1/");
	}
	
	public static final String uriFix(String uri) {
		return uri.startsWith("//") ? "https:" + uri : uri;
	}
	
	public static final URI uriBasename(URI uri) {
		return uri(Utils.afterLast(uri.getRawPath(), "/"));
	}
	
	public static final URI uriBasename(String uri) {
		return uriBasename(uri(uri));
	}
	
	public static final URI uriDirname(URI uri) {
		return uri.getRawPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
	}
	
	public static final URI uriDirname(String uri) {
		return uriDirname(uri(uri));
	}
	
	/** @since 00.02.09 */
	public static final String removeQuery(URI uri) {
		return uri != null ? Utils.beforeFirst(uri.toString(), "?") : null;
	}
	
	/** @since 00.02.09 */
	public static final String removeQuery(String uri) {
		return uri != null ? Utils.beforeFirst(uri, "?") : null;
	}
	
	public static final InputStream stream(URI uri, Duration timeout) throws IOException {
		return stream(url(uri), timeout);
	}
	
	public static final InputStream stream(URL url, Duration timeout) throws IOException {
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout((int) timeout.toMillis());
		return connection.getInputStream();
	}
	
	public static final URI resolve(URI uri, URI other) {
		return uri.resolve(other);
	}
	
	public static final URI resolve(URI uri, String other) {
		return uri.resolve(uri(other));
	}
	
	public static final String queryConstruct(QueryArgument argument) {
		StringBuilder builder = new StringBuilder();
		queryConstruct(builder, argument, null);
		return builder.toString();
	}
	
	public static final String queryConstruct(List<QueryArgument> arguments) {
		StringBuilder builder = new StringBuilder();
		
		if(!arguments.isEmpty()) {
			for(QueryArgument arg : arguments) {
				queryConstruct(builder, arg, null);
				builder.append('&');
			}
			
			// Remove the last ampersand
			builder.setLength(builder.length() - 1);
		}
		
		return builder.toString();
	}
	
	public static final QueryArgument queryDestruct(URI uri) {
		return queryDestruct(uri.getRawQuery());
	}
	
	public static final QueryArgument queryDestruct(String uri) {
		QueryArgument.Builder root = QueryArgument.Builder.ofRoot();
		String query = Utils.afterLast(uri, "?");
		StringTokenizer tokenizer = new StringTokenizer(query, "&");
		
		while(tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			String[] split = token.split("=");
			String name = decodeURL(split[0]);
			String value = decodeURL(split[1]);
			
			// Handle an array arguments
			int start;
			if((start = name.indexOf('[')) > 0) {
				String arrayName = name.substring(0, start);
				String firstName = null;
				QueryArgument.Builder top = root.valueOf(arrayName);
				
				int end;
				QueryArgument.Builder parent = top;
				
				do {
					if((end = name.indexOf(']', start + 1)) < 0) {
						break; // Invalid array item, just exit the loop
					}
					
					String itemName = name.substring(start + 1, end);
					
					// Remember the first item name so that we can use it to add the root
					if(firstName == null) {
						firstName = itemName;
					}
					
					parent = parent.valueOf(itemName);
				} while((start = name.indexOf('[', end + 1)) > 0);
				
				if(end < 0) {
					// Invalid array item, do not set the value
					continue;
				}
				
				parent.value(value);
			}
			// Handle a simple value
			else {
				QueryArgument.Builder arg = QueryArgument.Builder.ofName(name);
				arg.value(value);
				root.merge(arg);
			}
		}
		
		return root.build();
	}
	
	public static final QueryArgument createQuery(Map<String, Object> args) {
		QueryArgument.Builder root = QueryArgument.Builder.ofRoot();
		
		for(Entry<String, Object> entry : args.entrySet()) {
			String name = entry.getKey();
			String value = String.valueOf(entry.getValue());
			QueryArgument.Builder arg = QueryArgument.Builder.ofName(name);
			arg.value(value);
			root.merge(arg);
		}
		
		return root.build();
	}
	
	public static final QueryArgument createQuery(Object... args) {
		return createQuery(Utils.stringKeyMap(args));
	}
	
	public static final String queryString(Map<String, Object> args) {
		return queryConstruct(createQuery(args));
	}
	
	public static final String queryString(Object... args) {
		return queryConstruct(createQuery(args));
	}
	
	public static final Map<String, Object> queryMap(QueryArgument argument) {
		Map<String, Object> map = new LinkedHashMap<>();
		queryMap(map, argument, null);
		return map;
	}
	
	public static final Map<String, Object> queryMap(List<QueryArgument> arguments) {
		Map<String, Object> map = new LinkedHashMap<>();
		
		if(!arguments.isEmpty()) {
			for(QueryArgument arg : arguments) {
				queryMap(map, arg, null);
			}
		}
		
		return map;
	}
	
	public static interface QueryArgument {
		
		String name();
		QueryArgument argument();
		QueryArgument argumentOf(String name);
		QueryArgument argumentAt(int index);
		List<QueryArgument> arguments();
		Map<String, QueryArgument> argumentsMap();
		String value();
		String valueOf(String name);
		String valueOf(String name, String defaultValue);
		String valueAt(int index);
		String valueAt(int index, String defaultValue);
		List<String> values();
		int count();
		boolean isArray();
		
		public static QueryArgument ofValue(String name, String value) {
			return new OfValue(name, value);
		}
		
		static final class OfValue implements QueryArgument {
			
			private final String name;
			private final String value;
			
			private OfValue(String name, String value) {
				this.name = Objects.requireNonNull(name);
				this.value = Objects.requireNonNull(value);
			}
			
			private static final void notAnArray() {
				throw new UnsupportedOperationException("Not an array argument");
			}
			
			@Override public String name() { return name; }
			@Override public QueryArgument argument() { return this; }
			@Override public QueryArgument argumentOf(String name) { notAnArray(); return null; }
			@Override public QueryArgument argumentAt(int index) { notAnArray(); return null; }
			@Override public List<QueryArgument> arguments() { return List.of(this); }
			@Override public Map<String, QueryArgument> argumentsMap() { return Map.of(name, this); }
			@Override public String value() { return value; }
			@Override public String valueOf(String name) { notAnArray(); return null; }
			@Override public String valueOf(String name, String defaultValue) { return defaultValue; }
			@Override public String valueAt(int index) { notAnArray(); return null; }
			@Override public String valueAt(int index, String defaultValue) { return defaultValue; }
			@Override public List<String> values() { return List.of(value); }
			@Override public int count() { return 1; }
			@Override public boolean isArray() { return false; }
			
			@Override
			public int hashCode() {
				return Objects.hash(name, value);
			}
			
			@Override
			public boolean equals(Object obj) {
				if(this == obj)
					return true;
				if(obj == null)
					return false;
				if(getClass() != obj.getClass())
					return false;
				OfValue other = (OfValue) obj;
				return Objects.equals(name, other.name) && Objects.equals(value, other.value);
			}
			
			@Override
			public String toString() {
				return "QueryArgument(name='" + name + "', value='" + value + "')";
			}
		}
		
		static final class OfArray implements QueryArgument {
			
			private final String name;
			private final Map<String, QueryArgument> values;
			
			private OfArray(String name, Map<String, QueryArgument> values) {
				this.name = name; // May be null for a root argument
				this.values = Collections.unmodifiableMap(values); // Implicit null check
			}
			
			private final QueryArgument firstArgument() {
				return values.values().stream().findFirst().get();
			}
			
			private final String firstValue() {
				return firstArgument().value();
			}
			
			private final QueryArgument get(String name) {
				return values.get(name);
			}
			
			private final QueryArgument get(int index) {
				return values.get(Integer.toString(index));
			}
			
			private final List<QueryArgument> listOfArguments() {
				return List.copyOf(values.values());
			}
			
			private final Map<String, QueryArgument> mapOfArguments() {
				return values.entrySet().stream().collect(Collectors.toMap(
					Map.Entry::getKey,
					Map.Entry::getValue,
					(a, b) -> a,
					LinkedHashMap::new
				));
			}
			
			private final List<String> listOfValues() {
				return values.values().stream().flatMap((a) -> a.values().stream()).collect(Collectors.toList());
			}
			
			private final <T> String valueOrDefault(Function<T, QueryArgument> function, T input, String defaultValue) {
				return Optional.ofNullable(function.apply(input)).map(QueryArgument::value).orElse(defaultValue);
			}
			
			@Override public String name() { return name; }
			@Override public QueryArgument argument() { return firstArgument(); }
			@Override public QueryArgument argumentOf(String name) { return get(name); }
			@Override public QueryArgument argumentAt(int index) { return get(index); }
			@Override public List<QueryArgument> arguments() { return listOfArguments(); }
			@Override public Map<String, QueryArgument> argumentsMap() { return mapOfArguments(); }
			@Override public String value() { return firstValue(); }
			@Override public String valueOf(String name) { return argumentOf(name).value(); }
			@Override public String valueOf(String name, String defaultValue) { return valueOrDefault(this::argumentOf, name, defaultValue); }
			@Override public String valueAt(int index) { return argumentAt(index).value(); }
			@Override public String valueAt(int index, String defaultValue) { return valueOrDefault(this::argumentAt, index, defaultValue); }
			@Override public List<String> values() { return listOfValues(); }
			@Override public int count() { return values.size(); }
			@Override public boolean isArray() { return true; }
			
			@Override
			public int hashCode() {
				return Objects.hash(name, values);
			}
			
			@Override
			public boolean equals(Object obj) {
				if(this == obj)
					return true;
				if(obj == null)
					return false;
				if(getClass() != obj.getClass())
					return false;
				OfArray other = (OfArray) obj;
				return Objects.equals(name, other.name) && Objects.equals(values, other.values);
			}
			
			@Override
			public String toString() {
				return "QueryArgument(name=" + (name == null ? name : "'" + name + "'") + ", values=" + values + ")";
			}
		}
		
		static final class Builder {
			
			private String name;
			private String value;
			private Map<String, Builder> values;
			private int index;
			
			private Builder(String name) {
				this.name = name;
			}
			
			public static final Builder ofRoot() {
				return new Builder(null);
			}
			
			public static final Builder ofName(String name) {
				return new Builder(Objects.requireNonNull(name));
			}
			
			private final Map<String, Builder> values() {
				return values == null ? (values = new LinkedHashMap<>()) : values;
			}
			
			private final boolean isArray() {
				return values != null || name == null;
			}
			
			private final Builder name(String name) {
				this.name = Objects.requireNonNull(name);
				return this;
			}
			
			public QueryArgument build() {
				if(!isArray()) {
					return new OfValue(name, value);
				}
				
				Map<String, QueryArgument> args = values.entrySet().stream().collect(Collectors.toMap(
					Map.Entry::getKey,
					(e) -> e.getValue().build(),
					(a, b) -> a,
					LinkedHashMap::new
				));
				
				return new OfArray(name, args);
			}
			
			public Builder value(String value) {
				this.value = Objects.requireNonNull(value);
				return this;
			}
			
			public Builder valueOf(String name) {
				boolean isArray = isArray();
				Map<String, Builder> vals = values();
				
				if(!isArray && value != null) {
					// Add the existing value as the first item in the new array
					Builder arg = ofName(Integer.toString(index++));
					arg.value(value);
					vals.put(arg.name, arg);
					value = null;
				}
				
				if(name.isBlank()) {
					name = Integer.toString(index++);
				}
				
				return vals.computeIfAbsent(name, Builder::new);
			}
			
			public void merge(Builder argument) {
				boolean isArray = isArray();
				String name = argument.name;
				Map<String, Builder> vals = values();
				
				if(!isArray && value != null) {
					// Add the existing value as the first item in the new array
					Builder arg = ofName(Integer.toString(index++));
					arg.value(value);
					vals.put(arg.name, arg);
					value = null;
				}
				
				if(name.isBlank()) {
					name = Integer.toString(index++);
					argument.name(name);
					vals.put(name, argument);
					return;
				}
				
				if(isArray) {
					Builder item = vals.putIfAbsent(name, argument);
					
					if(item == null) {
						return; // Name was not associated, argument directly added
					}
					
					if(argument.isArray()) {
						for(Builder arg : argument.values.values()) {
							item.merge(arg);
						}
					} else {
						item.merge(argument);
					}
				} else {
					if(argument.isArray()) {
						vals.putAll(argument.values); // May replace the first item
						index = Math.max(index, argument.index);
					} else {
						if(name.equals(this.name)) {
							name = Integer.toString(index++);
						}
						
						argument.name(name);
						vals.put(name, argument);
					}
				}
			}
		}
	}
}