package sune.app.mediadown.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

public final class Reflection {
	
	/**
	 * The Unsafe instance.*/
	private static final Unsafe unsafe = UnsafeInstance.get();
	
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
	
	// Forbid anyone to create an instance of this class
	private Reflection() {
	}
}