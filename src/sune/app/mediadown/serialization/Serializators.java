package sune.app.mediadown.serialization;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import sune.app.mediadown.resource.cache.Cache;
import sune.app.mediadown.resource.cache.NoNullCache;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Reflection;

/** @since 00.02.09 */
public final class Serializators {
	
	// Forbid anyone to create an instance of this class
	private Serializators() {
	}
	
	public static class OfBinary {
		
		// SMDSSF = Sune Media Downloader State Serialized File
		private static final byte[] MAGIC = { 'S', 'M', 'D', 'S', 'S', 'F' };
		private static final int VERSION = 1;
		
		private static final int BUFFER_SIZE = 8192;
		private static final Charset UTF8 = StandardCharsets.UTF_8;
		
		private static final int NO_REFERENCE = 0;
		
		// Forbid anyone to create an instance of this class
		private OfBinary() {
		}
		
		public static final SerializationBinaryReader newInMemoryReader(byte[] array) {
			return new InMemoryReader(SchemaDataLoader.instance(), array);
		}
		
		public static final SerializationBinaryWriter newInMemoryWriter() {
			return new InMemoryWriter(SchemaDataSaver.instance());
		}
		
		public static final SerializationBinaryReader newFileReader(Path path) {
			return new FileReader(SchemaDataLoader.instance(), path);
		}
		
		public static final SerializationBinaryWriter newFileWriter(Path path) {
			return new FileWriter(SchemaDataSaver.instance(), path);
		}
		
		public static interface SerializationBinaryReader extends SerializationReader {
		}
		
		public static interface SerializationBinaryWriter extends SerializationWriter {
			
			@Override byte[] representation() throws IOException; // Type-specific override
		}
		
		private static final class InMemoryReader extends StreamReader {
			
			private final byte[] array;
			
			public InMemoryReader(SchemaDataLoader dataLoader, byte[] array) {
				super(dataLoader);
				this.array = Objects.requireNonNull(array);
			}
			
			@Override
			protected InputStream createStream() throws IOException {
				return new ByteArrayInputStream(array);
			}
			
			@Override
			public int available() throws IOException {
				return stream().available();
			}
		}
		
		private static final class InMemoryWriter extends StreamWriter {
			
			public InMemoryWriter(SchemaDataSaver dataSaver) {
				super(dataSaver);
			}
			
			@Override
			protected OutputStream createStream() throws IOException {
				return new ByteArrayOutputStream();
			}
			
			@Override
			public byte[] representation() throws IOException {
				flush();
				return ((ByteArrayOutputStream) stream()).toByteArray();
			}
		}
		
		private static final class FileReader extends StreamReader {
			
			private final Path path;
			
			public FileReader(SchemaDataLoader dataLoader, Path path) {
				super(dataLoader);
				this.path = Objects.requireNonNull(path);
			}
			
			@Override
			protected InputStream createStream() throws IOException {
				return Files.newInputStream(path, READ);
			}
			
			@Override
			public int available() throws IOException {
				return stream().available();
			}
		}
		
		private static final class FileWriter extends StreamWriter {
			
			private final Path path;
			
			public FileWriter(SchemaDataSaver dataSaver, Path path) {
				super(dataSaver);
				this.path = Objects.requireNonNull(path);
			}
			
			@Override
			protected OutputStream createStream() throws IOException {
				return Files.newOutputStream(path, WRITE, CREATE, TRUNCATE_EXISTING);
			}
			
			@Override
			public byte[] representation() throws IOException {
				flush();
				return Files.readAllBytes(path);
			}
		}
		
		private static abstract class StreamReader extends ReaderBase {
			
			private InputStream stream;
			
			protected StreamReader(SchemaDataLoader dataLoader) {
				super(dataLoader);
			}
			
			protected abstract InputStream createStream() throws IOException;
			
			protected final InputStream stream() {
				if(stream == null) {
					throw new IllegalStateException("Not open");
				}
				
				return stream;
			}
			
			@Override
			public void open() throws IOException {
				stream = createStream();
				
				if(stream == null) {
					throw new IllegalStateException("Stream is null");
				}
				
				pos = lim = 0;
				readHeader();
			}
			
			@Override
			public int read(byte[] buf, int off, int len) throws IOException {
				return stream().read(buf, off, len);
			}
			
			@Override
			public void close() throws IOException {
				if(stream == null) {
					return;
				}
				
				stream.close();
			}
		}
		
		private static abstract class StreamWriter extends WriterBase {
			
			private OutputStream stream;
			
			protected StreamWriter(SchemaDataSaver dataSaver) {
				super(dataSaver);
			}
			
			protected abstract OutputStream createStream() throws IOException;
			
			protected final OutputStream stream() {
				if(stream == null) {
					throw new IllegalStateException("Not open");
				}
				
				return stream;
			}
			
			@Override
			protected int flushBuffer() throws IOException {
				int flushed = super.flushBuffer();
				stream().flush();
				return flushed;
			}
			
			@Override
			public void open() throws IOException {
				stream = createStream();
				
				if(stream == null) {
					throw new IllegalStateException("Stream is null");
				}
				
				pos = 0;
				writeHeader();
			}
			
			@Override
			public int write(byte[] buf, int off, int len) throws IOException {
				stream().write(buf, off, len);
				return len;
			}
			
			@Override
			public void close() throws IOException {
				if(stream == null) {
					return;
				}
				
				flushBuffer();
				stream.close();
			}
		}
		
		private static abstract class ReaderBase implements SerializationBinaryReader {
			
			protected final byte[] buf;
			protected final int cap;
			protected int pos;
			protected int lim;
			
			protected final SchemaDataLoader dataLoader;
			protected final Map<Integer, Object> objects = new HashMap<>();
			protected int nextObjectId = NO_REFERENCE + 1;
			
			private ObjectStream stream;
			private Utf8LineReader lineReader;
			
			protected ReaderBase(SchemaDataLoader dataLoader) {
				this.dataLoader = Objects.requireNonNull(dataLoader);
				buf = new byte[BUFFER_SIZE];
				cap = buf.length;
			}
			
			protected void compactBuffer(int len) {
				len = Math.min(len, lim - pos);
				System.arraycopy(buf, pos, buf, 0, len);
				pos = 0;
				lim = len;
			}
			
