package sune.app.mediadown.library;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.util.Pair;
import sune.util.load.ModuleLazyLoader;
import sune.util.load.ModuleUtils;

public final class Libraries implements EventBindable<LibraryEvent> {
	
	// TODO: Refactor, clean up
	
	// Must retain the order of insertion
	private final Map<String, Library> libraries = new LinkedHashMap<>();
	private final EventRegistry<LibraryEvent> eventRegistry = new EventRegistry<>();
	
	// Forbid anyone to create an instance of this class
	private Libraries() {
	}
	
	public static final Libraries create() {
		return new Libraries();
	}
	
	public final void add(String path) {
		add(path, null);
	}
	
	public final void add(String path, String name) {
		add(Paths.get(path), name);
	}
	
	public final void add(Path path) {
		add(path, null);
	}
	
	public final void add(Path path, String name) {
		if((name == null)) {
			name = ModuleUtils.automaticModuleName(path);
		}
		libraries.put(name, new Library(name, path));
	}
	
	public final void remove(String name) {
		libraries.remove(name);
	}
	
	public final Collection<Library> all() {
		return Collections.unmodifiableCollection(libraries.values());
	}
	
	private final boolean load(Map<String, Library> libraries) {
		if((libraries == null))
			throw new IllegalArgumentException("Libraries cannot be null");
		ClassLoader loader = ClassLoader.getSystemClassLoader();
		boolean success = true;
		for(Library library : libraries.values()) {
			// Notify the listener, if needed
			eventRegistry.call(LibraryEvent.LOADING, library);
			String  name   = library.getName();
			Path    path   = library.getPath();
			boolean loaded = false;
			// Used for preserving the information about an exception, if any
			Exception exception = null;
			try {
				// Load the library into the loader
				loaded = ModuleLazyLoader.loadModule(path, name, loader) != null;
			} catch(Exception ex) {
				exception = ex;
			}
			
			// Module was not loaded
			if(loaded) {
				// Notify the listener, if needed
				eventRegistry.call(LibraryEvent.LOADED, library);
			} else {
				if((exception == null)) {
					exception = new RuntimeException("Unknown exception");
				}
				eventRegistry.call(LibraryEvent.NOT_LOADED, new Pair<>(library, exception));
				success = false;
			}
		}
		return success;
	}
	
	public final boolean load() {
		return load(libraries);
	}
	
	@Override
	public <V> void addEventListener(Event<? extends LibraryEvent, V> event, Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	@Override
	public <V> void removeEventListener(Event<? extends LibraryEvent, V> event, Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
}