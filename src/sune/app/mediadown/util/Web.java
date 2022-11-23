package sune.app.mediadown.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import sune.app.mediadown.Shared;

public final class Web {
	
	private static final String  USER_AGENT   = Shared.USER_AGENT;
	private static final Charset CHARSET      = Shared.CHARSET;
	private static final String  ENCODING     = CHARSET.name();
	private static final int     TIMEOUT      = 5000;
	private static final int     READ_TIMEOUT = 5000;
	
	private static final char   CHAR_COOKIES_DELIMITER    = ';';
	private static final char   CHAR_COOKIES_NV_DELIMITER = '=';
	private static final char   CHAR_PARAMS_DELIMITER     = '&';
	private static final char   CHAR_PARAMS_ASSIGN        = '=';
	private static final String CHAR_COOKIE_IT_DELIMITER  = ";";
	private static final String CHAR_COOKIE_NV_DELIMITER  = "=";
	private static final char   CHAR_QUOTES_SINGLE        = '\'';
	private static final char   CHAR_QUOTES_DOUBLE        = '\"';
	private static final char   CHAR_HEADER_IT_DELIMITER  = ';';
	private static final char   CHAR_HEADER_NV_DELIMITER  = '=';
	
	// Forbid anyone to create an instance of this class
	private Web() {
	}
	
	private static CookieManager COOKIE_MANAGER;
	private static final void ensureCookieManager() {
		if((COOKIE_MANAGER == null)) {
			COOKIE_MANAGER = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
			if((CookieHandler.getDefault() != COOKIE_MANAGER))
				CookieHandler.setDefault(COOKIE_MANAGER);
		}
	}
	
	private static final HttpCookie[] retrieveCookies(URI uri) {
		List<HttpCookie> cookies = COOKIE_MANAGER.getCookieStore().get(uri);
		return cookies != null ? cookies.toArray(new HttpCookie[cookies.size()]) : null;
	}
	
	private static final HttpURLConnection openConnection(URL url)
			throws MalformedURLException,
				   IOException {
		return (HttpURLConnection) url.openConnection();
	}
	
	private static final void closeConnection(HttpURLConnection connection)
			throws IOException {
		connection.disconnect();
	}
	
	private static final String getIdentifier(HttpURLConnection connection) {
		String identifier = connection.getHeaderField("ETag");
		if((identifier == null))
			identifier = connection.getHeaderField("Last-Modified");
		return identifier;
	}
	
	private static final boolean isStatusOK(int code) {
		return code >= 200 && code < 400;
	}
	
	private static final String cookies2string(HttpCookie[] cookies) {
		boolean       first = true;
		StringBuilder sb    = new StringBuilder();
		for(HttpCookie cookie : cookies) {
			if((first)) {
				first = false;
			} else {
				sb.append(CHAR_COOKIES_DELIMITER);
				sb.append(' ');
			}
			sb.append(cookie.getName());
			sb.append(CHAR_COOKIES_NV_DELIMITER);
			sb.append(cookie.getValue());
		}
		return sb.toString();
	}
	
	private static final String params2string(Map<String, String> params)
			throws UnsupportedEncodingException {
		boolean       first = true;
		StringBuilder sb    = new StringBuilder();
		for(Entry<String, String> e : params.entrySet()) {
			if((first)) {
				first = false;
			} else {
				sb.append(CHAR_PARAMS_DELIMITER);
			}
			sb.append(URLEncoder.encode(e.getKey(), ENCODING))
			  .append(CHAR_PARAMS_ASSIGN)
			  .append(URLEncoder.encode(e.getValue(), ENCODING));
		}
		return sb.toString();
	}
	
	private static final String stream2string(InputStream stream)
			throws IOException {
		return new String(stream.readAllBytes(), CHARSET);
	}
	
