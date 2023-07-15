package sune.app.mediadown.serialization;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import sune.app.mediadown.resource.cache.Cache;
import sune.app.mediadown.resource.cache.NoNullCache;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Reflection;

/** @since 00.02.09 */
//Package-private
final class SerializableCaller {
	
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	private static final Cache HANDLES = new NoNullCache();
	
	@SuppressWarnings("unchecked")
	private static final Pair<String, Class<?>[]>[] METHODS_TO_FIND = (Pair<String, Class<?>[]>[]) new Pair[] {
		methodPair("writeObject", ObjectOutputStream.class),
		methodPair("readObject", ObjectInputStream.class),
		methodPair("writeReplace"),
		methodPair("readResolve"),
	};
	
	private SerializableCaller() {
	}
	
	private static final Pair<String, Class<?>[]> methodPair(String methodName, Class<?>... argTypes) {
		return new Pair<>(methodName, argTypes);
	}
	
	private static final SerializableCaller.Handles findHandles(Class<?> clazz) {
		SerializableCaller.Handles handles = new Handles();
		
		for(Pair<String, Class<?>[]> pair : METHODS_TO_FIND) {
			try {
				Method method = clazz.getDeclaredMethod(pair.a, pair.b);
				Reflection.setAccessible(method, true);
				MethodHandle handle = LOOKUP.unreflect(method);
				handles.set(pair.a, handle);
			} catch(NoSuchMethodException
						| IllegalAccessException
						| IllegalArgumentException
						| SecurityException
						| NoSuchFieldException ex) {
				// Not found or cannot be found, ignore it
			}
		}
		
		return handles;
	}
	
	private static final SerializableCaller.Handles findHandlesCached(Class<?> clazz) throws Exception {
		return HANDLES.getChecked(clazz, () -> findHandles(clazz));
	}
	
	private static final Deque<Class<?>> inheritanceStack(Class<?> clazz) {
		Deque<Class<?>> stack = new ArrayDeque<>();
		
		for(Class<?> cls = clazz; includeInSerialization(cls); cls = cls.getSuperclass()) {
			stack.addLast(cls);
		}
		
		return stack;
	}
	
	private static final boolean includeInSerialization(Class<?> clazz) {
		return clazz != null && clazz != Object.class && clazz != Enum.class;
	}
	
	public static final SerializableCaller.Handles getHandles(Object instance) throws IOException {
		if(instance == null) {
			return null;
		}
		
		try {
			final Class<?> clazz = instance.getClass();
			return HANDLES.getChecked(clazz, () -> findHandlesCached(clazz));
		} catch(Exception ex) {
			throw new IOException(ex);
		}
	}
	
	public static final void writeObject(SerializableCaller.Handles handles, Object instance, ObjectOutputStream stream) throws IOException {
		MethodHandle handle;
		if((handle = handles.writeObject()) == null) {
			return;
		}
		
		try {
			handle.invoke(instance, stream);
		} catch(Throwable ex) {
			throw new IOException(ex);
		}
	}
	
	public static final void readObject(SerializableCaller.Handles handles, Object instance, ObjectInputStream stream) throws IOException {
		MethodHandle handle;
		if((handle = handles.readObject()) == null) {
			return;
		}
		
		try {
			handle.invoke(instance, stream);
		} catch(Throwable ex) {
			throw new IOException(ex);
		}
	}
	
	public static final Object writeReplace(SerializableCaller.Handles handles, Object instance) throws IOException {
		MethodHandle handle;
		if((handle = handles.writeReplace()) == null) {
			return null;
		}
		
		try {
			return handle.invoke(instance);
		} catch(Throwable ex) {
			throw new IOException(ex);
		}
	}
	
	public static final Object readResolve(SerializableCaller.Handles handles, Object instance) throws IOException {
		MethodHandle handle;
		if((handle = handles.readResolve()) == null) {
			return null;
		}
		
		try {
			return handle.invoke(instance);
		} catch(Throwable ex) {
			throw new IOException(ex);
		}
	}
	
	public static final void writeObject(Object instance, ObjectOutputStream stream) throws IOException {
		if(instance == null) {
			return;
		}
		
		Deque<Class<?>> stack = inheritanceStack(instance.getClass());
		
		try {
			for(Class<?> cls; (cls = stack.pollLast()) != null;) {
				SerializableCaller.Handles handles = findHandlesCached(cls);
				writeObject(handles, instance, stream);
			}
		} catch(Exception ex) {
			throw new IOException(ex);
		}
	}
	
	public static final void readObject(Object instance, ObjectInputStream stream) throws IOException {
		if(instance == null) {
			return;
		}
		
		Deque<Class<?>> stack = inheritanceStack(instance.getClass());
		
		try {
			for(Class<?> cls; (cls = stack.pollLast()) != null;) {
				SerializableCaller.Handles handles = findHandlesCached(cls);
				readObject(handles, instance, stream);
			}
		} catch(Exception ex) {
			throw new IOException(ex);
		}
	}
	
	public static final class Handles {
		
		private MethodHandle writeObject;
		private MethodHandle readObject;
		private MethodHandle writeReplace;
		private MethodHandle readResolve;
		
		public void set(String methodName, MethodHandle handle) {
			switch(methodName) {
				case "writeObject": writeObject = handle; break;
				case "readObject": readObject = handle; break;
				case "writeReplace": writeReplace = handle; break;
				case "readResolve": readResolve = handle; break;
				default: throw new IllegalArgumentException("Unsupported method name");
			}
		}
		
		public MethodHandle writeObject() { return writeObject; }
		public MethodHandle readObject() { return readObject; }
		public MethodHandle writeReplace() { return writeReplace; }
		public MethodHandle readResolve() { return readResolve; }
	}
}