package sune.app.mediadown.util;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javafx.scene.paint.Color;
import javafx.util.Callback;
import sune.app.mediadown.Shared;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.util.Web.GetRequest;
import sune.app.mediadown.util.Web.StreamResponse;

public final class Utils {
	
	private static final String  USER_AGENT = Shared.USER_AGENT;
	private static final Charset CHARSET    = Shared.CHARSET;
	private static final char[]  INVALID_FILE_NAME_CHARS = { '<', '>', ':', '"', '/', '\\', '|', '?', '*' };
	private static final String  REGEX_INVALID_FILE_NAME_CHARS;
	private static final char    URL_DELIMITER = '/';
	
	/** @since 00.02.05 */
	private static final String ALPHABET_ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	/** @since 00.02.05 */
	private static Random RANDOM;
	/** @since 00.02.05 */
	private static final Map<Character, Integer> ROMAN_NUMERALS = Map.of(
		'I', 1, 'V', 5, 'X', 10, 'L', 50, 'C', 100, 'D', 500, 'M', 1000
	);
	
	static {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for(char c : INVALID_FILE_NAME_CHARS) {
			if((c == '\\')) {
				// write the character twice
				sb.append(c);
			}
			sb.append(c);
		}
		sb.append(']');
		REGEX_INVALID_FILE_NAME_CHARS = sb.toString();
	}
	
	private Utils() {
	}
	
	// --- Public methods ---
	
	private static final String jsoup_baseUri(URI baseUri) {
		return baseUri == null ? "" : baseUri.toString();
	}
	
	public static final Document document(String url) {
		return document(uri(url));
	}
	
	/** @since 00.02.05 */
	public static final Document document(URL url) {
		return document(uri(url));
	}
	
	/** @since 00.02.05 */
	public static final Document document(URI uri) {
		uri = uri.normalize();
		try(StreamResponse response = Web.requestStream(new GetRequest(uri.toURL(), USER_AGENT))) {
			return Jsoup.parse(response.stream, CHARSET.name(), jsoup_baseUri(uri));
		} catch(Exception ex) {
			// Ignore
		}
		return null;
	}
	
	public static final Document parseDocument(String content) {
		return parseDocument(content, (URI) null);
	}
	
	public static final Document parseDocument(String content, String baseUri) {
		return parseDocument(content, uri(baseUri));
	}
	
	/** @since 00.02.05 */
	public static final Document parseDocument(String content, URL baseUri) {
		return parseDocument(content, uri(baseUri));
	}
	
	/** @since 00.02.05 */
	public static final Document parseDocument(String content, URI baseUri) {
		return Jsoup.parse(content, jsoup_baseUri(baseUri));
	}
	
	public static final <T> T[] toArray(Collection<T> collection, Class<? extends T> clazz) {
		if((collection == null || clazz == null))
			throw new IllegalArgumentException();
		@SuppressWarnings("unchecked")
		T[] array = (T[]) Array.newInstance(clazz, collection.size());
		return collection.toArray(array);
	}
	
	public static final String extractName(String url) {
		if(!isValidURL(url)) return null;
		if((url.endsWith("/"))) {
			url = url.substring(0, url.length()-1);
		}
		url = url.substring(url.lastIndexOf("/")+1);
		url = url.substring(url.indexOf("-")+1);
		url = url.replace("-", " ");
		url = url.substring(0, 1).toUpperCase() + url.substring(1);
		return validateFileName(url);
	}
	
	public static final String format(String string, Object... values) {
		if((values.length & 1) != 0) return null;
		Map<String, Object> map = new LinkedHashMap<>();
		String  n = null;
		boolean f = false;
		for(int i = 0, l = values.length; i < l; ++i) {
			Object v = values[i];
			if(f = !f) n = v.toString();
			else       map.put(n, v);
		}
		return format(string, map);
	}
	
	public static final String format(String string, Map<String, Object> values) {
		StringBuilder sb   = new StringBuilder(string);
		Set<String>   keys = values.keySet();
		List<Object>  list = new ArrayList<>();
		int pos = 1;
		for(String key : keys) {
			String fkey = "%{" + key + "}";
			String fpos = "%"  + pos + "$";
			int index = -1;
			while((index = sb.indexOf(fkey, index)) != -1) {
				sb.replace(index, index + fkey.length(), fpos);
				index += fpos.length();
			}
			list.add(values.get(key));
			++pos;
		}
		return String.format(sb.toString(), list.toArray());
	}
	
	public static final String validateFileName(String fileName) {
		return fileName.replaceAll(REGEX_INVALID_FILE_NAME_CHARS, "");
	}
	
	public static final String dirname(String url) {
		int index = url.lastIndexOf('/');
		return index >= 0 ? url.substring(0, index) : url;
	}
	
	public static final String basename(String url) {
		int index = url.lastIndexOf('/');
		return index >= 0 ? url.substring(index+1) : url;
	}
	
	public static final String urlBasename(String url) {
		return removeURLData(basename(url));
	}
	