			protected int ensureAvailable(int count) throws IOException {
				final int limit = Math.min(count, cap);
				final int available = lim - pos;
				
				if(available >= limit) {
					return limit; // Data available
				}
				
				if(available > 0) {
					compactBuffer(available);
				}
				
				final int filled = fillBuffer();
				return Math.min(filled, limit);
			}
			
			// Returns the number of available (unread) bytes or -1 if EOF and all bytes
			// has already been read.
			protected int fillBuffer() throws IOException {
				int free = cap - lim;
				
				if(free <= 0) {
					return lim - pos; // Already filled
				}
				
				int off = lim, read = 0;
				for(; off < cap && (read = read(buf, off, cap - off)) >= 0; off += read);
				lim = off;
				
				if(lim - pos == 0 && read < 0) {
					return -1; // EOF
				}
				
				return lim - pos;
			}
			
			protected final int readType() throws IOException {
				return readUncheckedByte() & 0xff;
			}
			
			protected final void checkType(int required) throws IOException {
				int type;
				if((type = readType()) != required) {
					throw new IllegalStateException("Type mismatch: required=" + required + ", actual=" + type);
				}
			}
			
			protected final void readHeader() throws IOException {
				final int length = MAGIC.length;
				ensureAvailable(length + 4);
				
				byte[] magic = new byte[length];
				System.arraycopy(buf, 0, magic, 0, length);
				pos += length;
				
				if(!Arrays.equals(MAGIC, magic)) {
					throw new IllegalStateException("Unsupported magic");
				}
				
				int version = readUncheckedInt();
				
				if(version > VERSION) {
					throw new IllegalStateException("Newer version '" + version + "', but current is '" + VERSION + "'");
				}
			}
			
			protected final boolean readUncheckedBoolean() throws IOException {
				ensureAvailable(1);
				byte val = buf[pos++];
				return val == 1;
			}
			
			protected final byte readUncheckedByte() throws IOException {
				ensureAvailable(1);
				byte val = buf[pos++];
				return val;
			}
			
			protected final char readUncheckedChar() throws IOException {
				ensureAvailable(2);
				int i0 = (int) (buf[pos+0] & 0xff);
				int i1 = (int) (buf[pos+1] & 0xff);
				pos += 2;
				return (char) ((i0 << 8) | (i1));
			}
			
			protected final short readUncheckedShort() throws IOException {
				ensureAvailable(2);
				int i0 = (int) (buf[pos+0] & 0xff);
				int i1 = (int) (buf[pos+1] & 0xff);
				pos += 2;
				return (short) ((i0 << 8) | (i1));
			}
			
			protected final int readUncheckedInt() throws IOException {
				ensureAvailable(4);
				int i0 = (int) (buf[pos+0] & 0xff);
				int i1 = (int) (buf[pos+1] & 0xff);
				int i2 = (int) (buf[pos+2] & 0xff);
				int i3 = (int) (buf[pos+3] & 0xff);
				pos += 4;
				return (int) ((i0 << 24) | (i1 << 16) | (i2 << 8) | (i3));
			}
			
			protected final long readUncheckedLong() throws IOException {
				ensureAvailable(8);
				int i0 = (int) (buf[pos+0] & 0xff);
				int i1 = (int) (buf[pos+1] & 0xff);
				int i2 = (int) (buf[pos+2] & 0xff);
				int i3 = (int) (buf[pos+3] & 0xff);
				int i4 = (int) (buf[pos+4] & 0xff);
				int i5 = (int) (buf[pos+5] & 0xff);
				int i6 = (int) (buf[pos+6] & 0xff);
				int i7 = (int) (buf[pos+7] & 0xff);
				pos += 8;
				long hi = (long) ((i0 << 24) | (i1 << 16) | (i2 << 8) | (i3));
				long lo = (long) ((i4 << 24) | (i5 << 16) | (i6 << 8) | (i7));
				return (hi << 32) | (lo);
			}
			
			protected final float readUncheckedFloat() throws IOException {
				return Float.intBitsToFloat(readUncheckedInt());
			}
			
			protected final double readUncheckedDouble() throws IOException {
				return Double.longBitsToDouble(readUncheckedLong());
			}
			
			protected final String readUncheckedString() throws IOException {
				final int length = readUncheckedInt();
				
				if(length < 0) {
					return null;
				}
				
				byte[] bytes = new byte[length];
				
				for(int dst = 0, read;
						dst < length && (read = ensureAvailable(length - dst)) >= 0;
						dst += read) {
					System.arraycopy(buf, pos, bytes, dst, read);
					pos += read;
				}
				
				return new String(bytes, UTF8);
			}
			
			protected final Class<?> readUncheckedObjectHeader() throws IOException {
				final byte isNull = readUncheckedByte();
				
				if(isNull == 1) {
					return null;
				}
				
				return SerializationUtils.classOf(readUncheckedString());
			}
			
			protected final Pair<Integer, Boolean> readUncheckedObjectReference() throws IOException {
				int reference = readUncheckedInt();
				boolean objectShouldExist = true;
				int objectId = reference;
				
				if(reference == NO_REFERENCE) {
					objectId = nextObjectId++;
					objectShouldExist = false;
				}
				
				return new Pair<>(objectId, objectShouldExist);
			}
			
			protected final Object readUncheckedObjectBody(Object instance) throws IOException {
				// Read the non-static and non-transient fields
				return dataLoader.loadAll(this, instance);
			}
			
			protected final Object readUncheckedObjectBodySerializable(Object instance) throws IOException {
				// Read the non-static and non-transient fields
				Object object = dataLoader.loadAll(this, instance);
				
				// Call the readObject method to load the custom contents
				SerializableCaller.readObject(object, objectStream());
				
				SerializableCaller.Handles handles = SerializableCaller.getHandles(object);
				Object serializable = object;
				
				if(handles.readResolve() != null) {
					serializable = SerializableCaller.readResolve(handles, object);
				}
				
				return serializable;
			}
			
