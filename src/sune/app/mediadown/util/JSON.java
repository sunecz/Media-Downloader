package sune.app.mediadown.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

import sune.app.mediadown.util.Utils.StringOps;

/**
 * Contains utilities for JSON strings, input streams and files.
 * @author Sune
 * @since 00.02.05
 */
public final class JSON {
	
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	
	private static final int CHAR_OBJECT_OPEN        = '{';
	private static final int CHAR_OBJECT_CLOSE       = '}';
	private static final int CHAR_ARRAY_OPEN         = '[';
	private static final int CHAR_ARRAY_CLOSE        = ']';
	private static final int CHAR_STRING_QUOTES      = '"';
	private static final int CHAR_ESCAPE_SLASH       = '\\';
	private static final int CHAR_SEPARATOR_PROPERTY = ':';
	private static final int CHAR_SEPARATOR_ELEMENT  = ',';
	
	// Forbid anyone to create an instance of this class
	private JSON() {
	}
	
	/** @since 00.02.09 */
	public static final JSONReader newReader(String string) {
		return newReader(string, DEFAULT_CHARSET);
	}
	
	/** @since 00.02.09 */
	public static final JSONReader newReader(String string, Charset charset) {
		return JSONReader.create(string, charset);
	}
	
	/** @since 00.02.09 */
	public static final JSONReader newReader(InputStream stream) {
		return newReader(stream, DEFAULT_CHARSET);
	}
	
	/** @since 00.02.09 */
	public static final JSONReader newReader(InputStream stream, Charset charset) {
		return JSONReader.create(stream, charset);
	}
	
	/** @since 00.02.09 */
	public static final JSONReader newReader(Path path) throws IOException {
		return newReader(path, DEFAULT_CHARSET);
	}
	
	/** @since 00.02.09 */
	public static final JSONReader newReader(Path path, Charset charset) throws IOException {
		return JSONReader.create(path, charset);
	}
	
	public static final JSONCollection read(String string) {
		return read(string, DEFAULT_CHARSET);
	}
	
	public static final JSONCollection read(String string, Charset charset) {
		try {
			return newReader(string, charset).read();
		} catch(IOException ex) {
			// Should not happen, but still throw it as unchecked
			throw new UncheckedIOException(ex);
		}
	}
	
	public static final JSONCollection read(InputStream stream) throws IOException {
		return read(stream, DEFAULT_CHARSET);
	}
	
	public static final JSONCollection read(InputStream stream, Charset charset) throws IOException {
		return newReader(stream, charset).read();
	}
	
	public static final JSONCollection read(Path path) throws IOException {
		return read(path, DEFAULT_CHARSET);
	}
	
	public static final JSONCollection read(Path path, Charset charset) throws IOException {
		return newReader(path, charset).read();
	}
	
	public static final class JSONReader {
		
		/* Implementation note:
		 * Use absolute CharBuffer::get(int) method for better performance, since Java allegedly generates
		 * worse code for the relative CharBuffer::get() method.
		 */
		
		private static final int BUFFER_SIZE = 8192;
		private static final int CHAR_BYTE_ORDER_MARK = 65279;
		
		private final Reader input;
		private final CharBuffer buf;
		private int pos;
		private int lim;
		
		private final Deque<Pair<String, JSONCollection>> parents;
		private final StringBuilder str;
		private final StringOps ops;
		private String lastStr;
		private JSONType lastType = JSONType.UNKNOWN;
		private Pair<String, JSONCollection> lastParent;
		private int c;
		
		/** @since 00.02.09 */
		private boolean allowUnquotedNames;
		
		private JSONReader(ReadableByteChannel input, Charset charset) {
			this.input = Channels.newReader(Objects.requireNonNull(input), charset);
			this.buf = CharBuffer.allocate(BUFFER_SIZE).flip();
			this.parents = new ArrayDeque<>();
			this.str = Utils.utf16StringBuilder();
			this.ops = new StringOps(str);
			this.allowUnquotedNames = false;
		}
		
		public static final JSONReader create(String string, Charset charset) {
			return create(new ByteArrayInputStream(Objects.requireNonNull(string).getBytes(charset)), charset);
		}
		
		public static final JSONReader create(InputStream stream, Charset charset) {
			return new JSONReader(Channels.newChannel(Objects.requireNonNull(stream)), charset);
		}
		
		public static final JSONReader create(Path path, Charset charset) throws IOException {
			return new JSONReader(Files.newByteChannel(path, StandardOpenOption.READ), charset);
		}
		
		// Temporary direct optimization until we use Java 17 to reduce the memory footprint.
		// See: https://bugs.openjdk.org/browse/JDK-4926314 and https://bugs.openjdk.org/browse/JDK-8266014
		/** @since 00.02.09 */
		private final int fillBuf() throws IOException {
			// Since we use CharBuffer::allocate, there will be a backing array.
			char[] cbuf = buf.array();
			int pos = buf.position();
			int rem = Math.max(buf.limit() - pos, 0);
			int off = buf.arrayOffset() + pos;
			int num = input.read(cbuf, off, rem);
			
			if(num > 0) {
				buf.position(pos + num);
			}
			
			return num;
		}
		
		private final int fill() throws IOException {
			buf.position(pos);
			buf.compact();
			int read = fillBuf();
			buf.flip();
			pos = 0;
			lim = buf.limit();
			return read;
		}
		
		private final int next() throws IOException {
			int r = lim - pos;
			if(r < 2 && fill() == -1) {
				if(r == 0)
					return -1;
				char a = buf.get(pos++);
				if(Character.isHighSurrogate(a))
					throw new IOException("Invalid character");
				return a;
			}
			char a = buf.get(pos++);
			if(!Character.isHighSurrogate(a))
				return a;
			char b = buf.get(pos++);
			if(!Character.isLowSurrogate(b))
				throw new IOException("Invalid character");
			return Character.toCodePoint(a, b);
		}
		
		private final int skipWhitespaces() throws IOException {
			while(Character.isWhitespace(c))
				c = next();
			return c;
		}
		
