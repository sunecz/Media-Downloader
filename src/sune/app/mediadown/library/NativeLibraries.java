package sune.app.mediadown.library;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;
import sune.app.mediadown.util.Pair;

public final class NativeLibraries {
	
	public static interface NativeLibraryLoadListener {
		
		void onLoading  (NativeLibrary   library);
		void onLoaded   (NativeLibrary   library, boolean success, Throwable exception);
		void onNotLoaded(NativeLibrary[] libraries);
	}
	
	private static final Set<NativeLibrary> libraries = new LinkedHashSet<>();
	
	private static final void add(NativeLibrary library) {
		if((library == null))
			throw new IllegalArgumentException("Native library cannot be null");
		libraries.add(library);
	}
	
	public static final void add(Path path, String name, String osName, String osArch, String version) {
		add(new NativeLibrary(path, name, osName, osArch, version));
	}
	
	private static final class NativeLibraryCompatibilityChecker {
		
		private final String osName;
		private final String osArch;
		
		public NativeLibraryCompatibilityChecker() {
			this(OSUtils.getSystemName(), OSUtils.getSystemArch());
		}
		
		private NativeLibraryCompatibilityChecker(String osName, String osArch) {
			if((osName == null || osName.isEmpty() || osArch == null || osArch.isEmpty()))
				throw new IllegalArgumentException();
			this.osName = osName;
			this.osArch = osArch;
		}
		
		public boolean check(NativeLibrary library) {
			return library != null
						&& library.getOSName().equalsIgnoreCase(osName)
						&& library.getOSArch().equalsIgnoreCase(osArch);
		}
	}
	
	private static final NativeLibraryCompatibilityChecker checker
		= new NativeLibraryCompatibilityChecker();
	
	private static final boolean isOSCompatible(NativeLibrary library) {
		return checker.check(library);
	}
	
	private static final boolean loadLibrary(NativeLibrary library, NativeLibraryLoadListener listener) {
		Throwable exception = null;
		if((listener != null))
			listener.onLoading(library);
		try {
			Path absPath = library.getPath().toAbsolutePath();
			if(!NIO.exists(absPath))
				throw new FileNotFoundException("Native library does not exist at '" + absPath.toString() + "'");
			System.load(absPath.toString());
		} catch(UnsatisfiedLinkError |
				SecurityException    |
				FileNotFoundException ex) {
			exception = ex;
		}
		boolean success = exception == null;
		if((listener != null))
			listener.onLoaded(library, success, exception);
		return success;
	}
	
	public static final boolean load() {
		return load(null);
	}
	
	public static final boolean load(NativeLibraryLoadListener listener) {
		List<NativeLibrary> notLoaded = libraries.stream()
			.filter(NativeLibraries::isOSCompatible)
			.map((library) -> new Pair<>(library, loadLibrary(library, listener)))
			.filter((pair) -> !pair.b)
			.map((pair) -> pair.a)
			.collect(Collectors.toCollection(LinkedList::new));
		if((listener != null && !notLoaded.isEmpty()))
			listener.onNotLoaded(notLoaded.toArray(new NativeLibrary[notLoaded.size()]));
		return notLoaded.isEmpty();
	}
	
	public static final Collection<NativeLibrary> all() {
		return Collections.unmodifiableCollection(libraries);
	}
}