			protected final Object readUncheckedObject() throws IOException {
				Class<?> clazz = readUncheckedObjectHeader();
				
				if(clazz == null) {
					return null; // Null object
				}
				
				Pair<Integer, Boolean> pair = readUncheckedObjectReference();
				int objectId = pair.a;
				boolean objectShouldExist = pair.b;
				
				if(objectShouldExist) {
					return getObject(objectId);
				}
				
				Object object = dataLoader.allocateNew(clazz);
				Object readObject = object;
				addObject(objectId, object);
				
				if(Serializable.class.isAssignableFrom(clazz)) {
					// Since we already read the header, read the body directly
					readObject = readUncheckedObjectBodySerializable(object);
				} else {
					readObject = readUncheckedObjectBody(object);
				}
				
				if(readObject != object) {
					// Serialization may change the object, see readResolve()
					addObject(objectId, readObject);
				}
				
				return readObject;
			}
			
			protected final Object readUncheckedObjectSerializable() throws IOException {
				Class<?> clazz = readUncheckedObjectHeader();
				
				if(clazz == null) {
					return null; // Null object
				}
				
				Pair<Integer, Boolean> pair = readUncheckedObjectReference();
				int objectId = pair.a;
				boolean objectShouldExist = pair.b;
				
				if(objectShouldExist) {
					return getObject(objectId);
				}
				
				Object object = dataLoader.allocateNew(clazz);
				Object readObject = object;
				addObject(objectId, object);
				
				readObject = readUncheckedObjectBodySerializable(object);
				
				if(readObject != object) {
					// Serialization may change the object, see readResolve()
					addObject(objectId, readObject);
				}
				
				return readObject;
			}
			
			protected final void addObject(int reference, Object object) {
				objects.put(reference, object);
			}
			
			protected final Object getObject(int reference) {
				Object object = objects.get(reference);
				
				if(object == null) {
					throw new IllegalStateException("Reference points to a non-existent object");
				}
				
				return object;
			}
			
			protected final int readDirect() throws IOException {
				return readUncheckedByte() & 0xff;
			}
			
			protected final int readUnsignedByte() throws IOException {
				return readByte() & 0xff;
			}
			
			protected final int readUnsignedShort() throws IOException {
				return readShort() & 0xffff;
			}
			
			protected final int readDirect(byte[] b, int off, int len) throws IOException {
				int start = off, available = 0;
				
				for(int end = off + len;
						off < end && (available = ensureAvailable(end - off)) >= 0;
						off += available,
						pos += available) {
					System.arraycopy(buf, pos, b, off, available);
				}
				
				return off == start && available < 0 ? available : off - start;
			}
			
			protected final ObjectStream objectStream() throws IOException {
				if(stream == null) {
					stream = new ObjectStream(this);
				}
				
				return stream;
			}
			
			protected final Utf8LineReader lineReader() {
				if(lineReader == null) {
					lineReader = new Utf8LineReader();
				}
				
				return lineReader;
			}
			
			@Override
			public boolean readBoolean() throws IOException {
				checkType(SchemaFieldType.VALUE | SchemaFieldType.BOOLEAN);
				return readUncheckedBoolean();
			}
			
			@Override
			public byte readByte() throws IOException {
				checkType(SchemaFieldType.VALUE | SchemaFieldType.BYTE);
				return readUncheckedByte();
			}
			
			@Override
			public char readChar() throws IOException {
				checkType(SchemaFieldType.VALUE | SchemaFieldType.CHAR);
				return readUncheckedChar();
			}
			
			@Override
			public short readShort() throws IOException {
				checkType(SchemaFieldType.VALUE | SchemaFieldType.SHORT);
				return readUncheckedShort();
			}
			
			@Override
			public int readInt() throws IOException {
				checkType(SchemaFieldType.VALUE | SchemaFieldType.INT);
				return readUncheckedInt();
			}
			
			@Override
			public long readLong() throws IOException {
				checkType(SchemaFieldType.VALUE | SchemaFieldType.LONG);
				return readUncheckedLong();
			}
			
			@Override
			public float readFloat() throws IOException {
				checkType(SchemaFieldType.VALUE | SchemaFieldType.FLOAT);
				return readUncheckedFloat();
			}
			
			@Override
			public double readDouble() throws IOException {
				checkType(SchemaFieldType.VALUE | SchemaFieldType.DOUBLE);
				return readUncheckedDouble();
			}
			
			@Override
			public String readString() throws IOException {
				checkType(SchemaFieldType.VALUE | SchemaFieldType.STRING);
				return readUncheckedString();
			}
			
			@Override
			public Object readObject() throws IOException {
				checkType(SchemaFieldType.VALUE | SchemaFieldType.OBJECT);
				return readUncheckedObject();
			}
			
			@Override
			public boolean[] readBooleanArray() throws IOException {
				checkType(SchemaFieldType.ARRAY | SchemaFieldType.BOOLEAN);
				
				final int length = readUncheckedInt();
				boolean[] array = new boolean[length];
				
				for(int i = 0; i < length; ++i) {
					array[i] = readUncheckedBoolean();
				}
				
				return array;
			}
			
			@Override
			public byte[] readByteArray() throws IOException {
				checkType(SchemaFieldType.ARRAY | SchemaFieldType.BYTE);
				
				final int length = readUncheckedInt();
				byte[] array = new byte[length];
				
				// Use direct batch-copy instead of per-element loop
				for(int dst = 0, read;
						dst < length && (read = ensureAvailable(length - dst)) >= 0;
						dst += read) {
					System.arraycopy(buf, pos, array, dst, read);
					pos += read;
				}
				
				return array;
			}
			
			@Override
			public char[] readCharArray() throws IOException {
				checkType(SchemaFieldType.ARRAY | SchemaFieldType.CHAR);
				
				final int length = readUncheckedInt();
				char[] array = new char[length];
				
				for(int i = 0; i < length; ++i) {
					array[i] = readUncheckedChar();
				}
				
				return array;
			}
			
			@Override
			public short[] readShortArray() throws IOException {
				checkType(SchemaFieldType.ARRAY | SchemaFieldType.SHORT);
				
				final int length = readUncheckedInt();
				short[] array = new short[length];
				
				for(int i = 0; i < length; ++i) {
					array[i] = readUncheckedShort();
				}
				
				return array;
			}
			
			@Override
			public int[] readIntArray() throws IOException {
				checkType(SchemaFieldType.ARRAY | SchemaFieldType.INT);
				
				final int length = readUncheckedInt();
				int[] array = new int[length];
				
				for(int i = 0; i < length; ++i) {
					array[i] = readUncheckedInt();
				}
				
				return array;
			}
			
