package sune.app.mediadown.plugin;

import java.io.File;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.Shared;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.plugin.PluginMemory.MemoryFile;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.ThrowableFunction;
import sune.app.mediadown.util.Utils;
import sune.util.load.ZIPLoader;
import sune.util.memory.GrowableMemory;
import sune.util.memory.MemoryPointer;
import sune.util.ssdf2.SSDF;

// Package-private
final class DefaultPluginLoader implements PluginLoader {
	
	private static final class ClearableGrowableMemory extends GrowableMemory {
		
		@Override
		public void close() {
			// Since the buffers variable is set to null in the close method,
			// save the references so it is possible to use them afterwards
			MappedByteBuffer[] copy = Arrays.copyOf(buffers, buffers.length);
			// Call the underlying method to close the channel
			super.close();
			// Actually clear the buffers
			for(MappedByteBuffer buf : copy) {
				try {
					NIO.unmap(buf);
				} catch(Exception ex) {
					// Ignore
				}
			}
			copy = null;
		}
	}
	
	private final Set<PluginFile> loadedPlugins = new LinkedHashSet<>();
	
	// Allow package-only instantiation
	DefaultPluginLoader() {
	}
	
	private static final boolean isLanguageFile(String entryName) {
		return entryName.endsWith(".ssdf") && Utils.dirname(entryName).endsWith("language");
	}
	
	private static final BinaryOperator<String> languageFileReducer(String langName) {
		return ((r, f) -> Utils.fileNameNoType(f).equals(langName) ? f : r);
	}
	
	private static final void initPluginMemory(PluginFile file) throws Exception {
		try(ZipFile zip = new ZipFile(new File(file.getPath()))) {
			// Initialize all resources
			PluginMemory memory = new PluginMemory(new ClearableGrowableMemory());
			memory.open();
			List<String> languageFiles = new ArrayList<>();
			Enumeration<? extends ZipEntry> entries = zip.entries();
			ZipEntry theEntry;
			String path;
			while(entries.hasMoreElements()) {
				theEntry = entries .nextElement();
				path     = theEntry.getName();
				if((isLanguageFile(path))) {
					// Add the potential language file to the collection of them
					languageFiles.add(path);
				} else if(!path.endsWith(".class")) {
					// Load any other non-class resource
					long pos = memory.getPosition();
					int size = 0;
					try(InputStream stream = zip.getInputStream(theEntry)) {
						byte[] buf = new byte[8192];
						int    len;
						while((len = stream.read(buf)) != -1) {
							memory.put(buf, 0, len);
							size += len;
						}
					}
					String name = Utils.basename(path);
					MemoryPointer pointer = new MemoryPointer(pos, size);
					MemoryFile memoryFile = new MemoryFile(pointer, path, name);
					memory.addFile(memoryFile);
				}
			}
			file.setMemory(memory);
			// Initialize the language file
			if((MediaDownloader.language() != null)) {
				String langName = MediaDownloader.language().name();
				if(langName.equalsIgnoreCase("auto")) // Replace auto language with the local one
					langName = MediaDownloader.Languages.localLanguage().name();
				String filePath = languageFiles.stream().reduce(null, languageFileReducer(langName));
				if((filePath == null)) {
					// No language file was found for the current language, therefore
					// try to find the language file for the default language.
					filePath = languageFiles.stream().reduce(null, languageFileReducer("english"));
				}
				// Care about the language file only when it is found for current or default language
				if((filePath != null)) {
					ZipEntry entryLang = zip.getEntry(filePath);
					try(InputStream stream = zip.getInputStream(entryLang)) {
						Translation translation = new Translation(stream);
						MediaDownloader.translation().combine(translation);
					}
				}
			}
		}
	}
	
	private static final void initPluginInstance(PluginFile file) throws Exception {
		file.setInstance((PluginBase) Class.forName(file.getPlugin().className()).getConstructor().newInstance());
	}
	
	private static final void initPlugin(PluginFile file) throws Exception {
		file.getInstance().init();
		
		// Only allow single setting of plugin configuration (either bootstrap or regular)
		if(file.getConfiguration().isEmpty()) {
			initConfiguration(file, file.getInstance().configuration());
		}
	}
	
	private static final void initConfiguration(PluginFile file, PluginConfiguration.Builder configuration) {
		if(configuration == null) return;
		// Load the plugin configuration, if it already exists
		Path configPath = NIO.localPath("resources/config/" + file.getPlugin().instance().name() + ".ssdf");
		if(NIO.exists(configPath)) configuration.loadData(SSDF.read(configPath.toFile()));
		file.setConfiguration(configuration.build());
	}
	
	private static final void disposePluginMemory(PluginFile file) {
		PluginMemory memory;
		if((memory = file.getMemory()) != null)
			memory.close();
	}
	
	private static final void disposePluginInstance(PluginFile file) throws Exception {
		file.setInstance(null);
		file.setBootstrapInstance(null);
	}
	
