package sune.app.mediadown.authentication;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import sune.app.mediadown.Shared;
import sune.app.mediadown.util.NIO;

/** @since 00.02.09 */
public class FileCredentialsStore implements CredentialsStore {
	
	private static final int VERSION = 1;
	private static final OpenOption[] OPEN_OPTIONS = {
		StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE
	};
	
	private FileChannel channel;
	private Table table;
	
	protected FileCredentialsStore() {
	}
	
	protected final void writeHeader() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
		writeInt(buf, VERSION);
		write(0L, buf.flip());
		channel.position(Integer.BYTES);
	}
	
	protected final void checkHeader() throws IOException {
		if(channel.size() == 0L) {
			writeHeader();
			return;
		}
		
		ByteBuffer buf = read(0L, Integer.BYTES);
		int version = readInt(buf);
		channel.position(Integer.BYTES);
		
		// Currently, this implementation of store does neither support any other version
		// than the current one, nor does it implement any compatibility procedures.
		if(version != VERSION) {
			throw new IllegalStateException("Unsupported version " + version + ", only " + VERSION + " is supported");
		}
	}
	
	protected final void open(Path path) throws IOException {
		Objects.requireNonNull(path);
		
		channel = FileChannel.open(path, OPEN_OPTIONS);
		checkHeader();
		
		long pos = channel.position();
		try {
			Table t = Table.from(channel);
			
			if(t == null) {
				throw new IllegalStateException("Unable to read the table");
			}
			
			table = t;
		} finally {
			channel.position(pos);
		}
	}
	
	protected final ByteBuffer read(long offset, long size) throws IOException {
		return NIO.read(channel, offset, size);
	}
	
	protected final void write(long offset, ByteBuffer buf) throws IOException {
		NIO.write(channel, offset, buf);
	}
	
	protected final void truncate(long offset, long size) throws IOException {
		NIO.truncate(channel, offset, size);
	}
	
	protected final void replace(long offset, long size, ByteBuffer buf) throws IOException {
		NIO.replace(channel, offset, size, buf);
	}
	
	protected final int readInt(ByteBuffer buf) {
		return buf.getInt();
	}
	
	// Note: The given buffer must contain the whole string, i.e. at least the length
	//       of the string and the string's contents.
	protected final String readString(ByteBuffer buf) {
		int length = buf.getInt();
		buf.limit(buf.position() + length);
		
		try {
			return Shared.CHARSET.decode(buf).toString();
		} finally {
			buf.limit(buf.capacity());
		}
	}
	
	protected final void writeInt(ByteBuffer buf, int value) {
		buf.putInt(value);
	}
	
	protected final void writeString(ByteBuffer buf, String string) {
		buf.putInt(string.length());
		buf.put(string.getBytes(Shared.CHARSET));
	}
	
	protected final ByteBuffer serialize(String path, Credentials credentials) {
		byte[] data = null;
		try {
			String type = credentials.getClass().getName();
			data = credentials.serialize();
			
			byte[] modified;
			if((modified = modifySerializationData(data)) != data) {
				CredentialsUtils.dispose(data);
			}
			
			data = modified;
			
			int size = path.length() + type.length() + data.length + 3 * Integer.BYTES;
			ByteBuffer buf = ByteBuffer.allocate(size);
			writeString(buf, path);
			writeString(buf, type);
			writeInt(buf, data.length);
			buf.put(data);
			return buf.flip();
		} finally {
			if(data != null) {
				CredentialsUtils.dispose(data);
				data = null;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected final Class<? extends Credentials> clazz(String type) throws IOException {
		try {
			return (Class<? extends Credentials>) Class.forName(type);
		} catch(ClassNotFoundException ex) {
			throw new IOException(ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected final <T extends Credentials> T newInstance(Class<? super T> clazz) throws IOException {
		try {
			return (T) clazz.getConstructor().newInstance();
		} catch(InstantiationException
					| IllegalAccessException
					| IllegalArgumentException
					| InvocationTargetException
					| NoSuchMethodException
					| SecurityException ex) {
			throw new IOException(ex);
		}
	}
	
	protected final Credentials deserialize(String type, ByteBuffer buf, int len) throws IOException {
		byte[] data = null;
		try {
			Credentials credentials = newInstance(clazz(type));
			data = new byte[len];
			buf.get(data);
			
			byte[] modified;
			if((modified = modifyDeserializationData(data)) != data) {
				CredentialsUtils.dispose(data);
			}
			
			data = modified;
			credentials.deserialize(data);
			return credentials;
		} finally {
			if(data != null) {
				CredentialsUtils.dispose(data);
				data = null;
			}
		}
	}
	
	protected byte[] modifySerializationData(byte[] data) {
		// By default do nothing
		return data;
	}
	
	protected byte[] modifyDeserializationData(byte[] data) {
		// By default do nothing
		return data;
	}
	
	@Override
	public Credentials get(String path) throws IOException {
		Objects.requireNonNull(path);
		
		Table.Row row;
		if((row = table.get(path)) == null) {
			return null;
		}
		
		ByteBuffer buf = null;
		try {
			buf = read(row.offset(), row.size());
			
			// Check that the path matches
			if(!path.equals(readString(buf))) {
				return null;
			}
			
			String type = readString(buf);
			int len = readInt(buf);
			return deserialize(type, buf, len);
		} finally {
			if(buf != null) {
				CredentialsUtils.dispose(buf);
				buf = null;
			}
		}
	}
	
	@Override
	public boolean has(String path) {
		Objects.requireNonNull(path);
		return table.has(path);
	}
	
	@Override
	public void set(String path, Credentials credentials) throws IOException {
		Objects.requireNonNull(path);
		Objects.requireNonNull(credentials);
		
		ByteBuffer buf = null;
		try {
			buf = serialize(path, credentials);
			int newSize = buf.remaining();
			
			Table.Row row;
			if((row = table.get(path)) != null) {
				table.set(path, newSize);
				replace(row.offset(), row.size(), buf);
			} else {
				row = table.set(path, newSize);
				write(row.offset(), buf);
			}
		} finally {
			if(buf != null) {
				CredentialsUtils.dispose(buf);
				buf = null;
			}
		}
	}
	
	@Override
	public void remove(String path) throws IOException {
		Objects.requireNonNull(path);
		
		Table.Row row;
		if((row = table.remove(path)) == null) {
			return;
		}
		
		truncate(row.offset(), row.size());
	}
	
	@Override
	public Set<String> paths() {
		return Collections.unmodifiableSet(table.paths());
	}
	
	@Override
	public void close() throws IOException {
		table.clear();
		channel.close();
	}
	
	protected static final class Table {
		
		private final TreeMap<Long, MapEntry> map = new TreeMap<>();
		private final Map<String, Long> mapOffsets = new HashMap<>();
		private final long baseOffset;
		
		private Table(long baseOffset) {
			this.baseOffset = Math.max(0L, baseOffset);
		}
		
		public static final Table from(FileChannel channel) throws IOException {
			return (new Reader(channel)).read();
		}
		
		private final Row add(String path, long size) {
			Entry<Long, MapEntry> lastEntry = map.lastEntry();
			long offset = baseOffset;
			
			if(lastEntry != null) {
				MapEntry last = lastEntry.getValue();
				offset = last.offset() + last.size();
			}
			
			map.put(offset, new MapEntry(path, offset, size));
			mapOffsets.put(path, offset);
			return new Row(offset, size);
		}
		
		private final void reoffset(Collection<MapEntry> items, int startIndex, int endIndex, long startOffset) {
			long offset = startOffset;
			Iterator<MapEntry> it = items.iterator();
			int size = endIndex - startIndex;
			
			while(it.hasNext() && startIndex-- > 0) {
				it.next();
				--endIndex;
			}
			
			// Also remember the MapEntry since there may be a conflict and the actual
			// old value would be removed.
			Map<Long, MapEntry> newOffsets = new LinkedHashMap<>(size);
			
			while(it.hasNext() && endIndex-- > 0) {
				MapEntry item = it.next();
				long oldOffset = item.offset();
				item.offset(offset);
				String path = item.path();
				mapOffsets.put(path, offset);
				newOffsets.put(oldOffset, item);
				offset += item.size();
			}
			
			for(Entry<Long, MapEntry> pair : newOffsets.entrySet()) {
				MapEntry entry = pair.getValue();
				map.remove(pair.getKey());
				map.put(entry.offset(), entry);
			}
		}
		
		public boolean has(String path) {
			return mapOffsets.containsKey(path);
		}
		
		public Row get(String path) {
			Long offset = mapOffsets.get(path);
			
			if(offset == null) {
				return null;
			}
			
			MapEntry entry = map.get(offset);
			return new Row(entry.offset(), entry.size());
		}
		
		public Row set(String path, long size) {
			Long offset = mapOffsets.get(path);
			
			if(offset == null) {
				return add(path, size);
			}
			
			NavigableMap<Long, MapEntry> tail = map.tailMap(offset, true);
			Entry<Long, MapEntry> firstEntry = tail.firstEntry();
			MapEntry first = firstEntry.getValue();
			first.size(size);
			reoffset(tail.values(), 1, tail.size(), first.offset() + size);
			return new Row(first.offset(), first.size());
		}
		
		public Row remove(String path) {
			Long offset = mapOffsets.remove(path);
			
			if(offset == null) {
				return null;
			}
			
			NavigableMap<Long, MapEntry> tail = map.tailMap(offset, false);
			MapEntry removed = map.remove(offset);
			
			if(!tail.isEmpty()) {
				reoffset(tail.values(), 0, tail.size(), offset);
			}
			
			return new Row(removed.offset(), removed.size());
		}
		
		public Set<String> paths() {
			return mapOffsets.keySet();
		}
		
		public void clear() {
			map.clear();
			mapOffsets.clear();
		}
		
		public static final class Row {
			
			private final long offset;
			private final long size;
			
			public Row(long offset, long size) {
				this.offset = offset;
				this.size = size;
			}
			
			public long offset() { return offset; }
			public long size() { return size; }
		}
		
		private static final class MapEntry {
			
			private final String path;
			private long offset;
			private long size;
			
			public MapEntry(String path, long offset, long size) {
				this.path = path;
				this.offset = offset;
				this.size = size;
			}
			
			public String path() { return path; }
			public void offset(long offset) { this.offset = offset; }
			public long offset() { return offset; }
			public void size(long size) { this.size = size; }
			public long size() { return size; }
		}
		
		private static final class Reader {
			
			private final FileChannel channel;
			
			private final ByteBuffer buf = ByteBuffer.allocate(8192).flip();
			private final CharBuffer chr = CharBuffer.allocate(4096);
			private final CharsetDecoder decoder = newStringDecoder();
			
			public Reader(FileChannel channel) {
				this.channel = Objects.requireNonNull(channel);
			}
			
			private final CharsetDecoder newStringDecoder() {
				return Shared.CHARSET.newDecoder()
					.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE);
			}
			
			/**
			 * @return The number of available (unread) bytes, or the count, whichever is the minimum,
			 * or {@code -1} if EOF and all bytes has already been read.
			 * @throws IOException If an I/O error occurs
			 */
			private final int ensureAvailable(int count) throws IOException {
				final int wanted = Math.min(count, buf.capacity());
				final int available = buf.remaining();
				
				if(available >= wanted) {
					return wanted; // Data available
				}
				
				buf.compact();
				final int filled = fillBuffer();
				
				if(filled < 0) {
					return filled; // Error
				}
				
				buf.flip();
				return Math.min(filled, wanted);
			}
			
			/**
			 * @return The number of available (unread) bytes, or {@code -1} if EOF and all bytes
			 * has already been read.
			 * @throws IOException If an I/O error occurs
			 */
			private final int fillBuffer() throws IOException {
				int pos = buf.position();
				
				if(buf.capacity() - pos <= 0) {
					return pos; // Already filled
				}
				
				int read = 0;
				while(buf.hasRemaining() && (read = channel.read(buf)) >= 0);
				
				if(pos == (pos = buf.position()) && read < 0) {
					return -1; // EOF
				}
				
				return pos;
			}
			
			private final int ensureAndCheck(int count) throws IOException {
				int n;
				if((n = ensureAvailable(count)) < 0) {
					throw new IllegalStateException("Not enough data");
				}
				
				return n;
			}
			
			private final boolean hasData() {
				return buf.hasRemaining();
			}
			
			private final int readInt() throws IOException {
				ensureAndCheck(Integer.BYTES);
				return buf.getInt();
			}
			
			private final String readString() throws IOException {
				int len = readInt();
				StringBuilder str = new StringBuilder();
				
				int num = 0, rem;
				for(rem = len; rem > 0 && (num = ensureAvailable(rem)) > 0; rem -= num) {
					int lim = buf.limit();
					buf.limit(buf.position() + num);
					
					CoderResult result;
					do {
						result = decoder.decode(buf, chr, rem - num > 0);
						
						if(result.isError()) {
							return null;
						}
						
						str.append(chr.flip());
						chr.clear();
					} while(result.isOverflow());
					
					buf.limit(lim);
				}
				
				return rem > 0 || num < 0 ? null : str.toString();
			}
			
			private final boolean skip(int count) throws IOException {
				int num = 0, rem;
				for(rem = count;
					rem > 0 && (num = ensureAvailable(rem)) > 0;
					rem -= num) {
					buf.position(buf.position() + num); // Skip
				}
				
				return rem == 0 && num >= 0;
			}
			
			private final int readIntAndSkip() throws IOException {
				int length = readInt();
				return skip(length) ? length : -1;
			}
			
			private final boolean readItem(Table table) throws IOException {
				String path = readString();
				
				if(path == null) {
					return false;
				}
				
				int lenType;
				if((lenType = readIntAndSkip()) < 0) { // Skip type
					return false;
				}
				
				int lenData;
				if((lenData = readIntAndSkip()) < 0) { // Skip data
					return false;
				}
				
				long size = path.length() + lenType + lenData + 3 * Integer.BYTES;
				table.set(path, size);
				
				return true;
			}
			
			private final boolean initBuffer() throws IOException {
				buf.compact();
				fillBuffer(); // Ignore EOF
				buf.flip();
				return true;
			}
			
			protected Table read() throws IOException {
				Table table = new Table(channel.position());
				
				if(!initBuffer()) {
					return null;
				}
				
				while(hasData() && readItem(table)); // Read all valid items
				return table;
			}
		}
	}
}