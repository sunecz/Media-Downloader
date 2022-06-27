package sune.app.mediadown.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import sun.misc.Unsafe;

/**
 * Defines methods that are required but were removed from the Unsafe class
 * in Java 11.
 * @author Sune
 * @since 00.02.00
 * @see sun.misc.Unsafe*/
public final class UnsafeLegacy {
	
	/**
	 * The Unsafe instance.*/
	private static final Unsafe unsafe = UnsafeInstance.get();
	
	// Reflection polyfill for better setAccessible functionality
	private static final class Reflection {
		
		private static final void unsafe_setFieldValue(Object instance, Field field, boolean value) {
			unsafe.putBoolean(instance, unsafe.objectFieldOffset(field), value);
		}
		
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
	}
	
	private static Method METHOD_DEFINE_CLASS;
	private static Method getMethod_defineClass()
			throws IllegalArgumentException,
				   IllegalAccessException {
		if((METHOD_DEFINE_CLASS == null)) {
			try {
				METHOD_DEFINE_CLASS = ClassLoader.class.getDeclaredMethod("defineClass",
					String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
				Reflection.setAccessible(METHOD_DEFINE_CLASS, true);
			} catch(NoSuchMethodException
						| NoSuchFieldException
						| SecurityException ex) {
				throw new IllegalStateException("Unable to access defineClass method", ex);
			}
		}
		return METHOD_DEFINE_CLASS;
	}
	
	/**
	 * Converts an array of bytes into an instance of class {@code Class},
	 * with a given {@code ProtectionDomain}, defining this class in the given
	 * {@code ClassLoader}.
	 * @param name The expected <a href="#binary-name">binary name</a> of the class,
	 * or {@code null} if not known
	 * @param b The bytes that make up the class data. The bytes in positions
	 * {@code off} through {@code off+len-1} should have the format of a valid
	 * class file as defined by
	 * <cite>The Java&trade; Virtual Machine Specification</cite>.
	 * @param off The start offset in {@code b} of the class data
	 * @param len The length of the class data
	 * @param loader The class loader where to define the class
	 * @param protectionDomain The {@code ProtectionDomain} of the class
	 * @return The {@code Class} object created from the data,
	 * and {@code ProtectionDomain}.
	 * @see ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)
	 */
	public static final Class<?> defineClass(String name, byte[] b, int off, int len, ClassLoader loader,
		ProtectionDomain protectionDomain)
			throws IllegalAccessException,
				   IllegalArgumentException,
				   InvocationTargetException {
		return (Class<?>) getMethod_defineClass().invoke(loader, name, b, off, len, protectionDomain);
	}
	
	// Forbid anyone to create an instance of this class
	private UnsafeLegacy() {
	}
}