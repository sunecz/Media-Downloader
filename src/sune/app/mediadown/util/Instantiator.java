package sune.app.mediadown.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import sune.app.mediadown.util.unsafe.Reflection;

public final class Instantiator<I> {
	
	public final <T extends I> T newInstance(Class<T> clazz)
			throws InvocationTargetException,
			       InstantiationException,
			       SecurityException,
			       IllegalAccessException,
			       IllegalArgumentException,
			       NoSuchMethodException,
			       NoSuchFieldException {
		Constructor<?> ctor = Reflection.getConstructor(clazz);
		return Reflection.newInstance(ctor);
	}
}