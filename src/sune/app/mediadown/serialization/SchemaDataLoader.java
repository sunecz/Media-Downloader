package sune.app.mediadown.serialization;

import java.io.IOException;

import sun.misc.Unsafe;
import sune.app.mediadown.util.UnsafeInstance;

/** @since 00.02.09 */
public final class SchemaDataLoader {
	
	private static final Unsafe unsafe = UnsafeInstance.get();
	
	private final void staticInitializeClass(Class<?> clazz) {
		if(clazz.getSuperclass() != null) {
			staticInitializeClass(clazz.getSuperclass());
		}
		
		for(Class<?> interfaceClazz : clazz.getInterfaces()) {
			staticInitializeClass(interfaceClazz);
		}
		
		unsafe.ensureClassInitialized(clazz);
	}
	
	private final <T> T newInstance(Class<T> clazz) throws IOException {
		staticInitializeClass(clazz);
		
		try {
			@SuppressWarnings("unchecked")
			T instance = (T) unsafe.allocateInstance(clazz);
			return instance;
		} catch(InstantiationException ex) {
			throw new IOException(ex); // Rethrow
		}
	}
	
	private final void loadValue(SerializationReader reader, int type, long offset, Object instance)
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
	
	private final void loadArray(SerializationReader reader, int type, long offset, Object instance)
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
	
	private final void load(SerializationReader reader, SchemaField field, Object instance) throws IOException {
		final int type = field.type();
		final long offset = field.offset();
		
		switch(type & SchemaFieldType.MASK_CATEGORY) {
			case SchemaFieldType.ARRAY: loadArray(reader, type & SchemaFieldType.MASK_TYPE, offset, instance); break;
			default: loadValue(reader, type & SchemaFieldType.MASK_TYPE, offset, instance); break;
		}
	}
	
	public <T> T allocateNew(Class<T> clazz) throws IOException {
		return newInstance(clazz);
	}
	
	public void load(SerializationReader reader, Schema schema, Object instance) throws IOException {
		System.out.println("Load class: " + schema.clazz());
		for(SchemaField field : schema.fields()) {
			System.out.println("Load field: " + field.name());
			load(reader, field, instance);
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
}