package sune.app.mediadown.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import sun.misc.Unsafe;

/**
 * Provides useful methods for accessing objects, modifying them, and obtaining their values
 * even if the objects are inaccessible (i.e. private, protected). Inaccessible objects
 * are also those objects to which the access is restricted, e.g. objects that are in modules
 * that are not exported and/or opened for the caller's module.
 * @version 1.0
 * @author Sune*/
public final class Reflection3 {
	
	// forbid anyone to create an instance of this class
	private Reflection3() {
		throw new IllegalStateException();
	}
	
	// ---------- Unsafe ----------
	
	private static final Unsafe unsafe = UnsafeInstance.get();
	
	private static final long unsafe_objectFieldOffset(Field field) {
		return unsafe.objectFieldOffset(field);
	}
	
	private static final Object unsafe_staticFieldBase(Field field) {
		return unsafe.staticFieldBase(field);
	}
	
	private static final long unsafe_staticFieldOffset(Field field) {
		return unsafe.staticFieldOffset(field);
	}
	
	private static final void unsafe_setFieldValue(Object instance, Field field, boolean value) {
		if((instance == null)) {
			unsafe.putBoolean(unsafe_staticFieldBase(field), unsafe_staticFieldOffset(field), value);
		} else {
			unsafe.putBoolean(instance, unsafe_objectFieldOffset(field), value);
		}
	}
	
	private static final void unsafe_setFieldValue(Object instance, Field field, byte value) {
		if((instance == null)) {
			unsafe.putByte(unsafe_staticFieldBase(field), unsafe_staticFieldOffset(field), value);
		} else {
			unsafe.putByte(instance, unsafe_objectFieldOffset(field), value);
		}
	}
	
	private static final void unsafe_setFieldValue(Object instance, Field field, char value) {
		if((instance == null)) {
			unsafe.putChar(unsafe_staticFieldBase(field), unsafe_staticFieldOffset(field), value);
		} else {
			unsafe.putChar(instance, unsafe_objectFieldOffset(field), value);
		}
	}
	
	private static final void unsafe_setFieldValue(Object instance, Field field, short value) {
		if((instance == null)) {
			unsafe.putShort(unsafe_staticFieldBase(field), unsafe_staticFieldOffset(field), value);
		} else {
			unsafe.putShort(instance, unsafe_objectFieldOffset(field), value);
		}
	}
	
	private static final void unsafe_setFieldValue(Object instance, Field field, int value) {
		if((instance == null)) {
			unsafe.putInt(unsafe_staticFieldBase(field), unsafe_staticFieldOffset(field), value);
		} else {
			unsafe.putInt(instance, unsafe_objectFieldOffset(field), value);
		}
	}
	
	private static final void unsafe_setFieldValue(Object instance, Field field, long value) {
		if((instance == null)) {
			unsafe.putLong(unsafe_staticFieldBase(field), unsafe_staticFieldOffset(field), value);
		} else {
			unsafe.putLong(instance, unsafe_objectFieldOffset(field), value);
		}
	}
	
	private static final void unsafe_setFieldValue(Object instance, Field field, float value) {
		if((instance == null)) {
			unsafe.putFloat(unsafe_staticFieldBase(field), unsafe_staticFieldOffset(field), value);
		} else {
			unsafe.putFloat(instance, unsafe_objectFieldOffset(field), value);
		}
	}
	
	private static final void unsafe_setFieldValue(Object instance, Field field, double value) {
		if((instance == null)) {
			unsafe.putDouble(unsafe_staticFieldBase(field), unsafe_staticFieldOffset(field), value);
		} else {
			unsafe.putDouble(instance, unsafe_objectFieldOffset(field), value);
		}
	}
	
	private static final void unsafe_setFieldValue(Object instance, Field field, Object value) {
		if((instance == null)) {
			unsafe.putObject(unsafe_staticFieldBase(field), unsafe_staticFieldOffset(field), value);
		} else {
			unsafe.putObject(instance, unsafe_objectFieldOffset(field), value);
		}
	}
	
