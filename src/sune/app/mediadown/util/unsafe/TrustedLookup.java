package sune.app.mediadown.util.unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

/** @since 00.02.09 */
public final class TrustedLookup {
	
	private static final MethodHandles.Lookup instance;
	
	static {
		try {
			Unsafe unsafe = UnsafeInstance.get();
			Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
			Object base = unsafe.staticFieldBase(field);
			long offset = unsafe.staticFieldOffset(field);
			instance = (MethodHandles.Lookup) unsafe.getObject(base, offset);
		} catch(Throwable th) {
			throw new ExceptionInInitializerError(th);
		}
	}
	
	private TrustedLookup() {
	}
	
	public static MethodHandles.Lookup get() {
		return instance;
	}
}