		private final void addPendingObject() {
			if(str.length() <= 0
					// Allow empty strings, but only quoted ones
					&& lastType != JSONType.STRING) {
				// Always reset the last type
				lastType = JSONType.UNKNOWN;
				return;
			}
			
			Pair<String, JSONCollection> pair = parents.peekFirst();
			JSONType parentType = pair.b.type();
			
			if(lastStr == null || lastStr.isEmpty()) {
				switch(parentType) {
					case OBJECT:
						throw new IllegalStateException("Object's item name cannot be null or empty");
					default:
						// All is OK
						break;
				}
			}
			
			String value = ops.replaceUnicodeEscapeSequences().unslash().toStringAndReset();
			JSONObject object = JSONObject.ofType(lastType, value);
			str.setLength(0);
			
			switch(parentType) {
				case OBJECT: pair.b.set(lastStr, object);         break;
				case ARRAY:  pair.b.add         (object);         break;
				default:     /* Collections, should not happen */ break;
			}
			
			// Always reset the last type
			lastType = JSONType.UNKNOWN;
		}
		
		private final int readAndMatchSequence(String sequence) throws IOException {
			int i = 0, l = sequence.length();
			
			for(int p, n; i < l; i += n) {
				p = sequence.codePointAt(i);
				n = Character.charCount(p);
				
				if(c != p) {
					break;
				}
				
				c = next();
			}
			
			if(i == l) {
				str.append(sequence);
				return -1;
			}
			
			return i;
		}
		
		private final void checkEndOfElementAfterSequence() throws IOException {
			c = skipWhitespaces();
			
			switch(c) {
				case CHAR_OBJECT_CLOSE:
				case CHAR_ARRAY_CLOSE:
				case CHAR_SEPARATOR_ELEMENT:
					// All is OK
					break;
				default:
					throw new IllegalStateException("Invalid character: " + Character.toString(c));
			}
		}
		
		/** @since 00.02.09 */
		private final void checkCollectionName() {
			if((lastStr == null || lastStr.isEmpty())
					&& parents.size() > 1
					&& parents.peek().b.type() != JSONType.ARRAY) {
				throw new IllegalStateException("Invalid name");
			}
		}
		
		private final void readObject() throws IOException {
			checkCollectionName();
			if(parents.isEmpty()) parents.push(new Pair<>(null,    JSONCollection.empty()));
			else                  parents.push(new Pair<>(lastStr, JSONCollection.empty()));
			c = next();
		}
		
		private final void readArray() throws IOException {
			checkCollectionName();
			if(parents.isEmpty()) parents.push(new Pair<>(null,    JSONCollection.emptyArray()));
			else                  parents.push(new Pair<>(lastStr, JSONCollection.emptyArray()));
			c = next();
		}
		
		private final boolean readSequence(String word, JSONType type) throws IOException {
			str.ensureCapacity(word.length()); // Optimization
			int end = readAndMatchSequence(word);
			
			if(allowUnquotedNames) {
				// The sequence was not fully read
				if(end > 0) {
					str.append(word, 0, end);
					return false;
				} else {
					lastType = type;
				}
			} else {
				checkEndOfElementAfterSequence();
				lastType = type;
			}
			
			return true;
		}
		
		private final boolean readTrue() throws IOException {
			return readSequence("true", JSONType.BOOLEAN);
		}
		
		private final boolean readFalse() throws IOException {
			return readSequence("false", JSONType.BOOLEAN);
		}
		
		private final boolean readNull() throws IOException {
			return readSequence("null", JSONType.NULL);
		}
		
		private final void readNumber() throws IOException {
			str.ensureCapacity(lim - pos); // Optimization
			if(c == '-') {
				str.appendCodePoint(c);
				c = next();
			}
			if(!Character.isDigit(c)) {
				throw new IllegalStateException("Invalid number");
			}
			boolean fraction = false;
			boolean exponent = false;
			loop:
			do {
				if(Character.isDigit(c)) {
					str.appendCodePoint(c);
				} else {
					switch(c) {
						case '.':
							if(fraction || exponent) {
								throw new IllegalStateException("Invalid number");
							}
							str.appendCodePoint(c);
							fraction = true;
							break;
						case 'e':
						case 'E':
							if(exponent) {
								throw new IllegalStateException("Invalid number");
							}
							str.appendCodePoint(c);
							exponent = true;
							c = next();
							if(c != '-' && c != '+') {
								throw new IllegalStateException("Invalid exponent");
							}
							str.appendCodePoint(c);
							c = next();
							if(!Character.isDigit(c)) {
								throw new IllegalStateException("Invalid exponent");
							}
							str.appendCodePoint(c);
							break;
						default:
							break loop;
					}
				}
			} while((c = next()) != -1);
			lastType = fraction ? JSONType.DECIMAL : JSONType.INTEGER;
		}
		
		private final void readString() throws IOException {
			if(c != CHAR_STRING_QUOTES)
				throw new IllegalStateException("Not a string");
			str.ensureCapacity(lim - pos); // Optimization
			boolean escaped = false;
			while((c = next()) != -1) {
				if(c == CHAR_STRING_QUOTES && !escaped) {
					c = next();
					break; // String closed
				} else if(c == CHAR_ESCAPE_SLASH) {
					str.appendCodePoint(c);
					escaped = !escaped;
				} else {
					str.appendCodePoint(c);
					if(escaped) escaped = false;
				}
			}
			lastType = JSONType.STRING;
		}
		
		private final void readStringUnquoted() throws IOException {
			str.ensureCapacity(lim - pos); // Optimization
			
			boolean escaped = false;
			do {
				if(!escaped
						&& (c == CHAR_SEPARATOR_PROPERTY
								|| c == CHAR_SEPARATOR_ELEMENT
								|| c == CHAR_OBJECT_CLOSE
								|| c == CHAR_ARRAY_CLOSE)) {
					break; // String closed
				} else if(c == CHAR_ESCAPE_SLASH) {
					str.appendCodePoint(c);
					escaped = !escaped;
				} else {
					str.appendCodePoint(c);
					if(escaped) escaped = false;
				}
			} while((c = next()) != -1);
			
			lastType = JSONType.STRING_UNQUOTED;
		}
		
