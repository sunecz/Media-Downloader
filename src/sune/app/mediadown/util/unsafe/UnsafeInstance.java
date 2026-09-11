package sune.app.mediadown.util.unsafe;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/** @since 00.02.09 */
public final class UnsafeInstance {
	
	private static final Unsafe instance;
	
	static {
		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			instance = (Unsafe) field.get(null);
		} catch(Throwable th) {
			throw new ExceptionInInitializerError(th);
		}
	}
	
	private UnsafeInstance() {
	}
	
	public static final Unsafe get() {
		return instance;
	}
}