			@Override
			public long[] readLongArray() throws IOException {
				checkType(SchemaFieldType.ARRAY | SchemaFieldType.LONG);
				
				final int length = readUncheckedInt();
				long[] array = new long[length];
				
				for(int i = 0; i < length; ++i) {
					array[i] = readUncheckedLong();
				}
				
				return array;
			}
			
			@Override
			public float[] readFloatArray() throws IOException {
				checkType(SchemaFieldType.ARRAY | SchemaFieldType.FLOAT);
				
				final int length = readUncheckedInt();
				float[] array = new float[length];
				
				for(int i = 0; i < length; ++i) {
					array[i] = readUncheckedFloat();
				}
				
				return array;
			}
			
			@Override
			public double[] readDoubleArray() throws IOException {
				checkType(SchemaFieldType.ARRAY | SchemaFieldType.DOUBLE);
				
				final int length = readUncheckedInt();
				double[] array = new double[length];
				
				for(int i = 0; i < length; ++i) {
					array[i] = readUncheckedDouble();
				}
				
				return array;
			}
			
			@Override
			public String[] readStringArray() throws IOException {
				checkType(SchemaFieldType.ARRAY | SchemaFieldType.STRING);
				
				final int length = readUncheckedInt();
				String[] array = new String[length];
				
				for(int i = 0; i < length; ++i) {
					array[i] = readUncheckedString();
				}
				
				return array;
			}
			
			@Override
			public Object[] readObjectArray() throws IOException {
				checkType(SchemaFieldType.ARRAY | SchemaFieldType.OBJECT);
				
				final Class<?> clazz = SerializationUtils.classOf(readUncheckedString());
				final int length = readUncheckedInt();
				Object[] array = (Object[]) Array.newInstance(clazz, length);
				
				for(int i = 0; i < length; ++i) {
					array[i] = readUncheckedObject();
				}
				
				return array;
			}
			
			@Override
			public long skip(long n) throws IOException {
				for(long remaining = n; remaining > 0L;) {
					int lim = (int) Math.min(remaining, buf.length);
					int rem = lim;
					
					for(int available;
							rem > 0 && (available = ensureAvailable(rem)) >= 0;
							rem -= available,
							pos += available);
					
					if(rem > 0) { // EOF
						return n - remaining;
					}
					
					remaining -= lim;
				}
				
				return n;
			}
			
			@Override
			public int available() throws IOException {
				// By default the number of available bytes is unknown
				return 0;
			}
			
			@Override
			public String readLine() throws IOException {
				return lineReader().readLine();
			}
			
			protected final class Utf8LineReader {
				
				/* UTF-8 encoding overview:
				 * 1 byte:  0xxxxxxx
				 * 2 bytes: 110xxxxx 10xxxxxx
				 * 3 bytes: 1110xxxx 10xxxxxx 10xxxxxx
				 * 4 bytes: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
				 */
				
				private static final int READ_LINE_NONE = -2;
				private static final int READ_LINE_ZERO = -1;
				
				private int remaining = READ_LINE_NONE;
				
				protected final int encodedBytesCount(int a) {
					if(a < 0b11100000) return 1;
					if(a < 0b11110000) return 2;
					return 3;
				}
				
				protected final int bytesCount(int c) {
					if(c <= 0x7f)  return 1;
					if(c <= 0x7ff) return 2;
					if(c <= 0xfff) return 3;
					return 4;
				}
				
				protected final int decodeMoreBytes(int a, int p, int n) {
					final int mask = 0b00111111;
					
					switch(n) {
						case 3:  return ((a & 0b00011111) << 24) | ((buf[p+1] & mask) << 16) | ((buf[p+2] & mask) << 8) | (buf[p+3] & mask);
						case 2:  return ((a & 0b00001111) << 16) | ((buf[p+1] & mask) <<  8) |  (buf[p+2] & mask);
						default: return ((a & 0b00000111) <<  8) |  (buf[p+1] & mask);
					}
				}
				
				protected final int nextCodePoint() throws IOException {
					int r = lim - pos;
					
					// There should always be at least 1 byte available, this truth
					// must be ensured before calling this method.
					if(r < 1) {
						throw new IllegalStateException("Not enough data");
					}
					
					int a = buf[pos] & 0xff;
					
					// Check whether it is a single byte character
					if((a >>> 7) == 0) {
						++pos;
						return a & 0xff;
					}
					
					int n = encodedBytesCount(a);
					
					// Check the zero bit in the leading byte
					if(((a >> (6 - n)) & 0b1) != 0) {
						throw new IllegalStateException("Invalid UTF-8 byte");
					}
					
					if(ensureAvailable(n) < n) {
						throw new IllegalStateException("Not enough data");
					}
					
					int c = decodeMoreBytes(a, pos, n);
					pos += n;
					
					return c;
				}
				
