package sune.app.mediadown.serialization;

import java.io.IOException;

/** @since 00.02.09 */
// Package-private
final class SerializationUtils {
	
	// Forbid anyone to create an instance of this class
	private SerializationUtils() {
	}
	
	public static final int typeOf(Class<?> clazz) {
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
	
	public static final Class<?> classOf(String className) throws IOException {
		try {
			return Class.forName(className);
		} catch(ClassNotFoundException ex) {
			throw new IOException(ex); // Rethrow
		}
	}
}