	public static final String urlConcat(String... parts) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0, l = parts.length; i < l; ++i) {
			if(i != 0) sb.append(URL_DELIMITER);
			sb.append(parts[i]);
		}
		return urlFixSlashes(sb.toString());
	}
	
	public static final String urlFixSlashes(String url) {
		return url.replaceAll("([^:])//+", "$1/");
	}
	
	public static final String urlDirname(String url) {
		int schema = url.indexOf("://");
		int index  = -1;
		if((schema >= 0)) {
			String subStr = url.substring(schema + 3);
			index = subStr.lastIndexOf(URL_DELIMITER);
			if((index >= 0)) {
				index += url.length() - subStr.length();
			}
		} else {
			index = url.lastIndexOf(URL_DELIMITER);
		}
		return index >= 0 ? url.substring(0, index) : url;
	}
	
	public static final InputStream urlStream(String url, int timeout) throws IOException {
		URLConnection con = new URL(url).openConnection();
		con.setConnectTimeout(timeout);
		return con.getInputStream();
	}
	
	public static final double convertToSeconds(String string) {
		double val  = 0.0;
		double mult = 60.0 * 60.0;
		int index = string.indexOf(':'), k;
		if((index > -1)) {
			// Hours
			String hour = string.substring(0, index);
			val += Integer.parseInt(hour) * mult;
			mult /= 60.0;
			index = string.indexOf(':', (k = index+1));
			if((index > -1)) {
				// Minutes
				String mins = string.substring(k, index);
				val += Integer.parseInt(mins) * mult;
				index = string.indexOf(':', (k = index));
				if((index > -1)) {
					// Seconds
					String secs = string.substring(k+1);
					val += Double.parseDouble(secs);
				}
			}
		}
		return val;
	}
	
	public static final double convertToMilliseconds(String string) {
		return convertToSeconds(string) * 1e3;
	}
	
	public static final void combineFiles(List<Path> paths, Path dest) {
		try {
			Files.deleteIfExists(dest);
		} catch(IOException ex) {
		}
		try(FileChannel out = FileChannel.open(dest, CREATE, APPEND)) {
			for(Path path : paths) {
				try(FileChannel ch = FileChannel.open(path, READ)) {
					ch.transferTo(0L, ch.size(), out);
				}
			}
		} catch(IOException ex) {
		}
	}
	
	public static final String urlFix(String url) {
		return urlFix(url, false);
	}
	
	public static final String urlFix(String url, boolean useHTTPS) {
		return url.startsWith("//") ? "http" + (useHTTPS ? "s:" : ":") + url : url;
	}
	
	public static final String fileName(String path) {
		return basename(path.replace('\\', '/'));
	}
	
	public static final String fileType(String path) {
		String name = removeURLData(fileName(path));
		int index = name.lastIndexOf('.');
		return index >= 0 ? name.substring(index+1) : null;
	}
	
	public static final String fileNameNoType(String path) {
		String name = fileName(path);
		int index = name.lastIndexOf('.');
		return index >= 0 ? name.substring(0, index) : name;
	}
	
	public static final String removeURLData(String url) {
		int index = url.lastIndexOf('?');
		return index >= 0 ? url.substring(0, index) : url;
	}
	
	// Provides realtively fast method for converting a string into an integer
	// not using any built-in function and ignoring all other characters that
	// are not defined as digits.
	public static final int extractInt(String string) {
		if(string == null || string.isEmpty()) {
			throw new IllegalArgumentException(
				"The given string cannot be null nor empty!");
		}
		boolean has	 = false;
		boolean neg  = false;
		int value 	 = 0;
		int temp  	 = 0;
		char[] chars = string.toCharArray();
		for(int i = 0, l = chars.length, c; i < l; ++i) {
			if((c 	 = chars[i]) == '-') neg = true; else
			if((temp = Character.digit(c, 10)) != -1) {
				int val = (value * 10) - temp;
				if((val > value) || (!neg && val == Integer.MIN_VALUE)) {
					throw new IllegalArgumentException(
						"The given string contains number outside of " +
						"the range of a signed integer!");
				}
				value = val;
				has   = true;
			}
		}
		if(!has) {
			throw new IllegalArgumentException(
				"The given string does not contain any digit!");
		}
		return neg ? value : -value;
	}
	
	public static final URI uri(String uri) {
		return URI.create(uri);
	}
	
	public static final URL url(String url) {
		try {
			return new URL(url);
		} catch(MalformedURLException ex) {
			// Ignore
		}
		return null;
	}
	
	public static final URL url(URI uri) {
		try {
			return uri.toURL();
		} catch(MalformedURLException ex) {
			// Ignore
		}
		return null;
	}
	
	/** @since 00.02.00 */
	public static final boolean isValidURL(String url) {
		return url != null && !url.isEmpty() && ignore(() -> { return new URL(url).toURI(); }, (URI) null) != null;
	}
	
	public static final <T> int indexOf(T needle, T[] haystack) {
		if((haystack == null || haystack.length == 0))
			return -1;
		if((needle == null)) {
			for(int i = 0, l = haystack.length; i < l; ++i) {
				if((haystack[i] == null))
					return i;
			}
		} else {
			for(int i = 0, l = haystack.length; i < l; ++i) {
				if((haystack[i] != null
						&& haystack[i].equals(needle)))
					return i;
			}
		}
		return -1;
	}
	
	@SafeVarargs
	public static final <T> T[] array(T... items) {
		return items;
	}
	
	public static final <T> T[] arrayOfSize(Class<? extends T> clazz, int size) {
		@SuppressWarnings("unchecked")
		T[] array = (T[]) Array.newInstance(clazz, size);
		return array;
	}
	
	public static final <T> T[] subArray(T[] array, int from) {
		return subArray(array, from, array.length);
	}
	
	public static final <T> T[] subArray(T[] array, int from, int to) {
		return Arrays.copyOfRange(array, from, to);
	}
	
	public static final Map<String, Object> stringKeyMap(Object... data) {
		if((data == null))
			throw new IllegalArgumentException("Data cannot be null");
		Map<String, Object> map = new LinkedHashMap<>();
		for(int i = 0, l = data.length; i < l; i+=2) {
			map.put(data[i] != null ? data[i].toString() : null, data[i+1]);
		}
		return map;
	}
	
	public static final String addFormatExtension(String name, MediaFormat format) {
		return name + (!format.fileExtensions().isEmpty() ? '.' + format.fileExtensions().get(0) : "");
	}
	
	public static final void visitURL(String url) {
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch(URISyntaxException |
				IOException ex) {
		}
	}
	
	public static final String num2string(double value, int decimals) {
		if(!Double.isFinite(value) || decimals < 0)
			return "?";
		if((decimals == 0)) {
			// round the integer correctly
			return Integer.toString((int) (value + 0.5));
		}
		double tens    = Math.pow(10, decimals);
		double rounded = Math.floor(value * tens + 0.5) / tens;
		return String.format("%." + decimals + "f", rounded)
					 // replace commas by dots
					 .replace(',', '.');
	}
	
	private static final String[] SIZE_TYPE = { "B", "kB", "MB", "GB" };
	public  static final String formatSize(double bytes, int decimals) {
		int    type = 0;
		double size = bytes;
		while((bytes /= 1024.0) >= 1.0) {
			size = bytes;
			if((++type == SIZE_TYPE.length - 1
					&& size >= 1024.0))
				break;
		}
		return num2string(size, decimals) + SIZE_TYPE[type];
	}
	
	// ---------------- [BEGIN] Base64 Decode
	
	private static Decoder DECODER_BASE64;
	private static final Decoder base64Decoder() {
		if((DECODER_BASE64 == null)) {
			DECODER_BASE64 = Base64.getDecoder();
		}
		return DECODER_BASE64;
	}
	
	private static Decoder DECODER_BASE64_URL;
	private static final Decoder base64URLDecoder() {
		if((DECODER_BASE64_URL == null)) {
			DECODER_BASE64_URL = Base64.getUrlDecoder();
		}
		return DECODER_BASE64_URL;
	}
	
	public static final byte[] base64DecodeRaw(String string) {
		return base64Decoder().decode(string.getBytes(CHARSET));
	}
	
	public static final String base64Decode(String string) {
		return new String(base64DecodeRaw(string), CHARSET);
	}
	
	public static final byte[] base64URLDecodeRaw(String string) {
		return base64URLDecoder().decode(string.getBytes(CHARSET));
	}
	
	public static final String base64URLDecode(String string) {
		return new String(base64URLDecodeRaw(string), CHARSET);
	}
	
	// ---------------- [END]   Base64 Decode
	
	// ---------------- [BEGIN] Base64 Encode
	
	private static Encoder ENCODER_BASE64;
	private static final Encoder base64Encoder() {
		if((ENCODER_BASE64 == null)) {
			ENCODER_BASE64 = Base64.getEncoder();
		}
		return ENCODER_BASE64;
	}
	
	private static Encoder ENCODER_BASE64_URL;
	private static final Encoder base64URLEncoder() {
		if((ENCODER_BASE64_URL == null)) {
			ENCODER_BASE64_URL = Base64.getUrlEncoder();
		}
		return ENCODER_BASE64_URL;
	}
	
	public static final byte[] base64EncodeRaw(String string) {
		return base64Encoder().encode(string.getBytes(CHARSET));
	}
	
	public static final String base64Encode(String string) {
		return new String(base64EncodeRaw(string), CHARSET);
	}
	
	public static final byte[] base64URLEncodeRaw(String string) {
		return base64URLEncoder().encode(string.getBytes(CHARSET));
	}
	
	public static final String base64URLEncode(String string) {
		return new String(base64URLEncodeRaw(string), CHARSET);
	}
	
	// ---------------- [END]   Base64 Encode
	
	// ---------------- [BEGIN] URL utilities
	
	public static final Map<String, String> urlParams(String params) {
		Map<String, String> map = new LinkedHashMap<>();
		int queryIndex;
		if((queryIndex = params.indexOf('?')) >= 0)
			params = params.substring(queryIndex + 1);
		for(String part : params.split("&")) {
			int index;
			if((index = part.indexOf('=')) > -1) {
				map.put(part.substring(0, index), decodeURL(part.substring(index+1)));
			}
		}
		return map;
	}
	
	public static final class URLParameter {
		
		private final String name;
		private final Object value;
		private final boolean isArray;
		
		private URLParameter(String name, Object value, boolean isArray) {
			this.name    = name;
			this.value   = value;
			this.isArray = isArray;
		}
		
		public static final URLParameter createString(String name, String value) {
			return new URLParameter(name, value, false);
		}
		
		public static final URLParameter createArray(String name, Map<String, URLParameter> value) {
			return new URLParameter(name, value, true);
		}
		
		public static final URLParameter createArray(String name) {
			return createArray(name, new LinkedHashMap<>());
		}
		
		public String getName() {
			return name;
		}
		
		public Object getValue() {
			return value;
		}
		
		public String stringValue() {
			if((isArray))
				throw new IllegalStateException("Parameter is an array");
			return (String) value;
		}
		
		public Map<String, URLParameter> arrayValue() {
			if(!isArray)
				throw new IllegalStateException("Parameter is a string");
			@SuppressWarnings("unchecked")
			Map<String, URLParameter> map = (Map<String, URLParameter>) value;
			return map;
		}
		
		public boolean isArray() {
			return isArray;
		}
		
		@Override
		public String toString() {
			return value.toString();
		}
	}
	
	private static final void insertURLParameter(String name, String value, Map<String, URLParameter> parent) {
		if((name.indexOf('[')) > 0) {
			String valName = name;
			for(int ii = 0, is = 1, ie; is > 0; ii = is + 1) {
				is = name.indexOf('[', ii);
				ie = name.indexOf(']', ii);
				if((is < 0)) {
					valName = name.substring(ii, ie);
					break;
				}
				String subName = name.substring(ii, Math.min(ie, is));
				URLParameter newParent = parent.computeIfAbsent(subName, (key) -> URLParameter.createArray(subName));
				parent = newParent.arrayValue();
			}
			name = valName;
		}
		parent.put(name, URLParameter.createString(name, value));
	}
	
	private static final void parseURLParameter(String param, Map<String, URLParameter> parent) {
		int index;
		if((index = param.indexOf('=')) <= 0)
			return;
		String name  = decodeURL(param.substring(0, index));
		String value = decodeURL(param.substring(index+1));
		insertURLParameter(name, value, parent);
	}
	
	public static final Map<String, URLParameter> urlParamsExtended(String url) {
		Map<String, URLParameter> params = new LinkedHashMap<>();
		int queryIndex;
		if((queryIndex = url.indexOf('?')) >= 0)
			url = url.substring(queryIndex + 1);
		for(String part : url.split("&"))
			parseURLParameter(part, params);
		return params;
	}
	
	private static final void convertArrayURLParameterToString(StringBuilder sb, String parentName, URLParameter param) {
		boolean first = true;
		for(URLParameter urlp : param.arrayValue().values()) {
			if((first)) first = false; else sb.append("&");
			String fullName = parentName + '[' + urlp.getName() + ']';
			if((urlp.isArray())) {
				convertArrayURLParameterToString(sb, fullName, urlp);
			} else {
				sb.append(fullName)
				  .append('=')
				  .append(encodeURL(urlp.stringValue()));
			}
		}
	}
	
	public static final String joinURLParamsExtended(Map<String, URLParameter> params) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for(Entry<String, URLParameter> entry : params.entrySet()) {
			if((first)) first = false; else sb.append("&");
			URLParameter param = entry.getValue();
			if((param.isArray())) {
				convertArrayURLParameterToString(sb, param.getName(), param);
			} else {
				sb.append(entry.getKey())
				  .append('=')
				  .append(encodeURL(param.stringValue()));
			}
		}
		return sb.toString();
	}
	
	public static final String joinURLParams(Map<String, String> params) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for(Entry<String, String> entry : params.entrySet()) {
			if((first)) first = false; else sb.append("&");
			sb.append(entry.getKey())
			  .append("=")
			  .append(encodeURL(entry.getValue()));
		}
		return sb.toString();
	}
	
	public static final String encodeURL(String url) {
		try {
			return URLEncoder.encode(url, CHARSET.name());
		} catch(Exception ex) {
		}
		return null;
	}
	
	public static final String decodeURL(String url) {
		try {
			return URLDecoder.decode(url, CHARSET.name());
		} catch(Exception ex) {
		}
		return null;
	}
	
	// ---------------- [END]   URL utilities
	
	// ---------------- [BEGIN] String utilities
	
	public static final byte[] hexStringToArray(String string) {
		char[] chars = string.toCharArray();
		int length 	 = chars.length;
		byte[] data  = new byte[length / 2];
		for(int i = 0, k = 0; i < length; i+=2, ++k) {
			data[k] = (byte) ((Character.digit(chars[i],   16) << 4)
							 + Character.digit(chars[i+1], 16));
		}
		return data;
	}
	
	public static final String repeat(String string, int count) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < count; ++i)
			sb.append(string);
		return sb.toString();
	}
	
	public static final String replaceAll(String regex, String string, Callback<MatchResult, String> callback) {
		return replaceAll(Pattern.compile(regex), string, callback);
	}
	
	/** @since 00.02.05 */
	public static final String replaceAll(Pattern regex, String string, Callback<MatchResult, String> callback) {
		Matcher matcher = regex.matcher(string);
		while(matcher.find()) {
			MatchResult matchResult = matcher.toMatchResult();
			String before = string.substring(0, matchResult.start());
			String replacement = callback.call(matchResult);
			String after = string.substring(matchResult.end());
			string = before + replacement + after;
			matcher.reset(string);
			matcher.region(before.length() + replacement.length(), string.length());
		}
		return string;
	}
	
	public static final Pair<Integer, Integer> stringBetween(String string, char chOpen, char chClose) {
		return stringBetween(string, chOpen, chClose, 0, string.length());
	}
	
	public static final Pair<Integer, Integer> stringBetween(String string, char chOpen, char chClose, int start, int end) {
		int count = 0;
		for(int i = start, c; i < end; ++i) {
			c = string.charAt(i);
			if((c == chOpen)) {
				if((count == 0)) {
					start  = i;
				}
				++count;
			} else if((c == chClose)) {
				--count;
				if((count == 0)) {
					return new Pair<>(start, i + 1);
				}
			}
		}
		return new Pair<>(start, end);
	}
	
	public static final Pair<Integer, Integer> stringBetweenReverse(String string, char chOpen, char chClose) {
		return stringBetweenReverse(string, chOpen, chClose, string.length() - 1, -1);
	}
	
	public static final Pair<Integer, Integer> stringBetweenReverse(String string, char chOpen, char chClose, int start, int end) {
		int count = 0;
		for(int i = start, c; i > end; --i) {
			c = string.charAt(i);
			if((c == chClose)) {
				if((count == 0)) {
					start  = i;
				}
				++count;
			} else if((c == chOpen)) {
				--count;
				if((count == 0)) {
					return new Pair<>(i, start + 1);
				}
			}
		}
		return new Pair<>(0, start + 1);
	}
	
	public static final String bracketSubstring(String string) {
		return bracketSubstring(string, '(', ')', false);
	}
	
	public static final String bracketSubstring(String string, boolean reverse) {
		return bracketSubstring(string, '(', ')', reverse);
	}
	
	public static final String bracketSubstring(String string, char chOpen, char chClose) {
		return bracketSubstring(string, chOpen, chClose, false);
	}
	
	public static final String bracketSubstring(String string, char chOpen, char chClose, boolean reverse) {
		Pair<Integer, Integer> range
			= reverse
				? stringBetweenReverse(string, chOpen, chClose)
				: stringBetween(string, chOpen, chClose);
		return string.substring(range.a, range.b);
	}
	
	public static final String bracketSubstring(String string, char chOpen, char chClose, boolean reverse, int start, int end) {
		Pair<Integer, Integer> range
			= reverse
				? stringBetweenReverse(string, chOpen, chClose, start, end)
				: stringBetween(string, chOpen, chClose, start, end);
		return string.substring(range.a, range.b);
	}
	
	public static final String substringTo(String string, char ch) {
		int index = string.indexOf(ch);
		return index >= 0 ? string.substring(0, index) : string;
	}
	
	private static Pattern PATTERN_U4D;
	private static final void ensurePatternU4D() {
		if((PATTERN_U4D == null)) {
			PATTERN_U4D = Pattern.compile("\\\\u(\\p{XDigit}{4})");
		}
	}
	
	public static final String replaceUnicode4Digits(String string) {
		ensurePatternU4D();
		StringBuilder builder = new StringBuilder(string.length());
		Matcher matcher = PATTERN_U4D.matcher(string);
		int position = 0;
		while(matcher.find()) {
			builder.append(string, position, matcher.start());
			String character = fromCharCode(Integer.parseInt(matcher.group(1), 16));
			builder.append(character);
			position = matcher.end();
		}
		builder.append(string, position, string.length());
		return builder.toString();
	}
	
	public static final String prefixUnicode4Digits(String string, String prefix) {
		ensurePatternU4D();
		StringBuilder builder = new StringBuilder(string.length());
		Matcher matcher = PATTERN_U4D.matcher(string);
		int position = 0;
		while(matcher.find()) {
			builder.append(string, position, matcher.start());
			builder.append(prefix);
			builder.append(matcher.group(0));
			position = matcher.end();
		}
		builder.append(string, position, string.length());
		return builder.toString();
	}
	
	public static final String fromCharCode(int... codePoints) {
		return new String(codePoints, 0, codePoints.length);
	}
	
	public static final int backTill(String string, char ch, int from) {
		return backTill(string, ch, from, 1);
	}
	
	public static final int backTill(String string, char ch, int from, int count) {
		for(int i = from, c; i >= 0; --i) {
			c = string.charAt(i);
			if((c == ch)) {
				if((--count == 0))
					return i;
			}
		}
		return 0;
	}
	
	public static final String backTillString(String string, char ch, int from) {
		return backTillString(string, ch, from, 1);
	}
	
	public static final String backTillString(String string, char ch, int from, int count) {
		int start = backTill(string, ch, from, count);
		return string.substring(start, from + 1);
	}
	
	public static final String trimEnd(String string, char ch) {
		int end = string.length();
		for(int i = end - 1, c; i >= 0; --i) {
			c = string.charAt(i);
			if((c == ch)) --end;
			else          break;
		}
		return string.substring(0, end);
	}
	
	public static final int indexOf(String string, String substring, int iter) {
		if((iter <= 0)) return -1;
		int start = 0, index;
		do {
			index = string.indexOf(substring, start);
			if((index >= 0)) {
				start = index + substring.length();
				if((--iter == 0)) return index;
			}
		} while(index >= 0 && iter > 0);
		// when the substring is not found or out of iterations
		return -1;
	}
	
	// ---------------- [END]   String utilities
	
	// ---------------- [BEGIN] Miscellaneous utilities
	
	public static final <T> List<T> lastItems(Collection<T> coll, int count) {
		List<T> list = new ArrayList<>();
		int size = coll.size(), index = 0, start = size - count;
		for(Iterator<T> it = coll.iterator(); it.hasNext();) {
			T item = it.next();
			if((index++ >= start))
				list.add(item);
		}
		return list;
	}
	
	public static final long getUnixTime(String zone) {
		return Instant.now(Clock.tickSeconds(ZoneId.of(zone))).getEpochSecond();
	}
	
	public static final Class<?>[] recognizeClasses(Object... arguments) {
		int length 		   = arguments.length;
		Class<?>[] classes = new Class<?>[length];
		for(int i = 0; i < length; ++i) {
			Class<?> clazz = arguments[i].getClass();
			classes[i] 	   = toPrimitive(clazz);
		}
		return classes;
	}
	
	public static final Class<?> toPrimitive(Class<?> clazz) {
		if(clazz == Boolean.class) 	 return boolean.class;
		if(clazz == Byte.class) 	 return byte.class;
		if(clazz == Character.class) return char.class;
		if(clazz == Short.class) 	 return short.class;
		if(clazz == Integer.class) 	 return int.class;
		if(clazz == Long.class) 	 return long.class;
		if(clazz == Float.class) 	 return float.class;
		if(clazz == Double.class) 	 return double.class;
		if(clazz == Void.class) 	 return void.class;
		return clazz;
	}
	
	public static final int[] intArray(Integer[] a) {
		int   l = a.length;
		int[] b = new int[l];
		for(int i = 0; i < l; ++i)
			b[i] = a[i];
		return b;
	}
	
	// ---------------- [END]   Miscellaneous utilities
	
	@SuppressWarnings("unchecked")
	public static final <K, V> Map<K, V> toMap(Object... objects) {
		if((objects.length & 1) == 1)
			throw new IllegalArgumentException("Objects count must be even");
		Map<K, V> map = new HashMap<>();
		K key = null;
		V val = null;
		for(int i = 0, l = objects.length; i < l; ++i) {
			if((key == null)) {
				key = (K) objects[i];
			} else {
				val = (V) objects[i];
				map.put(key, val);
				key = null;
			}
		}
		return map;
	}
	
	public static final <T, E> E getItemFromArray(T item, E[] array, Function<E, T> converter) {
		E e; T t;
		for(int i = 0, l = array.length; i < l; ++i) {
			e = array[i];
			t = converter.apply(e);
			if((t == null && item == null)
					|| (t != null && t.equals(item)))
				return e;
		}
		return null;
	}
	
	@SafeVarargs
	public static final <T> T[] array(Class<? extends T> clazz, T... items) {
		@SuppressWarnings("unchecked")
		T[] array = (T[]) Array.newInstance(clazz, items.length);
		for(int i = 0, l = items.length; i < l; ++i)
			array[i] = items[i];
		return array;
	}
	
	public static final boolean isRelativeURL(String url) {
		// this determines the protocol definition
		int index = url.indexOf("://");
		// at least one character for the protocol
		// at least one character for the path
		return index <= 0 || index >= url.length() - 3;
	}
	
	public static final <T> T loopTo(Iterator<T> iterator, int index) {
		int current = 0;
		T item;
		for(; iterator.hasNext(); ++current) {
			item = iterator.next();
			if((current == index))
				return item;
		}
		return null;
	}
	
	public static final String webColor(Color color) {
		return String.format("rgba(%d, %d, %d, %.2f)",
							 (int) (color.getRed()   * 255.0),
							 (int) (color.getGreen() * 255.0),
							 (int) (color.getBlue()  * 255.0),
							 color.getOpacity());
	}
	
	public static final boolean startsWithIgnoreCase(String string, String value) {
		return string.regionMatches(true, 0, value, 0, value.length());
	}
	
	public static final boolean classExists(String className) {
		try { Class.forName(className); return true; } catch(ClassNotFoundException ex) { return false; }
	}
	
	public static final Class<?> getClassNoException(String className) {
		try { return Class.forName(className); } catch(ClassNotFoundException ex) { return null; }
	}
	
	@SafeVarargs
	public static final <T> T[] arrayPrepend(T item, T... items) {
		@SuppressWarnings("unchecked")
		T[] array = (T[]) Array.newInstance(items.getClass().getComponentType(), items.length + 1);
		array[0] = item;
		System.arraycopy(items, 0, array, 1, items.length);
		return array;
	}
	
	@SafeVarargs
	public static final <T> T[] arrayAppend(T item, T... items) {
		@SuppressWarnings("unchecked")
		T[] array = (T[]) Array.newInstance(items.getClass().getComponentType(), items.length + 1);
		System.arraycopy(items, 0, array, 0, items.length);
		array[array.length - 1] = item;
		return array;
	}
	
	@SafeVarargs
	public static final <T> List<T> toList(T... items) {
		return Arrays.asList(items);
	}
	
	@SafeVarargs
	public static final <T> List<T> toModifiableList(T... items) {
		return new ArrayList<>(toList(items));
	}
	
	// inspired by java.util.AbstractCollection::contains(Object)
	public static final <T> boolean contains(T[] array, T item) {
		if((array == null || array.length == 0))
			return false;
		if((item == null)) {
			for(int i = 0, l = array.length; i < l; ++i) {
				if((item == array[i]))
					return true;
			}
		} else {
			for(int i = 0, l = array.length; i < l; ++i) {
				if((item.equals(array[i])))
					return true;
			}
		}
		return false;
	}
	
	@SafeVarargs
	public static final <T> String join(String joint, T... array) {
		return join(joint, array, 0, array.length);
	}
	
	public static final <T> String join(String joint, T[] array, int off) {
		return join(joint, array, off, array.length);
	}
	
	public static final <T> String join(String joint, T[] array, int off, int len) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		Object item;
		for(int i = off, l = Math.min(len, array.length); i < l; ++i) {
			item = array[i];
			if((first)) {
				first = false;
			} else {
				builder.append(joint);
			}
			if((item != null))
				builder.append(item.toString());
		}
		return builder.toString();
	}
	
	public static final int count(String string, int codePoint) {
		return (int) string.codePoints().filter((i) -> i == codePoint).count();
	}
	
	public static final String baseURL(String url) {
		String baseURL = Utils.urlDirname(url);
		if(!baseURL.endsWith("/")) baseURL += '/';
		return baseURL;
	}
	
	public static final InputStream stringStream(String string) {
		return new ByteArrayInputStream(string.getBytes(Shared.CHARSET));
	}
	
	public static final String streamToString(InputStream stream) {
		try {
			return new String(StreamUtils.readAllBytes(stream), Shared.CHARSET);
		} catch(IOException ex) {
			// Ignore
		}
		return null;
	}
	
	public static final String removeStringQuotes(String string) {
		return !(string = string.trim()).isEmpty()
					? string.substring(string.startsWith("\"") ? 1 : 0, string.length() - (string.endsWith("\"") ? 1 : 0))
					: string;
	}
	
	public static final String normalize(String string) {
		return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	}
	
	public static final Object[] merge(Object[] array, Object... others) {
		Object[] newArray = new Object[array.length + others.length];
		System.arraycopy(array,  0, newArray, 0,            array .length);
		System.arraycopy(others, 0, newArray, array.length, others.length);
		return newArray;
	}
	
	public static final String repeat(String string, int count, String delimiter) {
		if((string == null || count < 0))
			return null;
		if((count == 0))
			return "";
		if((string.isEmpty()))
			return repeat(delimiter, count, null);
		StringBuilder builder = new StringBuilder();
		if((delimiter == null)) {
			for(int i = 0; i < count; ++i) {
				builder.append(string);
			}
		} else {
			builder.append(string);
			for(int i = 1; i < count; ++i) {
				builder.append(delimiter);
				builder.append(string);
			}
		}
		return builder.toString();
	}
	
	public static final String titlize(String string) {
		return string != null
					? (!string.isEmpty()
							? string.substring(0, 1).toUpperCase() + (string.length() > 1 ? string.substring(1).toLowerCase() : "")
							: "")
					: null;
	}
	
	public static final String titlize(String string, boolean eachWord) {
		if(string == null || string.isEmpty())
			return string;
		return eachWord
					? transformEachWord(string, (str, ctr) -> Utils.titlize(str))
					: titlize(string);
	}
	
	public static final String toURL(Path path) {
		StringBuilder builder = new StringBuilder();
		for(Path part : path) {
			if((builder.length() > 0))
				builder.append('/');
			builder.append(encodeURL(part.toString()).replace("+", "%20"));
		}
		builder.insert(0, path.getRoot().toString().replace('\\', '/'));
		return "file:///" + builder.toString();
	}
	
	public static final <T> Callable<T> callable(CheckedRunnable runnable) {
		return callable(runnable, null);
	}
	
	public static final <T> Callable<T> callable(CheckedRunnable runnable, T defaultValue) {
		return (() -> { runnable.run(); return defaultValue; });
	}
	
	public static final <T> Consumer<T> suppressException(CheckedConsumer<T> consumer) {
		return suppressException(consumer, null);
	}
	
	public static final <T> Consumer<T> suppressException(CheckedConsumer<T> consumer,
			Consumer<Exception> exceptionHandler) {
		return ((t) -> {
			try {
				consumer.accept(t);
			} catch(Exception ex) {
				if((exceptionHandler != null))
					exceptionHandler.accept(ex);
			}
		});
	}
	
	public static final <T> Runnable suppressException(CheckedRunnable runnable) {
		return suppressException(runnable, (Consumer<Exception>) null);
	}
	
	public static final <T> Runnable suppressException(CheckedRunnable runnable,
			Consumer<Exception> exceptionHandler) {
		return suppressException(runnable,
								 exceptionHandler != null
									? ((ex) -> { exceptionHandler.accept(ex); return null; })
									: null);
	}
	
	/** @since 00.02.05 */
	public static final <T> Runnable suppressException(CheckedRunnable runnable,
			Function<Exception, RuntimeException> exceptionMapper) {
		return (() -> {
			try {
				runnable.run();
			} catch(Exception ex) {
				if(exceptionMapper != null) {
					RuntimeException rex = exceptionMapper.apply(ex);
					if(rex != null) throw rex;
				}
			}
		});
	}
	
	public static final <T> void ignore(CheckedRunnable runnable) {
		ignore(runnable, (Consumer<Exception>) null);
	}
	
	public static final <T> void ignore(CheckedRunnable runnable, Consumer<Exception> exceptionHandler) {
		ignore(runnable,
			   exceptionHandler != null
					? ((ex) -> { exceptionHandler.accept(ex); return null; })
					: null);
	}
	
	/** @since 00.02.05 */
	public static final <T> void ignore(CheckedRunnable runnable,
			Function<Exception, RuntimeException> exceptionMapper) {
		suppressException(runnable, exceptionMapper).run();
	}
	
	/** @since 00.02.04 */
	public static final <T> Supplier<Boolean> suppressExceptionWithCheck(CheckedRunnable runnable,
			Consumer<Exception> exceptionHandler) {
		return suppressExceptionWithCheck(runnable,
										  exceptionHandler != null
												? ((ex) -> { exceptionHandler.accept(ex); return null; })
												: null);
	}
	
	/** @since 00.02.05 */
	public static final <T> Supplier<Boolean> suppressExceptionWithCheck(CheckedRunnable runnable,
			Function<Exception, RuntimeException> exceptionMapper) {
		return (() -> {
			try {
				runnable.run();
				return true;
			} catch(Exception ex) {
				if(exceptionMapper != null) {
					RuntimeException rex = exceptionMapper.apply(ex);
					if(rex != null) throw rex;
				}
				return false;
			}
		});
	}
	
	/** @since 00.02.04 */
	public static final <T> boolean ignoreWithCheck(CheckedRunnable runnable) {
		return ignoreWithCheck(runnable, null);
	}
	
	/** @since 00.02.04 */
	public static final <T> boolean ignoreWithCheck(CheckedRunnable runnable, Consumer<Exception> exceptionHandler) {
		return suppressExceptionWithCheck(runnable, exceptionHandler).get();
	}
	
	public static final <T> T ignore(Callable<T> callable) {
		return ignore(callable, (T) null);
	}
	
	public static final <T> T ignore(Callable<T> callable, T defaultValue) {
		return ignore(callable, defaultValue, (Consumer<Exception>) null);
	}
	
	/** @since 00.02.00 */
	public static final <T> T ignore(Callable<T> callable, Supplier<T> defaultValueSupplier) {
		return ignore(callable, defaultValueSupplier, (Consumer<Exception>) null);
	}
	
	public static final <T> T ignore(Callable<T> callable, T defaultValue, Consumer<Exception> exceptionHandler) {
		return ignore(callable, (Supplier<T>) (() -> defaultValue), exceptionHandler);
	}
	
	/** @since 00.02.05 */
	public static final <T> T ignore(Callable<T> callable, T defaultValue,
			Function<Exception, RuntimeException> exceptionMapper) {
		return ignore(callable, (Supplier<T>) (() -> defaultValue), exceptionMapper);
	}
	
	/** @since 00.02.00 */
	public static final <T> T ignore(Callable<T> callable, Supplier<T> defaultValueSupplier,
			Consumer<Exception> exceptionHandler) {
		return ignore(callable, defaultValueSupplier,
					  exceptionHandler != null
							? ((ex) -> { exceptionHandler.accept(ex); return null; })
							: null);
	}
	
	/** @since 00.02.05 */
	public static final <T> T ignore(Callable<T> callable, Supplier<T> defaultValueSupplier,
			Function<Exception, RuntimeException> exceptionMapper) {
		try {
			return callable.call();
		} catch(Exception ex) {
			if(exceptionMapper != null) {
				RuntimeException rex = exceptionMapper.apply(ex);
				if(rex != null) throw rex;
			}
		}
		return defaultValueSupplier != null
					? defaultValueSupplier.get()
					: null;
	}
	
	public static final <T> Iterable<T> iterable(Iterator<T> iterator) {
		return (() -> iterator);
	}
	
	public static final <K, V> Map<K, V> merge(Map<K, V> a, Map<K, V> b) {
		a.putAll(b);
		return a;
	}
	
	public static final <K, V> Map<K, V> mergeNew(Map<K, V> a, Map<K, V> b) {
		return mergeNew(a, b, HashMap::new);
	}
	
	public static final <K, V> Map<K, V> mergeNew(Map<K, V> a, Map<K, V> b, Supplier<Map<K, V>> supplier) {
		if((supplier == null))
			throw new NullPointerException();
		Map<K, V> m = supplier.get();
		m.putAll(a);
		m.putAll(b);
		return m;
	}
	
	public static final <T> Collection<T> merge(Collection<T> a, Collection<T> b) {
		a.addAll(b);
		return a;
	}
	
	public static final <T> Collection<T> mergeNew(Collection<T> a, Collection<T> b) {
		return mergeNew(a, b, ArrayList::new);
	}
	
	public static final <T> Collection<T> mergeNew(Collection<T> a, Collection<T> b,
			Supplier<Collection<T>> supplier) {
		Collection<T> c = supplier.get();
		c.addAll(a);
		c.addAll(b);
		return c;
	}
	
	public static final String unquote(String string) {
		StringBuilder builder = new StringBuilder(string);
		int quoteCharacter = -1, length;
		length = builder.length();
		// Remove the first quote
		for(int i = 0, c; i < length; ++i) {
			c = builder.charAt(i);
			if((c == '"' || c == '\'')) {
				// Remove the quote character
				builder.deleteCharAt(i);
				quoteCharacter = c; // Unquote the same quote later
				break;
			}
		}
		length = builder.length();
		// Remove the second quote
		for(int i = length - 1, c; i >= 0; --i) {
			c = builder.charAt(i);
			if((c == quoteCharacter)) {
				// Remove the quote character
				builder.deleteCharAt(i);
				break;
			}
		}
		return builder.toString();
	}
	
	/** @since 00.01.27 */
	public static final Class<?> getClass(Object object) {
		return object != null ? object.getClass() : Object.class;
	}
	
	/** @since 00.01.27 */
	public static final void dumpThreads() {
		dumpThreads(System.out);
	}
	
	/** @since 00.01.27 */
	public static final void dumpThreads(PrintStream stream) {
		String delimiter = repeat("-", 25);
		dumpThreads((thread) -> {
			stream.println(delimiter);
			stream.println(thread);
			stream.println(delimiter);
		}, stream::println);
	}
	
	/** @since 00.01.27 */
	public static final void dumpThreads(Consumer<Thread> threadConsumer, Consumer<StackTraceElement> elementConsumer) {
		for(Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
			threadConsumer.accept(entry.getKey());
			Arrays.asList(entry.getValue()).forEach(elementConsumer);
		}
	}
	
	/** @since 00.01.27 */
	public static final <T> T getOrDefault(T value, T defaultValue) {
		return value != null ? value : defaultValue;
	}
	
	/** @since 00.02.00 */
	public static final Throwable throwable(Class<? extends Throwable> clazz, String message, Object... args) {
		String formattedMessage = String.format(message, args);
		return ignore(() -> clazz.getConstructor(String.class).newInstance(formattedMessage),
					  () -> new Throwable(formattedMessage));
	}
	
	/** @since 00.02.00 */
	public static final void throwISE(String message, Object... args) {
		// Cast the exception so it is seen as a RuntimeException by the compiler
		throw (IllegalStateException) throwable(IllegalStateException.class, message, args);
	}
	
	/** @since 00.02.02 */
	public static final <T> Stream<T> stream(Enumeration<T> enumeration) {
		return stream(enumeration.asIterator());
	}
	
	/** @since 00.02.04 */
	public static final String throwableToString(Throwable throwable) {
		try(StringWriter swriter = new StringWriter();
			PrintWriter  pwriter = new PrintWriter(swriter)) {
			throwable.printStackTrace(pwriter);
			return swriter.toString();
		} catch(IOException ex) {
			// Should not happen
		}
		return null;
	}
	
	/** @since 00.02.05 */
	@SuppressWarnings("unchecked")
	public static final <T> List<T> streamToList(Stream<? super T> stream) {
		return stream.collect(Collectors.mapping((m) -> (T) m, Collectors.toList()));
	}
	
	/** @since 00.02.05 */
	@SuppressWarnings("unchecked")
	public static final <T> List<T> streamToUnmodifiableList(Stream<? super T> stream) {
		return stream.collect(Collectors.mapping((m) -> (T) m, Collectors.toUnmodifiableList()));
	}
	
	/** @since 00.02.05 */
	public static final <T, I extends Iterable<T>> I nonNullItems(I iterable) {
		if(iterable == null) return null;
		for(T item : iterable) if(item == null) return null;
		return iterable;
	}
	
	/** @since 00.02.05 */
	public static final <T> T[] nonNullContent(T[] array) {
		if(array == null) return null;
		for(T item : array) if(item == null) return null;
		return array;
	}
	
	/** @since 00.02.05 */
	public static final <T, C extends Collection<T>> C nonNullContent(C collection) {
		return nonNullItems(collection);
	}
	
	/** @since 00.02.05 */
	public static final <T> boolean contains(Collection<T> collection, T value, Function<T, T> mapper) {
		return mapper != null
					? collection.stream().filter((v) -> Objects.equals(mapper.apply(v), value)).findAny().isPresent()
					: collection.contains(value);
	}
	
	/** @since 00.02.05 */
	public static final <T> boolean contains(Collection<T> collection, T value) {
		return contains(collection, value, null);
	}
	
	/** @since 00.02.05 */
	public static final <T> T cast(Object v) {
		@SuppressWarnings("unchecked")
		T casted = (T) v;
		return casted;
	}
	
	/** @since 00.02.05 */
	public static final <T> int indexOf(T needle, T[] haystack, BiFunction<T, T, Boolean> equals) {
		Objects.requireNonNull(equals);
		if(haystack == null || haystack.length == 0)
			return -1;
		if(needle == null) {
			for(int i = 0, l = haystack.length; i < l; ++i) {
				if(haystack[i] == null)
					return i;
			}
		} else {
			for(int i = 0, l = haystack.length; i < l; ++i) {
				if(haystack[i] != null
						&& equals.apply(needle, haystack[i]))
					return i;
			}
		}
		return -1;
	}
	
	/** @since 00.02.05 */
	public static final <T> int compareIndex(T a, T b, T[] all) {
		return Integer.compare(indexOf(a, all), indexOf(b, all));
	}
	
	/** @since 00.02.05 */
	public static final <T> int compareIndex(T a, T b, List<T> all) {
		return Integer.compare(all.indexOf(a), all.indexOf(b));
	}
	
	/** @since 00.02.05 */
	public static final <T> int compareIndex(T a, T b, T[] all, BiFunction<T, T, Boolean> equals) {
		return Integer.compare(indexOf(a, all, equals), indexOf(b, all, equals));
	}
	
	/** @since 00.02.05 */
	public static final URI uri(URL url) {
		return ignore(() -> url.toURI(), (URI) null, (Function<Exception, RuntimeException>) IllegalArgumentException::new);
	}
	
	/** @since 00.02.05 */
	public static final <T> Callable<T> callable(ThrowableCallable<T> callable) {
		return (() -> { try { return callable.run(); } catch(Throwable th) { throw new Exception(th); } });
	}
	
	/** @since 00.02.05 */
	public static final <T> T primitive(T value, Class<? extends T> clazz) {
		return value != null ? value : cast(ignore(() -> callable(() -> MethodHandles.zero(clazz).invoke(value))));
	}
	
	/** @since 00.02.05 */
	public static final boolean primitive(Boolean value) {
		return primitive(value, value.getClass());
	}
	
	/** @since 00.02.05 */
	public static final byte primitive(Byte value) {
		return primitive(value, value.getClass());
	}
	
	/** @since 00.02.05 */
	public static final char primitive(Character value) {
		return primitive(value, value.getClass());
	}
	
	/** @since 00.02.05 */
	public static final short primitive(Short value) {
		return primitive(value, value.getClass());
	}
	
	/** @since 00.02.05 */
	public static final int primitive(Integer value) {
		return primitive(value, value.getClass());
	}
	
	/** @since 00.02.05 */
	public static final long primitive(Long value) {
		return primitive(value, value.getClass());
	}
	
	/** @since 00.02.05 */
	public static final float primitive(Float value) {
		return primitive(value, value.getClass());
	}
	
	/** @since 00.02.05 */
	public static final double primitive(Double value) {
		return primitive(value, value.getClass());
	}
	
	/** @since 00.02.05 */
	public static final <T> Stream<T> stream(Iterator<T> iterator) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
	}
	
	/** @since 00.02.05 */
	public static final <T> Stream<T> stream(Iterable<T> iterable) {
		return stream(iterable.iterator());
	}
	
	/** @since 00.02.05 */
	public static final <T extends Comparable<? super T>> int compare(T a, T b) {
		return a.compareTo(b);
	}
	
	/** @since 00.02.05 */
	public static final int compare(Object... items) {
		if((items.length & 1) != 0)
			throw new IllegalArgumentException("Array length must be even");
		int cmp = 0;
		for(int i = 0, l = items.length; i < l && cmp == 0; i += 2) {
			cmp = compare(cast(items[i]), cast(items[i + 1]));
		}
		return cmp;
	}
	
	/** @since 00.02.05 */
	public static final Random random() {
		return RANDOM == null ? (RANDOM = new Random()) : RANDOM;
	}
	
	/** @since 00.02.05 */
	public static final String randomString(int length) {
		return randomString(length, ALPHABET_ALPHANUM);
	}
	
	/** @since 00.02.05 */
	public static final String randomString(int length, String alphabet) {
		Random random = random();
		StringBuilder builder = new StringBuilder(length);
		for(int i = 0, l = alphabet.length(); i < length; ++i) {
			builder.appendCodePoint(alphabet.codePointAt(random.nextInt(l)));
		}
		return builder.toString();
	}
	
	/** @since 00.02.05 */
	// Source: https://java2blog.com/convert-roman-number-to-integer-java/ (modified)
	public static final int romanToInteger(String roman) {
		int result = 0;
		char c, p = 0;
		for(int i = 0, l = roman.length(), u = 0, v = 0; i < l; ++i, p = c) {
			u = ROMAN_NUMERALS.getOrDefault(c = roman.charAt(i), -1);
			result += i > 0 && u > (v = ROMAN_NUMERALS.getOrDefault(p, -1)) ? u - 2 * v : u;
			if(u < 0 || v < 0) return -1;
		}
		return result;
	}
	
	/** @since 00.02.05 */
	public static final <T> List<T> deduplicate(List<T> list) {
		return new ArrayList<>(new LinkedHashSet<>(list)); // Modifiable list
	}
	
	/** @since 00.02.05 */
	public static final <T> boolean equalsNoOrder(List<? extends T> a, List<? extends T> b) {
		if(a == b) return true; // Fast-path
		if(a.size() != b.size()) return false; // Fast-path
		Set<T> set = new HashSet<>(a);
		for(T t : b) if(!set.contains(t)) return false;
		return true;
	}
	
	/** @since 00.02.05 */
	public static final <T> boolean equalsNoOrder(List<? extends T> a, List<? extends T> b,
			Function<T, Integer> hashCode, BiFunction<T, T, Boolean> equals) {
		if(a == b) return true; // Fast-path
		if(a.size() != b.size()) return false; // Fast-path
		Set<EqualityWrapper<T>> set = a.stream()
				.map((v) -> new EqualityWrapper<>(v, hashCode, equals))
				.collect(Collectors.toSet());
		for(T t : b) {
			if(!set.contains(new EqualityWrapper<>(t, hashCode, equals)))
				return false;
		}
		return true;
	}
	
	/** @since 00.02.05 */
	private static Pattern regexUnicodeWord;
	
	/** @since 00.02.05 */
	private static final Pattern regexUnicodeWord() {
		return regexUnicodeWord == null
					? (regexUnicodeWord = Pattern.compile("\\w+", Pattern.UNICODE_CHARACTER_CLASS))
					:  regexUnicodeWord;
	}
	
	/** @since 00.02.05 */
	public static final String transformEachWord(String string, BiFunction<String, Integer, String> transformer) {
		if(string == null || string.isEmpty()) return string;
		Objects.requireNonNull(transformer);
		Property<Integer> ctr = new Property<>(0);
		return replaceAll(regexUnicodeWord(), string, (matches) -> {
			String value = transformer.apply(matches.group(0), ctr.getValue());
			ctr.setValue(ctr.getValue() + 1);
			return value;
		});
	}
	
	/** @since 00.02.05 */
	public static final boolean isInteger(String string) {
		return Utils.ignoreWithCheck(() -> Integer.parseInt(string));
	}
	
	/** @since 00.02.05 */
	public static final boolean isDouble(String string) {
		return Utils.ignoreWithCheck(() -> Double.parseDouble(string));
	}
	
	/** @since 00.02.05 */
	private static final class EqualityWrapper<T> {
		
		private final Function<T, Integer> hashCode;
		private final BiFunction<T, T, Boolean> equals;
		private final T value;
		
		public EqualityWrapper(T value, Function<T, Integer> hashCode, BiFunction<T, T, Boolean> equals) {
			this.value = value;
			this.hashCode = Objects.requireNonNull(hashCode);
			this.equals = Objects.requireNonNull(equals);
		}
		
		@Override
		public int hashCode() {
			return hashCode.apply(value);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			@SuppressWarnings("unchecked")
			EqualityWrapper<T> other = (EqualityWrapper<T>) obj;
			return equals.apply(value, other.value);
		}
	}
	
	public static final class JS {
		
		// Forbid anyone to create an instance of this class
		private JS() {
		}
		
		private static final String PATTERN_STRING_EXTRACT_VARIABLE;
		private static final String PATTERN_STRING_EXTRACT_FUNCTION_PARAMS;
		private static final String PATTERN_STRING_INSERT_ARGUMENTS;
		
		static {
			PATTERN_STRING_EXTRACT_VARIABLE ="(?:(?:var|const|let)\\s+)?%s\\s*=\\s*";
			PATTERN_STRING_EXTRACT_FUNCTION_PARAMS ="%s\\s*\\(";
			PATTERN_STRING_INSERT_ARGUMENTS ="[\"']\\s*\\+\\s*([^\\+\\s]+)\\s*(?:\\+\\s*[\"'])?";
		}
		
		public static final String extractVariableContent(String script, String varName, int offset) {
			StringBuilder builder = new StringBuilder();
			Pattern pattern = Pattern.compile(String.format(PATTERN_STRING_EXTRACT_VARIABLE, Pattern.quote(varName)));
			Matcher matcher = pattern.matcher(script);
			if((matcher.find(offset))) {
				int start = matcher.end();
				boolean dq = false;
				boolean sq = false;
				for(int i = start, l = script.length(), c; i < l; ++i) {
					c = script.codePointAt(i);
					// Quotes logic
					if((c == '\"' && !sq)) dq = !dq; else
					if((c == '\'' && !dq)) sq = !sq;
					// Extract logic
					if(!dq && !sq) {
						if((c == ';')) break;
					}
					// Optimize character adding for lower positioned characters
					if((Character.isBmpCodePoint(c)))
						 builder.append((char) c);
					else builder.append(Character.toChars(c));
				}
			}
			return builder.toString();
		}
		
		public static final String extractFunctionPararms(String script, String funcName, int offset) {
			StringBuilder builder = new StringBuilder();
			Pattern pattern = Pattern.compile(String.format(PATTERN_STRING_EXTRACT_FUNCTION_PARAMS, Pattern.quote(funcName)));
			Matcher matcher = pattern.matcher(script);
			if((matcher.find(offset))) {
				int start = matcher.end();
				boolean dq = false;
				boolean sq = false;
				for(int i = start, l = script.length(), c; i < l; ++i) {
					c = script.codePointAt(i);
					// Quotes logic
					if((c == '\"' && !sq)) dq = !dq; else
					if((c == '\'' && !dq)) sq = !sq;
					// Extract logic
					if(!dq && !sq) {
						if((c == ')')) break;
					}
					// Optimize character adding for lower positioned characters
					if((Character.isBmpCodePoint(c)))
						 builder.append((char) c);
					else builder.append(Character.toChars(c));
				}
			}
			return builder.toString();
		}
		
		public static final String insertArgumentsToString(String statement, Map<String, String> args) {
			return Utils.replaceAll(PATTERN_STRING_INSERT_ARGUMENTS, statement, (result) -> {
				return args.getOrDefault(result.group(1), result.group(1));
			});
		}
	}
}