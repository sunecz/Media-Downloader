package sune.app.mediadown.net;

import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sune.app.mediadown.Shared;
import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.media.MediaConstants;
import sune.app.mediadown.util.Opt;
import sune.app.mediadown.util.Range;
import sune.app.mediadown.util.Regex;
import sune.app.mediadown.util.Utils;

/** @since 00.02.08 */
public final class Web {
	
	private static final String USER_AGENT = Shared.USER_AGENT;
	private static final Charset CHARSET = Shared.CHARSET;
	
	private static Duration defaultConnectTimeout = Duration.ofMillis(5000);
	private static Duration defaultReadTimeout = Duration.ofMillis(20000);
	
	private static final VarLoader<HttpClient> httpClientWithRedirect = VarLoader.of(Web::newDefaultHttpClientWithRedirect);
	private static final VarLoader<HttpClient> httpClientNoRedirect = VarLoader.of(Web::newDefaultHttpClientNoRedirect);
	private static final VarLoader<CookieManager> cookieManager = VarLoader.of(Web::newCookieManager);
	private static final VarLoader<HttpRequest.Builder> httpRequestBuilder = VarLoader.of(Web::newHttpRequestBuilder);
	
	private static final AtomicInteger clientId = new AtomicInteger();
	
	// Forbid anyone to create an instance of this class
	private Web() {
	}
	
	private static final ExecutorService newExecutor() {
		// Must create the new executor with a custom thread factory so that
		// the threads are closed properly.
		return Executors.newCachedThreadPool(new WebThreadFactory(clientId.getAndIncrement()));
	}
	
	private static final HttpClient.Builder newHttpClientBuilder() {
		return HttpClient.newBuilder()
					.connectTimeout(defaultConnectTimeout)
					.cookieHandler(cookieManager())
					.executor(newExecutor())
					.version(Version.HTTP_2);
	}
	
	private static final HttpClient newDefaultHttpClientWithRedirect() {
		return newHttpClientBuilder().followRedirects(Redirect.NORMAL).build();
	}
	
	private static final HttpClient newDefaultHttpClientNoRedirect() {
		return newHttpClientBuilder().followRedirects(Redirect.NEVER).build();
	}
	
	private static final CookieManager newCookieManager() {
		return new CookieManager(null, CookiePolicy.ACCEPT_ALL);
	}
	
	private static final HttpRequest.Builder newHttpRequestBuilder() {
		return HttpRequest.newBuilder()
					.setHeader("User-Agent", USER_AGENT)
					.timeout(defaultReadTimeout);
	}
	
	private static final Duration checkTimeout(Duration timeout) {
		if(timeout.isNegative()) {
			throw new IllegalArgumentException("Invalid timeout, must be >= 0");
		}
		
		return timeout;
	}
	
	private static final HttpRequest.Builder httpRequestBuilder() {
		return httpRequestBuilder.value().copy();
	}
	
	private static final HttpClient httpClientFor(Request request) {
		switch(request.followRedirects()) {
			case NORMAL:
			case ALWAYS:
				return httpClientWithRedirect.value();
			case NEVER:
			default:
				return httpClientNoRedirect.value();
		}
	}
	
	private static final <T, R extends Response> R doRequest(Request request,
			BiFunction<Request, HttpResponse<T>, R> constructor, BodyHandler<T> handler) throws Exception {
		try {
			return constructor.apply(
				request,
				httpClientFor(request)
					.sendAsync(request.toHttpRequest(), handler)
					.join()
			);
		} catch(CompletionException ex) {
			throw (Exception) ex.getCause(); // Propagate up
		}
	}
	
	private static final Regex regexContentRange() {
		// Source: https://httpwg.org/specs/rfc9110.html#field.content-range
		return Regex.of("^([!#$%&'*+\\-.^_`|~0-9A-Za-z]+) (?:(\\d+)-(\\d+)/(\\d+|\\*)|\\*/(\\d+))$");
	}
	
	public static final Duration defaultConnectTimeout() {
		return defaultConnectTimeout;
	}
	
