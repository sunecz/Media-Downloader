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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import sune.app.mediadown.Shared;
import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.util.Range;

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
	
	private static final <T, R extends Response<T>> R doRequest(Request request,
			BiFunction<Request, HttpResponse<T>, R> constructor, BodyHandler<T> handler) {
		return constructor.apply(
			request,
			httpClientFor(request)
				.sendAsync(request.toHttpRequest(), handler)
				.join()
		);
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
	
	private static final Request.HEAD toHEADRequest(Request request) {
		if(request instanceof Request.HEAD || request.method().equals("HEAD")) {
			return (Request.HEAD) request;
		}
		
		return (Request.HEAD) Request.Builder.of(request).HEAD();
	}
	
	public static final Response.OfStream peek(Request request) throws Exception {
		return requestStream(toHEADRequest(request));
	}
	
	public static final void clear() {
		if(cookieManager.isSet()) {
			cookieManager.value().getCookieStore().removeAll();
		}
	}
	
	// TODO: Add method: size(Response)
	// TODO: Add method: size(HttpHeaders)
	// TODO: Add class: Cookies (for various cookies-related operations)
	
	private static final class WebThreadFactory implements ThreadFactory {
		
		private final String namePrefix;
		private final AtomicInteger nextId = new AtomicInteger();
		
		public WebThreadFactory(int clientId) {
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
	
	public static abstract class Response<T> implements AutoCloseable {
		
		protected final Request request;
		protected final HttpResponse<T> response;
		
		public Response(Request request, HttpResponse<T> response) {
			this.request = Objects.requireNonNull(request);
			this.response = Objects.requireNonNull(response);
		}
		
		public Request request() { return request; }
		public HttpResponse<T> response() { return response; }
		
		public int statusCode() { return response.statusCode(); }
		public URI uri() { return response.uri(); }
		public HttpHeaders headers() { return response.headers(); }
		
		public static class OfString extends Response<String> {
			
			protected OfString(Request request, HttpResponse<String> response) {
				super(request, response);
			}
			
			@Override
			public void close() throws Exception {
				// Do nothing
			}
			
			public String body() { return response.body(); }
		}
		
		public static class OfStream extends Response<InputStream> {
			
			protected OfStream(Request request, HttpResponse<InputStream> response) {
				super(request, response);
			}
			
			@Override
			public void close() throws Exception {
				stream().close();
			}
			
			public InputStream stream() { return response.body(); }
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
		
		public abstract HttpRequest toHttpRequest();
		
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
		}
		
		protected static class POST extends Request {
			
			protected final String body;
			
			protected POST(Builder builder, String body) {
				super("POST", builder);
				this.body = Objects.requireNonNull(body);
			}
			
			@Override
			public HttpRequest toHttpRequest() {
				return httpRequestBuilder().POST(BodyPublishers.ofString(body, CHARSET)).build();
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
		}
		
		public static class Builder {
			
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
				return new Request.POST(ensureValues(), body);
			}
			
			public Request HEAD() {
				return new Request.HEAD(ensureValues());
			}
			
			public Builder uri(URI uri) { this.uri = Objects.requireNonNull(uri); return this; }
			public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
			
			public Builder addHeader(String name, Collection<String> values) {
				headers.compute(name, (k, v) -> v == null ? List.copyOf(values) : merge(v, values));
				return this;
			}
			
			public Builder setHeader(String name, Collection<String> values) {
				headers.put(name, List.copyOf(values));
				return this;
			}
			
			public Builder addHeader(String name, String... values) { return addHeader(name, List.of(values)); }
			public Builder setHeader(String name, String... values) { return setHeader(name, List.of(values)); }
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
}