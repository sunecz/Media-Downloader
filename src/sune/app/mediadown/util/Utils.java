package sune.app.mediadown.util;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
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
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import sune.app.mediadown.Shared;
import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.net.Net;

public final class Utils {
	
	private static final Charset CHARSET = Shared.CHARSET;
	private static final char[]  INVALID_FILE_NAME_CHARS = { '<', '>', ':', '"', '/', '\\', '|', '?', '*' };
	private static final String  REGEX_INVALID_FILE_NAME_CHARS;
	
	/** @since 00.02.05 */
	private static final String ALPHABET_ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	/** @since 00.02.05 */
	private static Random RANDOM;
	/** @since 00.02.05 */
	private static final Map<Character, Integer> ROMAN_NUMERALS = Map.of(
		'I', 1, 'V', 5, 'X', 10, 'L', 50, 'C', 100, 'D', 500, 'M', 1000
	);
	/** @since 00.02.09 */
	private static final Regex REGEX_FORMAT = Regex.of("%\\{\\s*([^\\}]+)\\s*\\}");
	/** @since 00.02.09 */
	private static final Object UNSET = new Object();
	
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
	
	public static final <T> T[] toArray(Collection<T> collection, Class<? extends T> clazz) {
		if((collection == null || clazz == null))
			throw new IllegalArgumentException();
		@SuppressWarnings("unchecked")
		T[] array = (T[]) Array.newInstance(clazz, collection.size());
		return collection.toArray(array);
	}
	
	public static final String extractName(String url) {
		if(!Net.isValidURI(url)) return null;
		if((url.endsWith("/"))) {
			url = url.substring(0, url.length()-1);
		}
		url = url.substring(url.lastIndexOf("/")+1);
		url = url.substring(url.indexOf("-")+1);
		url = url.replace("-", " ");
		url = url.substring(0, 1).toUpperCase() + url.substring(1);
		return validateFileName(url);
	}
	
	/** @since 00.02.09 */
	public static final Locale defaultLocale() {
		return Locale.getDefault(Locale.Category.FORMAT);
	}
	
	public static final String format(String string, Object... values) {
		return format(defaultLocale(), string, stringKeyMap(values));
	}
	
	public static final String format(String string, Map<String, Object> values) {
		return format(defaultLocale(), string, values);
	}
	
	/** @since 00.02.09 */
	public static final String format(Locale locale, String string, Object... values) {
		return format(locale, string, stringKeyMap(values));
	}
	
	/** @since 00.02.09 */
	public static final String format(Locale locale, String string, Map<String, Object> values) {
		StringBuilder builder = null;
		List<Object> args = null;
		int offset = 0, index = 1, start, end;
		
		for(Matcher matcher = REGEX_FORMAT.matcher(string); matcher.find(); offset = end) {
			String key = matcher.group(1);
			start = matcher.start();
			end = matcher.end();
			Object value = values.getOrDefault(key, UNSET);
			
			if(builder == null) {
				builder = new StringBuilder(string.length());
			}
			
			if(offset < start) {
				builder.append(string, offset, start);
			}
			
			if(value == UNSET) {
				builder.append('%'); // Escape
				builder.append(matcher.group());
			} else {
				builder.append('%');
				builder.append(index++);
				builder.append('$');
				
				if(args == null) {
					args = new ArrayList<>();
				}
				
				args.add(value);
			}
		}
		
		if(builder == null) {
			return string;
		}
		
		if(offset < (end = string.length())) {
			builder.append(string, offset, end);
		}
		
		if(args == null) {
			return builder.toString();
		}
		
		return String.format(locale, builder.toString(), args.toArray());
	}
	
