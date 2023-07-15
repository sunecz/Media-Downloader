package sune.app.mediadown.serialization;

import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.Map;

import sun.misc.Unsafe;
import sune.app.mediadown.util.UnsafeInstance;

/** @since 00.02.09 */
public class SchemaDataLoader {
	
	private static final SchemaDataLoader instance = new SchemaDataLoader();
	private static final Unsafe unsafe = UnsafeInstance.get();
	
	protected void staticInitializeClass(Class<?> clazz) {
		if(clazz.getSuperclass() != null) {
			staticInitializeClass(clazz.getSuperclass());
		}
		
		for(Class<?> interfaceClazz : clazz.getInterfaces()) {
			staticInitializeClass(interfaceClazz);
		}
		
		unsafe.ensureClassInitialized(clazz);
	}
	
	protected <T> T newInstance(Class<T> clazz) throws IOException {
		staticInitializeClass(clazz);
		
		try {
			@SuppressWarnings("unchecked")
			T instance = (T) unsafe.allocateInstance(clazz);
			return instance;
		} catch(InstantiationException ex) {
			throw new IOException(ex); // Rethrow
		}
	}
	
	protected void loadValue(SerializationReader reader, int type, long offset, Object instance)
			throws IOException {
		switch(type) {
			case SchemaFieldType.BOOLEAN: unsafe.putBoolean(instance, offset, reader.readBoolean()); break;
			case SchemaFieldType.BYTE: unsafe.putByte(instance, offset, reader.readByte()); break;
			case SchemaFieldType.CHAR: unsafe.putChar(instance, offset, reader.readChar()); break;
			case SchemaFieldType.SHORT: unsafe.putShort(instance, offset, reader.readShort()); break;
			case SchemaFieldType.INT: unsafe.putInt(instance, offset, reader.readInt()); break;
			case SchemaFieldType.LONG: unsafe.putLong(instance, offset, reader.readLong()); break;
			case SchemaFieldType.FLOAT: unsafe.putFloat(instance, offset, reader.readFloat()); break;
			case SchemaFieldType.DOUBLE: unsafe.putDouble(instance, offset, reader.readDouble()); break;
			case SchemaFieldType.STRING: unsafe.putObject(instance, offset, reader.readString()); break;
			default: unsafe.putObject(instance, offset, reader.readObject()); break;
		}
	}
	
	protected void loadArray(SerializationReader reader, int type, long offset, Object instance)
			throws IOException {
		switch(type) {
			case SchemaFieldType.BOOLEAN: unsafe.putObject(instance, offset, reader.readBooleanArray()); break;
			case SchemaFieldType.BYTE: unsafe.putObject(instance, offset, reader.readByteArray()); break;
			case SchemaFieldType.CHAR: unsafe.putObject(instance, offset, reader.readCharArray()); break;
			case SchemaFieldType.SHORT: unsafe.putObject(instance, offset, reader.readShortArray()); break;
			case SchemaFieldType.INT: unsafe.putObject(instance, offset, reader.readIntArray()); break;
			case SchemaFieldType.LONG: unsafe.putObject(instance, offset, reader.readLongArray()); break;
			case SchemaFieldType.FLOAT: unsafe.putObject(instance, offset, reader.readFloatArray()); break;
			case SchemaFieldType.DOUBLE: unsafe.putObject(instance, offset, reader.readDoubleArray()); break;
			case SchemaFieldType.STRING: unsafe.putObject(instance, offset, reader.readStringArray()); break;
			default: unsafe.putObject(instance, offset, reader.readObjectArray()); break;
		}
	}
	
	protected void load(SerializationReader reader, SchemaField field, Object instance) throws IOException {
		final int type = field.type();
		final long offset = field.offset();
		
		switch(type & SchemaFieldType.MASK_CATEGORY) {
			case SchemaFieldType.ARRAY: loadArray(reader, type & SchemaFieldType.MASK_TYPE, offset, instance); break;
			default: loadValue(reader, type & SchemaFieldType.MASK_TYPE, offset, instance); break;
		}
	}
	
	public static final SchemaDataLoader instance() {
		return instance;
	}
	
	public <T> T allocateNew(Class<T> clazz) throws IOException {
		return newInstance(clazz);
	}
	
	public void load(SerializationReader reader, Schema schema, Object instance) throws IOException {
		for(SchemaField field : schema.fields()) {
			reader.enterFieldContext(field);
			load(reader, field, instance);
			reader.leaveFieldContext();
		}
	}
	
	public <T> T loadAll(SerializationReader reader, T instance) throws IOException {
		if(instance == null) {
			return null; // Null object
		}
		
		Class<?> clazz = instance.getClass();
		
		for(Class<?> cls = clazz; cls != null && cls != Object.class; cls = cls.getSuperclass()) {
			Schema schema = Schema.of(cls);
			load(reader, schema, instance);
		}
		
		return instance;
	}
	