	public static final Duration defaultReadTimeout() {
		return defaultReadTimeout;
	}
	
	public static final void defaultConnectTimeout(Duration timeout) {
		defaultConnectTimeout = checkTimeout(timeout);
	}
	
	public static final void defaultReadTimeout(Duration timeout) {
		defaultReadTimeout = checkTimeout(timeout);
	}
	
	public static final CookieManager cookieManager() {
		return cookieManager.value();
	}
	
	public static final Response.OfString request(Request request) throws Exception {
		return doRequest(request, Response.OfString::new, BodyHandlers.ofString(CHARSET));
	}
	
	public static final Response.OfStream requestStream(Request request) throws Exception {
		return doRequest(request, Response.OfStream::new, BodyHandlers.ofInputStream());
	}
	
	public static final Response.OfStream peek(Request request) throws Exception {
		return requestStream(request.toHEAD());
	}
	
	public static final long size(Request request) throws Exception {
		return size(peek(request));
	}
	
	public static final long size(Response response) throws Exception {
		return response.statusCode() != 200 ? MediaConstants.UNKNOWN_SIZE : size(response.headers());
	}
	
	public static final long size(HttpHeaders headers) {
		Optional<String> contentRange = headers.firstValue("content-range");
		
		if(contentRange.isPresent()) {
			Matcher matcher = regexContentRange().matcher(contentRange.get());
			
			if(matcher.matches()) {
				String unit = matcher.group(1);
				
				switch(unit) {
					case "bytes": {
						String strRangeStart = matcher.group(2);
						
						if(strRangeStart == null) {
							return Long.valueOf(matcher.group(5));
						}
						
						long rangeStart = Long.valueOf(strRangeStart);
						long rangeEnd = Long.valueOf(matcher.group(3));
						return rangeEnd - rangeStart + 1L;
					}
					default: {
						// Not supported, ignore the header
						break;
					}
				}
			}
		}
		
		return headers.firstValueAsLong("content-length").orElse(MediaConstants.UNKNOWN_SIZE);
	}
	
	public static final void clear() {
		if(cookieManager.isSet()) {
			cookieManager.value().getCookieStore().removeAll();
		}
	}
	
	private static final class WebThreadFactory implements ThreadFactory {
		
		private final String namePrefix;
		private final AtomicInteger nextId = new AtomicInteger();
		
		private WebThreadFactory(int clientId) {
			this.namePrefix = "WebClient-" + clientId + "-Thread-";
		}
		
		@Override
		public Thread newThread(Runnable r) {
			String name = namePrefix + nextId.getAndIncrement();
			Thread thread = new Thread(null, r, name, 0, false);
			thread.setDaemon(true);
			return thread;
		}
	}
	
	public static abstract class Response implements AutoCloseable {
		
		protected final Request request;
		protected final HttpResponse<?> response;
		
		public Response(Request request, HttpResponse<?> response) {
			this.request = Objects.requireNonNull(request);
			this.response = Objects.requireNonNull(response);
		}
		
		public Request request() { return request; }
		public HttpResponse<?> response() { return response; }
		public int statusCode() { return response.statusCode(); }
		public URI uri() { return response.uri(); }
		public HttpHeaders headers() { return response.headers(); }
		
		public String identifier() {
			HttpHeaders headers = headers();
			Optional<String> identifier = headers.firstValue("etag");
			
			if(identifier.isEmpty()) {
				identifier = headers.firstValue("last-modified");
			}
			
			return identifier.orElse(null);
		}
		
		public static class OfString extends Response {
			
			protected OfString(Request request, HttpResponse<String> response) {
				super(request, response);
			}
			
			@Override
			public void close() throws Exception {
				// Do nothing
			}
			
			@SuppressWarnings("unchecked")
			public HttpResponse<String> response() { return (HttpResponse<String>) response; }
			
			public String body() { return response().body(); }
		}
		
		public static class OfStream extends Response {
			
			protected OfStream(Request request, HttpResponse<InputStream> response) {
				super(request, response);
			}
			
			@Override
			public void close() throws Exception {
				stream().close();
			}
			
