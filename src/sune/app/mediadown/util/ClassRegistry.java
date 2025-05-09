package sune.app.mediadown.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import sune.app.mediadown.util.unsafe.Reflection;

/** @since 00.02.09 */
public abstract class ClassRegistry<T> implements Iterable<Pair<String, T>> {
	
	private Map<String, T> values;
	private Map<Class<? extends T>, String> registeredClasses;
	private List<Pair<String, T>> allValues;
	
	protected static final Class<?> callerClass() {
		return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
	}
	
	protected static final ClassLoader callerClassLoader() {
		return callerClass().getClassLoader();
	}
	
	protected T newInstance(Class<? extends T> clazz)
			throws InstantiationException,
			       IllegalAccessException,
			       IllegalArgumentException,
			       InvocationTargetException,
			       NoSuchFieldException,
			       SecurityException,
			       NoSuchMethodException {
		Constructor<?> ctor = Reflection.getConstructor(clazz);
		@SuppressWarnings("unchecked")
		T instance = (T) Reflection.newInstance(ctor);
		return instance;
	}
	
	protected Class<? extends T> classOf(String className) throws ClassNotFoundException {
		Objects.requireNonNull(className);
		@SuppressWarnings("unchecked")
		Class<? extends T> clazz = (Class<? extends T>) Class.forName(className, true, callerClassLoader());
		return clazz;
	}
	
	protected void checkName(String name) {
		if(name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Name cannot be null or empty");
		}
	}
	
	protected void checkValue(T value) {
		if(value == null) {
			throw new IllegalArgumentException("Value cannot be null");
		}
	}
	
	protected abstract String toName(T value);
	
	public void register(Class<? extends T> clazz) {
		Objects.requireNonNull(clazz);
		
		synchronized(this) {
			if(registeredClasses == null) {
				registeredClasses = new HashMap<>();
			} else if(registeredClasses.containsKey(clazz)) {
				return; // Already registered
			}
			
			T value;
			try {
				value = newInstance(clazz);
			} catch(InstantiationException
						| IllegalAccessException
						| IllegalArgumentException
						| InvocationTargetException
						| NoSuchFieldException
						| SecurityException
						| NoSuchMethodException ex) {
				throw new IllegalStateException(ex);
			}
			
			String name = toName(value);
			checkName(name);
			checkValue(value);
			registeredClasses.put(clazz, name);
			
			if(values == null) {
				values = new LinkedHashMap<>();
			}
			
			values.putIfAbsent(name, value);
			allValues = null; // Invalidate
		}
	}
	
	public void register(String className) throws ClassNotFoundException {
		register(classOf(className));
	}
	
	public void unregister(Class<? extends T> clazz) {
		Objects.requireNonNull(clazz);
		
		synchronized(this) {
			if(registeredClasses == null || values == null) {
				return;
			}
			
			String name = registeredClasses.get(clazz);
			
			if(name == null) {
				return;
			}
			
			T value = values.remove(name);
			
			if(value != null) {
				registeredClasses.remove(value.getClass());
				allValues = null; // Invalidate
			}
		}
	}
	
	public void unregister(String className) throws ClassNotFoundException {
		unregister(classOf(className));
	}
	
	public T get(String name) {
		synchronized(this) {
			if(values == null) {
				return null;
			}
			
			return values.get(name);
		}
	}
	
	public List<Pair<String, T>> all() {
		if(allValues == null)  {
			synchronized(this) {
				if(allValues == null) {
					allValues = values != null
						? values.entrySet().stream()
								.map((e) -> new Pair<>(e.getKey(), e.getValue()))
								.collect(Collectors.toUnmodifiableList())
						: List.of();
				}
			}
		}
		
		return allValues;
	}
	
	public List<String> allNames() {
		return all().stream().map((p) -> p.a).collect(Collectors.toUnmodifiableList());
	}
	
	public List<T> allValues() {
		return all().stream().map((p) -> p.b).collect(Collectors.toUnmodifiableList());
	}
	
	public boolean isEmpty() {
		synchronized(this) {
			return values == null ? true : values.isEmpty();
		}
	}
	
	public int size() {
		synchronized(this) {
			return values == null ? 0 : values.size();
		}
	}
	
	@Override
	public Iterator<Pair<String, T>> iterator() {
		return all().iterator();
	}
}