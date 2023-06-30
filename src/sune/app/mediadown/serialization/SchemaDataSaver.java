package sune.app.mediadown.serialization;

import java.io.IOException;

import sun.misc.Unsafe;
import sune.app.mediadown.util.UnsafeInstance;

/** @since 00.02.09 */
public final class SchemaDataSaver {
	
	private static final Unsafe unsafe = UnsafeInstance.get();
	
	private final void saveValue(SerializationWriter writer, int type, long offset, Object instance)
			throws IOException {
		switch(type) {
			case SchemaFieldType.BOOLEAN: writer.write(unsafe.getBoolean(instance, offset)); break;
			case SchemaFieldType.BYTE: writer.write(unsafe.getByte(instance, offset)); break;
			case SchemaFieldType.CHAR: writer.write(unsafe.getChar(instance, offset)); break;
			case SchemaFieldType.SHORT: writer.write(unsafe.getShort(instance, offset)); break;
			case SchemaFieldType.INT: writer.write(unsafe.getInt(instance, offset)); break;
			case SchemaFieldType.LONG: writer.write(unsafe.getLong(instance, offset)); break;
			case SchemaFieldType.FLOAT: writer.write(unsafe.getFloat(instance, offset)); break;
			case SchemaFieldType.DOUBLE: writer.write(unsafe.getDouble(instance, offset)); break;
			case SchemaFieldType.STRING: writer.write((String) unsafe.getObject(instance, offset)); break;
			default: writer.write(unsafe.getObject(instance, offset)); break;
		}
	}
	
	private final void saveArray(SerializationWriter writer, int type, long offset, Object instance)
			throws IOException {
		switch(type) {
			case SchemaFieldType.BOOLEAN: writer.write((boolean[]) unsafe.getObject(instance, offset)); break;
			case SchemaFieldType.BYTE: writer.write((byte[]) unsafe.getObject(instance, offset)); break;
			case SchemaFieldType.CHAR: writer.write((char[]) unsafe.getObject(instance, offset)); break;
			case SchemaFieldType.SHORT: writer.write((short[]) unsafe.getObject(instance, offset)); break;
			case SchemaFieldType.INT: writer.write((int[]) unsafe.getObject(instance, offset)); break;
			case SchemaFieldType.LONG: writer.write((long[]) unsafe.getObject(instance, offset)); break;
			case SchemaFieldType.FLOAT: writer.write((float[]) unsafe.getObject(instance, offset)); break;
			case SchemaFieldType.DOUBLE: writer.write((double[]) unsafe.getObject(instance, offset)); break;
			case SchemaFieldType.STRING: writer.write((String[]) unsafe.getObject(instance, offset)); break;
			default: writer.write((Object[]) unsafe.getObject(instance, offset)); break;
		}
	}
	
	private final void save(SerializationWriter writer, SchemaField field, Object instance) throws IOException {
		final int type = field.type();
		final long offset = field.offset();
		
		switch(type & SchemaFieldType.MASK_CATEGORY) {
			case SchemaFieldType.ARRAY: saveArray(writer, type & SchemaFieldType.MASK_TYPE, offset, instance); break;
			default: saveValue(writer, type & SchemaFieldType.MASK_TYPE, offset, instance); break;
		}
	}
	
	public void save(SerializationWriter writer, Schema schema, Object instance) throws IOException {
		System.out.println("Save class: " + schema.clazz());
		for(SchemaField field : schema.fields()) {
			System.out.println("Save field: " + field.name());
			save(writer, field, instance);
		}
	}
	
	// TODO: Move elsewhere
	private static final int typeOf(Class<?> clazz) {
		if(clazz == boolean.class) return SchemaFieldType.BOOLEAN;
		if(clazz == byte.class) return SchemaFieldType.BYTE;
		if(clazz == char.class) return SchemaFieldType.CHAR;
		if(clazz == short.class) return SchemaFieldType.SHORT;
		if(clazz == int.class) return SchemaFieldType.INT;
		if(clazz == long.class) return SchemaFieldType.LONG;
		if(clazz == float.class) return SchemaFieldType.FLOAT;
		if(clazz == double.class) return SchemaFieldType.DOUBLE;
		if(clazz == String.class) return SchemaFieldType.STRING;
		return SchemaFieldType.OBJECT;
	}
	
	public void saveNew(SerializationWriter writer, Object object) throws IOException {
		if(object == null) {
			return; // Null object
		}
		
		Class<?> objectClass = object.getClass();
		
		if(objectClass.isArray()) {
			final int type = typeOf(objectClass.getComponentType());
			
			switch(type) {
				case SchemaFieldType.BOOLEAN: writer.write((boolean[]) object); break;
				case SchemaFieldType.BYTE: writer.write((byte[]) object); break;
				case SchemaFieldType.CHAR: writer.write((char[]) object); break;
				case SchemaFieldType.SHORT: writer.write((short[]) object); break;
				case SchemaFieldType.INT: writer.write((int[]) object); break;
				case SchemaFieldType.LONG: writer.write((long[]) object); break;
				case SchemaFieldType.FLOAT: writer.write((float[]) object); break;
				case SchemaFieldType.DOUBLE: writer.write((double[]) object); break;
				case SchemaFieldType.STRING: writer.write((String[]) object); break;
				default: writer.write((Object[]) object); break;
			}
			
			return;
		}
		
		for(Class<?> cls = objectClass; cls != null && cls != Object.class; cls = cls.getSuperclass()) {
			Schema schema = Schema.of(cls);
			save(writer, schema, object);
		}
	}
}