				// Delimiters: \n, \r, \r\n or EOF.
				public String readLine() throws IOException {
					// Since all Strings are always written with a header, when in the line mode
					// we must first read the String header.
					if(remaining == READ_LINE_NONE) {
						checkType(SchemaFieldType.VALUE | SchemaFieldType.STRING);
						remaining = readUncheckedInt();
					} else if(remaining == READ_LINE_ZERO) {
						remaining = READ_LINE_NONE;
						return null;
					} else if(remaining == 0) { // Handle trailing empty lines
						remaining = READ_LINE_ZERO;
						return "";
					}
					
					final int len = cap;
					int off = 0;
					byte[] tmp = null;
					List<byte[]> bufs = null;
					int rem = remaining;
					
					outer:
					for(int available; rem > 0 && (available = ensureAvailable(len)) >= 0;) {
						final int start = pos;
						int newLine = 0;
						
						// Simply check the bytes, since all delimiters can be represented by a single byte,
						// but correctly skip any character that cannot be represented by a single byte.
						for(int end = start + available, c; rem > 0 && pos < end; --rem) {
							c = nextCodePoint();
							
							if(c == '\n') {
								newLine = 1;
								break;
							} else if(c == '\r') {
								newLine = 1;
								
								// Must also check for \r\n, but only if there is enough data
								if(ensureAvailable(1) >= 0) {
									if((c = nextCodePoint()) == '\n') {
										newLine = 2;
										--rem;
									} else {
										pos -= bytesCount(c); // Go back
									}
								}
								
								break;
							}
						}
						
						// All bytes in the buffer were read, add them
						int read = pos - start;
						remaining -= read;
						read -= newLine;
						
						// All read, not ending with a new line
						if(newLine == 0 && remaining == 0) {
							remaining = READ_LINE_ZERO;
						}
						
						if(read == 0) {
							break outer;
						}
						
						if(tmp == null) {
							tmp = new byte[BUFFER_SIZE];
						}
						
						// Read bytes fit into the current buffer
						if(off + read <= BUFFER_SIZE) {
							System.arraycopy(buf, start, tmp, off, read);
							off += read;
						}
						// Read bytes do not fit into the current buffer
						else {
							int num = BUFFER_SIZE - off;
							System.arraycopy(buf, start, tmp, off, num);
							
							if(bufs == null) {
								bufs = new ArrayList<>();
							}
							
							bufs.add(tmp);
							
							tmp = new byte[BUFFER_SIZE];
							num = read - num;
							System.arraycopy(buf, start, tmp, 0, num);
							off = num;
						}
						
						// New line encountered, we're done
						if(newLine > 0) {
							break;
						}
					}
					
					// No characters not being a new line has been read
					if(off == 0) {
						// Read an empty line
						if(remaining >= 0) {
							return "";
						}
						
						return null;
					}
					
					// No need to combine multiple buffers
					if(bufs == null) {
						return new String(tmp, 0, off);
					}
					
					// Combine all buffers to a single one
					byte[] acc = new byte[bufs.size() * BUFFER_SIZE + off];
					int cur = 0;
					
					for(byte[] b : bufs) {
						System.arraycopy(b, 0, acc, cur, BUFFER_SIZE);
						cur += BUFFER_SIZE;
					}
					
					if(off > 0) {
						System.arraycopy(tmp, 0, acc, cur, off);
					}
					
					return new String(acc);
				}
			}
			
			protected static final class ObjectStream extends ObjectInputStream {
				
				private final ReaderBase reader;
				
				public ObjectStream(ReaderBase reader) throws IOException {
					this.reader = reader;
				}
				
				@Override public void readFully(byte[] b) throws IOException { reader.readDirect(b, 0, b.length); }
				@Override public void readFully(byte[] b, int off, int len) throws IOException { reader.readDirect(b, off, len); }
				@Override public int skipBytes(int n) throws IOException { return (int) reader.skip(n); }
				@Override public boolean readBoolean() throws IOException { return reader.readBoolean(); }
				@Override public byte readByte() throws IOException { return reader.readByte(); }
				@Override public int readUnsignedByte() throws IOException { return reader.readUnsignedByte(); }
				@Override public short readShort() throws IOException { return reader.readShort(); }
				@Override public int readUnsignedShort() throws IOException { return reader.readUnsignedShort(); }
				@Override public char readChar() throws IOException { return reader.readChar(); }
				@Override public int readInt() throws IOException { return reader.readInt(); }
				@Override public long readLong() throws IOException { return reader.readLong(); }
				@Override public float readFloat() throws IOException { return reader.readFloat(); }
				@Override public double readDouble() throws IOException { return reader.readDouble(); }
				@Override public String readLine() throws IOException { return reader.readLine(); }
				@Override public String readUTF() throws IOException { return reader.readString(); }
				@Override public int read() throws IOException { return reader.readDirect(); }
				@Override public int read(byte[] b) throws IOException { return reader.readDirect(b, 0, b.length); }
				@Override public int read(byte[] b, int off, int len) throws IOException { return reader.read(b, off, len); }
				@Override public long skip(long n) throws IOException { return reader.skip(n); }
				@Override public int available() throws IOException { return reader.available(); }
				@Override public void close() throws IOException { reader.close(); }
				
				@Override
				protected Object readObjectOverride() throws IOException, ClassNotFoundException {
					return reader.readUncheckedObjectSerializable();
				}
				
				@Override
				public void defaultReadObject() throws IOException, ClassNotFoundException {
					// Object's non-static and non-transient fields are always read automatically
				}
			}
		}
		
		private static abstract class WriterBase implements SerializationBinaryWriter {
			
			protected final byte[] buf;
			protected final int cap;
			protected int pos;
			
			protected SchemaDataSaver dataSaver;
			protected final Map<Object, Integer> objects = new IdentityHashMap<>();
			protected int nextObjectId = NO_REFERENCE + 1;
			protected Deque<ObjectArray> arrays;
			
			private ObjectStream objectStream;
			
			protected WriterBase(SchemaDataSaver dataSaver) {
				this.dataSaver = Objects.requireNonNull(dataSaver);
				buf = new byte[BUFFER_SIZE];
				cap = buf.length;
			}
			
			protected int ensureAvailable(int count) throws IOException {
				final int limit = Math.min(count, cap);
				final int available = cap - pos;
				
				if(available >= limit) {
					return limit; // Space available
				}
				
				final int flushed = flushBuffer();
				return Math.min(cap - flushed, limit);
			}
			
			protected int flushBuffer() throws IOException {
				final int available = pos;
				
				if(available <= 0) {
					return 0; // Nothing to flush
				}
				
				int off = 0, written = 0;
				for(; off < pos && (written = write(buf, off, pos - off)) >= 0; off += written);
				
				pos = 0;
				
				return written < 0 ? written : off;
			}
			
			protected final void writeType(int type) throws IOException {
				ensureAvailable(1);
				buf[pos++] = (byte) type;
			}
			
			protected final void writeHeader() throws IOException {
				final int length = MAGIC.length;
				System.arraycopy(MAGIC, 0, buf, 0, length);
				pos += length;
				writeUntyped(VERSION);
			}
			
			protected final void writeUntyped(boolean value) throws IOException {
				ensureAvailable(1);
				buf[pos++] = value ? (byte) 1 : (byte) 0;
			}
			
			protected final void writeUntyped(byte value) throws IOException {
				ensureAvailable(1);
				buf[pos++] = value;
			}
			
			protected final void writeUntyped(char value) throws IOException {
				ensureAvailable(2);
				buf[pos+0] = (byte) ((value >> 8) & 0xff);
				buf[pos+1] = (byte) ((value) & 0xff);
				pos += 2;
			}
			