	public <T> T loadNew(SerializationReader reader, Class<T> clazz) throws IOException {
		return loadAll(reader, allocateNew(clazz));
	}
	
	public GetField fieldMap(Object instance) {
		return FieldMap.create(instance);
	}
	
	private static class FieldMap extends GetField {
		
		private final ObjectStreamClass desc;
		private final Map<String, Object> values;
		
		private FieldMap(ObjectStreamClass desc, Map<String, Object> values) {
			this.desc = desc;
			this.values = values;
		}
		
		private static final Object getValue(int type, long offset, Object instance) {
			switch(type) {
				case SchemaFieldType.BOOLEAN: return unsafe.getBoolean(instance, offset);
				case SchemaFieldType.BYTE: return unsafe.getByte(instance, offset);
				case SchemaFieldType.CHAR: return unsafe.getChar(instance, offset);
				case SchemaFieldType.SHORT: return unsafe.getShort(instance, offset);
				case SchemaFieldType.INT: return unsafe.getInt(instance, offset);
				case SchemaFieldType.LONG: return unsafe.getLong(instance, offset);
				case SchemaFieldType.FLOAT: return unsafe.getFloat(instance, offset);
				case SchemaFieldType.DOUBLE: return unsafe.getDouble(instance, offset);
				case SchemaFieldType.STRING: return (String) unsafe.getObject(instance, offset);
				default: return unsafe.getObject(instance, offset);
			}
		}
		
		private static final Object get(SchemaField field, Object instance) {
			final int type = field.type();
			final long offset = field.offset();
			
			switch(type & SchemaFieldType.MASK_CATEGORY) {
				case SchemaFieldType.ARRAY: return getArray(type & SchemaFieldType.MASK_TYPE, offset, instance);
				default: return getValue(type & SchemaFieldType.MASK_TYPE, offset, instance);
			}
		}
		
		private static final Object getArray(int type, long offset, Object instance) {
			return unsafe.getObject(instance, offset); // Same for all types of arrays
		}
		
		public static final GetField create(Object instance) {
			if(instance == null) {
				return FieldMap.OfNull.INSTANCE; // Null object
			}
			
			Map<String, Object> values = new HashMap<>();
			Class<?> clazz = instance.getClass();
			ObjectStreamClass desc = ObjectStreamClass.lookup(clazz);
			
			for(Class<?> cls = clazz; cls != null && cls != Object.class; cls = cls.getSuperclass()) {
				Schema schema = Schema.of(cls);
				
				for(SchemaField field : schema.fields()) {
					values.put(field.name(), get(field, instance));
				}
			}
			
			return new FieldMap(desc, values);
		}
		
		private final <T> T getValue(String name, T defaultValue) {
			@SuppressWarnings("unchecked")
			T casted = (T) values.getOrDefault(name, defaultValue);
			return casted;
		}
		
		@Override public ObjectStreamClass getObjectStreamClass() { return desc; }
		@Override public boolean defaulted(String name) throws IOException { return !values.containsKey(name); }
		@Override public boolean get(String name, boolean val) throws IOException { return getValue(name, val); }
		@Override public byte get(String name, byte val) throws IOException { return getValue(name, val); }
		@Override public char get(String name, char val) throws IOException { return getValue(name, val); }
		@Override public short get(String name, short val) throws IOException { return getValue(name, val); }
		@Override public int get(String name, int val) throws IOException { return getValue(name, val); }
		@Override public long get(String name, long val) throws IOException { return getValue(name, val); }
		@Override public float get(String name, float val) throws IOException { return getValue(name, val); }
		@Override public double get(String name, double val) throws IOException { return getValue(name, val); }
		@Override public Object get(String name, Object val) throws IOException { return getValue(name, val); }
		
		private static final class OfNull extends GetField {
			
			public static final OfNull INSTANCE = new OfNull();
			
			private OfNull() {
			}
			
			@Override public ObjectStreamClass getObjectStreamClass() { return null; }
			@Override public boolean defaulted(String name) throws IOException { return true; }
			@Override public boolean get(String name, boolean val) throws IOException { return false; }
			@Override public byte get(String name, byte val) throws IOException { return 0; }
			@Override public char get(String name, char val) throws IOException { return 0; }
			@Override public short get(String name, short val) throws IOException { return 0; }
			@Override public int get(String name, int val) throws IOException { return 0; }
			@Override public long get(String name, long val) throws IOException { return 0; }
			@Override public float get(String name, float val) throws IOException { return 0; }
			@Override public double get(String name, double val) throws IOException { return 0; }
			@Override public Object get(String name, Object val) throws IOException { return null; }
		}
	}
}