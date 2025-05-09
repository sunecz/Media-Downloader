package sune.app.mediadown.util.unsafe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

/**
 * Defines methods that are required but were removed from the Unsafe class
 * in Java 11.
 * @author Sune
 * @since 00.02.00
 * @see sun.misc.Unsafe*/
public final class UnsafeLegacy {
	
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