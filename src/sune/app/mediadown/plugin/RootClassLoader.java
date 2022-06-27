package sune.app.mediadown.plugin;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import sune.app.mediadown.util.ThrowableFunction;
import sune.app.mediadown.util.UnsafeLegacy;

/**
 * Special class loader that loads a specific given class
 * and all its required classes.
 * @since 00.02.02
 */
public final class RootClassLoader {
	
	private final ClassLoader loader;
	private final ThrowableFunction<String, byte[]> resolver;
	private final ThrowableFunction<String, List<String>> resolverInnerClasses;
	
	public RootClassLoader(ClassLoader loader, ThrowableFunction<String, byte[]> resolver,
			ThrowableFunction<String, List<String>> resolverInnerClasses) {
		this.loader = Objects.requireNonNull(loader);
		this.resolver = Objects.requireNonNull(resolver);
		this.resolverInnerClasses = resolverInnerClasses; // Can be null
	}
	
	/**
	 * Extracts a full class name from the given {@code path}.
	 * @param path the path
	 * @return The class name.*/
	public static final String pathToClassName(String path) {
		return (path.endsWith(".class") ? path.substring(0, path.length() - ".class".length()) : path).replace('/', '.');
	}
	
	/**
	 * Converts a full class name given as the {@code name} to a path.
	 * @param name the class name
	 * @return The resource path.*/
	public static final String classNameToPath(String name) {
		return (name.endsWith(".class") ? name.substring(0, name.length() - ".class".length()) : name).replace('.', '/') + ".class";
	}
	
	/**
	 * Checks if the given {@code path} is a class file or not.<br><br>
	 * <em>Note:</em> this method does not classify {@code module-info.class}
	 * as a actual class file, therefore returning {@code false}.
	 * @param path the path
	 * @return {@code true}, if a file located at the path is a class file,
	 * otherwise {@code false}.*/
	public static final boolean isClassFile(String path) {
		return path.endsWith(".class") && !path.endsWith("module-info.class");
	}
	
	private static final Class<?> defineClass(ClassLoader loader, String name, byte[] bytes)
			throws InvocationTargetException,
				   IllegalAccessException,
				   IllegalArgumentException {
		try {
			// If the class was already loaded, just return it
			return Class.forName(name, false, loader);
		} catch(ClassNotFoundException ex) {
			// Ignore
		}
		// Define the requested class using the given bytes
		return UnsafeLegacy.defineClass(name, bytes, 0, bytes.length, loader, null);
	}
	
	private static final boolean classLoaded(ClassLoader loader, String name) {
		try {
			Class.forName(name, false, loader);
			return true;
		} catch(ClassNotFoundException ex) {
			return false;
		}
	}
	
	/**
	 * Gets bytes from a file located at the given {@code path} in
	 * the current {@code module}.
	 * @param path the path to the resource
	 * @return The content of the resource as an byte array.*/
	private final byte[] bytes(String path) throws Exception {
		try {
			return resolver.apply(path);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to obtain bytes of class: " + pathToClassName(path), ex);
		}
	}
	
	private final List<String> innerClasses(String path) throws Exception {
		return resolverInnerClasses != null ? resolverInnerClasses.apply(path) : List.of();
	}
	
	private final void pushToStackDirect(Deque<Entry<String, String>> stack, Set<String> queued, String path) {
		if(!queued.contains(path)) {
			stack.push(Map.entry(path, pathToClassName(path)));
			queued.add(path);
		}
	}
	
	private final void pushToStack(Deque<Entry<String, String>> stack, Set<String> queued, String path) throws Exception {
		pushToStackDirect(stack, queued, path);
		innerClasses(path).forEach((p) -> pushToStackDirect(stack, queued, p));
	}
	
	private final boolean isBuiltinDependency(String name) {
		return name.startsWith("java.");
	}
	
	private final List<String> dependencies(byte[] bytes) {
		return ClassDependencyAnalyzer.getDependencies(ByteBuffer.wrap(bytes)).stream()
					.filter((name) -> !isBuiltinDependency(name))
					.sorted((a, b) -> {
						int al = a.length() - a.replace("$", "").length();
						int bl = b.length() - b.replace("$", "").length();
						return al < bl ? -1 : (al > bl ? 1 : a.compareTo(b));
					})
					.collect(Collectors.toList());
	}
	
	/**
	 * Loads a class given by the {@code path}. This class is loaded into
	 * the current loader. All the classes that are required by
	 * this class are loaded beforehand. These classes must be already loaded
	 * or must be resolvable using the given resolver.
	 * @param path the path of the class
	 * @return The class object of a class file located at the given
	 * {@code path}.*/
	private final Class<?> loadClass(String path) throws Exception {
		if(!isClassFile(path)) return null; // Do not load non-class files
		Class<?> clazz = null;
		Set<String> loaded = new HashSet<>();
		Set<String> queued = new HashSet<>();
		Deque<Entry<String, String>> stack = new ArrayDeque<>();
		pushToStack(stack, queued, path);
		Entry<String, String> entry;
		String name;
		byte[] bytes;
		do {
			entry = stack.peek();
			path  = entry.getKey();
			name  = entry.getValue();
			bytes = bytes(path);
			// Push all class dependecies to the stack
			for(String depName : dependencies(bytes)) {
				if(loaded.contains(depName)
						|| (classLoaded(loader, depName) && loaded.add(depName)))
					continue; // Skip already loaded classes
				pushToStack(stack, queued, classNameToPath(depName));
			}
			// Dependencies will move the original class down the stack
			if(stack.peek() != entry) {
				entry = stack.peek();
				path  = entry.getKey();
				name  = entry.getValue();
				bytes = bytes(path);
			}
			try {
				clazz = defineClass(loader, name, bytes);
				// Remember that we already loaded this class
				loaded.add(clazz.getName());
				// Class was successfully defined, remove it from the stack
				stack.remove();
			} catch(InvocationTargetException
						| IllegalArgumentException
						| IllegalAccessException ex) {
				if((ex.getCause() instanceof NoClassDefFoundError)) {
					// The class needs another class to be defined
					String classPath = classNameToPath(ex.getCause().getMessage());
					pushToStack(stack, queued, classPath);
				} else {
					throw ex;
				}
			}
		}
		// Repeat till there are some class need defining
		while(!stack.isEmpty());
		// Return the requested class
		return clazz;
	}
	
	public final void load(String classPath) throws Exception {
		loadClass(classPath);
	}
}