	// ---------- Accessibility ----------
	
	private static Field FIELD_SET_ACCESSIBLE;
	private static Field getField_setAccessible()
			throws NoSuchFieldException,
				   SecurityException {
		if((FIELD_SET_ACCESSIBLE == null)) {
			FIELD_SET_ACCESSIBLE = AccessibleObject.class.getDeclaredField("override");
			unsafe_setFieldValue(FIELD_SET_ACCESSIBLE, FIELD_SET_ACCESSIBLE, true);
		}
		return FIELD_SET_ACCESSIBLE;
	}
	
	/**
	 * Sets the {@code accessible} flag for the given object to the given boolean value.
	 * A value of {@code true} indicates that the reflected object should suppress Java
	 * language access checking when it is used. A value of {@code false} indicates that
	 * the reflected object should enforce Java language access checks.
	 * <br><br>
	 * Unlike the orignal {@linkplain AccessibleObject#setAccessible(boolean) setAccessible}
	 * method, this method does not fail when a module is not exported and/or opened. After
	 * calling of this method, the given object should be accessible and ready for further
	 * actions requiring accessibility.
	 * @param object the object where to set the {@code accessible} flag
	 * @param flag the new value for the {@code accessible} flag*/
	public static final void setAccessible(AccessibleObject object, boolean flag)
			throws NoSuchFieldException,
			       SecurityException,
			       IllegalArgumentException,
				   IllegalAccessException {
		getField_setAccessible().setBoolean(object, true);
	}
	
	// ---------- Object instantiation ----------
	
	protected static final Class<?>[] objectsTypes(Object... objects) {
		int        length = objects.length;
		Class<?>[] array  = new Class<?>[length];
		for(int i = 0; i < length; ++i)
			array[i] = toPrimitiveClass(objects[i] != null ? objects[i].getClass() : Object.class);
		return array;
	}
	
	protected static final Class<?> toPrimitiveClass(Class<?> clazz) {
		if((clazz == Boolean.class))   return boolean.class;
		if((clazz == Byte.class))      return byte.class;
		if((clazz == Character.class)) return char.class;
		if((clazz == Short.class))     return short.class;
		if((clazz == Integer.class))   return int.class;
		if((clazz == Long.class))      return long.class;
		if((clazz == Float.class))     return float.class;
		if((clazz == Double.class))    return double.class;
		if((clazz == Void.class))      return void.class;
		return clazz;
	}
	
	public static final <T> T newInstance(Class<T> clazz, Object... args)
			throws NoSuchMethodException,
			   SecurityException,
			   NoSuchFieldException,
			   IllegalArgumentException,
			   IllegalAccessException,
			   InstantiationException,
			   InvocationTargetException {
		return newInstance(clazz, objectsTypes(args), args);
	}
	
	public static final <T> T newInstance(Class<T> clazz, Class<?>[] argsTypes, Object... args)
			throws NoSuchMethodException,
				   SecurityException,
				   NoSuchFieldException,
				   IllegalArgumentException,
				   IllegalAccessException,
				   InstantiationException,
				   InvocationTargetException {
		Constructor<T> ctor = clazz.getDeclaredConstructor(argsTypes);
		return newInstance(ctor, args);
	}
	
	public static final <T> T newInstance(Constructor<T> ctor, Object... args)
			throws NoSuchMethodException,
				   SecurityException,
				   NoSuchFieldException,
				   IllegalArgumentException,
				   IllegalAccessException,
				   InstantiationException,
				   InvocationTargetException {
		setAccessible(ctor, true);
		return ctor.newInstance(args);
	}
	
	public static final <T> T allocateInstance(Class<T> clazz)
			throws InstantiationException {
		@SuppressWarnings("unchecked")
		T instance = (T) unsafe.allocateInstance(clazz);
		return instance;
	}
	