			protected final void writeUntyped(short value) throws IOException {
				ensureAvailable(2);
				buf[pos+0] = (byte) ((value >> 8) & 0xff);
				buf[pos+1] = (byte) ((value) & 0xff);
				pos += 2;
			}
			
			protected final void writeUntyped(int value) throws IOException {
				ensureAvailable(4);
				buf[pos+0] = (byte) ((value >> 24) & 0xff);
				buf[pos+1] = (byte) ((value >> 16) & 0xff);
				buf[pos+2] = (byte) ((value >> 8) & 0xff);
				buf[pos+3] = (byte) ((value) & 0xff);
				pos += 4;
			}
			
			protected final void writeUntyped(long value) throws IOException {
				ensureAvailable(8);
				buf[pos+0] = (byte) ((value >> 56) & 0xff);
				buf[pos+1] = (byte) ((value >> 48) & 0xff);
				buf[pos+2] = (byte) ((value >> 40) & 0xff);
				buf[pos+3] = (byte) ((value >> 32) & 0xff);
				buf[pos+4] = (byte) ((value >> 24) & 0xff);
				buf[pos+5] = (byte) ((value >> 16) & 0xff);
				buf[pos+6] = (byte) ((value >> 8) & 0xff);
				buf[pos+7] = (byte) ((value) & 0xff);
				pos += 8;
			}
			
			protected final void writeUntyped(float value) throws IOException {
				writeUntyped(Float.floatToRawIntBits(value));
			}
			
			protected final void writeUntyped(double value) throws IOException {
				writeUntyped(Double.doubleToRawLongBits(value));
			}
			
			protected final void writeUntyped(String value) throws IOException {
				if(value == null) {
					writeUntyped(-1);
					return;
				}
				
				byte[] bytes = value.getBytes(UTF8);
				
				final int length = bytes.length;
				writeUntyped(length);
				
				for(int src = 0, written;
						src < length && (written = ensureAvailable(length - src)) >= 0;
						src += written) {
					System.arraycopy(bytes, src, buf, pos, written);
					pos += written;
				}
			}
			
			protected final boolean writeUntypedObjectHeader(Object value) throws IOException {
				if(value == null) {
					writeUntyped((byte) 1);
					return false;
				}
				
				writeUntyped((byte) 0);
				
				String className;
				if((className = value.getClass().getName()) == null) {
					throw new IllegalStateException("Not serializable due to invalid class name: " + value);
				}
				
				writeUntyped(className);
				return true;
			}
			
			protected final int writeUntypedObjectReference(Object value) throws IOException {
				if(value == null) {
					return NO_REFERENCE; // Null object
				}
				
				Pair<Integer, Boolean> pair = addObject(value);
				int objectId = pair.a;
				boolean objectAlreadyWritten = pair.b;
				
				if(objectAlreadyWritten) {
					writeUntyped(objectId);
					return objectId;
				}
				
				writeUntyped(NO_REFERENCE);
				return NO_REFERENCE;
			}
			
			protected final void writeUntypedObjectBody(Object value) throws IOException {
				// Write the non-static and non-transient fields
				dataSaver.saveNew(this, value);
			}
			
			protected final void writeUntypedObjectBodySerializable(Object value) throws IOException {
				// Write the non-static and non-transient fields
				dataSaver.saveNew(this, value);
				// Call the writeObject method to write the custom contents
				SerializableCaller.writeObject(value, objectStream());
			}
			
			protected final void writeUntyped(Object value) throws IOException {
				// Handle Serializable objects separately
				if(value instanceof Serializable) {
					objectStream().writeObject(value);
					return;
				}
				
				if(!writeUntypedObjectHeader(value)) {
					return; // Null object
				}
				
				if(writeUntypedObjectReference(value) != NO_REFERENCE) {
					return;
				}
				
				writeUntypedObjectBody(value);
			}
			
			protected final void writeUntypedSerializable(Object value) throws IOException {
				// Before anything else, we must ensure that the serializable object should
				// be replaced with some other object for serialization.
				SerializableCaller.Handles handles = SerializableCaller.getHandles(value);
				Object serializable = value;
				
				if(handles.writeReplace() != null) {
					serializable = SerializableCaller.writeReplace(handles, value);
				}
				
				if(!writeUntypedObjectHeader(serializable)) {
					return; // Null object
				}
				
				if(writeUntypedObjectReference(serializable) != NO_REFERENCE) {
					return;
				}
				
				writeUntypedObjectBodySerializable(serializable);
			}
			
			protected final Pair<Integer, Boolean> addObject(Object object) {
				boolean objectAlreadyWritten = true;
				Integer prevObjectId = objects.putIfAbsent(object, nextObjectId);
				int objectId;
				
				if(prevObjectId == null) {
					objectId = nextObjectId++;
					objectAlreadyWritten = false;
				} else {
					objectId = prevObjectId;
				}
				
				return new Pair<>(objectId, objectAlreadyWritten);
			}
			
			protected final void writeDirect(int b) throws IOException {
				writeUntyped((byte) b);
			}
			
			protected final void writeDirect(byte[] b, int off, int len) throws IOException {
				for(int remaining = len, available;
						remaining > 0 && (available = ensureAvailable(remaining)) >= 0;
						remaining -= available) {
					System.arraycopy(b, off, buf, pos, available);
					off += available;
					pos += available;
				}
			}
			
			protected final ObjectStream objectStream() throws IOException {
				if(objectStream == null) {
					objectStream = new ObjectStream(this);
				}
				
				return objectStream;
			}
			
			@Override
			public void write(boolean value) throws IOException {
				writeType(SchemaFieldType.VALUE | SchemaFieldType.BOOLEAN);
				writeUntyped(value);
			}
			
			@Override
			public void write(byte value) throws IOException {
				writeType(SchemaFieldType.VALUE | SchemaFieldType.BYTE);
				writeUntyped(value);
			}
			
			@Override
			public void write(char value) throws IOException {
				writeType(SchemaFieldType.VALUE | SchemaFieldType.CHAR);
				writeUntyped(value);
			}
			
			@Override
			public void write(short value) throws IOException {
				writeType(SchemaFieldType.VALUE | SchemaFieldType.SHORT);
				writeUntyped(value);
			}
			
			@Override
			public void write(int value) throws IOException {
				writeType(SchemaFieldType.VALUE | SchemaFieldType.INT);
				writeUntyped(value);
			}
			
