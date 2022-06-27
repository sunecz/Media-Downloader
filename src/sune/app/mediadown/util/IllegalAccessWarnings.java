package sune.app.mediadown.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public final class IllegalAccessWarnings {
	
	// Source: https://stackoverflow.com/a/46458447
	public static final void tryDisable() {
		try {
			Class<?> classLogger = Class.forName("jdk.internal.module.IllegalAccessLogger");
			Field    fieldLogger = classLogger.getDeclaredField("logger");
			Unsafe unsafe = UnsafeInstance.get();
			long   offset = unsafe.staticFieldOffset(fieldLogger);
			unsafe.putObjectVolatile(classLogger, offset, null);
		} catch(Exception ex) {
			// Ignore
		}
	}
	
	// Forbid anyone to create an instance of this class
	private IllegalAccessWarnings() {
	}
}