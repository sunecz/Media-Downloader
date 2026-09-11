package sune.app.mediadown.util.unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.ProtectionDomain;

/**
 * Defines methods that are required but were removed from the Unsafe class
 * in Java 11.
 * 
 * @author Sune
 * @since 00.02.09
 * 
 * @see sun.misc.Unsafe
 */
public final class UnsafeLegacy {
	
	private static final MethodHandle mh_ClassLoader_defineClass;
	
	static {
		try {
			MethodHandles.Lookup lookup = TrustedLookup.get();
			MethodHandle mh_defineClass = lookup.findVirtual(
				ClassLoader.class, "defineClass",
				MethodType.methodType(
					Class.class,
					String.class, byte[].class, int.class, int.class, ProtectionDomain.class
				)
			);
			
			mh_ClassLoader_defineClass = mh_defineClass;
		} catch(Throwable th) {
			throw new ExceptionInInitializerError(th);
		}
	}
	
	private UnsafeLegacy() {
	}
	
	/**
	 * Converts an array of bytes into an instance of class {@code Class},
	 * with a given {@code ProtectionDomain}, defining this class in the given
	 * {@code ClassLoader}.
	 * 
	 * @param name The expected <a href="#binary-name">binary name</a> of the class,
	 * or {@code null} if not known
	 * @param b The bytes that make up the class data. The bytes in positions
	 * {@code off} through {@code off+len-1} should have the format of a valid
	 * class file as defined by <cite>The Java&trade; Virtual Machine Specification</cite>.
	 * @param off The start offset in {@code b} of the class data
	 * @param len The length of the class data
	 * @param loader The class loader where to define the class
	 * @param protectionDomain The {@code ProtectionDomain} of the class
	 * 
	 * @return The {@code Class} object created from the data,
	 * and {@code ProtectionDomain}.
	 * 
	 * @see ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)
	 */
	public static final Class<?> defineClass(
		ClassLoader loader,
		String name, byte[] b, int off, int len, ProtectionDomain protectionDomain
	) {
		try {
			return (Class<?>) mh_ClassLoader_defineClass.invoke(
				loader,
				name, b, off, len, protectionDomain
			);
		} catch(Throwable th) {
			throw new RuntimeException(th);
		}
	}
}