			@Override
			public void write(long value) throws IOException {
				writeType(SchemaFieldType.VALUE | SchemaFieldType.LONG);
				writeUntyped(value);
			}
			
			@Override
			public void write(float value) throws IOException {
				writeType(SchemaFieldType.VALUE | SchemaFieldType.FLOAT);
				writeUntyped(value);
			}
			
			@Override
			public void write(double value) throws IOException {
				writeType(SchemaFieldType.VALUE | SchemaFieldType.DOUBLE);
				writeUntyped(value);
			}
			
			@Override
			public void write(String value) throws IOException {
				writeType(SchemaFieldType.VALUE | SchemaFieldType.STRING);
				writeUntyped(value);
			}
			
			@Override
			public void write(Object value) throws IOException {
				writeType(SchemaFieldType.VALUE | SchemaFieldType.OBJECT);
				writeUntyped(value);
			}
			
			@Override
			public void write(boolean[] array) throws IOException {
				writeType(SchemaFieldType.ARRAY | SchemaFieldType.BOOLEAN);
				
				final int length = array.length;
				writeUntyped(length);
				
				for(int i = 0; i < length; ++i) {
					writeUntyped(array[i]);
				}
			}
			
			@Override
			public void write(byte[] array) throws IOException {
				writeType(SchemaFieldType.ARRAY | SchemaFieldType.BYTE);
				
				final int length = array.length;
				writeUntyped(length);
				
				// Use direct batch-copy instead of per-element loop
				for(int src = 0, written;
						src < length && (written = ensureAvailable(length - src)) >= 0;
						src += written) {
					System.arraycopy(array, src, buf, pos, written);
					pos += written;
				}
			}
			
			@Override
			public void write(char[] array) throws IOException {
				writeType(SchemaFieldType.ARRAY | SchemaFieldType.CHAR);
				
				final int length = array.length;
				writeUntyped(length);
				
				for(int i = 0; i < length; ++i) {
					writeUntyped(array[i]);
				}
			}
			
			@Override
			public void write(short[] array) throws IOException {
				writeType(SchemaFieldType.ARRAY | SchemaFieldType.SHORT);
				
				final int length = array.length;
				writeUntyped(length);
				
				for(int i = 0; i < length; ++i) {
					writeUntyped(array[i]);
				}
			}
			
			@Override
			public void write(int[] array) throws IOException {
				writeType(SchemaFieldType.ARRAY | SchemaFieldType.INT);
				
				final int length = array.length;
				writeUntyped(length);
				
				for(int i = 0; i < length; ++i) {
					writeUntyped(array[i]);
				}
			}
			
			@Override
			public void write(long[] array) throws IOException {
				writeType(SchemaFieldType.ARRAY | SchemaFieldType.LONG);
				
				final int length = array.length;
				writeUntyped(length);
				
				for(int i = 0; i < length; ++i) {
					writeUntyped(array[i]);
				}
			}
			
			@Override
			public void write(float[] array) throws IOException {
				writeType(SchemaFieldType.ARRAY | SchemaFieldType.FLOAT);
				
				final int length = array.length;
				writeUntyped(length);
				
				for(int i = 0; i < length; ++i) {
					writeUntyped(array[i]);
				}
			}
			
			@Override
			public void write(double[] array) throws IOException {
				writeType(SchemaFieldType.ARRAY | SchemaFieldType.DOUBLE);
				
				final int length = array.length;
				writeUntyped(length);
				
				for(int i = 0; i < length; ++i) {
					writeUntyped(array[i]);
				}
			}
			
			@Override
			public void write(String[] array) throws IOException {
				writeType(SchemaFieldType.ARRAY | SchemaFieldType.STRING);
				
				final int length = array.length;
				writeUntyped(length);
				
				for(int i = 0; i < length; ++i) {
					writeUntyped(array[i]);
				}
			}
			
			@Override
			public void write(Object[] array) throws IOException {
				writeType(SchemaFieldType.ARRAY | SchemaFieldType.OBJECT);
				
				final int length = array.length;
				writeUntyped(array.getClass().getComponentType().getName());
				writeUntyped(length);
				
				for(int i = 0; i < length; ++i) {
					writeUntyped(array[i]);
				}
			}
			
			@Override
			public void beginObjectArray(Class<?> clazz) throws IOException {
				if(arrays == null) {
					arrays = new ArrayDeque<>();
				}
				
				arrays.addLast(new ObjectArray(clazz));
			}
			
			@Override
			public void writeObjectArrayItem(Object item) throws IOException {
				arrays.getLast().add(item);
			}
			
			@Override
			public void endObjectArray() throws IOException {
				writeType(SchemaFieldType.ARRAY | SchemaFieldType.OBJECT);
				
				ObjectArray array = arrays.pollLast();
				List<Object> items = array.items();
				final String className = array.clazz().getName();
				final int length = items.size();
				writeUntyped(className);
				writeUntyped(length);
				
				for(Object item : items) {
					writeUntyped(item);
				}
			}
			
			@Override
			public void flush() throws IOException {
				flushBuffer();
			}
			
			protected static final class ObjectArray {
				
				private final Class<?> clazz;
				private final List<Object> items;
				
				public ObjectArray(Class<?> clazz) {
					this.clazz = Objects.requireNonNull(clazz);
					this.items = new ArrayList<>();
				}
				
				public void add(Object item) {
					items.add(item);
				}
				
				public Class<?> clazz() {
					return clazz;
				}
				
				public List<Object> items() {
					return items;
				}
			}
			
			protected static final class ObjectStream extends ObjectOutputStream {
				
				private final WriterBase writer;
				
				public ObjectStream(WriterBase writer) throws IOException {
					this.writer = writer;
				}
				
