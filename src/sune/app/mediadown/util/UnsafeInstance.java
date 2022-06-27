package sune.app.mediadown.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public final class UnsafeInstance {
	
	private static final Unsafe unsafe;
	
	static {
		Unsafe _unsafe = null;
		try {
			Class<?> clazz = Class.forName("sun.misc.Unsafe");
			Field    field = clazz.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			_unsafe = (Unsafe) field.get(null);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to obtain Unsafe instance");
		}
		unsafe = _unsafe;
	}
	
	public static final Unsafe get() {
		return unsafe;
	}
}