	private static final void disposePlugin(PluginFile file) throws Exception {
		file.getInstance().dispose();
		PluginBootstrapBase instanceBootstrap;
		if((instanceBootstrap = file.getBootstrapInstance()) != null)
			instanceBootstrap.dispose();
	}
	
	private static final void dispose(PluginFile file) throws Exception {
		disposePlugin(file);
		disposePluginInstance(file);
		disposePluginMemory(file);
	}
	
	private static final void loadClassFromZIP(ClassLoader classLoader, Path path, String className) throws Exception {
		try(ZipFile file = new ZipFile(path.toFile(), Shared.CHARSET)) {
			ThrowableFunction<String, byte[]> resolver = ((classPath) -> {
				ZipEntry entry = file.getEntry(classPath);
				if(entry == null)
					throw new IllegalStateException("Class not found at: " + classPath);
				return file.getInputStream(entry).readAllBytes();
			});
			// For optimization purposes, get all existing inner classes, so searching is faster
			List<String> innerClasses = Utils.stream(file.entries()).filter((entry) -> {
				return Utils.fileName(entry.getName()).contains("$");
			}).map((entry) -> entry.getName()).collect(Collectors.toList());
			// Create a resolver that for a class path gets all its inner classes' paths
			ThrowableFunction<String, List<String>> resolverInnerClasses = ((classPath) -> {
				String classPathNoType = classPath.replaceAll(".class$", "");
				return innerClasses.stream()
							.filter((name) -> name.startsWith(classPathNoType))
							.collect(Collectors.toList());
			});
			RootClassLoader rootClassLoader = new RootClassLoader(classLoader, resolver, resolverInnerClasses);
			rootClassLoader.load(RootClassLoader.classNameToPath(className));
		}
	}
	
	@Override
	public final void load(Collection<PluginFile> plugins, PluginLoadListener listener) throws Exception {
		if(plugins == null)
			throw new IllegalArgumentException("Plugins cannot be null");
		ClassLoader loader = ClassLoader.getSystemClassLoader();
		Set<PluginFile> cannotLoad = new LinkedHashSet<>();
		for(PluginFile plugin : plugins) {
			// Notify the listener, if needed
			if(listener != null)
				listener.onLoading(plugin);
			PluginLoaderContext.setContext(plugin);
			Path    path   = Path.of(plugin.getPath());
			boolean loaded = false;
			try {
				if((Files.exists(path))) {
					// Get the module name from the plugin's annotation or its file name
					Plugin annPlugin = plugin.getPlugin().instance();
					String moduleName = annPlugin.moduleName();
					if(moduleName.isEmpty())
						moduleName = annPlugin.name().replace('_', '.');
					// Check for bootstrap plugin
					AnnotatedClass<PluginBootstrap> bootstrap = plugin.getPluginBootstrap();
					if(bootstrap != null) {
						String bootstrapClassName = plugin.getPluginBootstrap().className();
						loadClassFromZIP(loader, path, bootstrapClassName);
						PluginBootstrapBase instanceBootstrap = 
								(PluginBootstrapBase) Class.forName(bootstrapClassName).getConstructor().newInstance();
						plugin.setBootstrapInstance(instanceBootstrap);
						// Try to initialize configuration early (before init()), it can be null
						initConfiguration(plugin, instanceBootstrap.configuration());
						instanceBootstrap.init();
					}
					// Load the plugin itself, define and load the classes
					ZIPLoader.load(path, moduleName, loader);
					initPluginMemory(plugin);
					initPluginInstance(plugin);
					initPlugin(plugin);
					loaded = loadedPlugins.add(plugin);
					// Notify the listener, if needed
					if(listener != null)
						listener.onLoaded(plugin, loaded);
				}
			} catch(Exception ex) {
				String message = String.format("Cannot load plugin: %s", plugin.getPlugin().instance().name());
				MediaDownloader.error(new IllegalStateException(message, ex));
			}
			// Notify the listener, if needed
			if(listener != null)
				listener.onLoaded(plugin, loaded);
			// Plugin was not loaded
			if(!loaded) cannotLoad.add(plugin);
		}
		PluginLoaderContext.setContext(null);
		// Notify the listener, if needed
		if(listener != null && !cannotLoad.isEmpty()) {
			PluginFile[] array = new PluginFile[cannotLoad.size()];
			listener.onNotLoaded(cannotLoad.toArray(array));
		}
	}
	
	@Override
	public final void dispose() throws Exception {
		for(PluginFile plugin : loadedPlugins) {
			try {
				dispose(plugin);
			} catch(Exception ex) {
				String message = String.format("Cannot dispose plugin: %s", plugin.getPlugin().instance().name());
				MediaDownloader.error(new IllegalStateException(message, ex));
			}
		}
	}
	
	@Override
	public final Collection<PluginFile> getLoadedPlugins() {
		return Collections.unmodifiableCollection(loadedPlugins);
	}
}