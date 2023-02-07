package sune.app.mediadown.library;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.LibraryEvent;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.util.Pair;
import sune.util.load.ModuleLazyLoader;
import sune.util.load.ModuleUtils;

/** @since 00.02.08 */
public final class Libraries implements EventBindable<LibraryEvent> {
	
	// Use Linked version so that the order of insertion is preserved
	private final Map<String, Library> libraries = new LinkedHashMap<>();
	private final EventRegistry<LibraryEvent> eventRegistry = new EventRegistry<>();
	
	// Forbid anyone to create an instance of this class
	private Libraries() {
	}
	
	public static final Libraries create() {
		return new Libraries();
	}
	
	private final boolean load(Map<String, Library> libraries, ClassLoader loader) {
		if(libraries == null) {
			throw new IllegalArgumentException("Libraries cannot be null");
		}
		
		boolean success = true;
		
		for(Library library : libraries.values()) {
			eventRegistry.call(LibraryEvent.LOADING, library);
			
			String name = library.name();
			Path path = library.path();
			boolean loaded = false;
			Exception exception = null;
			
			try {
				loaded = ModuleLazyLoader.loadModule(path, name, loader) != null;
			} catch(Exception ex) {
				exception = ex;
			}
			
			if(loaded) {
				eventRegistry.call(LibraryEvent.LOADED, library);
			} else {
				if(exception == null) {
					exception = new RuntimeException("Unknown exception");
				}
				
				eventRegistry.call(LibraryEvent.NOT_LOADED, new Pair<>(library, exception));
				success = false;
			}
		}
		
		return success;
	}
	
	public final void add(String path) {
		add(path, null);
	}
	
	public final void add(String path, String name) {
		add(Path.of(path), name);
	}
	
	public final void add(Path path) {
		add(path, null);
	}
	
	public final void add(Path path, String name) {
		if(name == null) {
			name = ModuleUtils.automaticModuleName(path);
		}
		
		libraries.put(name, new Library(name, path));
	}
	
	public final void remove(String name) {
		libraries.remove(name);
	}
	
	public final List<Library> all() {
		return List.copyOf(libraries.values());
	}
	
	public final boolean load(ClassLoader loader) {
		return load(libraries, loader);
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