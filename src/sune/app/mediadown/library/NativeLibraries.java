package sune.app.mediadown.library;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.NativeLibraryLoaderEvent;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;
import sune.app.mediadown.util.Pair;

public final class NativeLibraries {
	
	private static final Set<NativeLibrary> libraries = new LinkedHashSet<>();
	private static final NativeLibraryCompatibilityChecker checker = new NativeLibraryCompatibilityChecker();
	private static final EventRegistry<NativeLibraryLoaderEvent> eventRegistry = new EventRegistry<>();
	
	// Forbid anyone to create an instance of this class
	private NativeLibraries() {
	}
	
	private static final void add(NativeLibrary library) {
		libraries.add(Objects.requireNonNull(library));
	}
	
	private static final boolean isOSCompatible(NativeLibrary library) {
		return checker.check(library);
	}
	
	/** @since 00.02.08 */
	private static final <V> void call(Event<NativeLibraryLoaderEvent, V> event, V value) {
		eventRegistry.call(event, value);
	}
	
	private static final boolean loadLibrary(NativeLibrary library) {
		call(NativeLibraryLoaderEvent.LOADING, library);
		
		Throwable exception = null;
		try {
			Path absPath = library.getPath().toAbsolutePath();
			
			if(!NIO.exists(absPath)) {
				throw new FileNotFoundException("Native library does not exist at '" + absPath.toString() + "'");
			}
			
			System.load(absPath.toString());
		} catch(UnsatisfiedLinkError |
				SecurityException    |
				FileNotFoundException ex) {
			exception = ex;
		} finally {
			call(NativeLibraryLoaderEvent.LOADED, new Pair<>(library, exception));
		}
		
		return exception == null;
	}
	
	public static final void add(Path path, String name, String osName, String osArch, String version) {
		add(new NativeLibrary(path, name, osName, osArch, version));
	}
	
	public static final boolean load() {
		List<NativeLibrary> notLoaded = libraries.stream()
			.filter(NativeLibraries::isOSCompatible)
			.map((library) -> new Pair<>(library, loadLibrary(library)))
			.filter((pair) -> !pair.b)
			.map((pair) -> pair.a)
			.collect(Collectors.toCollection(ArrayList::new));
		
		if(!notLoaded.isEmpty()) {
			call(NativeLibraryLoaderEvent.NOT_LOADED, notLoaded);
		}
		
		return notLoaded.isEmpty();
	}
	
	public static final Collection<NativeLibrary> all() {
		return Collections.unmodifiableCollection(libraries);
	}
	
	public static final <V> void addEventListener(Event<? extends NativeLibraryLoaderEvent, V> event,
			Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	public static final <V> void removeEventListener(Event<? extends NativeLibraryLoaderEvent, V> event,
			Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
	
	private static final class NativeLibraryCompatibilityChecker {
		
		private final String osName;
		private final String osArch;
		
		public NativeLibraryCompatibilityChecker() {
			this(OSUtils.getSystemName(), OSUtils.getSystemArch());
		}
		
		private NativeLibraryCompatibilityChecker(String osName, String osArch) {
			this.osName = checkString(osName);
			this.osArch = checkString(osArch);
		}
		
		private static final String checkString(String string) {
			if(string == null || string.isEmpty())
				throw new IllegalArgumentException();
			
			return string;
		}
		
		public boolean check(NativeLibrary library) {
			return library != null
						&& library.getOSName().equalsIgnoreCase(osName)
						&& library.getOSArch().equalsIgnoreCase(osArch);
		}
	}
}