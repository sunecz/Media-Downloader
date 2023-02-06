package sune.app.mediadown.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * Contains methods for disabling illegal access warnings.
 * These warnings are generated when an unprivileged access action is taken,
 * such as accessing private or protected methods or fields.
 * @author apangin
 * @author Sune
 */
public final class IllegalAccessWarnings {
	
	// Forbid anyone to create an instance of this class
	private IllegalAccessWarnings() {
	}
	
	/**
	 * Tries to disable the illegal access warnings messages that occur when
	 * an unprivileged access action is taken. This method does <strong>not</strong>
	 * guarantee successful disabling.<br><br>
	 * The official method of disabling these messages is using an JVM argument:
	 * {@code --illegal-access=deny}. However, this method tries to overcome
	 * the issue when the program is unable or simply does not want to be run
	 * with this argument.
	 * @return {@code true}, if the disabling process was successful,
	 * otherwise {@code false}.
	 * @see <a href="https://stackoverflow.com/a/46458447">Source</a>
	 */
	public static final boolean tryDisable() {
		try {
			Class<?> classLogger = Class.forName("jdk.internal.module.IllegalAccessLogger");
			Field    fieldLogger = classLogger.getDeclaredField("logger");
			Unsafe unsafe = UnsafeInstance.get();
			long   offset = unsafe.staticFieldOffset(fieldLogger);
			unsafe.putObject(classLogger, offset, null);
			return true;
		} catch(ClassNotFoundException
					| NoSuchFieldException
					| SecurityException ex) {
			// Ignore
		}
		return false;
	}
}