		private final void readNext() throws IOException {
			while((c = skipWhitespaces()) != -1) {
				switch(c) {
					case CHAR_STRING_QUOTES: readString(); break;
					case CHAR_SEPARATOR_PROPERTY:
						c = next();
						lastStr = str.toString();
						str.setLength(0);
						break;
					case CHAR_OBJECT_OPEN: readObject(); break;
					case CHAR_ARRAY_OPEN: readArray(); break;
					case CHAR_OBJECT_CLOSE:
					case CHAR_ARRAY_CLOSE:
						c = next();
						addPendingObject();
						Pair<String, JSONCollection> pair = parents.pop();
						lastParent = pair;
						if(!parents.isEmpty()) {
							pair = parents.peekFirst();
							switch(pair.b.type()) {
								case OBJECT: pair.b.set(lastParent.a, lastParent.b); break;
								case ARRAY:  pair.b.add(lastParent.b);               break;
								default:     /* Collections, should not happen */    break;
							}
						}
						break;
					case CHAR_SEPARATOR_ELEMENT:
						c = next();
						addPendingObject();
						break;
					default: {
						if(c == '-' || Character.isDigit(c)) {
							readNumber();
							break;
						}
						
						boolean success = false;
						switch(c) {
							case 't': success = readTrue(); break;
							case 'f': success = readFalse(); break;
							case 'n': success = readNull(); break;
						}
						
						if(success) {
							break;
						}
						
						if(allowUnquotedNames) {
							readStringUnquoted();
							break;
						}
						
						throw new IOException("Invalid JSON");
					}
				}
			}
		}
		
		public final JSONCollection read() throws IOException {
			try(input) {
				c = next(); // Bootstrap
				
				if(c == CHAR_BYTE_ORDER_MARK) {
					// Skip the BOM
					c = next();
				}
				
				readNext();
				return lastParent.b;
			}
		}
		
		/** @since 00.02.09 */
		public final JSONReader allowUnquotedNames(boolean value) {
			allowUnquotedNames = value;
			return this;
		}
	}
	
	/** @since 00.02.09 */
	public static final class JSONString {
		
		private static final char UE = 0b100000000;
		private static final char DE = 0b010000000;
		
		private static final char[] charsEscape = {
			UE |   0, UE |   1, UE |   2, UE |   3, UE |   4, UE |   5, UE |   6, UE |   7,
			DE | 'b', DE | 't', DE | 'n', UE |  11, DE | 'f', DE | 'r', UE |  14, UE |  15,
			UE |  16, UE |  17, UE |  18, UE |  19, UE |  20, UE |  21, UE |  22, UE |  23,
			UE |  24, UE |  25, UE |  26, UE |  27, UE |  28, UE |  29, UE |  30, UE |  31,
			       0,        0,      '"',        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,     '\\',        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
		};
		
		private static final char[] charsUnescape = {
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,      '"',        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
			       0,        0,        0,        0,     '\\',        0,        0,        0,
			       0,        0,     '\b',        0,        0,        0,     '\f',        0,
			       0,        0,        0,        0,        0,        0,     '\n',        0,
			       0,        0,     '\r',        0,     '\t',        0,        0,        0,
			       0,        0,        0,        0,        0,        0,        0,        0,
		};
		
		private static final char[] charsHex = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
		};
		
		// Forbid anyone to create an instance of this class
		private JSONString() {
		}
		
		private static final int nonZero(int v) {
			return (v | -v) >>> 31;
		}
		
		private static final int negative(int v) {
			return v >>> 31;
		}
		
		private static final boolean shouldEscape(int c) {
			// Reference: https://www.ietf.org/rfc/rfc4627.txt, section 2.5
			// quotation mark = 34
			// backslash = 92
			// control characters = <0, 31>
			return (negative(c - 32) | (nonZero(c - 34) ^ nonZero(c - 92))) == 1;
		}
		
		public static final void escape(String string, StringBuilder output) {
			final char[] backslashes = { CHAR_ESCAPE_SLASH, CHAR_ESCAPE_SLASH };
			
			final int len = string.length();
			int off = 0;
			
			for(int i = 0, c, v; i < len; ++i) {
				c = string.charAt(i);
				
				if(shouldEscape(c)) {
					output.append(string, off, i);
					v = charsEscape[c];
					
					if((v & UE) != 0) {
						output.append(backslashes);
						output.append("u00")
							.append(c >> 4)
							.append(charsHex[c & 0xf]);
					} else if((v & DE) != 0) {
						output.append(backslashes);
						output.append((char) (v & ~DE));
					} else {
						output.append((char) CHAR_ESCAPE_SLASH);
						output.append((char) v);
					}
					
					off = i + 1;
				} else if(c > 0x7f) {
					output.append(string, off, i);
					
					output.append(backslashes);
					output.append('u')
						.append(charsHex[(c >> 12) & 0xf])
						.append(charsHex[(c >>  8) & 0xf])
						.append(charsHex[(c >>  4) & 0xf])
						.append(charsHex[(c      ) & 0xf]);
					
					off = i + 1;
				}
			}
			
			if(off < len) {
				output.append(string, off, len);
			}
		}
		
		public static final String escape(String string) {
			final int len = string.length();
			StringBuilder output = Utils.utf16StringBuilder(len);
			escape(string, output);
			return output.toString();
		}
		
		public static final void unescape(String string, StringBuilder output) {
			final int len = string.length();
			int off = 0;
			boolean escaped = false;
			
			for(int i = 0, c, v; i < len; ++i) {
				c = string.charAt(i);
				
				if(escaped) {
					if(c <= 0x7f
							&& (v = charsUnescape[c & 0xff]) != 0) {
						output.append(string, off, i - 1);
						output.append((char) v);
						off = i + 1;
					}
					
					escaped = false;
				} else if(c == CHAR_ESCAPE_SLASH) {
					escaped = true;
				}
			}
			
			if(off < len) {
				output.append(string, off, len);
			}
			
			StringBuilder input = output;
			output = Utils.utf16StringBuilder(input.length());
			Utils.UnicodeEscapeSequence.replace(input, output);
			input.setLength(0);
			input.append(output);
		}
		