			@SuppressWarnings("unchecked")
			public HttpResponse<InputStream> response() { return (HttpResponse<InputStream>) response; }
			
			public InputStream stream() { return response().body(); }
		}
	}
	
	public static abstract class Request {
		
		protected final String method;
		protected final URI uri;
		protected final String userAgent;
		protected final Map<String, List<String>> headers;
		protected final List<HttpCookie> cookies;
		protected final Redirect followRedirects;
		protected final String identifier;
		protected final Range<Long> range;
		protected final Duration timeout;
		
		protected Request(String method, Builder builder) {
			this.method = Objects.requireNonNull(method);
			this.uri = Objects.requireNonNull(builder.uri());
			this.userAgent = Objects.requireNonNull(builder.userAgent());
			this.headers = Objects.requireNonNull(builder.headers());
			this.cookies = Objects.requireNonNull(builder.cookies());
			this.followRedirects = Objects.requireNonNull(builder.followRedirects());
			this.identifier = builder.identifier();
			this.range = builder.range();
			this.timeout = Objects.requireNonNull(builder.timeout());
		}
		
		public static Builder of(URI uri) {
			return new Builder(uri);
		}
		
		protected HttpRequest.Builder httpRequestBuilder() {
			HttpRequest.Builder builder = Web.httpRequestBuilder();
			
			builder.uri(uri);
			
			for(Entry<String, List<String>> entry : headers.entrySet()) {
				String name = entry.getKey();
				
				for(String value : entry.getValue()) {
					builder.header(name, value);
				}
			}
			
			builder.setHeader("Charset", CHARSET.name());
			builder.setHeader("User-Agent", userAgent);
			
			if(!cookies.isEmpty()) {
				CookieStore cookieStore = Web.cookieManager().getCookieStore();
				
				for(HttpCookie cookie : cookies) {
					cookieStore.add(uri, cookie);
				}
			}
			
			if(range != null && range.from() >= 0L && (range.to() < 0L || range.from() <= range.to())) {
				builder.setHeader(
					"Range",
					range.to() >= 0L
						? String.format("bytes=%d-%d", range.from(), range.to())
						: String.format("bytes=%d-", range.from())
				);
				
				if(identifier != null && identifier.isBlank()) {
					builder.setHeader("If-Range", identifier);
				}
			}
			
			builder.timeout(timeout);
			
			return builder;
		}
		
		protected Builder builder() {
			return Request.Builder.of(this);
		}
		
		public abstract HttpRequest toHttpRequest();
		public abstract Request toHEAD();
		public abstract Request toRanged(Range<Long> range, String identifier);
		public abstract Request ofURI(URI uri);
		
		public Request toRanged(Range<Long> range) { return toRanged(range, null); }
		
		public String method() { return method; }
		public URI uri() { return uri; }
		public String userAgent() { return userAgent; }
		public Map<String, List<String>> headers() { return headers; }
		public List<HttpCookie> cookies() { return cookies; }
		public Redirect followRedirects() { return followRedirects; }
		public String identifier() { return identifier; }
		public Range<Long> range() { return range; }
		public Duration timeout() { return timeout; }
		
		protected static class GET extends Request {
			
			protected GET(Builder builder) {
				super("GET", builder);
			}
			
			@Override
			public HttpRequest toHttpRequest() {
				return httpRequestBuilder().GET().build();
			}
			
			@Override
			public Request toHEAD() {
				return builder().HEAD();
			}
			
			@Override
			public Request toRanged(Range<Long> range, String identifier) {
				return builder().range(range).identifier(identifier).GET();
			}
			
			@Override
			public Request ofURI(URI uri) {
				return builder().uri(uri).GET();
			}
		}
		
		protected static class POST extends Request {
			
			protected final String body;
			protected final String contentType;
			
			protected POST(Builder builder, String body, String contentType) {
				super("POST", builder);
				this.body = Objects.requireNonNull(body);
				this.contentType = Objects.requireNonNull(contentType);
			}
			
