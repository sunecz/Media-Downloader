package sune.app.mediadown.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDCollectionType;
import sune.util.ssdf2.SSDNode;
import sune.util.ssdf2.SSDObject;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

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
	
	public static final SSDCollection read(String string) {
		return read(string, DEFAULT_CHARSET);
	}
	
	public static final SSDCollection read(String string, Charset charset) {
		try {
			return JSONReader.create(string, charset).read();
		} catch(IOException ex) {
			// Should not happen, but still throw it as unchecked
			throw new UncheckedIOException(ex);
		}
	}
	
	public static final SSDCollection read(InputStream stream) throws IOException {
		return read(stream, DEFAULT_CHARSET);
	}
	
	public static final SSDCollection read(InputStream stream, Charset charset) throws IOException {
		return JSONReader.create(stream, charset).read();
	}
	
	public static final SSDCollection read(Path path) throws IOException {
		return read(path, DEFAULT_CHARSET);
	}
	
	public static final SSDCollection read(Path path, Charset charset) throws IOException {
		return JSONReader.create(path, charset).read();
	}
	
	private static final class JSONReader {
		
		/* Implementation note:
		 * Use absolute CharBuffer::get(int) method for better performance, since Java allegedly generates
		 * worse code for the relative CharBuffer::get() method.
		 */
		
		private static final int BUFFER_SIZE = 8192;
		
		private static final MethodHandle mh_SSDCollection;
		private static final MethodHandle mh_SSDValue;
		private static final MethodHandle mh_SSDObject;
		
		static {
			MethodHandles.Lookup lookup = MethodHandles.lookup();
			try {
				Constructor<?> c_SSDCollection = SSDCollection.class.getDeclaredConstructor(SSDNode.class, boolean.class);
				Reflection.setAccessible(c_SSDCollection, true);
				mh_SSDCollection = lookup.unreflectConstructor(c_SSDCollection);
			} catch(NoSuchMethodException
						| IllegalAccessException
						| IllegalArgumentException
						| NoSuchFieldException
						| SecurityException ex) {
				throw new IllegalStateException("Unable to obtain SSDCollection constructor");
			}
			try {
				Constructor<?> c_SSDValue = SSDValue.class.getDeclaredConstructor(Object.class);
				Reflection.setAccessible(c_SSDValue, true);
				mh_SSDValue = lookup.unreflectConstructor(c_SSDValue);
			} catch(NoSuchMethodException
						| IllegalAccessException
						| IllegalArgumentException
						| NoSuchFieldException
						| SecurityException ex) {
				throw new IllegalStateException("Unable to obtain SSDValue constructor");
			}
			try {
				Constructor<?> c_SSDObject = SSDObject.class.getDeclaredConstructor(SSDNode.class, String.class,
					SSDType.class, SSDValue.class, SSDValue.class);
				Reflection.setAccessible(c_SSDObject, true);
				mh_SSDObject = lookup.unreflectConstructor(c_SSDObject);
			} catch(NoSuchMethodException
						| IllegalAccessException
						| IllegalArgumentException
						| NoSuchFieldException
						| SecurityException ex) {
				throw new IllegalStateException("Unable to obtain SSDObject constructor");
			}
		}
		
		private final Reader input;
		private final CharBuffer buf;
		private int pos;
		private int lim;
		
		private final Deque<Pair<String, SSDCollection>> parents;
		private final StringBuilder str;
		private String lastStr;
		private SSDType lastType = SSDType.UNKNOWN;
		private Pair<String, SSDCollection> lastParent;
		private int c;
		
		private JSONReader(ReadableByteChannel input, Charset charset) {
			this.input = Channels.newReader(Objects.requireNonNull(input), charset);
			this.buf = CharBuffer.allocate(BUFFER_SIZE).flip();
			this.parents = new ArrayDeque<>();
			this.str = new StringBuilder();
		}
		
		private static final String fixObjectName(String name) {
			// Since SSDF supports Annotations that use colon (:) as the control (access) character,
			// we must escape it beforehand.
			return name.replace(":", "_");
		}
		
		private static final SSDCollection createRoot(boolean isArray) {
			try {
				return (SSDCollection) mh_SSDCollection.invoke(null, isArray);
			} catch(Throwable ex) {
				throw new IllegalStateException("Unable to create root", ex);
			}
		}
		
		private static final SSDValue createValue(String value) {
			try {
				return (SSDValue) mh_SSDValue.invoke(value);
			} catch(Throwable ex) {
				throw new IllegalStateException("Unable to create value", ex);
			}
		}
		
		private static final SSDObject createObject(SSDType type, String name, String rawValue) {
			try {
				SSDValue fixValue, frmValue;
				if(type == SSDType.STRING) {
					fixValue = createValue('\"' + rawValue + '\"');
					frmValue = createValue(Utils.replaceUnicode4Digits(rawValue).replaceAll("\\\\(.)", "$1"));
				} else {
					fixValue = frmValue = createValue(rawValue); // Optimization
				}
				return (SSDObject) mh_SSDObject.invoke(null, name, type, fixValue, frmValue);
			} catch(Throwable ex) {
				throw new IllegalStateException("Unable to create object", ex);
			}
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
		
		private final int fill() throws IOException {
			buf.position(pos);
			buf.compact();
			int read = input.read(buf);
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
			if(str.length() <= 0) return;
			
			Pair<String, SSDCollection> pair = parents.peekFirst();
			SSDCollectionType parentType = pair.b.getType();
			
			if(lastStr == null || lastStr.isEmpty()) {
				switch(parentType) {
					case OBJECT:
						throw new IllegalStateException("Object's item name cannot be null or empty");
					case ARRAY:
						lastStr = "";
						break;
				}
			}
			
			SSDObject object = createObject(lastType, lastStr, str.toString());
			str.setLength(0);
			
			switch(parentType) {
				case OBJECT: pair.b.setDirect(lastStr, object); break;
				case ARRAY:  pair.b.add               (object); break;
			}
		}
		
		private final void readAndMatchSequence(String sequence) throws IOException {
			boolean success = true;
			for(int i = 0, l = sequence.length(); i < l; ++i) {
				if(c != sequence.codePointAt(i)) {
					success = false;
					break;
				}
				c = next();
			}
			if(success) {
				str.append(sequence);
			}
		}
		
		private final void checkEndOfElementAfterSequence() throws IOException {
			if(Character.isWhitespace(c))
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
		
		private final void readObject() throws IOException {
			if(parents.size() > 1 && (lastStr == null || lastStr.isEmpty()))
				throw new IllegalStateException("Invalid name");
			if(parents.isEmpty()) parents.push(new Pair<>(null,    createRoot(false)));
			else                  parents.push(new Pair<>(lastStr, SSDCollection.empty()));
			c = next();
		}
		
		private final void readArray() throws IOException {
			if(parents.size() > 1 && (lastStr == null || lastStr.isEmpty()))
				throw new IllegalStateException("Invalid name");
			if(parents.isEmpty()) parents.push(new Pair<>(null,    createRoot(true)));
			else                  parents.push(new Pair<>(lastStr, SSDCollection.emptyArray()));
			c = next();
		}
		
		private final void readTrue() throws IOException {
			readAndMatchSequence("true");
			checkEndOfElementAfterSequence();
			lastType = SSDType.BOOLEAN;
		}
		
		private final void readFalse() throws IOException {
			readAndMatchSequence("false");
			checkEndOfElementAfterSequence();
			lastType = SSDType.BOOLEAN;
		}
		
		private final void readNull() throws IOException {
			readAndMatchSequence("null");
			checkEndOfElementAfterSequence();
			lastType = SSDType.NULL;
		}
		
	    private final void readNumber() throws IOException {
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
			lastType = fraction ? SSDType.DECIMAL : SSDType.INTEGER;
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
			lastType = SSDType.STRING;
	    }
		
		private final void readNext() throws IOException {
			while((c = skipWhitespaces()) != -1) {
				switch(c) {
					case CHAR_STRING_QUOTES: readString(); break;
					case CHAR_SEPARATOR_PROPERTY:
						c = next();
						lastStr = fixObjectName(str.toString());
						str.setLength(0);
						break;
					case CHAR_OBJECT_OPEN: readObject(); break;
					case CHAR_ARRAY_OPEN: readArray(); break;
					case CHAR_OBJECT_CLOSE:
					case CHAR_ARRAY_CLOSE:
						c = next();
						addPendingObject();
						Pair<String, SSDCollection> pair = parents.pop();
						lastParent = pair;
						if(!parents.isEmpty()) {
							pair = parents.peekFirst();
							switch(pair.b.getType()) {
								case OBJECT: pair.b.set(lastParent.a, lastParent.b); break;
								case ARRAY:  pair.b.add(lastParent.b);               break;
							}
						}
						break;
					case CHAR_SEPARATOR_ELEMENT:
						c = next();
						addPendingObject();
						break;
					case 't': readTrue(); break;
					case 'f': readFalse(); break;
					case 'n': readNull(); break;
					default:
						if(c == '-' || Character.isDigit(c))
							readNumber();
						break;
				}
			}
		}
		
		public final SSDCollection read() throws IOException {
			c = next(); // Bootstrap
			readNext();
			return lastParent.b;
		}
	}
}