	// ---------- Fields manipulation ----------
	
	public static final void setFieldValue(Object instance, Class<?> clazz, String fieldName, boolean value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		setFieldValue(instance, field, value);
	}
	
	public static final void setFieldValue(Object instance, Class<?> clazz, String fieldName, byte value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		setFieldValue(instance, field, value);
	}
	
	public static final void setFieldValue(Object instance, Class<?> clazz, String fieldName, char value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		setFieldValue(instance, field, value);
	}
	
	public static final void setFieldValue(Object instance, Class<?> clazz, String fieldName, short value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		setFieldValue(instance, field, value);
	}
	
	public static final void setFieldValue(Object instance, Class<?> clazz, String fieldName, int value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		setFieldValue(instance, field, value);
	}
	
	public static final void setFieldValue(Object instance, Class<?> clazz, String fieldName, long value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		setFieldValue(instance, field, value);
	}
	
	public static final void setFieldValue(Object instance, Class<?> clazz, String fieldName, float value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		setFieldValue(instance, field, value);
	}
	
	public static final void setFieldValue(Object instance, Class<?> clazz, String fieldName, double value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		setFieldValue(instance, field, value);
	}
	
	public static final void setFieldValue(Object instance, Class<?> clazz, String fieldName, Object value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		setFieldValue(instance, field, value);
	}
	
	public static final void setFieldValue(Object instance, Field field, boolean value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		if((Modifier.isFinal(field.getModifiers()))) {
			unsafe_setFieldValue(instance, field, value);
		} else {
			field.setBoolean(instance, value);
		}
	}
	
	public static final void setFieldValue(Object instance, Field field, byte value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		if((Modifier.isFinal(field.getModifiers()))) {
			unsafe_setFieldValue(instance, field, value);
		} else {
			field.setByte(instance, value);
		}
	}
	
	public static final void setFieldValue(Object instance, Field field, char value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		if((Modifier.isFinal(field.getModifiers()))) {
			unsafe_setFieldValue(instance, field, value);
		} else {
			field.setChar(instance, value);
		}
	}
	
	public static final void setFieldValue(Object instance, Field field, short value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		if((Modifier.isFinal(field.getModifiers()))) {
			unsafe_setFieldValue(instance, field, value);
		} else {
			field.setShort(instance, value);
		}
	}
	
	public static final void setFieldValue(Object instance, Field field, int value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		if((Modifier.isFinal(field.getModifiers()))) {
			unsafe_setFieldValue(instance, field, value);
		} else {
			field.setInt(instance, value);
		}
	}
	
	public static final void setFieldValue(Object instance, Field field, long value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		if((Modifier.isFinal(field.getModifiers()))) {
			unsafe_setFieldValue(instance, field, value);
		} else {
			field.setLong(instance, value);
		}
	}
	
	public static final void setFieldValue(Object instance, Field field, float value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		if((Modifier.isFinal(field.getModifiers()))) {
			unsafe_setFieldValue(instance, field, value);
		} else {
			field.setFloat(instance, value);
		}
	}
	
	public static final void setFieldValue(Object instance, Field field, double value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		if((Modifier.isFinal(field.getModifiers()))) {
			unsafe_setFieldValue(instance, field, value);
		} else {
			field.setDouble(instance, value);
		}
	}
	
	public static final void setFieldValue(Object instance, Field field, Object value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		if((Modifier.isFinal(field.getModifiers()))) {
			unsafe_setFieldValue(instance, field, value);
		} else {
			field.set(instance, value);
		}
	}
	