		public static final String unescape(String string) {
			final int len = string.length();
			StringBuilder output = Utils.utf16StringBuilder(len);
			unescape(string, output);
			return output.toString();
		}
	}
	
	/** @since 00.02.09 */
	public static enum JSONType {
		
		NULL,
		BOOLEAN,
		INTEGER,
		DECIMAL,
		STRING,
		STRING_UNQUOTED,
		ARRAY,
		OBJECT,
		UNKNOWN;
	}
	
	/** @since 00.02.09 */
	public static abstract class JSONNode {
		
		protected JSONCollection parent;
		protected String name;
		protected JSONType type;
		
		public JSONNode(JSONCollection parent, String name, JSONType type) {
			this.parent = parent; // null for root
			this.name = name;
			this.type = Objects.requireNonNull(type);
		}
		
		protected final void assign(JSONCollection parent, String name) {
			this.parent = parent;
			this.name = name;
		}
		
		protected final void unassign() {
			this.parent = null;
			this.name = null;
		}
		
		protected abstract void toString(StringBuilder builder, int depth, boolean compress);
		
		public abstract JSONNode copy();
		public abstract boolean isObject();
		public abstract boolean isCollection();
		
		public JSONCollection parent() { return parent; }
		public String fullName() { return parent != null ? parent.fullName() + (name != null ? name : "") : name; }
		public String name() { return name; }
		public JSONType type() { return type; }
		
		public String toString() { return toString(0, false); }
		public String toString(boolean compress) { return toString(0, compress); }
		
		public String toString(int depth, boolean compress) {
			StringBuilder builder = new StringBuilder();
			toString(builder, depth, compress);
			return builder.toString();
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(name, parent, type);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			JSONNode other = (JSONNode) obj;
			return Objects.equals(name, other.name) && Objects.equals(parent, other.parent) && type == other.type;
		}
	}
	
	/** @since 00.02.09 */
	public static final class JSONObject extends JSONNode {
		
		private Object value;
		
		private JSONObject(JSONType type, Object value) {
			this(null, null, type, value);
		}
		
		private JSONObject(JSONCollection parent, String name, JSONType type, Object value) {
			super(parent, name, type);
			this.value = value;
		}
		
		public static final JSONObject of(Object value) {
			if(value == null) {
				return ofNull();
			}
			
			Class<?> clazz = value.getClass();
			
			if(clazz == Boolean.class) return ofBoolean((Boolean) value);
			if(clazz == Byte.class) return ofByte((Byte) value);
			if(clazz == Short.class) return ofShort((Short) value);
			if(clazz == Integer.class) return ofInt((Integer) value);
			if(clazz == Long.class) return ofLong((Long) value);
			if(clazz == Float.class) return ofFloat((Float) value);
			if(clazz == Double.class) return ofDouble((Double) value);
			
			return ofString(String.valueOf(value));
		}
		
		public static final JSONObject ofType(JSONType type, String value) {
			switch(type) {
				case NULL: return ofNull();
				case BOOLEAN: return ofBoolean(Boolean.valueOf(value));
				case INTEGER: return ofLong(Long.valueOf(value));
				case DECIMAL: return ofDouble(Double.valueOf(value));
				case STRING: return ofString(value);
				case STRING_UNQUOTED: return ofStringUnquoted(value);
				default: throw new IllegalArgumentException("Invalid type");
			}
		}
		
		public static final JSONObject ofNull() { return new JSONObject(JSONType.NULL, null); }
		public static final JSONObject ofBoolean(boolean value) { return new JSONObject(JSONType.BOOLEAN, value); }
		public static final JSONObject ofByte(byte value) { return new JSONObject(JSONType.INTEGER, (long) value); }
		public static final JSONObject ofShort(short value) { return new JSONObject(JSONType.INTEGER, (long) value); }
		public static final JSONObject ofInt(int value) { return new JSONObject(JSONType.INTEGER, (long) value); }
		public static final JSONObject ofLong(long value) { return new JSONObject(JSONType.INTEGER, value); }
		public static final JSONObject ofFloat(float value) { return new JSONObject(JSONType.DECIMAL, (double) value); }
		public static final JSONObject ofDouble(double value) { return new JSONObject(JSONType.DECIMAL, value); }
		public static final JSONObject ofString(String value) { return new JSONObject(JSONType.STRING, JSONString.unescape(value)); }
		
		// Do not expose to the public interface
		private static final JSONObject ofStringUnquoted(String value) { return new JSONObject(JSONType.STRING_UNQUOTED, value); }
		
		@Override public JSONObject copy() { return new JSONObject(parent, name, type, value); }
		@Override public boolean isObject() { return true; }
		@Override public boolean isCollection() { return false; }
		
		@SuppressWarnings("unchecked")
		private final <T> T typedValue(Class<T> clazz) {
			return value == null ? null : (clazz.isAssignableFrom(value.getClass()) ? (T) value : null);
		}
		
		public Object nullValue() { return null; }
		public boolean booleanValue() { Boolean v; return (v = typedValue(Boolean.class)) != null ? v : false; }
		public byte byteValue() { return (byte) longValue(); }
		public short shortValue() { return (short) longValue(); }
		public int intValue() { return (int) longValue(); }
		public long longValue() { Long v; return (v = typedValue(Long.class)) != null ? v : 0L; }
		public float floatValue() { return (float) doubleValue(); }
		public double doubleValue() { Double v; return (v = typedValue(Double.class)) != null ? v : 0.0; }
		public String stringValue() { return String.valueOf(value); }
		public Object value() { return value; }
		
		@Override
		public void toString(StringBuilder builder, int depth, boolean compress) {
			switch(type) {
				case BOOLEAN: { builder.append((boolean) value); break; }
				case INTEGER: { builder.append((long) value); break; }
				case DECIMAL: { builder.append((double) value); break; }
				case STRING: {
					builder.append('"');
					JSONString.escape(String.valueOf(value), builder);
					builder.append('"');
					break;
				}
				case STRING_UNQUOTED: { builder.append(String.valueOf(value)); break; }
				default: { builder.append((Object) null); break; }
			}
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(value);
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(!super.equals(obj))
				return false;
			if(getClass() != obj.getClass())
				return false;
			JSONObject other = (JSONObject) obj;
			return Objects.equals(value, other.value);
		}
	}
	
	/** @since 00.02.09 */
	public static final class JSONCollection extends JSONNode implements Iterable<JSONNode> {
		
		private static final int CHAR_NAME_SEPARATOR = '.';
		
		private Map<String, JSONNode> nodes;
		
		private JSONCollection(boolean isArray) {
			this(null, null, isArray ? JSONType.ARRAY : JSONType.OBJECT, null);
		}
		
		private JSONCollection(JSONCollection parent, String name, JSONType type, Map<String, JSONNode> nodes) {
			super(parent, name, type);
			this.nodes = nodes != null ? new LinkedHashMap<>(nodes) : null;
		}
		
		private static final String indexName(int index) {
			return String.valueOf(index);
		}
		
		private static final String tabString(int depth) {
			return "\t".repeat(depth);
		}
		
		private static final boolean isOnlyDigits(String string) {
			for(int i = 0, l = string.length(), c; i < l; i += Character.charCount(c)) {
				c = string.codePointAt(i);
				
				if(!Character.isDigit(c)) {
					return false;
				}
			}
			
			return true;
		}
		
		private static final TraverseResult traverse(JSONCollection parent, String name) {
			TraverseResult result = new TraverseResult();
			
			for(int off = 0, len = name.length(), idx; off < len; off = idx + 1) {
				idx = name.indexOf(CHAR_NAME_SEPARATOR, off);
				
				if(idx < 0) {
					JSONNode node = parent.directGet(name.substring(off));
					result.parent = parent;
					result.node = node;
					result.success = node != null;
					result.offset = off;
					break;
				}
				
				JSONNode node = parent.directGet(name.substring(off, idx));
				
				if(node == null || !node.isCollection()) {
					result.parent = parent;
					result.node = node;
					result.success = false;
					result.offset = off;
					break;
				}
				
				parent = (JSONCollection) node;
			}
			
			return result;
		}
		
		private static final <T> List<T> iterableToList(Iterable<T> iterable) {
			List<T> collection = new ArrayList<>();
			iterableToCollection(collection, iterable);
			return collection;
		}
		
		private static final <T> void iterableToCollection(Collection<T> collection, Iterable<T> iterable) {
			for(T t : iterable) collection.add(t);
		}
		
		public static final JSONCollection empty(boolean isArray) { return new JSONCollection(isArray); }
		public static final JSONCollection empty() { return empty(false); }
		public static final JSONCollection emptyArray() { return empty(true); }
		
		public static final JSONCollection ofObject(Object... namesAndNodes) {
			if(namesAndNodes == null || namesAndNodes.length <= 0) {
				return empty(false);
			}
			
			JSONCollection collection = new JSONCollection(false);
			Map<String, JSONNode> nodesMap = new LinkedHashMap<>();
			
			for(int i = 0, l = namesAndNodes.length; i < l; i += 2) {
				String name = (String) namesAndNodes[i];
				JSONNode node = (JSONNode) namesAndNodes[i + 1];
				node.assign(collection, name);
				nodesMap.put(name, node);
			}
			
			collection.setNodesMap(nodesMap);
			return collection;
		}
		
		public static final JSONCollection ofArray(JSONNode... nodes) {
			if(nodes == null || nodes.length <= 0) {
				return empty(true);
			}
			
			JSONCollection collection = new JSONCollection(true);
			Map<String, JSONNode> nodesMap = new LinkedHashMap<>();
			
			for(int i = 0, l = nodes.length; i < l; ++i) {
				String name = indexName(i);
				JSONNode node = (JSONNode) nodes[i];
				node.assign(collection, name);
				nodesMap.put(name, node);
			}
			
			collection.setNodesMap(nodesMap);
			return collection;
		}
		
		private final void setNodesMap(Map<String, JSONNode> nodesMap) {
			nodes = nodesMap;
		}
		
		private final int nextIndex() {
			return nodes != null ? nodes.size() : 0;
		}
		
		private final void reindex() {
			List<JSONNode> nodesToAdd = new ArrayList<>(nodes.size());
			boolean foundMismatch = false;
			
			int index = 0;
			for(Iterator<Entry<String, JSONNode>> it = nodes.entrySet().iterator(); it.hasNext();) {
				Entry<String, JSONNode> entry = it.next();
				
				if(foundMismatch
						|| Integer.valueOf(entry.getKey()) != index++) {
					nodesToAdd.add(entry.getValue());
					it.remove();
					foundMismatch = true;
				}
			}
			
			if(foundMismatch) {
				index = nodes.size();
				
				for(JSONNode node : nodesToAdd) {
					String name = indexName(index++);
					node.assign(this, name);
					nodes.put(name, node);
				}
			}
		}
		
		private final JSONNode directGet(String name) {
			return nodes != null ? nodes.get(name) : null;
		}
		
		private final void directSet(String name, JSONNode node) {
			if(nodes == null) {
				nodes = new LinkedHashMap<>();
			}
			
			if(type == JSONType.ARRAY) {
				int lastIndex = nodes.size();
				
				if(Integer.valueOf(name) > lastIndex) {
					name = indexName(lastIndex);
				}
			}
			
			node.assign(this, name);
			nodes.put(name, node);
		}
		
		private final void directRemove(String name) {
			if(nodes != null) {
				JSONNode node = nodes.remove(name);
				
				if(node != null) {
					if(type == JSONType.ARRAY) {
						int index = Integer.valueOf(name);
						
						if(index < nodes.size() - 1) {
							reindex();
						}
					}
					
					node.unassign();
				}
			}
		}
		
		private final TraverseResult traverse(String name) {
			return traverse(this, name);
		}
		
		private final boolean has(String name, JSONType type) {
			TraverseResult result = traverse(name);
			return result.success && result.node.type() == type;
		}
		
		private final <T> T get(String name, JSONType type, Function<JSONObject, T> op, T defaultValue) {
			TraverseResult result = traverse(name);
			return result.success ? (result.node.type() == type ? op.apply((JSONObject) result.node) : defaultValue) : defaultValue;
		}
		
		private final void remove(String name, JSONType type) {
			TraverseResult result = traverse(name);
			
			if(result.success && result.node.type() == type) {
				result.parent.directRemove(result.node.name());
			}
		}
		
		public boolean has(String name) {
			TraverseResult result = traverse(name);
			return result.success;
		}
		
		public boolean hasObject(String name) {
			TraverseResult result = traverse(name);
			return result.success && result.node.isObject();
		}
		
		public boolean hasCollection(String name) {
			TraverseResult result = traverse(name);
			return result.success && result.node.isCollection();
		}
		
		public boolean hasNull(String name) { return has(name, JSONType.NULL); }
		public boolean hasBoolean(String name) { return has(name, JSONType.BOOLEAN); }
		public boolean hasByte(String name) { return has(name, JSONType.INTEGER); }
		public boolean hasShort(String name) { return has(name, JSONType.INTEGER); }
		public boolean hasInt(String name) { return has(name, JSONType.INTEGER); }
		public boolean hasLong(String name) { return has(name, JSONType.INTEGER); }
		public boolean hasFloat(String name) { return has(name, JSONType.DECIMAL); }
		public boolean hasDouble(String name) { return has(name, JSONType.DECIMAL); }
		public boolean hasString(String name) { return has(name, JSONType.STRING); }
		
		public boolean has(int index) { return has(indexName(index)); }
		public boolean hasObject(int index) { return hasObject(indexName(index)); }
		public boolean hasCollection(int index) { return hasCollection(indexName(index)); }
		
		public boolean hasNull(int index) { return hasNull(indexName(index)); }
		public boolean hasBoolean(int index) { return hasBoolean(indexName(index)); }
		public boolean hasByte(int index) { return hasByte(indexName(index)); }
		public boolean hasShort(int index) { return hasShort(indexName(index)); }
		public boolean hasInt(int index) { return hasInt(indexName(index)); }
		public boolean hasLong(int index) { return hasLong(indexName(index)); }
		public boolean hasFloat(int index) { return hasFloat(indexName(index)); }
		public boolean hasDouble(int index) { return hasDouble(indexName(index)); }
		public boolean hasString(int index) { return hasString(indexName(index)); }
		
		public JSONNode get(String name, JSONNode defaultValue) {
			TraverseResult result = traverse(name);
			return result.success ? result.node : defaultValue;
		}
		
		public JSONObject getObject(String name, JSONObject defaultValue) {
			JSONNode node = get(name);
			return node == null ? defaultValue : (node.isObject() ? (JSONObject) node : defaultValue);
		}
		
		public JSONCollection getCollection(String name, JSONCollection defaultValue) {
			JSONNode node = get(name);
			return node == null ? defaultValue : (node.isCollection() ? (JSONCollection) node : defaultValue);
		}
		
		public Object getNull(String name, Object defaultValue) { return get(name, JSONType.NULL, JSONObject::nullValue, defaultValue); }
		public boolean getBoolean(String name, boolean defaultValue) { return get(name, JSONType.BOOLEAN, JSONObject::booleanValue, defaultValue); }
		public byte getByte(String name, byte defaultValue) { return get(name, JSONType.INTEGER, JSONObject::byteValue, defaultValue); }
		public short getShort(String name, short defaultValue) { return get(name, JSONType.INTEGER, JSONObject::shortValue, defaultValue); }
		public int getInt(String name, int defaultValue) { return get(name, JSONType.INTEGER, JSONObject::intValue, defaultValue); }
		public long getLong(String name, long defaultValue) { return get(name, JSONType.INTEGER, JSONObject::longValue, defaultValue); }
		public float getFloat(String name, float defaultValue) { return get(name, JSONType.DECIMAL, JSONObject::floatValue, defaultValue); }
		public double getDouble(String name, double defaultValue) { return get(name, JSONType.DECIMAL, JSONObject::doubleValue, defaultValue); }
		public String getString(String name, String defaultValue) { return get(name, JSONType.STRING, JSONObject::stringValue, defaultValue); }
		
		public JSONNode get(String name) { return get(name, null); }
		public JSONObject getObject(String name) { return getObject(name, null); }
		public JSONCollection getCollection(String name) { return getCollection(name, null); }
		
		public Object getNull(String name) { return getNull(name, null); }
		public boolean getBoolean(String name) { return getBoolean(name, false); }
		public byte getByte(String name) { return getByte(name, (byte) 0); }
		public short getShort(String name) { return getShort(name, (short) 0); }
		public int getInt(String name) { return getInt(name, 0); }
		public long getLong(String name) { return getLong(name, 0L); }
		public float getFloat(String name) { return getFloat(name, 0.0f); }
		public double getDouble(String name) { return getDouble(name, 0.0); }
		public String getString(String name) { return getString(name, null); }
		
		public JSONNode get(int index, JSONNode defaultValue) { return get(indexName(index), defaultValue); }
		public JSONObject getObject(int index, JSONObject defaultValue) { return getObject(indexName(index), defaultValue); }
		public JSONCollection getCollection(int index, JSONCollection defaultValue) { return getCollection(indexName(index), defaultValue); }
		
		public Object getNull(int index, Object defaultValue) { return getNull(indexName(index), defaultValue); }
		public boolean getBoolean(int index, boolean defaultValue) { return getBoolean(indexName(index), defaultValue); }
		public byte getByte(int index, byte defaultValue) { return getByte(indexName(index), defaultValue); }
		public short getShort(int index, short defaultValue) { return getShort(indexName(index), defaultValue); }
		public int getInt(int index, int defaultValue) { return getInt(indexName(index), defaultValue); }
		public long getLong(int index, long defaultValue) { return getLong(indexName(index), defaultValue); }
		public float getFloat(int index, float defaultValue) { return getFloat(indexName(index), defaultValue); }
		public double getDouble(int index, double defaultValue) { return getDouble(indexName(index), defaultValue); }
		public String getString(int index, String defaultValue) { return getString(indexName(index), defaultValue); }
		
		public JSONNode get(int index) { return get(indexName(index)); }
		public JSONObject getObject(int index) { return getObject(indexName(index)); }
		public JSONCollection getCollection(int index) { return getCollection(indexName(index)); }
		
		public Object getNull(int index) { return getNull(indexName(index)); }
		public boolean getBoolean(int index) { return getBoolean(indexName(index)); }
		public byte getByte(int index) { return getByte(indexName(index)); }
		public short getShort(int index) { return getShort(indexName(index)); }
		public int getInt(int index) { return getInt(indexName(index)); }
		public long getLong(int index) { return getLong(indexName(index)); }
		public float getFloat(int index) { return getFloat(indexName(index)); }
		public double getDouble(int index) { return getDouble(indexName(index)); }
		public String getString(int index) { return getString(indexName(index)); }
		
		public void set(String name, JSONNode node) {
			Objects.requireNonNull(node);
			
			TraverseResult result = traverse(name);
			
			// Replace already existing node
			if(result.success) {
				result.parent.directSet(result.node.name(), node);
			}
			// Add a new node
			else {
				JSONCollection parent = result.parent;
				
				for(int off = result.offset, len = name.length(), idx; off < len; off = idx + 1) {
					idx = name.indexOf(CHAR_NAME_SEPARATOR, off);
					
					if(idx < 0) {
						parent.directSet(name.substring(off), node);
						break;
					}
					
					int nextOff = idx + 1;
					int nextIdx = name.indexOf(CHAR_NAME_SEPARATOR, nextOff);
					String nextName;
					
					if(nextIdx < 0) {
						nextName = name.substring(nextOff);
					} else {
						nextName = name.substring(nextOff, nextIdx);
					}
					
					boolean isArray = isOnlyDigits(nextName);
					JSONNode newNode = new JSONCollection(isArray);
					parent.directSet(name.substring(off, idx), newNode);
					
					parent = (JSONCollection) newNode;
				}
			}
		}
		
		public void set(int index, JSONNode node) { set(indexName(index), node); }
		
		public void setNull(String name) { set(name, JSONObject.ofNull()); }
		public void set(String name, boolean value) { set(name, JSONObject.ofBoolean(value)); }
		public void set(String name, byte value) { set(name, JSONObject.ofByte(value)); }
		public void set(String name, short value) { set(name, JSONObject.ofShort(value)); }
		public void set(String name, int value) { set(name, JSONObject.ofInt(value)); }
		public void set(String name, long value) { set(name, JSONObject.ofLong(value)); }
		public void set(String name, float value) { set(name, JSONObject.ofFloat(value)); }
		public void set(String name, double value) { set(name, JSONObject.ofDouble(value)); }
		public void set(String name, String value) { set(name, JSONObject.ofString(value)); }
		
		public void setNull(int index) { setNull(indexName(index)); }
		public void set(int index, boolean value) { set(indexName(index), value); }
		public void set(int index, byte value) { set(indexName(index), value); }
		public void set(int index, short value) { set(indexName(index), value); }
		public void set(int index, int value) { set(indexName(index), value); }
		public void set(int index, long value) { set(indexName(index), value); }
		public void set(int index, float value) { set(indexName(index), value); }
		public void set(int index, double value) { set(indexName(index), value); }
		public void set(int index, String value) { set(indexName(index), value); }
		
		public void add(JSONNode node) {
			directSet(indexName(nextIndex()), Objects.requireNonNull(node));
		}
		
		public void addNull() { add(JSONObject.ofNull()); }
		public void add(boolean value) { add(JSONObject.ofBoolean(value)); }
		public void add(byte value) { add(JSONObject.ofByte(value)); }
		public void add(short value) { add(JSONObject.ofShort(value)); }
		public void add(int value) { add(JSONObject.ofInt(value)); }
		public void add(long value) { add(JSONObject.ofLong(value)); }
		public void add(float value) { add(JSONObject.ofFloat(value)); }
		public void add(double value) { add(JSONObject.ofDouble(value)); }
		public void add(String value) { add(JSONObject.ofString(value)); }
		
		public void remove(String name) {
			TraverseResult result = traverse(name);
			
			if(result.success) {
				result.parent.directRemove(result.node.name());
			}
		}
		
		public void removeObject(String name) {
			TraverseResult result = traverse(name);
			
			if(result.success && result.node.isObject()) {
				result.parent.directRemove(result.node.name());
			}
		}
		
		public void removeCollection(String name) {
			TraverseResult result = traverse(name);
			
			if(result.success && result.node.isCollection()) {
				result.parent.directRemove(result.node.name());
			}
		}
		
		public void removeNull(String name) { remove(name, JSONType.NULL); }
		public void removeBoolean(String name) { remove(name, JSONType.BOOLEAN); }
		public void removeByte(String name) { remove(name, JSONType.INTEGER); }
		public void removeShort(String name) { remove(name, JSONType.INTEGER); }
		public void removeInt(String name) { remove(name, JSONType.INTEGER); }
		public void removeLong(String name) { remove(name, JSONType.INTEGER); }
		public void removeFloat(String name) { remove(name, JSONType.DECIMAL); }
		public void removeDouble(String name) { remove(name, JSONType.DECIMAL); }
		public void removeString(String name) { remove(name, JSONType.STRING); }
		
		public void remove(int index) { remove(indexName(index)); }
		public void removeObject(int index) { removeObject(indexName(index)); }
		public void removeCollection(int index) { removeCollection(indexName(index)); }
		
		public void removeNull(int index) { remove(indexName(index), JSONType.NULL); }
		public void removeBoolean(int index) { remove(indexName(index), JSONType.BOOLEAN); }
		public void removeByte(int index) { remove(indexName(index), JSONType.INTEGER); }
		public void removeShort(int index) { remove(indexName(index), JSONType.INTEGER); }
		public void removeInt(int index) { remove(indexName(index), JSONType.INTEGER); }
		public void removeLong(int index) { remove(indexName(index), JSONType.INTEGER); }
		public void removeFloat(int index) { remove(indexName(index), JSONType.DECIMAL); }
		public void removeDouble(int index) { remove(indexName(index), JSONType.DECIMAL); }
		public void removeString(int index) { remove(indexName(index), JSONType.STRING); }
		
		@Override public JSONCollection copy() { return new JSONCollection(parent, name, type, nodes); }
		@Override public boolean isObject() { return false; }
		@Override public boolean isCollection() { return true; }
		
		public int length() { return nodes == null ? 0 : nodes.size(); }
		
		@Override public Iterator<JSONNode> iterator() { return nodesIterator(); }
		public Iterator<JSONNode> nodesIterator() { return new Iterators.Nodes(this); }
		public Iterator<JSONObject> objectsIterator() { return new Iterators.Objects(this); }
		public Iterator<JSONCollection> collectionsIterator() { return new Iterators.Collections(this); }
		public Iterable<JSONNode> nodesIterable() { return () -> nodesIterator(); }
		public Iterable<JSONObject> objectsIterable() { return () -> objectsIterator(); }
		public Iterable<JSONCollection> collectionsIterable() { return () -> collectionsIterator(); }
		public List<JSONNode> nodes() { return Collections.unmodifiableList(iterableToList(nodesIterable())); }
		public List<JSONObject> objects() { return Collections.unmodifiableList(iterableToList(objectsIterable())); }
		public List<JSONCollection> collections() { return Collections.unmodifiableList(iterableToList(collectionsIterable())); }
		
		@Override
		public void toString(StringBuilder builder, int depth, boolean compress) {
			if(nodes == null || nodes.isEmpty()) {
				builder.append(type == JSONType.ARRAY ? "[]" : "{}");
				return;
			}
			
			String tabs = tabString(depth + 1);
			boolean isArray = type == JSONType.ARRAY;
			
			char chOpen = '{';
			char chClose = '}';
			
			if(isArray) {
				chOpen = '[';
				chClose = ']';
			}
			
			builder.append(chOpen);
			
			if(!compress) {
				builder.append('\n');
			}
			
			boolean first = true;
			for(JSONNode node : nodes.values()) {
				if(first) {
					first = false;
				} else {
					builder.append(',');
					
					if(!compress) {
						builder.append('\n');
					}
				}
				
				if(!compress) {
					builder.append(tabs);
				}
				
				if(!isArray) {
					builder.append('"').append(node.name()).append('"').append(':');
					
					if(!compress) {
						builder.append(' ');
					}
				}
				
				node.toString(builder, depth + 1, compress);
			}
			
			
			if(!compress) {
				builder.append('\n');
				builder.append(tabs, 0, tabs.length() - 1);
			}
			
			builder.append(chClose);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(nodes);
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(!super.equals(obj))
				return false;
			if(getClass() != obj.getClass())
				return false;
			JSONCollection other = (JSONCollection) obj;
			return Objects.equals(nodes, other.nodes);
		}
		
		private static final class TraverseResult {
			
			JSONCollection parent;
			JSONNode node;
			boolean success;
			int offset;
		}
		
		private static final class Iterators {
			
			private static final Iterator<JSONNode> maybeEmptyIterator(JSONCollection collection) {
				return collection.nodes != null ? collection.nodes.values().iterator() : Empty.instance();
			}
			
			private static abstract class GenericBiTypeIterator<A, B> implements Iterator<B> {
				
				private final Iterator<A> iterator;
				private boolean hasNext;
				private A lastItem;
				
				protected GenericBiTypeIterator(Iterator<A> theIterator) {
					iterator = theIterator;
				}
				
				protected abstract boolean isOfCorrectType(A node);
				protected abstract B castToCorrectType(A node);
				
				@Override
				public boolean hasNext() {
					while((hasNext = iterator.hasNext())
							&& !isOfCorrectType(lastItem = iterator.next()));
					
					if(!hasNext) {
						lastItem = null;
					}
					
					return hasNext && isOfCorrectType(lastItem);
				}
				
				@Override
				public B next() {
					// Happens at the beginning or there are no more items
					if(lastItem == null && !hasNext) {
						if(iterator.hasNext()) {
							return castToCorrectType(lastItem = iterator.next());
						}
						
						// Otherwise, respect the documentation
						throw new NoSuchElementException();
					}
					
					// Happens when hasNext() has been called at least once
					return castToCorrectType(lastItem);
				}
			}
			
			public static final class Empty<T> implements Iterator<T> {
				
				private static final Empty<?> EMPTY = new Empty<>();
				
				private Empty() {}
				
				public static final <T> Empty<T> instance() {
					@SuppressWarnings("unchecked")
					Empty<T> empty = (Empty<T>) EMPTY;
					return empty;
				}
				
				@Override public boolean hasNext() { return false; }
				@Override public T next() { throw new NoSuchElementException(); }
			}
			
			public static final class Nodes implements Iterator<JSONNode> {
				
				private final Iterator<JSONNode> it;
				
				protected Nodes(JSONCollection collection) { it = maybeEmptyIterator(collection); }
				
				@Override public boolean hasNext() { return it.hasNext(); }
				@Override public JSONNode next() { return it.next(); }
			}
			
			public static final class Objects extends GenericBiTypeIterator<JSONNode, JSONObject> {
				
				protected Objects(JSONCollection collection) { super(maybeEmptyIterator(collection)); }
				
				@Override protected boolean isOfCorrectType(JSONNode node) { return node.isObject(); }
				@Override protected JSONObject castToCorrectType(JSONNode node) { return (JSONObject) node; }
			}
			
			public static final class Collections extends GenericBiTypeIterator<JSONNode, JSONCollection> {
				
				protected Collections(JSONCollection collection) { super(maybeEmptyIterator(collection)); }
				
				@Override protected boolean isOfCorrectType(JSONNode node) { return node.isCollection(); }
				@Override protected JSONCollection castToCorrectType(JSONNode node) { return (JSONCollection) node; }
			}
		}
	}
}