	public static final String validateFileName(String fileName) {
		return fileName.replaceAll(REGEX_INVALID_FILE_NAME_CHARS, "");
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
	
	// Provides realtively fast method for converting a string into an integer
	// not using any built-in function and ignoring all other characters that
	// are not defined as digits.
	public static final int extractInt(String string) {
		return extractIntOrDefault(string, null);
	}
	
	/** @since 00.02.09 */
	public static final int extractInt(String string, int defaultValue) {
		return extractIntOrDefault(string, defaultValue);
	}
	
	// Provides realtively fast method for converting a string into an integer
	// not using any built-in function and ignoring all other characters that
	// are not defined as digits.
	/** @since 00.02.09 */
	private static final int extractIntOrDefault(String string, Integer defaultValue) {
		if(string == null || string.isEmpty()) {
			throw new IllegalArgumentException(
				"The given string cannot be null nor empty!"
			);
		}
		
		int value = 0;
		boolean has	= false;
		boolean neg = false;
		
		for(int i = 0, l = string.length(), c, n, t; i < l; i += n) {
			c = string.codePointAt(i);
			n = charCount(c);
			
			if(c == '-') {
				neg = true;
			} else if((t = Character.digit(c, 10)) != -1) {
				int val = value * 10 - t;
				
				if(val > value || (!neg && val == Integer.MIN_VALUE)) {
					throw new IllegalArgumentException(
						"The given string contains number outside of " +
						"the range of a signed integer!"
					);
				}
				
				value = val;
				has   = true;
			}
		}
		
		if(!has) {
			if(defaultValue == null) {
				throw new IllegalArgumentException(
					"The given string does not contain any digit!"
				);
			}
			
			return defaultValue;
		}
		
		return neg ? value : -value;
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
		if(data == null || (data.length & 1) != 0) {
			throw new IllegalArgumentException("Data must be non-null and of even length");
		}
		
		Map<String, Object> map = new LinkedHashMap<>();
		
		for(int i = 0, l = data.length; i < l; i += 2) {
			map.put(data[i] != null ? data[i].toString() : null, data[i + 1]);
		}
		
		return map;
	}
	
	public static final String addFormatExtension(String name, MediaFormat format) {
		return name + (!format.fileExtensions().isEmpty() ? '.' + format.fileExtensions().get(0) : "");
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
	
	/** @since 00.02.09 */
	public static final byte[] base64DecodeRaw(byte[] bytes) {
		return base64Decoder().decode(bytes);
	}
	
	/** @since 00.02.09 */
	public static final String base64DecodeRawAsString(byte[] bytes) {
		return new String(base64DecodeRaw(bytes), CHARSET);
	}
	
	public static final byte[] base64DecodeRaw(String string) {
		return base64DecodeRaw(string.getBytes(CHARSET));
	}
	
	public static final String base64Decode(String string) {
		return new String(base64DecodeRaw(string), CHARSET);
	}
	
	/** @since 00.02.09 */
	public static final byte[] base64URLDecodeRaw(byte[] bytes) {
		return base64URLDecoder().decode(bytes);
	}
	
	/** @since 00.02.09 */
	public static final String base64URLDecodeRawAsString(byte[] bytes) {
		return new String(base64URLDecodeRaw(bytes), CHARSET);
	}
	
	public static final byte[] base64URLDecodeRaw(String string) {
		return base64URLDecodeRaw(string.getBytes(CHARSET));
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
	
	/** @since 00.02.09 */
	public static final byte[] base64EncodeRaw(byte[] bytes) {
		return base64Encoder().encode(bytes);
	}
	
	/** @since 00.02.09 */
	public static final String base64EncodeRawAsString(byte[] bytes) {
		return new String(base64EncodeRaw(bytes), CHARSET);
	}
	
	public static final byte[] base64EncodeRaw(String string) {
		return base64EncodeRaw(string.getBytes(CHARSET));
	}
	
	public static final String base64Encode(String string) {
		return new String(base64EncodeRaw(string), CHARSET);
	}
	
	/** @since 00.02.09 */
	public static final byte[] base64URLEncodeRaw(byte[] bytes) {
		return base64URLEncoder().encode(bytes);
	}
	
	/** @since 00.02.09 */
	public static final String base64URLEncodeRawAsString(byte[] bytes) {
		return new String(base64URLEncodeRaw(bytes), CHARSET);
	}
	
	public static final byte[] base64URLEncodeRaw(String string) {
		return base64URLEncodeRaw(string.getBytes(CHARSET));
	}
	
	public static final String base64URLEncode(String string) {
		return new String(base64URLEncodeRaw(string), CHARSET);
	}
	
	// ---------------- [END]   Base64 Encode
	
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
	
	public static final Pair<Integer, Integer> stringBetween(String string, int chOpen, int chClose) {
		return stringBetween(string, chOpen, chClose, 0, string.length());
	}
	
	public static final Pair<Integer, Integer> stringBetween(String string, int chOpen, int chClose, int start, int end) {
		int count = 0;
		
		for(int i = start, c, n; i < end; i += n) {
			c = string.codePointAt(i);
			n = charCount(c);
			
			if(c == chOpen) {
				if(count == 0) {
					start = i;
				}
				
				++count;
			} else if(c == chClose) {
				--count;
				
				if(count == 0) {
					return new Pair<>(start, i + n);
				}
			}
		}
		
		return new Pair<>(start, end);
	}
	
	public static final Pair<Integer, Integer> stringBetweenReverse(String string, int chOpen, int chClose) {
		return stringBetweenReverse(string, chOpen, chClose, codePointAlign(string, string.length() - 1), -1);
	}
	
	public static final Pair<Integer, Integer> stringBetweenReverse(String string, int chOpen, int chClose, int start, int end) {
		int count = 0;
		
		for(int i = start, c, n; i > end; i -= n) {
			c = string.codePointAt(i);
			n = charCount(c);
			
			if(c == chClose) {
				if(count == 0) {
					start = i;
				}
				
				++count;
			} else if(c == chOpen) {
				--count;
				
				if(count == 0) {
					return new Pair<>(i, start + n);
				}
			}
		}
		
		return new Pair<>(0, start + charCount(string, start));
	}
	
	public static final String bracketSubstring(String string) {
		return bracketSubstring(string, '(', ')', false);
	}
	
	public static final String bracketSubstring(String string, boolean reverse) {
		return bracketSubstring(string, '(', ')', reverse);
	}
	
	public static final String bracketSubstring(String string, int chOpen, int chClose) {
		return bracketSubstring(string, chOpen, chClose, false);
	}
	
	public static final String bracketSubstring(String string, int chOpen, int chClose, boolean reverse) {
		Pair<Integer, Integer> range
			= reverse
				? stringBetweenReverse(string, chOpen, chClose)
				: stringBetween(string, chOpen, chClose);
		return string.substring(range.a, range.b);
	}
	
	public static final String bracketSubstring(String string, int chOpen, int chClose, boolean reverse, int start, int end) {
		Pair<Integer, Integer> range
			= reverse
				? stringBetweenReverse(string, chOpen, chClose, start, end)
				: stringBetween(string, chOpen, chClose, start, end);
		return string.substring(range.a, range.b);
	}
	
	public static final String substringTo(String string, int ch) {
		int index = string.indexOf(ch);
		return index >= 0 ? string.substring(0, index) : string;
	}
	
	public static final String replaceUnicodeEscapeSequences(String string) {
		return UnicodeEscapeSequence.replace(string);
	}
	
	public static final String prefixUnicodeEscapeSequences(String string, String prefix) {
		return UnicodeEscapeSequence.prefix(string, prefix);
	}
	
	/** @since 00.02.09 */
	public static final void unslash(StringBuilder input, StringBuilder output) {
		final int len = input.length();
		unslash(input, output, len);
	}
	
	/** @since 00.02.09 */
	public static final void unslash(StringBuilder input, StringBuilder output, final int len) {
		int end = 0;
		boolean escaped = false;
		
		for(int i = 0, c, n; i < len; i += n) {
			c = input.codePointAt(i);
			n = Character.charCount(c);
			
			if(escaped) {
				escaped = false;
			} else if(c == '\\') {
				output.append(input, end, i);
				end = i + 1;
				escaped = true;
			}
		}
		
		if(end < len) {
			output.append(input, end, len);
		}
	}
	
	/** @since 00.02.09 */
	// Since CharSequence does not have codePointAt method, we must copy the method above
	// with the only exception of using a String instead of a StringBuilder as an input.
	public static final void unslash(String input, StringBuilder output, final int len) {
		int end = 0;
		boolean escaped = false;
		
		for(int i = 0, c, n; i < len; i += n) {
			c = input.codePointAt(i);
			n = Character.charCount(c);
			
			if(escaped) {
				escaped = false;
			} else if(c == '\\') {
				output.append(input, end, i);
				end = i + 1;
				escaped = true;
			}
		}
		
		if(end < len) {
			output.append(input, end, len);
		}
	}
	
	/** @since 00.02.09 */
	public static final String unslash(String string) {
		final int len = string.length();
		StringBuilder builder = utf16StringBuilder(len);
		unslash(string, builder, len);
		return builder.toString();
	}
	
	public static final int backTill(String string, int ch, int from) {
		return backTill(string, ch, from, 1);
	}
	
	public static final int backTill(String string, int ch, int from, int count) {
		for(int i = from, c, n; i >= 0; i -= n) {
			c = string.codePointAt(i);
			n = charCount(c);
			
			if(c == ch && --count == 0) {
				return i;
			}
		}
		
		return 0;
	}
	
	public static final String backTillString(String string, int ch, int from) {
		return backTillString(string, ch, from, 1);
	}
	
	public static final String backTillString(String string, int ch, int from, int count) {
		from = codePointAlign(string, from);
		int start = backTill(string, ch, from, count);
		return string.substring(start, from + charCount(string, from));
	}
	
	/** @since 00.02.07 */
	public static final int charCount(int codePoint) {
		return Character.charCount(codePoint);
	}
	
	/** @since 00.02.07 */
	public static final int charCount(String string, int index) {
		return charCount(codePointAt(string, index));
	}
	
	/** @since 00.02.07 */
	public static final int codePointAt(String string, int index) {
		int c = string.codePointAt(index);
		
		if(Character.isBmpCodePoint(c)
				&& Character.isLowSurrogate((char) c)) {
			c = string.codePointBefore(index);
		}
		
		return c;
	}
	
	/** @since 00.02.07 */
	public static final int codePointAlign(String string, int index) {
		int c = string.codePointAt(index);
		
		if(Character.isBmpCodePoint(c)
				&& Character.isLowSurrogate((char) c)) {
			return index - 1;
		}
		
		return index;
	}
	
	/** @since 00.02.07 */
	public static final String substring(String string, int start, int end) {
		return string.substring(codePointAlign(string, start), codePointAlign(string, end));
	}
	
	public static final String trimEnd(String string, int ch) {
		int end = string.length();
		
		for(int i = codePointAlign(string, end - 1), c, n; i >= 0; i -= n) {
			c = string.codePointAt(i);
			n = charCount(c);
			
			if(c == ch) --end;
			else        break;
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
	
	/** @since 00.02.08 */
	public static final String join(String separator, String... strings) {
		StringBuilder builder = new StringBuilder();
		
		for(String string : strings) {
			builder.append(string).append(separator);
		}
		
		int length;
		if((length = builder.length()) > 0) {
			builder.setLength(length - separator.length());
		}
		
		return builder.toString();
	}
	
	/** @since 00.02.08 */
	public static final String before(String string, int index) {
		return before(string, index, 0);
	}
	
	/** @since 00.02.08 */
	public static final String before(String string, int index, int offset) {
		return index >= 0 ? string.substring(0, index + offset) : string;
	}
	
	/** @since 00.02.08 */
	public static final String beforeFirst(String string, String what) {
		return before(string, string.indexOf(what), 0);
	}
	
	/** @since 00.02.08 */
	public static final String beforeLast(String string, String what) {
		return before(string, string.lastIndexOf(what), 0);
	}
	
	/** @since 00.02.08 */
	public static final String after(String string, int index) {
		return after(string, index, 0);
	}
	
	/** @since 00.02.08 */
	public static final String after(String string, int index, int offset) {
		return index >= 0 ? string.substring(index + offset) : string;
	}
	
	/** @since 00.02.08 */
	public static final String afterFirst(String string, String what) {
		return after(string, string.indexOf(what), what.length());
	}
	
	/** @since 00.02.08 */
	public static final String afterLast(String string, String what) {
		return after(string, string.lastIndexOf(what), what.length());
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
	
	public static final InputStream stringStream(String string) {
		return new ByteArrayInputStream(string.getBytes(Shared.CHARSET));
	}
	
	public static final String streamToString(InputStream stream) {
		try {
			return new String(stream.readAllBytes(), Shared.CHARSET);
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
	
	/** @since 00.02.08 */
	@SafeVarargs
	public static final <T> T[] merge(T[] array, T... others) {
		if(array == null) {
			return null;
		}
		
		if(others == null || others.length == 0) {
			return array;
		}
		
		@SuppressWarnings("unchecked")
		T[] newArray = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length + others.length);
		
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
	
	public static final <T> Callable<T> callable(CheckedRunnable runnable) {
		return callable(runnable, null);
	}
	
	public static final <T> Callable<T> callable(CheckedRunnable runnable, T defaultValue) {
		return (() -> { runnable.run(); return defaultValue; });
	}
	
	/** @since 00.02.07 */
	public static final CheckedRunnable checked(Runnable runnable) {
		return runnable::run;
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
	
	/** @since 00.02.08 */
	// Source: https://stackoverflow.com/a/23529010
	public static final <A, B, C> Stream<C> zip(Stream<? extends A> a, Stream<? extends B> b,
			BiFunction<? super A, ? super B, ? extends C> zipper) {
		Objects.requireNonNull(zipper);
		Spliterator<? extends A> aSpliterator = Objects.requireNonNull(a).spliterator();
		Spliterator<? extends B> bSpliterator = Objects.requireNonNull(b).spliterator();
		
		// Zipping looses DISTINCT and SORTED characteristics
		int characteristics = aSpliterator.characteristics()
				            & bSpliterator.characteristics()
				            & ~(Spliterator.DISTINCT | Spliterator.SORTED);
		
		long zipSize = ((characteristics & Spliterator.SIZED) != 0)
			? Math.min(aSpliterator.getExactSizeIfKnown(), bSpliterator.getExactSizeIfKnown())
			: -1L;
		
		Iterator<A> aIterator = Spliterators.iterator(aSpliterator);
		Iterator<B> bIterator = Spliterators.iterator(bSpliterator);
		Iterator<C> cIterator = new Iterator<C>() {
			
			@Override
			public boolean hasNext() {
				return aIterator.hasNext() && bIterator.hasNext();
			}
			
			@Override
			public C next() {
				return zipper.apply(aIterator.next(), bIterator.next());
			}
		};
		
		Spliterator<C> split = Spliterators.spliterator(cIterator, zipSize, characteristics);
		return StreamSupport.stream(split, a.isParallel() || b.isParallel());
	}
	
	/** @since 00.02.08 */
	public static final <A, B, C> Iterator<? extends C> zipIterator(Stream<? extends A> a, Stream<? extends B> b,
			BiFunction<? super A, ? super B, ? extends C> zipper) {
		return zip(a, b, zipper).iterator();
	}
	
	/** @since 00.02.08 */
	public static final <A, B, C> Iterable<? extends C> zipIterable(Stream<? extends A> a, Stream<? extends B> b,
			BiFunction<? super A, ? super B, ? extends C> zipper) {
		return iterable(zipIterator(a, b, zipper));
	}
	
	public static final String unquote(String string) {
		StringBuilder builder = new StringBuilder(string);
		int quoteCharacter = -1, length;
		
		length = builder.length();
		// Remove the first quote
		for(int i = 0, c, n; i < length; i += n) {
			c = builder.codePointAt(i);
			n = charCount(c);
			
			if(c == '"' || c == '\'') {
				// Remove the quote character
				builder.deleteCharAt(i);
				quoteCharacter = c; // Unquote the same quote later
				break;
			}
		}
		
		length = builder.length();
		// Remove the second quote
		for(int i = codePointAlign(string, length - 1), c, n; i >= 0; i -= n) {
			c = builder.codePointAt(i);
			n = charCount(c);
			
			if(c == quoteCharacter) {
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
		return Ignore.supplier(() -> clazz.getConstructor(String.class).newInstance(formattedMessage), () -> new Throwable(formattedMessage));
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
		if(throwable == null) {
			return null;
		}
		
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
	public static final <T> T primitive(T value, Class<? extends T> clazz) {
		if(value != null) {
			return value;
		}
		
		try {
			return cast(MethodHandles.zero(clazz).invoke(value));
		} catch(Throwable th) {
			throw new RuntimeException(th);
		}
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
	private static Regex regexUnicodeWord;
	
	/** @since 00.02.05 */
	private static final Regex regexUnicodeWord() {
		return regexUnicodeWord == null
					? (regexUnicodeWord = Regex.of("\\w+", Regex.Flags.UNICODE_CHARACTER_CLASS))
					:  regexUnicodeWord;
	}
	
	/** @since 00.02.05 */
	public static final String transformEachWord(String string, BiFunction<String, Integer, String> transformer) {
		if(string == null || string.isEmpty()) return string;
		Objects.requireNonNull(transformer);
		Ref.Mutable<Integer> ctr = new Ref.Mutable<>(0);
		return regexUnicodeWord().replaceAll(string, (matches) -> {
			String value = transformer.apply(matches.group(0), ctr.get());
			ctr.set(ctr.get() + 1);
			return value;
		});
	}
	
	/** @since 00.02.05 */
	public static final boolean isInteger(String string) {
		return Ignore.callAndCheck(() -> Integer.parseInt(string));
	}
	
	/** @since 00.02.05 */
	public static final boolean isDouble(String string) {
		return Ignore.callAndCheck(() -> Double.parseDouble(string));
	}
	
	/** @since 00.02.09 */
	public static final int compareCodePoint(int a, int b) {
		return Integer.compare(a, b);
	}
	
	/** @since 00.02.09 */
	public static final int compareCodePointIgnoreCase(int a, int b) {
		if(a == b) return 0;
		
		a = Character.toUpperCase(a);
		b = Character.toUpperCase(b);
		if(a == b) return 0;
		
		a = Character.toLowerCase(a);
		b = Character.toLowerCase(b);
		return a - b;
	}
	
	/** @since 00.02.09 */
	public static final int compareNatural(String a, String b) {
		return compareNatural(a, b, false);
	}
	
	/** @since 00.02.09 */
	public static final int compareNaturalIgnoreCase(String a, String b) {
		return compareNatural(a, b, true);
	}
	
	/** @since 00.02.09 */
	private static final int compareNatural(String a, String b, boolean ignoreCase) {
		if(a == b)    return  0; // nulls are also equal
		if(a == null) return  1; // null will be higher in a list
		if(b == null) return -1; // null will be higher in a list
		
		final int lenA = a.length();
		final int lenB = b.length();
		
		for(int idxA = 0, idxB = 0, len = Math.min(lenA, lenB), cpA, cpB, result;
				idxA < len && idxB < len;
				idxA += Character.charCount(cpA),
				idxB += Character.charCount(cpB)) {
			cpA = a.codePointAt(idxA);
			cpB = b.codePointAt(idxB);
			
			boolean isDigitA = Character.isDigit(cpA);
			boolean isDigitB = Character.isDigit(cpB);
			
			// Start of a new digits sequence
			if(isDigitA && isDigitB) {
				int startA = idxA;
				int startB = idxB;
				
				// Find the end of the digits sequence for string A
				for(idxA += Character.charCount(cpA);
						idxA < lenA && Character.isDigit(a.codePointAt(idxA));
						idxA += Character.charCount(cpA));
				
				// Find the end of the digits sequence for string B
				for(idxB += Character.charCount(cpB);
						idxB < lenB && Character.isDigit(b.codePointAt(idxB));
						idxB += Character.charCount(cpB));
				
				// Compare the digits sequences as numbers
				int valA = Integer.parseInt(a, startA, idxA, 10);
				int valB = Integer.parseInt(b, startB, idxB, 10);
				result = Integer.compare(valA, valB);
			} else {
				// Simply compare the code points
				result = ignoreCase
					? compareCodePointIgnoreCase(cpA, cpB)
					: compareCodePoint(cpA, cpB);
			}
			
			if(result != 0) {
				return result;
			}
		}
		
		// Reached the end of the smaller strings with them being equal up to this point
		return Integer.compare(lenA, lenB);
	}
	
	/** @since 00.02.09 */
	public static final boolean isSimpleDateTime(String string) {
		return Detections.OfSimpleDateTime.matches(string);
	}
	
	/** @since 00.02.09 */
	private static final int compareSimpleDateTime(Matcher ma, Matcher mb) {
		final int[] idx = { 3, 2, 1, 4, 5, 6 };
		int i = 0, l = idx.length, cmp, a, b;
		String sa = ma.group(idx[i]); // Non-null
		String sb = mb.group(idx[i]); // Non-null
		
		do {
			a = Integer.parseInt(sa);
			b = Integer.parseInt(sb);
		} while((cmp = Integer.compare(a, b)) == 0
					&& ++i < l
					&& (sa = ma.group(idx[i])) != null
					&& (sb = mb.group(idx[i])) != null);
		
		return cmp;
	}
	
	/** @since 00.02.09 */
	public static final int compareSimpleDateTime(String a, String b) {
		if(a == b)    return  0; // nulls are also equal
		if(a == null) return  1; // null will be higher in a list
		if(b == null) return -1; // null will be higher in a list
		
		Matcher ma, mb;
		if((ma = Detections.OfSimpleDateTime.match(a)).find() && ma.start() == 0
				&& (mb = Detections.OfSimpleDateTime.match(b)).find() && mb.start() == 0) {
			int cmp = compareSimpleDateTime(ma, mb);
			Detections.OfSimpleDateTime.unmatch(ma);
			Detections.OfSimpleDateTime.unmatch(mb);
			return cmp;
		}
		
		return a.compareTo(b);
	}
	
	/** @since 00.02.09 */
	public static final int compareNaturalWithDateTime(String a, String b) {
		return compareNaturalWithDateTime(a, b, false);
	}
	
	/** @since 00.02.09 */
	public static final int compareNaturalWithDateTimeIgnoreCase(String a, String b) {
		return compareNaturalWithDateTime(a, b, true);
	}
	
	/** @since 00.02.09 */
	private static final int compareNaturalWithDateTime(String a, String b, boolean ignoreCase) {
		if(a == b)    return  0; // nulls are also equal
		if(a == null) return  1; // null will be higher in a list
		if(b == null) return -1; // null will be higher in a list
		
		Matcher ma, mb;
		if((ma = Detections.OfSimpleDateTime.match(a)).find() && ma.start() == 0
				&& (mb = Detections.OfSimpleDateTime.match(b)).find() && mb.start() == 0) {
			int cmp = compareSimpleDateTime(ma, mb);
			Detections.OfSimpleDateTime.unmatch(ma);
			Detections.OfSimpleDateTime.unmatch(mb);
			return cmp;
		}
		
		return compareNatural(a, b, ignoreCase);
	}
	
	/** @since 00.02.09 */
	public static final StringBuilder utf16StringBuilder() {
		return OfStringBuilder.newUTF16();
	}
	
	/** @since 00.02.09 */
	public static final StringBuilder utf16StringBuilder(int capacity) {
		return OfStringBuilder.newUTF16(capacity);
	}
	
	/** @since 00.02.09 */
	public static final <T> String toString(List<T> list, Function<T, String> mapper) {
		StringBuilder builder = new StringBuilder();
		
		for(T item : list) {
			builder.append(mapper.apply(item)).append('\n');
		}
		
		return builder.toString();
	}
	
	/** @since 00.02.09 */
	public static final <T> Iterator<T> reversed(List<T> list) {
		Objects.requireNonNull(list);
		return new ReversedListIterator<>(list.listIterator(list.size()));
	}
	
	/** @since 00.02.09 */
	public static final <T> Iterable<T> asReversed(List<T> list) {
		Objects.requireNonNull(list);
		return iterable(reversed(list));
	}
	
	/** @since 00.02.09 */
	private static final class ReversedListIterator<T> implements Iterator<T> {
		
		private final ListIterator<T> iterator;
		
		public ReversedListIterator(ListIterator<T> iterator) {
			this.iterator = iterator;
		}
		
		@Override public boolean hasNext() { return iterator.hasPrevious(); }
		@Override public T next() { return iterator.previous(); }
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
	
	/** @since 00.02.08 */
	private static final class Internal {
		
		// Forbid anyone to create an instance of this class
		private Internal() {
		}
		
		public static final Function<Exception, Exception>
				exceptionMapper(Consumer<Exception> exceptionHandler) {
			return exceptionHandler != null
						? ((ex) -> { exceptionHandler.accept(ex); return null; })
						: null;
		}
		
		/** @since 00.02.09 */
		public static final void throwException(Exception exception,
				Function<Exception, Exception> exceptionMapper) throws Exception {
			if(exceptionMapper == null) {
				return;
			}
			
			Exception ex;
			if((ex = exceptionMapper.apply(exception)) != null) {
				throw ex;
			}
		}
		
		public static final void throwRuntimeException(Exception exception,
				Function<Exception, Exception> exceptionMapper) throws RuntimeException {
			if(exceptionMapper == null) {
				return;
			}
			
			Exception ex;
			if((ex = exceptionMapper.apply(exception)) != null) {
				throw new RuntimeException(ex);
			}
		}
	}
	
	/** @since 00.02.09 */
	private static final class OfStringBuilder {
		
		private static final int DEFAULT_CAPACITY = 16;
		private static final StringBuilder UTF16 = newInternalBuilder();
		
		private static final StringBuilder newInternalBuilder() {
			// Create a new builder to accommodate 1 Latin1 character.
			// Assume that compact strings are turned on, therefore this
			// builder will have the Latin1 coder.
			StringBuilder builder = new StringBuilder(1);
			// Append a character that cannot be encoded in Latin1,
			// thus changing the coder to UTF16. The character must fulfill
			// (c >>> 8) == 1 to force this change.
			builder.appendCodePoint(1 << 8);
			// The builder has UTF16 coder with byte array of length 2 now.
			// To be able to reuse this buffer, clear it.
			builder.setLength(0);
			// Now, we have an empty builder with the UTF16 coder, with
			// the minimal possible internal capacity of 2.
			return builder;
		}
		
		public static final StringBuilder newUTF16() {
			return newUTF16(DEFAULT_CAPACITY);
		}
		
		public static final StringBuilder newUTF16(int capacity) {
			// Create a new builder with no capacity to minimize the memory
			// footprint. Assume that this builder has the Latin1 coder.
			StringBuilder builder = new StringBuilder(0);
			// Force the new builder to use the UTF16 coder using the pre-built
			// internal builder.
			builder.append(UTF16);
			// Resize the builder to the desired capacity.
			builder.ensureCapacity(capacity);
			// Now the builder is using the UTF16 coder and has the desired
			// capacity.
			return builder;
		}
	}
	
	/** @since 00.02.09 */
	private static final class Detections {
		
		private Detections() {
		}
		
		private static final Matcher matcher(String key, Regex regex) {
			return ThreadLocals.getOrSupply(key, MatcherPool::new).create(regex);
		}
		
		private static final void dispose(String key, Matcher matcher) {
			ThreadLocals.getOrSupply(key, MatcherPool::new).dispose(matcher);
		}
		
		private static final class OfSimpleDateTime {
			
			private static final String KEY = "simpleDateTime";
			
			private static final Regex REGEX_SIMPLE_DATE_TIME = Regex.of(
				  "(3[01]|[12][0-9]|0?[1-9])\\.\\s*" // Day (optional leading zero)
				+ "(1[0-2]|0?[1-9])\\.\\s*" // Month (optional leading zero)
				+ "(19[7-9][0-9]|[2-9][0-9]{3}|[1-9][0-9]{4,})" // Year (1970+)
				+ "(?:" // Start of optional group for time
				+ "(?:T|\\s+)" // Time separator (T or whitespaces characters)
				+ "(2[0-3]|1[0-9]|0?[0-9]):" // Hours (0-24 with optional leading zero)
				+ "(0[0-9]|[1-5][0-9])" // Minutes (00-59 with required leading zero)
				+ "(?::(0[0-9]|[1-5][0-9]))?" // Seconds (optional, 00-59 with required leading zero)
				+ ")?" // End of optional group for time
			);
			
			public static final Matcher match(String string) {
				return matcher(KEY, REGEX_SIMPLE_DATE_TIME).reset(string);
			}
			
			public static final boolean matches(String string) {
				Matcher matcher = match(string);
				boolean result = matcher.matches();
				unmatch(matcher);
				return result;
			}
			
			public static final void unmatch(Matcher matcher) {
				dispose(KEY, matcher);
			}
		}
		
		private static final class MatcherPool {
			
			private Map<Matcher, Boolean> matchers;
			
			private final Matcher emptyMatcher(Regex regex) {
				return regex.matcher("");
			}
			
			public Matcher create(Regex regex) {
				if(matchers == null) {
					matchers = new WeakHashMap<>(4);
				}
				
				for(Entry<Matcher, Boolean> entry : matchers.entrySet()) {
					if(!entry.getValue()) {
						entry.setValue(true);
						return entry.getKey();
					}
				}
				
				Matcher matcher = emptyMatcher(regex);
				matchers.put(matcher, true);
				
				return matcher;
			}
			
			public void dispose(Matcher matcher) {
				if(matchers == null) {
					return;
				}
				
				matchers.put(matcher, false);
			}
		}
		
		private static final class ThreadLocals {
			
			private static final ThreadLocal<Map<String, Object>> map = ThreadLocal.withInitial(HashMap::new);
			
			private ThreadLocals() {
			}
			
			public static final <T> T getOrSupply(String key, Supplier<T> supplier) {
				Map<String, Object> values = map.get();
				Object value = values.getOrDefault(key, UNSET);
				
				if(value == UNSET) {
					value = supplier.get();
					values.put(key, value);
				}
				
				@SuppressWarnings("unchecked")
				T casted = (T) value;
				return casted;
			}
		}
	}
	
	/** @since 00.02.09 */
	public static final class OfString {
		
		private static final VarLoader<Collator> collatorNormalized = VarLoader.of(OfString::newCollatorNormalized);
		
		// Forbid anyone to create an instance of this class
		private OfString() {
		}
		
		private static final Collator newCollatorNormalized() {
			Collator collator = Collator.getInstance();
			collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
			return collator;
		}
		
		private static final int intCharOrSign(int c, int radix) {
			int v;
			return (v = Character.digit(c, radix)) != -1
						|| (v = c) == '-' || (v = c) == '+'
					? v : -1;
		}
		
		private static final int intCharOrSignOrRoman(int c, int radix) {
			int v;
			return (v = Character.digit(c, radix)) != -1
						|| (v = c) == '-' || (v = c) == '+'
						|| (v = ROMAN_NUMERALS.getOrDefault((char) c, -1)) != -1
					? v : -1;
		}
		
		private static final int intChar(int c, int radix) {
			int v;
			return (v = Character.digit(c, radix)) != -1 ? v : -1;
		}
		
		private static final int intCharOrRoman(int c, int radix) {
			int v;
			return (v = Character.digit(c, radix)) != -1
						|| (v = ROMAN_NUMERALS.getOrDefault((char) c, -1)) != -1
					? v : -1;
		}
		
		private static final int intCharRadix(int c, int radix) {
			int v;
			return (v = Character.digit(c, radix)) != -1 ? v : -1;
		}
		
		private static final int intCharRoman(int c) {
			return ROMAN_NUMERALS.getOrDefault((char) c, -1);
		}
		
		private static final boolean isIntSign(int c) {
			return c == '-' || c == '+';
		}
		
		private static final boolean isIntRoman(int c) {
			return ROMAN_NUMERALS.containsKey((char) c);
		}
		
		private static final Integer asInteger(String string, int radix, boolean romanNumerals, int start, int end,
				Integer defaultValue) {
			if(string == null) {
				throw new NullPointerException();
			}
			
			do {
				int i, c = 0, v = 0, value = 0;
				boolean isNegative = false;
				
				if(romanNumerals) {
					// Skip to the first integer character or a sign symbol
					for(i = start;
						i < end && (v = intCharOrSignOrRoman(c = string.codePointAt(i), radix)) == -1;
						i += charCount(c));
				} else {
					// Skip to the first integer character or a sign symbol
					for(i = start;
						i < end && (v = intCharOrSign(c = string.codePointAt(i), radix)) == -1;
						i += charCount(c));
				}
				
				// No integer character or sign symbol found
				if(i >= end) {
					return defaultValue;
				}
				
				// Sign symbol can only be the first character
				if(isIntSign(c)) {
					isNegative = c == '-';
					i += charCount(c);
					
					if(romanNumerals) {
						// Move to the next character and check
						if((v = intCharOrRoman(c = string.codePointAt(i), radix)) == -1) {
							// Cannot have just the sign symbol, try to find the next integer
							start = i + 1;
							continue;
						}
					} else {
						// Move to the next character and check
						if((v = intChar(c = string.codePointAt(i), radix)) == -1) {
							// Cannot have just the sign symbol, try to find the next integer
							start = i + 1;
							continue;
						}
					}
				}
				
				if(romanNumerals && isIntRoman(c)) {
					value -= v;
					i += charCount(c);
					
					// Roman integer
					for(int t, u = v;
						i < end && (v = intCharRoman(c = string.codePointAt(i))) != -1;
						i += charCount(c), value = t, u = v) {
						t = value - (v > u ? v - 2 * u : v);
						
						// Check of integer overflow
						if(t > value) {
							throw new ArithmeticException("Integer overflow");
						}
					}
				} else {
					value -= v;
					i += charCount(c);
					
					// Radix integer
					for(int t;
						i < end && (v = intCharRadix(c = string.codePointAt(i), radix)) != -1;
						i += charCount(c), value = t) {
						t = value * radix - v;
						
						// Check of integer overflow
						if(t > value) {
							throw new ArithmeticException("Integer overflow");
						}
					}
				}
				
				// Check for non-negatable integer minimum value
				if(!isNegative && value == Integer.MIN_VALUE) {
					throw new ArithmeticException("Integer overflow");
				}
				
				return isNegative ? value : -value;
			} while(true);
		}
		
		private static final int checkInt(Integer v) {
			if(v == null) {
				throw new IllegalStateException("No integer present");
			}
			
			return (int) v;
		}
		
		public static final int compareNormalized(String a, String b) {
			return collatorNormalized.value().compare(a, b);
		}
		
		public static final Comparator<String> comparingNormalized() {
			return OfString::compareNormalized;
		}
		
		public static final <T> Comparator<T> comparingNormalized(Function<T, String> extractor) {
			Objects.requireNonNull(extractor);
			return (a, b) -> compareNormalized(extractor.apply(a), extractor.apply(b));
		}
		
		public static final boolean nonEmpty(String value) {
			return value != null && !value.isEmpty();
		}
		
		public static final boolean nonBlank(String value) {
			return value != null && !value.isBlank();
		}
		
		public static final String concat(String separator, String... values) {
			return concat(separator, null, values);
		}
		
		public static final String concat(String separator, Predicate<String> include, String... values) {
			Objects.requireNonNull(separator);
			
			if(values.length == 0) {
				return "";
			}
			
			StringBuilder string = new StringBuilder();
			boolean first = true;
			
			if(include == null) {
				for(String value : values) {
					if(first) first = false;
					else string.append(separator);
					
					string.append(value);
				}
			} else {
				for(String value : values) {
					if(!include.test(value)) {
						continue;
					}
					
					if(first) first = false;
					else string.append(separator);
					
					string.append(value);
				}
			}
			
			return string.toString();
		}
		
		public static final String[] split(String string) {
			return split(string, 0, 0, string.length());
		}
		
		public static final String[] split(String string, int limit) {
			return split(string, limit, 0, string.length());
		}
		
		public static final String[] split(String string, int limit, int start) {
			return split(string, limit, start, string.length());
		}
		
		public static final String[] split(String string, int limit, int start, int end) {
			if(string == null) {
				throw new NullPointerException();
			}
			
			if(start < 0 || start >= end) {
				return new String[0];
			}
			
			IntStream stream = string.codePoints();
			int count = end - start - 1;
			
			if(limit > 0) {
				count = Math.min(count, limit - 1);
			}
			
			return Stream.concat(
				stream.skip(start).limit(count).mapToObj(Character::toString),
				Stream.of(string.substring(start + count, end))
			).toArray(String[]::new);
		}
		
		public static final String[] split(String string, String separator) {
			return split(string, separator, 0, 0, string.length());
		}
		
		public static final String[] split(String string, String separator, int limit) {
			return split(string, separator, limit, 0, string.length());
		}
		
		public static final String[] split(String string, String separator, int limit, int start) {
			return split(string, separator, limit, start, string.length());
		}
		
		public static final String[] split(String string, String separator, int limit, int start, int end) {
			if(string == null || separator == null) {
				throw new NullPointerException();
			}
			
			if(separator.isEmpty()) {
				return split(string, limit, start, end);
			}
			
			if(start < 0 || start >= end) {
				return new String[0];
			}
			
			int pos = start;
			int idx = string.indexOf(separator, pos);
			
			// Separator not found in <start, end)
			if(idx < 0) {
				return new String[] { string.substring(start, end) };
			}
			
			int len = separator.length();
			
			// Separator found outside <start, end)
			if(idx > end - len) {
				return new String[] { string.substring(start, end) };
			}
			
			String[] parts;
			
			if(limit > 0) {
				parts = new String[limit];
				
				for(int i = 0;
					--limit > 0
						&& (idx = string.indexOf(separator, pos)) >= 0
						&& (idx) <= end - len;
					++i, pos = idx + len) {
					parts[i] = string.substring(pos, idx);
				}
				
				parts[parts.length - 1] = string.substring(pos, end);
			} else {
				List<String> list = new ArrayList<>();
				
				do {
					list.add(string.substring(pos, idx));
					pos = idx + len;
				} while((idx = string.indexOf(separator, pos)) >= 0 && (idx) <= end - len);
				
				if(pos <= end) {
					list.add(string.substring(pos, end));
				}
				
				int to = list.size();
				
				// Remove trailing empty strings
				if(limit == 0) {
					for(; to > 0 && list.get(to - 1).equals(""); --to);
				}
				
				parts = list.subList(0, to).toArray(String[]::new);
			}
			
			return parts;
		}
		
		public static final String delete(String string, int start, int end) {
			return replace(string, start, end, null);
		}
		
		public static final String replace(String string, int start, int end, String replacement) {
			if(string == null) {
				throw new IllegalArgumentException();
			}
			
			int length = string.length();
			
			if(start < 0 || start >= length || end < start || end > length) {
				throw new StringIndexOutOfBoundsException();
			}
			
			int newLength = string.length() - (end - start);
			
			if(replacement != null) {
				newLength += replacement.length();
			}
			
			StringBuilder builder = new StringBuilder(newLength);
			
			if(start > 0) {
				builder.append(string, 0, start);
			}
			
			if(replacement != null) {
				builder.append(replacement);
			}
			
			if(end < length) {
				builder.append(string, end, length);
			}
			
			return builder.toString();
		}
		
		public static final Integer asInteger(String string) {
			return asInteger(string, 10, true, 0, string.length(), null);
		}
		
		public static final Integer asInteger(String string, int radix) {
			return asInteger(string, radix, true, 0, string.length(), null);
		}
		
		public static final Integer asInteger(String string, int radix, boolean romanNumerals) {
			return asInteger(string, radix, romanNumerals, 0, string.length(), null);
		}
		
		public static final Integer asInteger(String string, int start, int end) {
			return asInteger(string, 10, true, start, end, null);
		}
		
		public static final Integer asInteger(String string, int radix, int start, int end) {
			return asInteger(string, radix, true, start, end, null);
		}
		
		public static final Integer asInteger(String string, int radix, boolean romanNumerals, int start, int end) {
			return asInteger(string, radix, romanNumerals, start, end, null);
		}
		
		public static final Integer asIntegerOrDefault(String string, Integer defaultValue) {
			return asInteger(string, 10, true, 0, string.length(), defaultValue);
		}
		
		public static final Integer asIntegerOrDefault(String string, int radix, Integer defaultValue) {
			return asInteger(string, radix, true, 0, string.length(), defaultValue);
		}
		
		public static final Integer asIntegerOrDefault(String string, int radix, boolean romanNumerals,
				Integer defaultValue) {
			return asInteger(string, radix, romanNumerals, 0, string.length(), defaultValue);
		}
		
		public static final Integer asIntegerOrDefault(String string, int start, int end, Integer defaultValue) {
			return asInteger(string, 10, true, start, end, defaultValue);
		}
		
		public static final Integer asIntegerOrDefault(String string, int radix, int start, int end,
				Integer defaultValue) {
			return asInteger(string, radix, true, start, end, defaultValue);
		}
		
		public static final Integer asIntegerOrDefault(String string, int radix, boolean romanNumerals, int start,
				int end, Integer defaultValue) {
			return asInteger(string, radix, romanNumerals, start, end, defaultValue);
		}
		
		public static final int asInt(String string) {
			return checkInt(asInteger(string, 10, true, 0, string.length(), null));
		}
		
		public static final int asInt(String string, int radix) {
			return checkInt(asInteger(string, radix, true, 0, string.length(), null));
		}
		
		public static final int asInt(String string, int radix, boolean romanNumerals) {
			return checkInt(asInteger(string, radix, romanNumerals, 0, string.length(), null));
		}
		
		public static final int asInt(String string, int start, int end) {
			return checkInt(asInteger(string, 10, true, start, end, null));
		}
		
		public static final int asInt(String string, int radix, int start, int end) {
			return checkInt(asInteger(string, radix, true, start, end, null));
		}
		
		public static final int asInt(String string, int radix, boolean romanNumerals, int start, int end) {
			return checkInt(asInteger(string, radix, romanNumerals, start, end, null));
		}
		
		public static final int asIntOrDefault(String string, int defaultValue) {
			return asInteger(string, 10, true, 0, string.length(), defaultValue);
		}
		
		public static final int asIntOrDefault(String string, int radix, int defaultValue) {
			return asInteger(string, radix, true, 0, string.length(), defaultValue);
		}
		
		public static final int asIntOrDefault(String string, int radix, boolean romanNumerals, int defaultValue) {
			return asInteger(string, radix, romanNumerals, 0, string.length(), defaultValue);
		}
		
		public static final int asIntOrDefault(String string, int start, int end, int defaultValue) {
			return asInteger(string, 10, true, start, end, defaultValue);
		}
		
		public static final int asIntOrDefault(String string, int radix, int start, int end, int defaultValue) {
			return asInteger(string, radix, true, start, end, defaultValue);
		}
		
		public static final int asIntOrDefault(String string, int radix, boolean romanNumerals, int start, int end,
				int defaultValue) {
			return asInteger(string, radix, romanNumerals, start, end, defaultValue);
		}
	}
	
	/** @since 00.02.09 */
	public static final class StringOps {
		
		private StringBuilder input;
		private StringBuilder output;
		private boolean swapped;
		
		public StringOps(StringBuilder input) {
			this.input = Objects.requireNonNull(input);
			this.output = null;
		}
		
		private final void ensureOutput() {
			if(output == null) {
				output = utf16StringBuilder(input.length());
			}
		}
		
		private final void swap() {
			StringBuilder temp = input;
			input = output;
			output = temp;
			swapped = !swapped;
		}
		
		private final void clearOutput() {
			if(output != null) {
				output.setLength(0);
				output.ensureCapacity(input.length());
			}
		}
		
		private final void swapAndClear() {
			swap();
			clearOutput();
		}
		
		public StringOps replaceUnicodeEscapeSequences() {
			ensureOutput();
			UnicodeEscapeSequence.replace(input, output);
			swapAndClear();
			return this;
		}
		
		public StringOps prefixUnicodeEscapeSequences(String prefix) {
			ensureOutput();
			UnicodeEscapeSequence.prefix(input, output, prefix);
			swapAndClear();
			return this;
		}
		
		public StringOps unslash() {
			ensureOutput();
			Utils.unslash(input, output);
			swapAndClear();
			return this;
		}
		
		public StringOps reset() {
			if(swapped) {
				swap();
			}
			
			clearOutput();
			return this;
		}
		
		public String toStringAndReset() {
			String string = input.toString();
			reset();
			return string;
		}
		
		@Override
		public String toString() {
			return input.toString();
		}
	}
	
	/** @since 00.02.09 */
	public static final class UnicodeEscapeSequence {
		
		/* Implementation notes
		 * 
		 * The searching of Unicode escape sequences is done using a simple DFA
		 * (Deterministic Finite Automaton) with the following graph:
		 * 
		 * (0)----+-[x]->(1)-[y]->(2)-------+-[z]->(3)-[z]->(4)-[z]->(5)-[z]->(6)
		 *  ^     |       |        |^       |       |        |        |        |
		 *  |     |       |        | \      |       |        |        |        |
		 *  +-[A]-+       |        |  +-[y]-+       |        |        |        |
		 *  |             |        |                |        |        |        |
		 *  +-----[B]-----+        |                |        |        |        |
		 *  |                      |                |        |        |        |
		 *  +----------[C]---------+                |        |        |        |
		 *  |                                       |        |        |        |
		 *  +--------------[D]----------------------+        |        |        |
		 *  |                                                |        |        |
		 *  +-------------------[D]--------------------------+        |        |
		 *  |                                                         |        |
		 *  +-----------------------[D]-------------------------------+        |
		 *  |                                                                  |
		 *  +---------------------------[e]------------------------------------+
		 * 
		 * Where:
		 *     - (.) is a state (0 - 6)
		 *     - [.] is a transition symbols (x, y, z, A, B, C, D, e)
		 *     - E = the whole alphabet
		 * 
		 * Transition symbols:
		 *     - x = { '\' }
		 *     - y = { 'u' }
		 *     - z = { A-F, a-f, 0-9 }
		 *     - A = E \ x
		 *     - B = E \ y
		 *     - C = E \ y \ z
		 *     - D = E \ z
		 *     - e = automatic transition 6 -> 0, executing the desired operation
		 * 
		 * The DFA implements the following regular expression:
		 *     \\u+([A-Fa-f0-9]{4})
		 * where the matching group is then forwarded as an argument to the desired
		 * operation, such as replace or prefix.
		 * 
		 * The run of the DFA is done branchless using only arithmetic operations
		 * with the exception of checking for few states to simulate the capturing
		 * of an Unicode espace sequence.
		 */
		
		private static final int CHAR_BWSLASH = '\\';
		private static final int CHAR_U = 'u';
		private static final int INTERVAL_LO_AF = 'A';
		private static final int INTERVAL_HI_AF = 'F';
		private static final int INTERVAL_LO_af = 'a';
		private static final int INTERVAL_HI_af = 'f';
		private static final int INTERVAL_LO_09 = '0';
		private static final int INTERVAL_HI_09 = '9';
		
		// Returns 1, if c == r, otherwise 0.
		private static final int eq(int c, int r) {
			// c > r: (c - r) >>> 31 = 0, (r - c) >>> 31 = 1, xor = 1, 1 - xor = 0
			// c < r: (c - r) >>> 31 = 1, (r - c) >>> 31 = 0, xor = 1, 1 - xor = 0
			// c = r: (c - r) >>> 31 = 0, (r - c) >>> 31 = 0, xor = 0, 1 - xor = 1
			return 1 - (((c - r) >>> 31) ^ ((r - c) >>> 31));
		}
		
		// Returns 1, if c > r, otherwise 0.
		private static final int gt(int c, int r) {
			// c >  r, (r - c) >>> 31 = 1
			// c <= r, (r - c) >>> 31 = 0
			return ((r - c) >>> 31);
		}
		
		// Returns 1, if c is in the interval [a, b] (inclusive), where a <= b, otherwise 0.
		private static final int in(int c, int a, int b) {
			// c <  a, c <  b, (c - a) >>> 31 = 1, (b - c) >>> 31 = 0, xor = 1, 1 - xor = 0
			// c >= a, c <  b, (c - a) >>> 31 = 0, (b - c) >>> 31 = 0, xor = 0, 1 - xor = 1
			// c >= a, c >= b, (c - a) >>> 31 = 0, (b - c) >>> 31 = 1, xor = 1, 1 - xor = 0
			return 1 - (((c - a) >>> 31) ^ ((b - c) >>> 31));
		}
		
		// Returns 1, if c is in [A-Fa-f0-9], otherwise 0.
		private static final int isHex(int c) {
			// Individually check intervals A-F, a-f, 0-9 and combine results using the OR operation.
			return (
				in(c, INTERVAL_LO_AF, INTERVAL_HI_AF) |
				in(c, INTERVAL_LO_af, INTERVAL_HI_af) |
				in(c, INTERVAL_LO_09, INTERVAL_HI_09)
			);
		}
		
		// x = c == '\'
		// y = c == 'u'
		// z = c is hex
		private static final int[] dfaTable = {
		//  x, y, z
			1, 0, 0,
			0, 2, 0,
			0, 2, 3,
			0, 0, 4,
			0, 0, 5,
			0, 0, 6
		};
		
		// Works only for 0 <= r <= 4.
		// Assume that f(i) = i with only the lowest one bit set, then
		// this returns:
		//     0, if f(i) == 0,
		//     1, if f(i) == 1,
		//     2, if f(i) == 2,
		//     3, if f(i) == 4,
		private static final int dfaIdx(int r) {
			return Integer.lowestOneBit(r) - gt(r, 3);
		}
		
		private static final int transition(int s, int c) {
			/* f(s, c) = {
			 *     c == '\\'                                  -> 0,
			 *     c == 'u'                                   -> 1,
			 *     c in [A-Fa-f0-9]                           -> 2,
			 *     c != '\\'                        && s == 0 -> 3,
			 *     c != 'u'                         && s == 1 -> 4,
			 *     c not in [A-Fa-f0-9]             && s  > 2 -> 5,
			 *     c not in [A-Fa-f0-9] && c != 'u' && s == 2 -> 6,
			 * }
			 */
			final int idx = dfaIdx(
				(isHex(c)            << 2) |
				(eq(c, CHAR_U)       << 1) |
				(eq(c, CHAR_BWSLASH) << 0)
			), gt0 = gt(idx, 0);
			
			return gt0 * dfaTable[s * 3 + (idx - gt0 * 1)];
		}
		
		private static final void doOperation(StringBuilder input, StringBuilder output, final int len,
				OperationStringBuilder operation) {
			int end = 0;
			
			for(int i = 0, c, n, state = 0, next, start = -1; i < len; i += n, state = next) {
				c = input.codePointAt(i);
				n = Character.charCount(c);
				next = transition(state, c);
				
				switch(next) {
					case 1: {
						start = i;
						break;
					}
					case 6: {
						output.append(input, end, start);
						
						end = i + n;
						operation.execute(output, input, start, end);
						
						next = 0;
						start = -1;
						break;
					}
				}
			}
			
			if(end < len) {
				output.append(input, end, len);
			}
		}
		
		// Since CharSequence does not have codePointAt method, we must copy the method above
		// with the only exception of using a String instead of a StringBuilder as an input.
		private static final void doOperation(String input, StringBuilder output, int len, OperationString operation) {
			int end = 0;
			
			for(int i = 0, c, n, state = 0, next, start = -1; i < len; i += n, state = next) {
				c = input.codePointAt(i);
				n = Character.charCount(c);
				next = transition(state, c);
				
				switch(next) {
					case 1: {
						start = i;
						break;
					}
					case 6: {
						output.append(input, end, start);
						
						end = i + n;
						operation.execute(output, input, start, end);
						
						next = 0;
						start = -1;
						break;
					}
				}
			}
			
			if(end < len) {
				output.append(input, end, len);
			}
		}
		
		private static final void opReplace(StringBuilder output, StringBuilder input, int start, int end) {
			output.appendCodePoint(Integer.parseInt(input, start + 2, end, 16));
		}
		
		private static final void opPrefix(StringBuilder output, StringBuilder input, int start, int end, String prefix) {
			output.append(prefix).append(input, start, end);
		}
		
		private static final void opReplace(StringBuilder output, String input, int start, int end) {
			output.appendCodePoint(Integer.parseInt(input, start + 2, end, 16));
		}
		
		private static final void opPrefix(StringBuilder output, String input, int start, int end, String prefix) {
			output.append(prefix).append(input, start, end);
		}
		
		public static final void replace(StringBuilder input, StringBuilder output) {
			final int len = input.length();
			doOperation(input, output, len, UnicodeEscapeSequence::opReplace);
		}
		
		public static final void replace(String input, StringBuilder output) {
			final int len = input.length();
			doOperation(input, output, len, UnicodeEscapeSequence::opReplace);
		}
		
		public static final String replace(StringBuilder input) {
			final int len = input.length();
			StringBuilder builder = utf16StringBuilder(len);
			doOperation(input, builder, len, UnicodeEscapeSequence::opReplace);
			return builder.toString();
		}
		
		public static final String replace(String string) {
			final int len = string.length();
			StringBuilder builder = utf16StringBuilder(len);
			doOperation(string, builder, len, UnicodeEscapeSequence::opReplace);
			return builder.toString();
		}
		
		public static final void prefix(StringBuilder input, StringBuilder output, String prefix) {
			final int len = input.length();
			doOperation(input, output, len, (out, in, start, end) -> opPrefix(out, in, start, end, prefix));
		}
		
		public static final void prefix(String input, StringBuilder output, String prefix) {
			final int len = input.length();
			doOperation(input, output, len, (out, in, start, end) -> opPrefix(out, in, start, end, prefix));
		}
		
		public static final String prefix(StringBuilder input, String prefix) {
			final int len = input.length();
			StringBuilder builder = utf16StringBuilder(len);
			doOperation(input, builder, len, (out, in, start, end) -> opPrefix(out, in, start, end, prefix));
			return builder.toString();
		}
		
		public static final String prefix(String string, String prefix) {
			final int len = string.length();
			StringBuilder builder = utf16StringBuilder(len);
			doOperation(string, builder, len, (out, in, start, end) -> opPrefix(out, in, start, end, prefix));
			return builder.toString();
		}
		
		private static interface OperationStringBuilder {
			
			void execute(StringBuilder output, StringBuilder input, int start, int end);
		}
		
		private static interface OperationString {
			
			void execute(StringBuilder output, String input, int start, int end);
		}
	}
	
	/** @since 00.02.08 */
	public static final class Suppress {
		
		// Forbid anyone to create an instance of this class
		private Suppress() {
		}
		
		public static final <T> Consumer<T> consumer(CheckedConsumer<T> consumer) {
			return consumer(consumer, (Function<Exception, Exception>) null);
		}
		
		public static final <T> Consumer<T> consumer(CheckedConsumer<T> consumer,
				Consumer<Exception> exceptionHandler) {
			return consumer(consumer, Internal.exceptionMapper(exceptionHandler));
		}
		
		public static final <T> Consumer<T> consumer(CheckedConsumer<T> consumer,
				Function<Exception, Exception> exceptionMapper) {
			return ((t) -> {
				try {
					consumer.accept(t);
				} catch(Exception ex) {
					Internal.throwRuntimeException(ex, exceptionMapper);
				}
			});
		}
		
		public static final <T> Runnable runnable(CheckedRunnable runnable) {
			return runnable(runnable, (Function<Exception, Exception>) null);
		}
		
		public static final <T> Runnable runnable(CheckedRunnable runnable,
				Consumer<Exception> exceptionHandler) {
			return runnable(runnable, Internal.exceptionMapper(exceptionHandler));
		}
		
		public static final <T> Runnable runnable(CheckedRunnable runnable,
				Function<Exception, Exception> exceptionMapper) {
			return (() -> supplier(runnable, exceptionMapper).get());
		}
		
		public static final <T> Supplier<Boolean> supplier(CheckedRunnable runnable) {
			return supplier(runnable, (Function<Exception, Exception>) null);
		}
		
		public static final <T> Supplier<Boolean> supplier(CheckedRunnable runnable,
				Consumer<Exception> exceptionHandler) {
			return supplier(runnable, Internal.exceptionMapper(exceptionHandler));
		}
		
		public static final <T> Supplier<Boolean> supplier(CheckedRunnable runnable,
				Function<Exception, Exception> exceptionMapper) {
			return (() -> {
				try {
					runnable.run();
					return true;
				} catch(Exception ex) {
					Internal.throwRuntimeException(ex, exceptionMapper);
					return false;
				}
			});
		}
	}
	
	/** @since 00.02.08 */
	public static final class Ignore {
		
		// Forbid anyone to create an instance of this class
		private Ignore() {
		}
		
		/** @since 00.02.09 */
		private static final <T> T throwingIgnore(Callable<T> callable, Supplier<T> defaultValueSupplier,
				Function<Exception, Exception> exceptionMapper) throws Exception {
			try {
				return callable.call();
			} catch(Exception ex) {
				Internal.throwException(ex, exceptionMapper);
			}
			
			return defaultValueSupplier != null
						? defaultValueSupplier.get()
						: null;
		}
		
		private static final <T> T ignore(Callable<T> callable, Supplier<T> defaultValueSupplier,
				Function<Exception, Exception> exceptionMapper) {
			try {
				return callable.call();
			} catch(Exception ex) {
				Internal.throwRuntimeException(ex, exceptionMapper);
			}
			
			return defaultValueSupplier != null
						? defaultValueSupplier.get()
						: null;
		}
		
		public static final <T> T call(Callable<T> callable) {
			return call(callable, (Function<Exception, Exception>) null);
		}
		
		public static final <T> T call(Callable<T> callable, Consumer<Exception> exceptionHandler) {
			return call(callable, Internal.exceptionMapper(exceptionHandler));
		}
		
		public static final <T> T call(Callable<T> callable, Function<Exception, Exception> exceptionMapper) {
			return defaultValue(callable, null, exceptionMapper);
		}
		
		public static final <T> void callVoid(CheckedRunnable runnable) {
			callVoid(runnable, (Function<Exception, Exception>) null);
		}
		
		public static final <T> void callVoid(CheckedRunnable runnable, Consumer<Exception> exceptionHandler) {
			callVoid(runnable, Internal.exceptionMapper(exceptionHandler));
		}
		
		public static final <T> void callVoid(CheckedRunnable runnable,
				Function<Exception, Exception> exceptionMapper) {
			Suppress.runnable(runnable, exceptionMapper).run();
		}
		
		public static final <T> boolean callAndCheck(CheckedRunnable runnable) {
			return callAndCheck(runnable, (Function<Exception, Exception>) null);
		}
		
		public static final <T> boolean callAndCheck(CheckedRunnable runnable, Consumer<Exception> exceptionHandler) {
			return callAndCheck(runnable, Internal.exceptionMapper(exceptionHandler));
		}
		
		public static final <T> boolean callAndCheck(CheckedRunnable runnable,
				Function<Exception, Exception> exceptionMapper) {
			return Suppress.supplier(runnable, exceptionMapper).get();
		}
		
		public static final <T> T defaultValue(Callable<T> callable, T defaultValue) {
			return defaultValue(callable, defaultValue, (Function<Exception, Exception>) null);
		}
		
		public static final <T> T defaultValue(Callable<T> callable, T defaultValue,
				Consumer<Exception> exceptionHandler) {
			return supplier(callable, () -> defaultValue, exceptionHandler);
		}
		
		public static final <T> T defaultValue(Callable<T> callable, T defaultValue,
				Function<Exception, Exception> exceptionMapper) {
			return supplier(callable, () -> defaultValue, exceptionMapper);
		}
		
		public static final <T> T supplier(Callable<T> callable, Supplier<T> defaultValueSupplier) {
			return supplier(callable, defaultValueSupplier, (Function<Exception, Exception>) null);
		}
		
		public static final <T> T supplier(Callable<T> callable, Supplier<T> defaultValueSupplier,
				Consumer<Exception> exceptionHandler) {
			return supplier(callable, defaultValueSupplier, Internal.exceptionMapper(exceptionHandler));
		}
		
		public static final <T> T supplier(Callable<T> callable, Supplier<T> defaultValueSupplier,
				Function<Exception, Exception> exceptionMapper) {
			return ignore(callable, defaultValueSupplier, exceptionMapper);
		}
		
		public static final class Cancellation {
			
			// Forbid anyone to create an instance of this class
			private Cancellation() {
			}
			
			/** @since 00.02.09 */
			private static final Function<Exception, Exception> exceptionMapper(
					Consumer<Exception> exceptionHandler) {
				if(exceptionHandler == null) {
					return null;
				}
				
				return ((exception) -> {
					if(exception instanceof CancellationException
							|| exception instanceof InterruptedException) {
						return null;
					}
					
					exceptionHandler.accept(exception);
					return null;
				});
			}
			
			private static final Function<Exception, Exception> exceptionMapper(
					Function<Exception, Exception> exceptionMapper) {
				return ((exception) -> {
					if(exception instanceof CancellationException
							|| exception instanceof InterruptedException) {
						return null;
					}
					
					return exceptionMapper != null
								? exceptionMapper.apply(exception)
								: exception;
				});
			}
			
			public static final <T> T call(Callable<T> callable) throws Exception {
				return call(callable, (Function<Exception, Exception>) null);
			}
			
			public static final <T> T call(Callable<T> callable, Consumer<Exception> exceptionHandler) throws Exception {
				return call(callable, exceptionMapper(exceptionHandler));
			}
			
			public static final <T> T call(Callable<T> callable, Function<Exception, Exception> exceptionMapper)
					throws Exception {
				return defaultValue(callable, null, exceptionMapper);
			}
			
			public static final <T> void callVoid(CheckedRunnable runnable) throws Exception {
				callVoid(runnable, (Function<Exception, Exception>) null);
			}
			
			public static final <T> void callVoid(CheckedRunnable runnable, Consumer<Exception> exceptionHandler)
					throws Exception {
				callVoid(runnable, exceptionMapper(exceptionHandler));
			}
			
			public static final <T> void callVoid(CheckedRunnable runnable,
					Function<Exception, Exception> exceptionMapper) throws Exception {
				Suppress.runnable(runnable, exceptionMapper(exceptionMapper)).run();
			}
			
			public static final <T> boolean callAndCheck(CheckedRunnable runnable) throws Exception {
				return callAndCheck(runnable, (Function<Exception, Exception>) null);
			}
			
			public static final <T> boolean callAndCheck(CheckedRunnable runnable, Consumer<Exception> exceptionHandler)
					throws Exception {
				return callAndCheck(runnable, exceptionMapper(exceptionHandler));
			}
			
			public static final <T> boolean callAndCheck(CheckedRunnable runnable,
					Function<Exception, Exception> exceptionMapper) throws Exception {
				return Suppress.supplier(runnable, exceptionMapper(exceptionMapper)).get();
			}
			
			public static final <T> T defaultValue(Callable<T> callable, T defaultValue) throws Exception {
				return defaultValue(callable, defaultValue, (Function<Exception, Exception>) null);
			}
			
			public static final <T> T defaultValue(Callable<T> callable, T defaultValue,
					Consumer<Exception> exceptionHandler) throws Exception {
				return supplier(callable, () -> defaultValue, exceptionHandler);
			}
			
			public static final <T> T defaultValue(Callable<T> callable, T defaultValue,
					Function<Exception, Exception> exceptionMapper) throws Exception {
				return supplier(callable, () -> defaultValue, exceptionMapper);
			}
			
			public static final <T> T supplier(Callable<T> callable, Supplier<T> defaultValueSupplier)
					throws Exception {
				return supplier(callable, defaultValueSupplier, (Function<Exception, Exception>) null);
			}
			
			public static final <T> T supplier(Callable<T> callable, Supplier<T> defaultValueSupplier,
					Consumer<Exception> exceptionHandler) throws Exception {
				return supplier(callable, defaultValueSupplier, exceptionMapper(exceptionHandler));
			}
			
			public static final <T> T supplier(Callable<T> callable, Supplier<T> defaultValueSupplier,
					Function<Exception, Exception> exceptionMapper) throws Exception {
				return throwingIgnore(callable, defaultValueSupplier, exceptionMapper(exceptionMapper));
			}
		}
	}
	
	/** @since 00.02.08 */
	public static final class OfPath {
		
		// Forbid anyone to create an instance of this class
		private OfPath() {
		}
		
		public static final Info info(String path) {
			return Info.of(path);
		}
		
		public static final Info info(Path path) {
			return Info.of(path);
		}
		
		/** @since 00.02.09 */
		public static final String string(Path path) {
			return path != null ? path.toAbsolutePath().toString() : null;
		}
		
		/** @since 00.02.09 */
		public static final String normalize(Path path) {
			return normalize(string(path));
		}
		
		/** @since 00.02.09 */
		public static final String normalize(String path) {
			return path != null ? path.replace('\\', '/') : null;
		}
		
		/** @since 00.02.09 */
		public static final String dirName(Path path) {
			Path dir; return (dir = path.getParent()) != null ? baseName(dir) : null;
		}
		
		/** @since 00.02.09 */
		public static final String dirName(String path) {
			return afterLast(beforeLast(normalize(path), "/"), "/");
		}
		
		/** @since 00.02.09 */
		public static final String dirPath(Path path) {
			Path dir; return (dir = path.getParent()) != null ? normalize(string(dir)) : null;
		}
		
		/** @since 00.02.09 */
		public static final String dirPath(String path) {
			return beforeLast(path, "/");
		}
		
		/** @since 00.02.09 */
		public static final String baseName(Path path) {
			return path != null ? path.getFileName().toString() : null;
		}
		
		/** @since 00.02.09 */
		public static final String baseName(String path) {
			return afterLast(normalize(path), "/");
		}
		
		/** @since 00.02.09 */
		public static final String fileName(Path path) {
			return beforeLast(baseName(path), ".");
		}
		
		/** @since 00.02.09 */
		public static final String fileName(String path) {
			return beforeLast(baseName(path), ".");
		}
		
		/** @since 00.02.09 */
		public static final String fileType(Path path) {
			return afterLast(baseName(path), ".");
		}
		
		/** @since 00.02.09 */
		public static final String fileType(String path) {
			return afterLast(baseName(path), ".");
		}
		
		public static final class Info {
			
			private final Path path;
			private final String baseName;
			private final String fileName;
			private final String extension;
			
			private Info(Path path, String baseName, String fileName, String extension) {
				this.path = path;
				this.baseName = Objects.requireNonNull(baseName);
				this.fileName = Objects.requireNonNull(fileName);
				this.extension = extension;
			}
			
			private static final String extractBaseName(String path) {
				String normalized = path.replace('\\', '/');
				
				int index;
				if((index = normalized.lastIndexOf('/')) != -1) {
					return normalized.substring(index + 1);
				}
				
				return path;
			}
			
			private static final Info of(Path path, String baseName) {
				String fileName = baseName;
				String extension = null;
				
				int index;
				if((index = baseName.lastIndexOf('.')) != -1) {
					fileName = baseName.substring(0, index);
					extension = baseName.substring(index + 1);
				}
				
				return new Info(path, baseName, fileName, extension);
			}
			
			private static final Info of(String path) {
				return of(null, extractBaseName(path));
			}
			
			private static final Info of(Path path) {
				return of(path.toAbsolutePath(), path.getFileName().toString());
			}
			
			public Path path() {
				return path;
			}
			
			public Path directory() {
				return path.getParent();
			}
			
			public Path root() {
				return path.getRoot();
			}
			
			public String baseName() {
				return baseName;
			}
			
			public String fileName() {
				return fileName;
			}
			
			public String extension() {
				return extension;
			}
			
			public boolean hasExtension() {
				return extension != null;
			}
		}
	}
	
	/** @since 00.02.08 */
	public static final class OfFormat {
		
		// Forbid anyone to create an instance of this class
		private OfFormat() {
		}
		
		public static final String time(double time, TimeUnit unit, boolean alwaysShowMs) {
			return time((long) time, unit, alwaysShowMs);
		}
		
		public static final String time(long time, TimeUnit unit, boolean alwaysShowMs) {
			StringBuilder builder = new StringBuilder();
			boolean written = false;
			
			long hours = unit.toHours(time);
			if(hours > 0L) {
				builder.append(hours).append('h');
				time = time - unit.convert(hours, TimeUnit.HOURS);
				written = true;
			}
			
			long minutes = unit.toMinutes(time);
			if(minutes > 0L || written) {
				if(written) {
					builder.append(' ');
				}
				
				builder.append(minutes).append('m');
				time = time - unit.convert(minutes, TimeUnit.MINUTES);
				written = true;
			}
			
			if(written) {
				builder.append(' ');
			}
			
			long seconds = unit.toSeconds(time);
			builder.append(seconds);
			time = time - unit.convert(seconds, TimeUnit.SECONDS);
			
			long millis = unit.toMillis(time);
			if(millis > 0L || alwaysShowMs) {
				builder.append('.').append(String.format("%03d", millis));
			}
			
			builder.append('s');
			
			return builder.toString();
		}
		
		public static final String size(double size, SizeUnit unit, int decimals) {
			long base = unit.base();
			SizeUnit[] units = SizeUnit.units(base);
			
			if(units.length == 0) {
				throw new IllegalStateException("No units found for base: " + base);
			}
			
			double value = size;
			SizeUnit resultUnit;
			
			if(units.length == 1) {
				resultUnit = units[0];
				value = resultUnit.convert(size, unit);
			} else {
				int type = indexOf(unit, units);
				int maxType = units.length - 1;
				
				for(double div = Math.pow(base, units[type + 1].power() - units[type].power());
						value >= div && type < maxType;
						value /= div,
							div = Math.pow(base, units[type + 1].power() - units[type].power()),
							++type);
				
				resultUnit = units[type];
			}
			
			return String.format(Locale.US, "%." + decimals + "f %s", value, resultUnit);
		}
	}
	
	/** @since 00.02.08 */
	public static enum SizeUnit {
		
		// Constants are sorted, do not change the order
		
		BYTES("B", 10, 0),
		KILOBYTES("kB", 10, 3),
		MEGABYTES("MB", 10, 6),
		GIGABYTES("GB", 10, 9),
		TERABYTES("TB", 10, 12);
		
		private static SizeUnit[] values;
		private static SizeUnit[] valuesBase10;
		
		private final String abbreviation;
		private final long base;
		private final long power;
		
		private SizeUnit(String abbreviation, long base, long power) {
			this.abbreviation = abbreviation;
			this.base = base;
			this.power = power;
		}
		
		private static final SizeUnit[] ofBase(long base) {
			return Stream.of(units()).filter((u) -> u.base == base).toArray(SizeUnit[]::new);
		}
		
		public static final SizeUnit[] units() {
			return values == null ? values = values() : values;
		}
		
		public static final SizeUnit[] units(long base) {
			if(base == 10L) {
				return valuesBase10 == null ? valuesBase10 = ofBase(base) : valuesBase10;
			}
			
			return ofBase(base);
		}
		
		public double convert(double value, SizeUnit unit) {
			if(base == unit.base) {
				return value * Math.pow(base, unit.power - power);
			}
			
			return value * Math.pow(10.0, unit.power * Math.log10(unit.base) - power * Math.log10(base));
		}
		
		public String abbreviation() {
			return abbreviation;
		}
		
		public long base() {
			return base;
		}
		
		public long power() {
			return power;
		}
		
		@Override
		public String toString() {
			return abbreviation();
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
			PATTERN_STRING_EXTRACT_VARIABLE = "(?:(?:var|const|let)\\s+)?%s\\s*=\\s*";
			PATTERN_STRING_EXTRACT_FUNCTION_PARAMS = "%s\\s*\\(";
			PATTERN_STRING_INSERT_ARGUMENTS = "[\"']\\s*\\+\\s*([^\\+\\s]+)\\s*(?:\\+\\s*[\"'])?";
		}
		
		public static final String extractVariableContent(String script, String varName, int offset) {
			StringBuilder builder = new StringBuilder();
			Regex pattern = Regex.of(String.format(PATTERN_STRING_EXTRACT_VARIABLE, Regex.quote(varName)));
			Matcher matcher = pattern.matcher(script);
			if((matcher.find(offset))) {
				int start = matcher.end();
				boolean dq = false;
				boolean sq = false;
				for(int i = start, l = script.length(), c, n; i < l; i += n) {
					c = script.codePointAt(i);
					n = charCount(c);
					// Quotes logic
					if((c == '\"' && !sq)) dq = !dq; else
					if((c == '\'' && !dq)) sq = !sq;
					// Extract logic
					if(!dq && !sq) {
						if((c == ';')) break;
					}
					builder.appendCodePoint(c);
				}
			}
			return builder.toString();
		}
		
		public static final String extractFunctionPararms(String script, String funcName, int offset) {
			StringBuilder builder = new StringBuilder();
			Regex pattern = Regex.of(String.format(PATTERN_STRING_EXTRACT_FUNCTION_PARAMS, Regex.quote(funcName)));
			Matcher matcher = pattern.matcher(script);
			if((matcher.find(offset))) {
				int start = matcher.end();
				boolean dq = false;
				boolean sq = false;
				for(int i = start, l = script.length(), c, n; i < l; i += n) {
					c = script.codePointAt(i);
					n = charCount(c);
					// Quotes logic
					if((c == '\"' && !sq)) dq = !dq; else
					if((c == '\'' && !dq)) sq = !sq;
					// Extract logic
					if(!dq && !sq) {
						if((c == ')')) break;
					}
					builder.appendCodePoint(c);
				}
			}
			return builder.toString();
		}
		
		public static final String insertArgumentsToString(String statement, Map<String, String> args) {
			return Regex.of(PATTERN_STRING_INSERT_ARGUMENTS).replaceAll(statement, (result) -> {
				return args.getOrDefault(result.group(1), result.group(1));
			});
		}
	}
}