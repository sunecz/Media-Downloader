package sune.app.mediadown.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public final class Reflection2 {
	
	public static final class InstanceCreationException extends ReflectiveOperationException {
		
		private static final long serialVersionUID = -7389440697331797625L;
		
		public InstanceCreationException() {
			super();
		}
		
		public InstanceCreationException(String message, Throwable cause) {
			super(message, cause);
		}
		
		public InstanceCreationException(String message) {
			super(message);
		}
		
		public InstanceCreationException(Throwable cause) {
			super(cause);
		}
	}
	
	static final Class<?>[] recognizeClasses(Object... arguments) {
		int length 		   = arguments.length;
		Class<?>[] classes = new Class<?>[length];
		for(int i = 0; i < length; ++i) {
			Class<?> clazz = arguments[i].getClass();
			classes[i] 	   = toPrimitive(clazz);
		}
		return classes;
	}
	
	static final Class<?> toPrimitive(Class<?> clazz) {
		if(clazz == Boolean.class) 	 return boolean.class;
	    if(clazz == Byte.class) 	 return byte.class;
	    if(clazz == Character.class) return char.class;
	    if(clazz == Short.class) 	 return short.class;
	    if(clazz == Integer.class) 	 return int.class;
	    if(clazz == Long.class) 	 return long.class;
	    if(clazz == Float.class) 	 return float.class;
	    if(clazz == Double.class) 	 return double.class;
	    if(clazz == Void.class) 	 return void.class;
		return clazz;
	}
	
	public static final <T> T newInstance(Class<? extends T> clazz, Object... args)
			throws InstanceCreationException {
		return newInstance(clazz, recognizeClasses(args), args);
	}
	
	public static final <T> T newInstance(Class<? extends T> clazz, Class<?>[] argsTypes, Object... args)
			throws InstanceCreationException {
		try {
			Constructor<? extends T> ctor = clazz.getDeclaredConstructor(argsTypes);
			Reflection.setAccessible(ctor, true);
			return ctor.newInstance(args);
		} catch(NoSuchFieldException      |
				SecurityException         |
				IllegalArgumentException  |
				IllegalAccessException    |
				NoSuchMethodException     |
				InvocationTargetException |
				InstantiationException ex) {
			throw new InstanceCreationException(ex);
		}
	}
	
	public static final void setField(Class<?> clazz, Object instance, String fieldName, Object value) {
		try {
			Field field = clazz.getDeclaredField(fieldName);
			Reflection.setAccessible(field, true);
			int modifiers = field.getModifiers();
			boolean isFinal = Modifier.isFinal(modifiers);
			// remove the final modifier
			if((isFinal)) {
				setModifiers(field, modifiers & (~Modifier.FINAL));
			}
			field.set(instance, value);
			// set the previous modifiers back
			if((isFinal)) {
				setModifiers(field, modifiers);
			}
		} catch(NoSuchFieldException     |
				SecurityException        |
				IllegalArgumentException |
				IllegalAccessException ex) {
		}
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> T getField(Class<?> clazz, Object instance, String fieldName) {
		try {
			Field field = clazz.getDeclaredField(fieldName);
			Reflection.setAccessible(field, true);
			return (T) field.get(instance);
		} catch(NoSuchFieldException     |
				SecurityException        |
				IllegalArgumentException |
				IllegalAccessException ex) {
		}
		return null;
	}
	
	public static final Class<?> getClass(String name) {
		try {
			return Class.forName(name);
		} catch(ClassNotFoundException ex) {
		}
		return null;
	}
	
	private static final void setModifiers(Field field, int modifiers) {
		setField(Field.class, field, "modifiers", modifiers);
	}
}