	private static final URL urlEnsureEncoded(URL url) {
		try {
			String path = URLEncoder.encode(url.getPath().substring(1), ENCODING);
			URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(),
				url.getPort(), '/' + path, url.getQuery(), null);
			return uri.toURL();
		} catch(MalformedURLException
					| UnsupportedEncodingException
					| URISyntaxException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	private static final URI toURI(URL url) {
		try {
			URL urlEnsured = urlEnsureEncoded(url);
			return urlEnsured != null ? urlEnsured.toURI() : null;
		} catch(URISyntaxException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	public static enum RequestMethod {
		GET, POST, HEAD
	}
	
	public static abstract class Request {
		
		public final URL                 url;
		public final RequestMethod       method;
		public final String              userAgent;
		public final Map<String, String> params;
		public final String              body;
		public final HttpCookie[]        cookies;
		public final Map<String, String> headers;
		public final boolean			 followRedirects;
		public final String              identifier;
		public final long                rangeStart;
		public final long                rangeEnd;
		public final int                 timeout;
		public final int                 readTimeout;
		
		public Request(URL url, RequestMethod method, String userAgent,
		               Map<String, String> params, HttpCookie[] cookies,
		               Map<String, String> headers, boolean followRedirects,
		               String identifier, long rangeStart, long rangeEnd,
		               int timeout, String body, int readTimeout) {
			this.url        	 = url;
			this.method     	 = method;
			this.userAgent  	 = userAgent;
			this.params 	     = params;
			this.body            = body;
			this.cookies    	 = cookies;
			this.headers		 = headers;
			this.followRedirects = followRedirects;
			this.identifier      = identifier;
			this.rangeStart      = rangeStart;
			this.rangeEnd        = rangeEnd;
			this.timeout         = timeout;
			this.readTimeout     = readTimeout;
		}
		
		public abstract Request setURL(URL url);
	}
	
	public static final class GetRequest extends Request {
		
		public GetRequest(URL url) {
			this(url, USER_AGENT, null, null, true);
		}
		
		public GetRequest(URL url, String userAgent) {
			this(url, userAgent, null, null, true);
		}
		
		public GetRequest(URL url, String userAgent, HttpCookie[] cookies) {
			this(url, userAgent, cookies, null, true);
		}
		
		public GetRequest(URL url, String userAgent, Map<String, String> headers) {
			this(url, userAgent, null, headers, true);
		}
		
		public GetRequest(URL url, String userAgent, HttpCookie[] cookies, Map<String, String> headers) {
			this(url, userAgent, cookies, headers, true);
		}
		
		public GetRequest(URL url, String userAgent, HttpCookie[] cookies, Map<String, String> headers,
		                  boolean followRedirects) {
			this(url, userAgent, cookies, headers, followRedirects, null, -1L, -1L);
		}
		
		public GetRequest(URL url, String userAgent, HttpCookie[] cookies, Map<String, String> headers,
		                  boolean followRedirects, String identifier, long rangeStart, long rangeEnd) {
			this(url, userAgent, cookies, headers, followRedirects, identifier, rangeStart, rangeEnd, TIMEOUT);
		}
		
		public GetRequest(URL url, String userAgent, HttpCookie[] cookies, Map<String, String> headers,
		                  boolean followRedirects, String identifier, long rangeStart, long rangeEnd,
		                  int timeout) {
			this(url, userAgent, cookies, headers, followRedirects, identifier, rangeStart, rangeEnd, timeout,
			     READ_TIMEOUT);
		}
		
		public GetRequest(URL url, String userAgent, HttpCookie[] cookies, Map<String, String> headers,
                          boolean followRedirects, String identifier, long rangeStart, long rangeEnd,
                          int timeout, int readTimeout) {
			super(url, RequestMethod.GET, userAgent, null, cookies, headers, followRedirects, identifier,
			      rangeStart, rangeEnd, timeout, null, readTimeout);
		}
		
		public final HeadRequest toHeadRequest() {
			return new HeadRequest(url, userAgent, cookies, headers, followRedirects);
		}
		
		public final GetRequest toRangedRequest(long rangeStart, long rangeEnd) {
			return new GetRequest(url, userAgent, cookies, headers, followRedirects, identifier, rangeStart, rangeEnd);
		}
		
		public final GetRequest toRangedRequest(String identifier, long rangeStart, long rangeEnd) {
			return new GetRequest(url, userAgent, cookies, headers, followRedirects, identifier, rangeStart, rangeEnd);
		}
		
		@Override
		public final GetRequest setURL(URL url) {
			return new GetRequest(url, userAgent, cookies, headers, followRedirects, identifier,
			                      rangeStart, rangeEnd, timeout);
		}
	}
	
	public static final class PostRequest extends Request {
		
		public PostRequest(URL url, Map<String, String> params) {
			this(url, USER_AGENT, params, null, null, true);
		}
		
		public PostRequest(URL url, String userAgent, Map<String, String> params) {
			this(url, userAgent, params, null, null, true);
		}
		
		public PostRequest(URL url, String userAgent, Map<String, String> params, HttpCookie[] cookies) {
			this(url, userAgent, params, cookies, null, true);
		}
		
		public PostRequest(URL url, String userAgent, Map<String, String> params, Map<String, String> headers) {
			this(url, userAgent, params, null, headers, true);
		}
		
		public PostRequest(URL url, String userAgent, Map<String, String> params, HttpCookie[] cookies,
		                   Map<String, String> headers) {
			this(url, userAgent, params, cookies, headers, true);
		}
		
		public PostRequest(URL url, String userAgent, Map<String, String> params, HttpCookie[] cookies,
		                   Map<String, String> headers, boolean followRedirects) {
			this(url, userAgent, params, cookies, headers, followRedirects, null, -1L, -1L);
		}
		
		public PostRequest(URL url, String userAgent, Map<String, String> params, HttpCookie[] cookies,
		                   Map<String, String> headers, boolean followRedirects, String identifier,
		                   long rangeStart, long rangeEnd) {
			this(url, userAgent, params, cookies, headers, followRedirects, identifier, rangeStart, rangeEnd, TIMEOUT);
		}
		
		public PostRequest(URL url, String userAgent, Map<String, String> params, HttpCookie[] cookies,
		                   Map<String, String> headers, boolean followRedirects, String identifier,
		                   long rangeStart, long rangeEnd, int timeout) {
			this(url, userAgent, params, cookies, headers, followRedirects, identifier, rangeStart, rangeEnd, timeout,
			     READ_TIMEOUT);
		}
		
		public PostRequest(URL url, String userAgent, Map<String, String> params, HttpCookie[] cookies,
				           Map<String, String> headers, boolean followRedirects, String identifier,
				           long rangeStart, long rangeEnd, int timeout, String body) {
			this(url, userAgent, params, cookies, headers, followRedirects, identifier, rangeStart, rangeEnd,
			     timeout, body, READ_TIMEOUT);
	    }
		
		public PostRequest(URL url, String userAgent, Map<String, String> params, HttpCookie[] cookies,
		                   Map<String, String> headers, boolean followRedirects, String identifier,
		                   long rangeStart, long rangeEnd, int timeout, int readTimeout) {
			super(url, RequestMethod.POST, userAgent, params, cookies, headers, followRedirects, identifier, rangeStart, rangeEnd,
			      timeout, null, readTimeout);
		}
		
		public PostRequest(URL url, String userAgent, Map<String, String> params, HttpCookie[] cookies,
				           Map<String, String> headers, boolean followRedirects, String identifier,
				           long rangeStart, long rangeEnd, int timeout, String body, int readTimeout) {
			super(url, RequestMethod.POST, userAgent, params, cookies, headers, followRedirects, identifier, rangeStart, rangeEnd,
			      timeout, body, readTimeout);
		}
		
		public final HeadRequest toHeadRequest() {
			return new HeadRequest(url, userAgent, cookies, headers, followRedirects);
		}
		
		public final PostRequest toRangedRequest(long rangeStart, long rangeEnd) {
			return new PostRequest(url, userAgent, params, cookies, headers, followRedirects, identifier, rangeStart, rangeEnd);
		}
		
		public final PostRequest toRangedRequest(String identifier, long rangeStart, long rangeEnd) {
			return new PostRequest(url, userAgent, params, cookies, headers, followRedirects, identifier, rangeStart, rangeEnd);
		}
		
		public final PostRequest toBodyRequest(String body) {
			return new PostRequest(url, userAgent, params, cookies, headers, followRedirects,
			                       identifier, rangeStart, rangeEnd, timeout, body);
		}
		
		@Override
		public final PostRequest setURL(URL url) {
			return new PostRequest(url, userAgent, params, cookies, headers, followRedirects,
			                       identifier, rangeStart, rangeEnd, timeout, body);
		}
	}
	
	public static final class HeadRequest extends Request {
		
		public HeadRequest(URL url) {
			this(url, USER_AGENT, null, null, true);
		}
		
		public HeadRequest(URL url, String userAgent) {
			this(url, userAgent, null, null, true);
		}
		
		public HeadRequest(URL url, String userAgent, HttpCookie[] cookies) {
			this(url, userAgent, cookies, null, true);
		}
		
		public HeadRequest(URL url, String userAgent, Map<String, String> headers) {
			this(url, userAgent, null, headers, true);
		}
		
		public HeadRequest(URL url, String userAgent, HttpCookie[] cookies, Map<String, String> headers) {
			this(url, userAgent, cookies, headers, true);
		}
		
		public HeadRequest(URL url, String userAgent, HttpCookie[] cookies, Map<String, String> headers,
		                   boolean followRedirects) {
			this(url, userAgent, cookies, headers, followRedirects, TIMEOUT);
		}
		
		public HeadRequest(URL url, String userAgent, HttpCookie[] cookies, Map<String, String> headers,
		                   boolean followRedirects, int timeout) {
			this(url, userAgent, cookies, headers, followRedirects, timeout, READ_TIMEOUT);
		}
		
		public HeadRequest(URL url, String userAgent, HttpCookie[] cookies, Map<String, String> headers,
		                   boolean followRedirects, int timeout, int readTimeout) {
			super(url, RequestMethod.HEAD, userAgent, null, cookies, headers, followRedirects, null, -1L, -1L,
			      timeout, null, readTimeout);
		}
		
		@Override
		public final HeadRequest setURL(URL url) {
			return new HeadRequest(url, userAgent, cookies, headers, followRedirects, timeout);
		}
	}
	
	public static class Response {
		
		protected final HttpURLConnection connection;
		
		public final URL                       url;
		public final HttpCookie[]              cookies;
		public final Map<String, List<String>> headers;
		public final int		               code;
		public final String                    identifier;
		
		Response(URL url, HttpCookie[] cookies, Map<String, List<String>> headers, int code,
		         String identifier, HttpURLConnection connection) {
			if(connection == null || url == null)
				throw new IllegalArgumentException();
			this.connection = connection;
			this.url        = url;
			this.cookies    = cookies;
			this.headers    = headers;
			this.code	    = code;
			this.identifier = identifier;
		}
	}
	
	public static final class StringResponse extends Response {
		
		public final String content;
		
		StringResponse(URL url, String content, HttpCookie[] cookies, Map<String, List<String>> headers,
		               int code, String identifier, HttpURLConnection connection) {
			super(url, cookies, headers, code, identifier, connection);
			if(content == null)
				throw new IllegalArgumentException();
			this.content = content;
		}
	}
	
	public static final class StreamResponse extends Response implements AutoCloseable {
		
		public final InputStream stream;
		
		StreamResponse(URL url, InputStream stream, HttpCookie[] cookies, Map<String, List<String>> headers,
		               int code, String identifier, HttpURLConnection connection) {
			super(url, cookies, headers, code, identifier, connection);
			if(stream == null)
				throw new IllegalArgumentException();
			this.stream = stream;
		}
		
		@Override
		public void close() throws Exception {
			closeConnection(connection);
		}
	}
	
	public static final StringResponse request(Request request) throws Exception {
		try(StreamResponse response = requestStream(request)) {
			String content = stream2string(response.stream);
			return new StringResponse(response.url, content, response.cookies, response.headers, 
				response.code, response.identifier, response.connection);
		}
	}
	
	public static final StreamResponse requestStream(Request request) throws Exception {
		ensureCookieManager();
		HttpURLConnection con = openConnection(request.url);
		con.setInstanceFollowRedirects(request.followRedirects);
		con.setRequestMethod(request.method.name());
		con.setRequestProperty("Charset", ENCODING);
		con.setRequestProperty("Connection", "keep-alive");
		if((request.headers != null && !request.headers.isEmpty())) {
			for(Entry<String, String> h : request.headers.entrySet()) {
				con.setRequestProperty(h.getKey(), h.getValue());
			}
		}
		if((request.userAgent != null && !request.userAgent.isEmpty())) {
			con.setRequestProperty("User-Agent", request.userAgent);
		}
		if((request.cookies != null && request.cookies.length > 0)) {
			con.setRequestProperty("Cookie", cookies2string(request.cookies));
		}
		String strParams = null;
		if((request.method == RequestMethod.POST)) {
			con.setDoInput(true);
			if((request.body != null && !request.body.isEmpty())) {
				strParams = request.body;
				con.setRequestProperty("Content-Length", Integer.toString(strParams.length()));
			} else if((request.params != null && !request.params.isEmpty())) {
				strParams = params2string(request.params);
				con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				con.setRequestProperty("Content-Length", Integer.toString(strParams.length()));
			}
		}
		if((request.rangeStart >= 0L && (request.rangeEnd < 0L || request.rangeStart <= request.rangeEnd))) {
			// do not use conditional range request if not really needed
			if((request.identifier != null)) {
				con.setRequestProperty("If-Range", request.identifier);
			}
			con.setRequestProperty("Range", "bytes=" + request.rangeStart + "-" + (request.rangeEnd >= 0L ? request.rangeEnd : ""));
		}
		con.setDoOutput(true);
		con.setUseCaches(false);
		con.setConnectTimeout(request.timeout);
		con.setReadTimeout(request.readTimeout);
		con.connect();
		if((request.method == RequestMethod.POST
				&& strParams != null && !strParams.isEmpty())) {
			con.getOutputStream().write(strParams.getBytes(CHARSET));
		}
		int rescode = con.getResponseCode();
		InputStream cstream = isStatusOK(rescode) ? con.getInputStream() : con.getErrorStream();
		HttpCookie[] arrcook = retrieveCookies(toURI(con.getURL()));
		String identif = getIdentifier(con);
		Map<String, List<String>> hfields = con.getHeaderFields();
		return new StreamResponse(request.url, cstream, arrcook, hfields, rescode, identif, con);
	}
	
	public static final StreamResponse peek(HeadRequest request) throws Exception {
		return requestStream(request);
	}
	
	public static final long size(HeadRequest request) throws Exception {
		ensureCookieManager();
		HttpURLConnection con = openConnection(request.url);
		con.setInstanceFollowRedirects(request.followRedirects);
		con.setRequestMethod("HEAD");
		if((request.headers != null && !request.headers.isEmpty())) {
			for(Entry<String, String> h : request.headers.entrySet()) {
				con.setRequestProperty(h.getKey(), h.getValue());
			}
		}
		if((request.userAgent != null && !request.userAgent.isEmpty())) {
			con.setRequestProperty("User-Agent", request.userAgent);
		}
		if((request.cookies != null && request.cookies.length > 0)) {
			con.setRequestProperty("Cookie", cookies2string(request.cookies));
		}
		con.setDoOutput(true);
		con.setUseCaches(false);
		con.setConnectTimeout(request.timeout);
		con.setReadTimeout(request.readTimeout);
		con.connect();
		long size = con.getContentLengthLong();
		closeConnection(con);
		return size;
	}
	
	public static final long size(Map<String, List<String>> headers) {
		List<String> range = headers.get("Content-Range");
		if((range != null && !range.isEmpty())) {
			String strRange = range.get(0);
			int indRange = strRange.indexOf(' ');
			if((indRange > 0)) {
				String unit = strRange.substring(0, indRange);
				String data = strRange.substring(indRange + 1);
				switch(unit) {
					case "bytes":
						indRange = data.indexOf('/');
						if((indRange > 0)) {
							String theSize = data.substring(indRange + 1);
							if((theSize.equals("*"))) {
								// unknown size, use the range
								String theRange = data.substring(0, indRange);
								indRange = theRange.indexOf('-');
								if((indRange > 0)) {
									String rangeStart = theRange.substring(0, indRange);
									String rangeEnd   = theRange.substring(indRange + 1);
									return Long.parseLong(rangeEnd) - Long.parseLong(rangeStart);
								} // else invalid syntax
							} else {
								return Long.parseLong(theSize);
							}
						} // else invalid syntax
						break;
					default:
						// unsupported units
						break;
				}
			} // else invalid syntax
		}
		// if no Content-Range is specified or is specified but invalid syntax, use Content-Length
		List<String> length = headers.get("Content-Length");
		return length != null && !length.isEmpty() ? Long.parseLong(length.get(0)) : -1L;
	}
	
	public static final HttpCookie[] cookies(String cookies) {
		List<HttpCookie> list = new ArrayList<>();
		if((cookies != null)) {
			String[] split = cookies.split(CHAR_COOKIE_IT_DELIMITER);
			for(String part : split) {
				int index = part.indexOf(CHAR_COOKIE_NV_DELIMITER);
				if((index >= 0)) {
					String name  = part.substring(0, index);
					String value = part.substring(index+1);
					list.add(new HttpCookie(name, value));
				}
			}
		}
		return list.toArray(new HttpCookie[list.size()]);
	}
	
	/** @since 00.01.27 */
	public static final HttpCookie[] cookies(Map<String, List<String>> map) {
		return map.entrySet().stream()
					.flatMap((entry) -> entry.getValue().stream().map((e) -> new HttpCookie(entry.getKey(), e)))
					.toArray(HttpCookie[]::new);
	}
	
	public static final Map<String, String> headers(String data) {
		if(data == null) return null; // Do not continue
		
		Map<String, String> map = new LinkedHashMap<>();
		boolean dq = false;
		boolean sq = false;
		String 		  tn = null;
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0, l = data.length(), c, n; i < l; i += n) {
			c = data.codePointAt(i);
			n = Utils.charCount(c);
			
			// Quotes logic
			if(c == CHAR_QUOTES_DOUBLE && !sq) dq = !dq; else
			if(c == CHAR_QUOTES_SINGLE && !dq) sq = !sq;
			
			// Extract logic
			else {
				if(!sq && !dq) {
					if(c == CHAR_HEADER_NV_DELIMITER) {
						tn = sb.toString();
						sb.setLength(0);
					} else if(c == CHAR_HEADER_IT_DELIMITER) {
						if(tn != null) {
							map.put(tn, sb.toString());
							sb.setLength(0);
							tn = null;
						}
					} else {
						sb.appendCodePoint(c);
					}
				} else {
					sb.appendCodePoint(c);
				}
			}
		}
		
		if(tn != null) {
			map.put(tn, sb.toString());
		}
		
		return map;
	}
	
	public static final void clear() {
		if((COOKIE_MANAGER != null))
			COOKIE_MANAGER.getCookieStore().removeAll();
		COOKIE_MANAGER = null;
	}
	
	/** @since 00.01.27 */
	public static final class Cookies {
		
		private static CookieHandler HANDLER = CookieHandler.getDefault();
		
		public static final void setHandler(CookieHandler handler) {
			CookieHandler.setDefault(HANDLER = handler);
		}
		
		public static final CookieHandler getHandler() {
			return HANDLER;
		}
		
		public static final void set(URI uri, Map<String, List<String>> cookies) throws IOException {
			HANDLER.put(uri, cookies);
		}
		
		public static final void add(URI uri, Map<String, String> cookies) throws IOException {
			Map<String, List<String>> headers = new LinkedHashMap<>(get(uri));
			List<String> mapped = cookies.entrySet().stream()
					.map((e) -> String.format("%s=%s", e.getKey(), e.getValue()))
					.collect(Collectors.toList());
			headers.put("Set-Cookie", mapped);
			set(uri, headers);
		}
		
		public static final void add(URI uri, String name, String value) throws IOException {
			Map<String, List<String>> headers = new LinkedHashMap<>(get(uri));
			headers.put("Set-Cookie", Arrays.asList(String.format("%s=%s", name, value)));
			set(uri, headers);
		}
		
		public static final Map<String, List<String>> get(URI uri) throws IOException {
			return HANDLER.get(uri, Collections.emptyMap());
		}
		
		private static final Pair<String, String> stringToCookie(String string) {
			int index;
			return (index = string.indexOf(CHAR_COOKIE_NV_DELIMITER)) >= 0
						? new Pair<>(string.substring(0, index), string.substring(index + 1))
						: null;
		}
		
		public static final Map<String, List<String>> getCookie(URI uri) throws IOException {
			List<String> cookies = get(uri).get("Cookie");
			Map<String, List<String>> map = new LinkedHashMap<>();
			if((cookies == null)) return map;
			cookies.stream().map(Cookies::stringToCookie)
				.filter((c) -> c != null)
				.forEach((p) -> map.computeIfAbsent(p.a, (k) -> new ArrayList<>()).add(p.b));
			return map;
		}
		
		public static final void clear(URI uri) throws IOException {
			HANDLER.put(uri, Collections.emptyMap());
		}
	}
}