			@Override
			public HttpRequest toHttpRequest() {
				return httpRequestBuilder()
							.header("content-type", contentType)
							.POST(BodyPublishers.ofString(body, CHARSET))
							.build();
			}
			
			@Override
			public Request toHEAD() {
				return builder().HEAD();
			}
			
			@Override
			public Request toRanged(Range<Long> range, String identifier) {
				return builder().range(range).identifier(identifier).POST(body, contentType);
			}
			
			@Override
			public Request ofURI(URI uri) {
				return builder().uri(uri).POST(body, contentType);
			}
		}
		
		protected static class HEAD extends Request {
			
			protected HEAD(Builder builder) {
				super("HEAD", builder);
			}
			
			@Override
			public HttpRequest toHttpRequest() {
				return httpRequestBuilder().method(method(), BodyPublishers.noBody()).build();
			}
			
			@Override
			public Request toHEAD() {
				return this;
			}
			
			@Override
			public Request toRanged(Range<Long> range, String identifier) {
				return builder().range(range).identifier(identifier).HEAD();
			}
			
			@Override
			public Request ofURI(URI uri) {
				return builder().uri(uri).HEAD();
			}
		}
		
		public static class Builder {
			
			private static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded";
			
			private URI uri;
			private String userAgent;
			private final Map<String, List<String>> headers;
			private final List<HttpCookie> cookies;
			private Redirect followRedirects;
			private String identifier;
			private Range<Long> range;
			private Duration timeout;
			
			private Builder(URI uri) {
				this.uri = Objects.requireNonNull(uri);
				headers = new HashMap<>();
				cookies = new ArrayList<>();
				followRedirects = Redirect.NORMAL;
			}
			
			private Builder(Request request) {
				uri = request.uri();
				userAgent = request.userAgent();
				headers = request.headers();
				cookies = request.cookies();
				followRedirects = request.followRedirects();
				identifier = request.identifier();
				range = request.range();
				timeout = request.timeout();
			}
			
			protected static final <T> List<T> merge(List<T> list, Collection<T> values) {
				list.addAll(values);
				return list;
			}
			
			public static Builder of(URI uri) {
				return new Builder(uri);
			}
			
			public static Builder of(Request request) {
				return new Builder(request);
			}
			
			protected Builder ensureValues() {
				if(uri == null) {
					throw new IllegalArgumentException("URI cannot be null");
				}
				
				if(userAgent == null || userAgent.isBlank()) {
					userAgent = USER_AGENT;
				}
				
				if(followRedirects == null) {
					followRedirects = Redirect.NEVER;
				}
				
				if(timeout == null || timeout.isNegative()) {
					timeout = defaultReadTimeout;
				}
				
				return this;
			}
			
			public Request GET() {
				return new Request.GET(ensureValues());
			}
			
			public Request POST(String body) {
				return POST(body, DEFAULT_CONTENT_TYPE);
			}
			
			public Request POST(String body, String contentType) {
				return new Request.POST(ensureValues(), body, contentType);
			}
			
			public Request HEAD() {
				return new Request.HEAD(ensureValues());
			}
			
			public Builder uri(URI uri) { this.uri = Objects.requireNonNull(uri); return this; }
			public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
			
			public Builder addHeaders(HttpHeaders headers) {
				return addHeaders(headers.map());
			}
			
			public Builder addHeaders(Map<String, List<String>> headers) {
				headers.forEach((k, v) -> this.headers.compute(k, (a, b) -> b == null ? v : merge(b, v)));
				return this;
			}
			
			public Builder addHeaders(Object... values) {
				return addHeaders(Headers.ofSingle(values));
			}
			
			public Builder addHeader(String name, Collection<String> values) {
				headers.compute(name, (k, v) -> v == null ? List.copyOf(values) : merge(v, values));
				return this;
			}
			
			public Builder headers(HttpHeaders headers) {
				return headers(headers.map());
			}
			
			public Builder headers(Map<String, List<String>> headers) {
				this.headers.clear();
				this.headers.putAll(headers);
				return this;
			}
			
			public Builder headers(Object... values) {
				return headers(Headers.ofSingle(values));
			}
			
