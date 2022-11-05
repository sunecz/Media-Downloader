package sune.app.mediadown.plugin;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import eu.infomas.annotation.AnnotationDetector.TypeReporter;
import sune.app.mediadown.util.UnsafeLegacy;

// Package-private class
final class PluginTypeReporter implements TypeReporter {
	
	@SuppressWarnings("unchecked")
	private static final Class<? extends Annotation>[] annotations = new Class[] { Plugin.class, PluginBootstrap.class };
	private static ClassLoader loader = new DummyClassLoader();
	
	private final Path file;
	private AnnotatedClass<Plugin> plugin;
	private AnnotatedClass<PluginBootstrap> pluginBootstrap;
	private Exception exception;
	
	public PluginTypeReporter(Path file) {
		this.file = file;
	}
	
	private static final String toEntryName(String className) {
		return className.replace('.', '/') + ".class";
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
		// Define the requested class by the given bytes
		return UnsafeLegacy.defineClass(name, bytes, 0, bytes.length, loader, null);
	}
	
	public static final void resetClassLoader() {
		loader = new DummyClassLoader();
	}
	
	public static final ClassLoader getClassLoader() {
		return loader;
	}
	
	@Override
	public final void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
		exception = null;
		try(ZipFile zip = new ZipFile(file.toFile())) {
			String   entryName = toEntryName(className);
			ZipEntry entry     = zip.getEntry(entryName);
			try(InputStream stream = zip.getInputStream(entry)) {
				byte[]     array = stream.readAllBytes();
				Class<?>   clazz = defineClass(loader, className, array);
				Annotation ann   = clazz.getAnnotation(annotation);
				if(annotation == Plugin.class) {
					if(plugin == null) {
						plugin = new AnnotatedClass<>((Plugin) ann, className);
					}
				} else if(annotation == PluginBootstrap.class) {
					if(pluginBootstrap == null) {
						pluginBootstrap = new AnnotatedClass<>((PluginBootstrap) ann, className);
					}
				}
			}
		} catch(Exception ex) {
			exception = ex;
		}
	}
	
	@Override
	public final Class<? extends Annotation>[] annotations() {
		return annotations;
	}
	
	/** @since 00.02.02 */
	public AnnotatedClass<Plugin> getPlugin() {
		return plugin;
	}
	
	/** @since 00.02.02 */
	public AnnotatedClass<PluginBootstrap> getPluginBootstrap() {
		return pluginBootstrap;
	}
	
	public Exception getException() {
		return exception;
	}
	
	// Used for loading plugin information. Since all plugins are later loaded
	// to the system class loader, we do not want to load any of their classes
	// before actually deciding if the plugin should be loaded.
	private static final class DummyClassLoader extends ClassLoader {
		
		// Allow instantiation
		public DummyClassLoader() {
		}
	}
}