package sune.app.mediadown.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class Instantiator<I> {
	
	public final <T extends I> T newInstance(Class<T> clazz)
			throws InvocationTargetException,
			       InstantiationException,
			       SecurityException,
			       IllegalAccessException,
			       IllegalArgumentException,
			       NoSuchMethodException,
			       NoSuchFieldException {
		Constructor<T> ctor = clazz.getDeclaredConstructor();
		Reflection.setAccessible(ctor, true);
		T instance = ctor.newInstance();
		Reflection.setAccessible(ctor, false);
		return instance;
	}
}