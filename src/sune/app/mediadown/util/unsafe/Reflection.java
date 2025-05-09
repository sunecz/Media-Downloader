package sune.app.mediadown.util.unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/** @since 00.02.09 */
public final class Reflection {
	
	private Reflection() {
	}
	
	public static final void setAccessible(AccessibleObject object, boolean flag) {
		Objects.requireNonNull(object);
		SpecialHolders.vh_override.set(object, flag);
	}
	
	public static final boolean getAccessible(AccessibleObject object) {
		Objects.requireNonNull(object);
		return (boolean) SpecialHolders.vh_override.get(object);
	}
	
	public static final Class<?> getClass(String name) {
		Objects.requireNonNull(name);
		
		try {
			return Class.forName(name);
		} catch(ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static final Field getField(Class<?> clazz, String name) {
		Objects.requireNonNull(clazz);
		Objects.requireNonNull(name);
		
		try {
			Field[] fields = (Field[]) FieldHolders.mh_getDeclaredFields0.invoke(clazz, false);
			Field field = (Field) FieldHolders.mh_searchFields.invoke(fields, name);
			
			if(field == null) {
				throw new NoSuchFieldException(name);
			}
			
			return field;
		} catch(Throwable th) {
			throw new RuntimeException(th);
		}
	}
	
	public static final <T> T getValue(Field field, Object instance) {
		Objects.requireNonNull(field);
		
		boolean accessible = getAccessible(field);
		if(!accessible) {
			setAccessible(field, true);
		}
		
		try {
			@SuppressWarnings("unchecked")
			T value = (T) field.get(instance);
			return value;
		} catch(IllegalArgumentException
					| IllegalAccessException ex) {
			throw new RuntimeException(ex);
		} finally {
			if(!accessible) {
				setAccessible(field, false);
			}
		}
	}
	
	public static final void setField(Field field, Object instance, Object value) {
		Objects.requireNonNull(field);
		
		boolean accessible = getAccessible(field);
		if(!accessible) {
			setAccessible(field, true);
		}
		
		try {
			field.set(instance, value);
		} catch(IllegalArgumentException
					| IllegalAccessException ex) {
			throw new RuntimeException(ex);
		} finally {
			if(!accessible) {
				setAccessible(field, false);
			}
		}
	}
	
	public static final Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
		Objects.requireNonNull(clazz);
		Objects.requireNonNull(name);
		
		try {
			Method[] methods = (Method[]) MethodHolders.mh_getDeclaredMethods0.invoke(clazz, false);
			Method method = (Method) MethodHolders.mh_searchMethods.invoke(methods, name, parameterTypes);
			
			if(method == null) {
				throw new NoSuchMethodException(
					name + "(" + Arrays.toString(parameterTypes) + ")"
				);
			}
			
			return method;
		} catch(Throwable th) {
			throw new RuntimeException(th);
		}
	}
	
	public static final <T> T invokeMethod(Method method, Object instance, Object... arguments) {
		Objects.requireNonNull(method);
		
		boolean accessible = getAccessible(method);
		if(!accessible) {
			setAccessible(method, true);
		}
		
		try {
			@SuppressWarnings("unchecked")
			T value = (T) method.invoke(instance, arguments);
			return value;
		} catch(IllegalArgumentException
					| InvocationTargetException
					| IllegalAccessException ex) {
			throw new RuntimeException(ex);
		} finally {
			if(!accessible) {
				setAccessible(method, false);
			}
		}
	}
	
	public static final Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) {
		Objects.requireNonNull(clazz);
		
		try {
			return clazz.getDeclaredConstructor(parameterTypes);
		} catch(Throwable th) {
			throw new RuntimeException(th);
		}
	}
	
	public static final <T> T newInstance(Constructor<?> constructor, Object... arguments) {
		Objects.requireNonNull(constructor);
		
		boolean accessible = getAccessible(constructor);
		if(!accessible) {
			setAccessible(constructor, true);
		}
		
		try {
			@SuppressWarnings("unchecked")
			T instance = (T) constructor.newInstance(arguments);
			return instance;
		} catch(IllegalArgumentException
					| InvocationTargetException
					| IllegalAccessException
					| InstantiationException ex) {
			throw new RuntimeException(ex);
		} finally {
			if(!accessible) {
				setAccessible(constructor, false);
			}
		}
	}
	
	private static final class SpecialHolders {
		
		private static final VarHandle vh_override;
		
		static {
			MethodHandles.Lookup lookup = TrustedLookup.get();
			
			try {
				vh_override = lookup.findVarHandle(
					AccessibleObject.class, "override",
					boolean.class
				);
			} catch(Throwable th) {
				throw new ExceptionInInitializerError(th);
			}
		}
		
		private SpecialHolders() {
		}
	}
	
	private static final class FieldHolders {
		
		private static final MethodHandle mh_getDeclaredFields0;
		private static final MethodHandle mh_searchFields;
		
		static {
			MethodHandles.Lookup lookup = TrustedLookup.get();
			
			try {
				mh_getDeclaredFields0 = lookup.findVirtual(
					Class.class, "getDeclaredFields0",
					MethodType.methodType(Field[].class, boolean.class)
				);
				
				mh_searchFields = lookup.findStatic(
					Class.class, "searchFields",
					MethodType.methodType(Field.class, Field[].class, String.class)
				);
			} catch(Throwable th) {
				throw new ExceptionInInitializerError(th);
			}
		}
		
		private FieldHolders() {
		}
	}
	
	private static final class MethodHolders {
		
		private static final MethodHandle mh_getDeclaredMethods0;
		private static final MethodHandle mh_searchMethods;
		
		static {
			MethodHandles.Lookup lookup = TrustedLookup.get();
			
			try {
				mh_getDeclaredMethods0 = lookup.findVirtual(
					Class.class, "getDeclaredMethods0",
					MethodType.methodType(Method[].class, boolean.class)
				);
				
				mh_searchMethods = lookup.findStatic(
					Class.class, "searchMethods",
					MethodType.methodType(Method.class, Method[].class, String.class, Class[].class)
				);
			} catch(Throwable th) {
				throw new ExceptionInInitializerError(th);
			}
		}
		
		private MethodHolders() {
		}
	}
}
