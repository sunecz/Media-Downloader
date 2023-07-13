package sune.app.mediadown.serialization;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import sun.misc.Unsafe;
import sune.app.mediadown.resource.cache.Cache;
import sune.app.mediadown.resource.cache.NoNullCache;
import sune.app.mediadown.util.UnsafeInstance;

/** @since 00.02.09 */
public final class Schema {
	
	private static final Unsafe unsafe = UnsafeInstance.get();
	private static final Cache cache = new NoNullCache();
	
	private final Class<?> clazz;
	private final SchemaField[] fields;
	
	private Schema(Class<?> clazz, SchemaField[] fields) {
		this.clazz = Objects.requireNonNull(clazz);
		this.fields = Objects.requireNonNull(fields);
	}
	
	private static final int fieldType(Class<?> clazz) {
		int type = SchemaFieldType.VALUE;
		
		if(clazz.isArray()) {
			clazz = clazz.getComponentType();
			type = SchemaFieldType.ARRAY;
		}
		
		return type | SerializationUtils.typeOf(clazz);
	}
	
	private static final Schema create(Class<?> clazz) {
		if(clazz.isArray()) {
			throw new IllegalArgumentException("No schema for arrays");
		}
		
		Field[] fields = clazz.getDeclaredFields();
		List<SchemaField> schemaFields = new ArrayList<>(fields.length);
		
		for(int i = 0, l = fields.length; i < l; ++i) {
			Field field = fields[i];
			
			final int modifiers = field.getModifiers();
			if((modifiers & Modifier.STATIC) != 0
					|| (modifiers & Modifier.TRANSIENT) != 0) {
				continue;
			}
			
			int type = fieldType(field.getType());
			long offset = unsafe.objectFieldOffset(field);
			schemaFields.add(new SchemaField(type, offset));
		}
		
		return new Schema(clazz, schemaFields.toArray(SchemaField[]::new));
	}
	
	public static final Schema of(Class<?> clazz) {
		return cache.get(clazz, () -> Schema.create(clazz));
	}
	
	public SchemaField[] fields() {
		return fields;
	}
	
	public Class<?> clazz() {
		return clazz;
	}
}