			public Builder header(String name, Collection<String> values) {
				headers.put(name, List.copyOf(values));
				return this;
			}
			
			public Builder addHeader(String name, String... values) { return addHeader(name, List.of(values)); }
			public Builder header(String name, String... values) { return header(name, List.of(values)); }
			public Builder addCookie(HttpCookie cookie) { cookies.add(cookie); return this; }
			public Builder followRedirects(Redirect followRedirects) { this.followRedirects = followRedirects; return this; }
			public Builder identifier(String identifier) { this.identifier = identifier; return this; }
			public Builder range(Range<Long> range) { this.range = range; return this; }
			public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }
			
			public URI uri() { return uri; }
			public String userAgent() { return userAgent; }
			public Map<String, List<String>> headers() { return Collections.unmodifiableMap(headers); }
			public List<HttpCookie> cookies() { return Collections.unmodifiableList(cookies); }
			public Redirect followRedirects() { return followRedirects; }
			public String identifier() { return identifier; }
			public Range<Long> range() { return range; }
			public Duration timeout() { return timeout; }
		}
	}
	
	public static final class Headers {
		
		private static final VarLoader<BiPredicate<String, String>> filter = VarLoader.of(Headers::newFilter);
		private static final VarLoader<HttpHeaders> empty = VarLoader.of(Headers::newEmpty);
		
		// Forbid anyone to create an instance of this class
		private Headers() {
		}
		
		private static final BiPredicate<String, String> newFilter() {
			return (a, b) -> true;
		}
		
		private static final BiPredicate<String, String> noFilter() {
			return filter.value();
		}
		
		private static final HttpHeaders newEmpty() {
			return HttpHeaders.of(Map.of(), noFilter());
		}
		
		public static final HttpHeaders empty() {
			return empty.value();
		}
		
		public static final HttpHeaders ofMap(Map<String, List<String>> headers) {
			return HttpHeaders.of(headers, noFilter());
		}
		
		public static final HttpHeaders ofSingleMap(Map<String, String> headers) {
			return ofMap(
				headers.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, (v) -> List.of(v.getValue())))
			);
		}
		
		public static final HttpHeaders ofSingle(Object... values) {
			Map<String, List<String>> headers = new HashMap<>(values.length / 2);
			
			for(int i = 0, l = values.length; i < l; i += 2) {
				headers.put(String.valueOf(values[i]), List.of(String.valueOf(values[i + 1])));
			}
			
			return ofMap(headers);
		}
		
		public static final HttpHeaders ofString(String string) {
			Map<String, List<String>> map = new LinkedHashMap<>();
			(new Parser(string)).read(map);
			return ofMap(map);
		}
		
		private static final class Parser {
			
			private static final int CHAR_QUOTES_SINGLE   = '\'';
			private static final int CHAR_QUOTES_DOUBLE   = '"';
			private static final int CHAR_ESCAPE_SLASH    = '\\';
			private static final int CHAR_ITEM_DELIMITER1 = '\r';
			private static final int CHAR_ITEM_DELIMITER2 = '\n';
			private static final int CHAR_NAME_DELIMITER  = ':';
			
			private final String string;
			private int cursor;
			private int c;
			
			private Parser(String string) {
				this.string = Objects.requireNonNull(string);
			}
			
			private static final boolean isWhitespace(int c) {
				return c != CHAR_ITEM_DELIMITER1 && c != CHAR_ITEM_DELIMITER2 && Character.isWhitespace(c);
			}
			
			private static final boolean isNameDelimiter(int c) {
				return c == CHAR_ITEM_DELIMITER1 || c == CHAR_ITEM_DELIMITER2 || c == CHAR_NAME_DELIMITER;
			}
			
			private static final boolean isValueDelimiter(int c) {
				return c == CHAR_ITEM_DELIMITER1 || c == CHAR_ITEM_DELIMITER2;
			}
			
			private static final List<String> merge(String name, String value, List<String> existing) {
				if(existing == null) {
					List<String> list = new ArrayList<>();
					list.add(value);
					return list;
				}
				
				existing.add(value);
				return existing;
			}
			
			private static final String lower(String string) {
				return string.toLowerCase(Locale.ROOT);
			}
			
			private final int next() {
				if(cursor >= string.length()) {
					return -1;
				}
				
				int v = string.codePointAt(cursor);
				cursor += Utils.charCount(v);
				return v;
			}
			
			private final int skipWhitespaces() {
				while(isWhitespace(c)) {
					c = next();
				}
				
				return c;
			}
			
			private final void readString(StringBuilder str, Predicate<Integer> isDelimiter) {
				boolean escaped = false;
				boolean qs = false;
				boolean qd = false;
				
				do {
					if(c == CHAR_ESCAPE_SLASH) {
						str.appendCodePoint(c);
						escaped = !escaped;
					} else if(escaped) {
						str.appendCodePoint(c);
						escaped = false;
					} else if(isDelimiter.test(c)) {
						break; // String closed
					} else {
						if(!qd && c == CHAR_QUOTES_DOUBLE) {
							qs = !qs;
						} else if(!qs && c == CHAR_QUOTES_SINGLE) {
							qd = !qd;
						}
						
						str.appendCodePoint(c);
					}
				} while((c = next()) != -1);
			}
			
			private final void readName(StringBuilder str) {
				readString(str, Parser::isNameDelimiter);
			}
			
			private final void readValue(StringBuilder str) {
				readString(str, Parser::isValueDelimiter);
			}
			
			private final void reset() {
				cursor = 0;
				c = -1;
			}
			
			public void read(Map<String, List<String>> map) {
				reset();
				
				String name = null;
				StringBuilder tmp = new StringBuilder();
				
				c = next(); // Initialize
				while((c = skipWhitespaces()) != -1) {
					if(c == CHAR_NAME_DELIMITER) {
						name = tmp.toString();
						tmp.setLength(0);
						c = next();
					} else if(c == CHAR_ITEM_DELIMITER1) {
						c = next();
						
						if(c == CHAR_ITEM_DELIMITER2) {
							String value = tmp.toString();
							map.compute(lower(name), (k, v) -> merge(k, value, v));
							name = null;
							tmp.setLength(0);
							c = next();
						}
					} else if(name == null) {
						readName(tmp);
					} else {
						readValue(tmp);
					}
				}
				
				if(name != null) {
					String value = tmp.toString();
					map.compute(lower(name), (k, v) -> merge(k, value, v));
					name = null;
					tmp.setLength(0);
				}
			}
		}
	}
	
	public static final class Cookies {
		
		// Forbid anyone to create an instance of this class
		private Cookies() {
		}
		
		public static final void add(URI uri, HttpCookie cookie, HttpCookie... more) {
			add(uri, more.length > 0 ? Utils.streamToList(Stream.of(List.of(cookie), more)) : List.of(cookie));
		}
		
		public static final void add(URI uri, List<HttpCookie> cookies) {
			CookieStore store = cookieManager().getCookieStore();
			cookies.forEach((cookie) -> store.add(uri, cookie));
		}
		
		public static final void set(URI uri, HttpCookie cookie, HttpCookie... more) {
			clear(uri);
			add(uri, cookie, more);
		}
		
		public static final void set(URI uri, List<HttpCookie> cookies) {
			clear(uri);
			add(uri, cookies);
		}
		
		public static final List<HttpCookie> get(URI uri) {
			return cookieManager().getCookieStore().get(uri);
		}
		
		public static final HttpCookie stringToCookie(String string) {
			return Opt.of(HttpCookie.parse(string)).ifFalse(List::isEmpty).map((l) -> l.get(0)).orElse(null);
		}
		
		public static final String cookieToString(HttpCookie cookie) {
			return cookie.toString();
		}
		
		public static final void clear(URI uri) {
			CookieStore store = cookieManager().getCookieStore();
			store.get(uri).forEach((cookie) -> store.remove(uri, cookie));
		}
		
		public static final void clear() {
			cookieManager().getCookieStore().removeAll();
		}
	}
}