				@Override public void writeBoolean(boolean v) throws IOException { writer.write(v); }
				@Override public void writeByte(int v) throws IOException { writer.write(v); }
				@Override public void writeShort(int v) throws IOException { writer.write(v); }
				@Override public void writeChar(int v) throws IOException { writer.write(v); }
				@Override public void writeInt(int v) throws IOException { writer.write(v); }
				@Override public void writeLong(long v) throws IOException { writer.write(v); }
				@Override public void writeFloat(float v) throws IOException { writer.write(v); }
				@Override public void writeDouble(double v) throws IOException { writer.write(v); }
				@Override public void writeBytes(String s) throws IOException { writer.write(s); }
				@Override public void writeChars(String s) throws IOException { writer.write(s); }
				@Override public void writeUTF(String s) throws IOException { writer.write(s); }
				@Override public void write(int b) throws IOException { writer.writeDirect(b); }
				@Override public void write(byte[] b) throws IOException { writer.writeDirect(b, 0, b.length); }
				@Override public void write(byte[] b, int off, int len) throws IOException { writer.writeDirect(b, off, len); }
				@Override public void flush() throws IOException { writer.flush(); }
				@Override public void close() throws IOException { writer.close(); }
				
				@Override
				protected void writeObjectOverride(Object obj) throws IOException {
					writer.writeUntypedSerializable(obj);
				}
				
				@Override
				public void defaultWriteObject() throws IOException {
					// Object's non-static and non-transient fields are always written automatically
				}
			}
		}
	}
	
	private static final class SerializableCaller {
		
		private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
		private static final Cache HANDLES = new NoNullCache();
		
		@SuppressWarnings("unchecked")
		private static final Pair<String, Class<?>[]>[] METHODS_TO_FIND = (Pair<String, Class<?>[]>[]) new Pair[] {
			methodPair("writeObject", ObjectOutputStream.class),
			methodPair("readObject", ObjectInputStream.class),
			methodPair("writeReplace"),
			methodPair("readResolve"),
		};
		
		private SerializableCaller() {
		}
		
		private static final Pair<String, Class<?>[]> methodPair(String methodName, Class<?>... argTypes) {
			return new Pair<>(methodName, argTypes);
		}
		
		private static final Handles findHandles(Class<?> clazz) {
			Handles handles = new Handles();
			
			for(Pair<String, Class<?>[]> pair : METHODS_TO_FIND) {
				try {
					Method method = clazz.getDeclaredMethod(pair.a, pair.b);
					Reflection.setAccessible(method, true);
					MethodHandle handle = LOOKUP.unreflect(method);
					handles.set(pair.a, handle);
				} catch(NoSuchMethodException
							| IllegalAccessException
							| IllegalArgumentException
							| SecurityException
							| NoSuchFieldException ex) {
					// Not found or cannot be found, ignore it
				}
			}
			
			return handles;
		}
		
		private static final Handles findHandlesCached(Class<?> clazz) throws Exception {
			return HANDLES.getChecked(clazz, () -> findHandles(clazz));
		}
		
		private static final Deque<Class<?>> inheritanceStack(Class<?> clazz) {
			Deque<Class<?>> stack = new ArrayDeque<>();
			
			for(Class<?> cls = clazz; includeInSerialization(cls); cls = cls.getSuperclass()) {
				stack.addLast(cls);
			}
			
			return stack;
		}
		
		private static final boolean includeInSerialization(Class<?> clazz) {
			return clazz != null && clazz != Object.class && clazz != Enum.class;
		}
		
		public static final Handles getHandles(Object instance) throws IOException {
			if(instance == null) {
				return null;
			}
			
			try {
				final Class<?> clazz = instance.getClass();
				return HANDLES.getChecked(clazz, () -> findHandlesCached(clazz));
			} catch(Exception ex) {
				throw new IOException(ex);
			}
		}
		
		public static final void writeObject(Handles handles, Object instance, ObjectOutputStream stream) throws IOException {
			MethodHandle handle;
			if((handle = handles.writeObject()) == null) {
				return;
			}
			
			try {
				handle.invoke(instance, stream);
			} catch(Throwable ex) {
				throw new IOException(ex);
			}
		}
		
		public static final void readObject(Handles handles, Object instance, ObjectInputStream stream) throws IOException {
			MethodHandle handle;
			if((handle = handles.readObject()) == null) {
				return;
			}
			
			try {
				handle.invoke(instance, stream);
			} catch(Throwable ex) {
				throw new IOException(ex);
			}
		}
		
		public static final Object writeReplace(Handles handles, Object instance) throws IOException {
			MethodHandle handle;
			if((handle = handles.writeReplace()) == null) {
				return null;
			}
			
			try {
				return handle.invoke(instance);
			} catch(Throwable ex) {
				throw new IOException(ex);
			}
		}
		
		public static final Object readResolve(Handles handles, Object instance) throws IOException {
			MethodHandle handle;
			if((handle = handles.readResolve()) == null) {
				return null;
			}
			
			try {
				return handle.invoke(instance);
			} catch(Throwable ex) {
				throw new IOException(ex);
			}
		}
		
		public static final void writeObject(Object instance, ObjectOutputStream stream) throws IOException {
			if(instance == null) {
				return;
			}
			
			Deque<Class<?>> stack = inheritanceStack(instance.getClass());
			
			try {
				for(Class<?> cls; (cls = stack.pollLast()) != null;) {
					Handles handles = findHandlesCached(cls);
					writeObject(handles, instance, stream);
				}
			} catch(Exception ex) {
				throw new IOException(ex);
			}
		}
		
		public static final void readObject(Object instance, ObjectInputStream stream) throws IOException {
			if(instance == null) {
				return;
			}
			
			Deque<Class<?>> stack = inheritanceStack(instance.getClass());
			
			try {
				for(Class<?> cls; (cls = stack.pollLast()) != null;) {
					Handles handles = findHandlesCached(cls);
					readObject(handles, instance, stream);
				}
			} catch(Exception ex) {
				throw new IOException(ex);
			}
		}
		
		public static final class Handles {
			
			private MethodHandle writeObject;
			private MethodHandle readObject;
			private MethodHandle writeReplace;
			private MethodHandle readResolve;
			
			public void set(String methodName, MethodHandle handle) {
				switch(methodName) {
					case "writeObject": writeObject = handle; break;
					case "readObject": readObject = handle; break;
					case "writeReplace": writeReplace = handle; break;
					case "readResolve": readResolve = handle; break;
					default: throw new IllegalArgumentException("Unsupported method name");
				}
			}
			
			public MethodHandle writeObject() { return writeObject; }
			public MethodHandle readObject() { return readObject; }
			public MethodHandle writeReplace() { return writeReplace; }
			public MethodHandle readResolve() { return readResolve; }
		}
	}
}