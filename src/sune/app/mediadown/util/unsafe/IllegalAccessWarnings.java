package sune.app.mediadown.util.unsafe;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * Contains methods for disabling illegal access warnings.
 * These warnings are generated when an unprivileged access action is taken,
 * such as accessing private or protected methods or fields.
 * 
 * @author apangin
 * @author Sune
 * @since 00.02.09
 */
public final class IllegalAccessWarnings {
	
	private IllegalAccessWarnings() {
	}
	
	/**
	 * <p>
	 * Tries to disable the illegal access warnings messages that occur when
	 * an unprivileged access action is taken. This method does <strong>not</strong>
	 * guarantee successful disabling.
	 * </p>
	 * 
	 * <p>
	 * The official method of disabling these messages is using a JVM argument
	 * {@code --illegal-access=deny}. However, this method tries to overcome
	 * the issue when the program is unable or simply does not want to be run
	 * with this argument.
	 * </p>
	 * 
	 * @return {@code true}, if the disabling process was successful,
	 * otherwise {@code false}.
	 * 
	 * @see <a href="https://stackoverflow.com/a/46458447">Source</a>
	 */
	public static final boolean tryDisable() {
		try {
			Unsafe unsafe = UnsafeInstance.get();
			Class<?> clazz = Class.forName("jdk.internal.module.IllegalAccessLogger");
			Field field = clazz.getDeclaredField("logger");
			Object base = unsafe.staticFieldBase(field);
			long offset = unsafe.staticFieldOffset(field);
			unsafe.putObject(base, offset, null);
			return true;
		} catch(Throwable th) {
			// Ignore
		}
		
		return false;
	}
}