	public static final boolean getFieldValueBoolean(Object instance, Class<?> clazz, String fieldName)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		return getFieldValueBoolean(instance, field);
	}
	
	public static final byte getFieldValueByte(Object instance, Class<?> clazz, String fieldName)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		return getFieldValueByte(instance, field);
	}
	
	public static final char getFieldValueChar(Object instance, Class<?> clazz, String fieldName)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		return getFieldValueChar(instance, field);
	}
	
	public static final short getFieldValueShort(Object instance, Class<?> clazz, String fieldName)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		return getFieldValueShort(instance, field);
	}
	
	public static final int getFieldValueInt(Object instance, Class<?> clazz, String fieldName)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		return getFieldValueInt(instance, field);
	}
	
	public static final long getFieldValueLong(Object instance, Class<?> clazz, String fieldName)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		return getFieldValueLong(instance, field);
	}
	
	public static final float getFieldValueFloat(Object instance, Class<?> clazz, String fieldName)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		return getFieldValueFloat(instance, field);
	}
	
	public static final double getFieldValueDouble(Object instance, Class<?> clazz, String fieldName)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		return getFieldValueDouble(instance, field);
	}
	
	public static final Object getFieldValue(Object instance, Class<?> clazz, String fieldName)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		return getFieldValue(instance, field);
	}
	
	public static final boolean getFieldValueBoolean(Object instance, Field field)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		return field.getBoolean(instance);
	}
	
	public static final byte getFieldValueByte(Object instance, Field field)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		return field.getByte(instance);
	}
	
	public static final char getFieldValueChar(Object instance, Field field)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		return field.getChar(instance);
	}
	
	public static final short getFieldValueShort(Object instance, Field field)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		return field.getShort(instance);
	}
	
	public static final int getFieldValueInt(Object instance, Field field)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		return field.getInt(instance);
	}
	
	public static final long getFieldValueLong(Object instance, Field field)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		return field.getLong(instance);
	}
	
	public static final float getFieldValueFloat(Object instance, Field field)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		return field.getFloat(instance);
	}
	
	public static final double getFieldValueDouble(Object instance, Field field)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		return field.getDouble(instance);
	}
	
	public static final Object getFieldValue(Object instance, Field field)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		setAccessible(field, true);
		return field.get(instance);
	}
	
	// ---------- Methods invocation ----------
	
	public static final Object invoke(Object instance, Class<?> clazz, String methodName, Object... args)
			throws NoSuchMethodException,
				   SecurityException,
				   NoSuchFieldException,
				   IllegalArgumentException,
				   IllegalAccessException,
				   InvocationTargetException {
		return invoke(instance, clazz, methodName, objectsTypes(args), args);
	}
	
	public static final Object invoke(Object instance, Class<?> clazz, String methodName, Class<?>[] argsTypes, Object... args)
			throws NoSuchMethodException,
				   SecurityException,
				   NoSuchFieldException,
				   IllegalArgumentException,
				   IllegalAccessException,
				   InvocationTargetException {
		Method method = clazz.getDeclaredMethod(methodName, argsTypes);
		return invoke(instance, method, args);
	}
	
	public static final Object invoke(Object instance, Method method, Object... args)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException,
				   InvocationTargetException {
		setAccessible(method, true);
		return method.invoke(instance, args);
	}
	
	public static final Object invokeStatic(Class<?> clazz, String methodName, Object... args)
			throws NoSuchMethodException,
				   SecurityException,
				   NoSuchFieldException,
				   IllegalArgumentException,
				   IllegalAccessException,
				   InvocationTargetException {
		return invoke(null, clazz, methodName, args);
	}
	
	public static final Object invokeStatic(Class<?> clazz, String methodName, Class<?>[] argsTypes, Object... args)
			throws NoSuchMethodException,
				   SecurityException,
				   NoSuchFieldException,
				   IllegalArgumentException,
				   IllegalAccessException,
				   InvocationTargetException {
		return invoke(null, clazz, methodName, argsTypes, args);
	}
	
	public static final Object invokeStatic(Method method, Object... args)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException,
				   InvocationTargetException {
		return invoke(null, method, args);
	}
}