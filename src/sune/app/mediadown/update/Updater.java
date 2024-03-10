package sune.app.mediadown.update;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.function.BiPredicate;

import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.FileDownloader;
import sune.app.mediadown.event.CheckEvent;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.update.FileChecker.FileCheckerEntry;
import sune.app.mediadown.util.BiCallback;
import sune.app.mediadown.util.CheckedBiFunction;
import sune.app.mediadown.util.CheckedCallback;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

public abstract class Updater implements EventBindable<CheckEvent> {
	
	private static final String NAME_CONFIG = "config";
	private static final String NAME_DATA   = "data";
	private static final String NAME_LIST   = "list";
	
	/** @since 00.02.08 */
	protected final EventRegistry<CheckEvent> eventRegistry = new EventRegistry<>();
	
	protected Updater() {
	}
	
	private static final InputStream stream(String url, int timeout) throws IOException {
		return Net.stream(Net.uri(url), Duration.ofMillis(timeout)); // Forward call
	}
	
	private static final String content(String url, int timeout) {
		return Ignore.defaultValue(() -> Utils.streamToString(stream(url, timeout)), (String) null); // Forward call
	}
	
	private static final String urlConcat(String... strings) {
		return Net.uriConcat(strings); // Forward call
	}
	
	private static final boolean shouldDownloadEntry(FileCheckerEntry entry, String requiredHash) {
		// We do not know anything, rather download it just to be safe,
		// however this should not happen.
		if(entry == null) {
			return true;
		}
		
		String hash = entry.getHash();
		// This happens when the library is not present locally,
		// that can mean either:
		// (a) the file is just not present and is required,
		// (b) the file is not required by the current OS
		if(hash == null) {
			Requirements requirements = entry.getRequirements();
			return requirements == Requirements.ANY
						|| requirements.equals(Requirements.CURRENT);
		}
		
		// File does exist, so just compare the hashes
		return !hash.equals(requiredHash);
	}
	
	public static final boolean compare(String currentVersion, String newestVersion) {
		return compare(Version.of(currentVersion), Version.of(newestVersion));
	}
	
	/** @since 00.02.07 */
	public static final boolean compare(Version currentVersion, Version newestVersion) {
		return currentVersion.compareTo(newestVersion) < 0;
	}
	
	/** @since 00.02.08 */
	public static final Updater ofRemoteFiles(RemoteConfiguration cfgRemote, String remoteDirURL, Path localDir,
			int timeout, FileChecker checker, FileDownloader downloader, Collection<Path> updatedPaths) {
		return new OfRemoteFiles(cfgRemote, remoteDirURL, NIO.localPath(), timeout, checker,
			(String webPath, Path entryPath) -> {
				Path path = localDir.resolve(entryPath);
				Request request = Request.of(Net.uri(webPath)).GET();
				NIO.createDir(path.getParent()); // Ensure parent directory
				downloader.start(request, path, DownloadConfiguration.ofDefault());
				return path;
			},
			(Path entryPath, String webDir) -> urlConcat(webDir, localDir.relativize(entryPath).toString().replace('\\', '/')),
			(Path entryPath) -> localDir.relativize(entryPath),
			(FileCheckerEntry locEntry, FileCheckerEntry entry) -> shouldDownloadEntry(locEntry, entry.getHash()),
			updatedPaths);
	}
	
	/** @since 00.02.08 */
	public static final Updater ofRemoteFiles(RemoteConfiguration cfgRemote, String remoteDirURL, Path dir, int timeout,
			FileChecker checker, CheckedBiFunction<String, Path, Path> callback,
			BiCallback<Path, String, String> urlResolver, CheckedCallback<Path, Path> entryPathFixer,
			BiPredicate<FileCheckerEntry, FileCheckerEntry> shouldDownloadPredicate, Collection<Path> updatedPaths) {
		return new OfRemoteFiles(cfgRemote, remoteDirURL, dir, timeout, checker, callback, urlResolver, entryPathFixer,
			shouldDownloadPredicate, updatedPaths);
	}
	
	/** @since 00.02.08 */
	public static final Updater ofLibraries(String baseURL, Path dir, int timeout, FileChecker checker,
			FileDownloader downloader, Collection<Path> updatedPaths) throws Exception {
		RemoteConfiguration cfg = RemoteConfiguration.from(stream(urlConcat(baseURL, NAME_CONFIG), timeout));
		return ofRemoteFiles(cfg, baseURL, dir, timeout, checker, downloader, updatedPaths);
	}
	
	/** @since 00.02.08 */
	public static final Updater ofResources(String baseURL, Path dir, int timeout, FileChecker checker,
			CheckedBiFunction<String, Path, Path> callback, BiCallback<Path, String, String> urlResolver,
			CheckedCallback<Path, Path> entryPathFixer,
			BiPredicate<FileCheckerEntry, FileCheckerEntry> shouldDownloadPredicate,
			Collection<Path> updatedPaths) throws Exception {
		RemoteConfiguration cfg = RemoteConfiguration.from(stream(urlConcat(baseURL, NAME_CONFIG), timeout));
		return ofRemoteFiles(cfg, baseURL, dir, timeout, checker, callback, urlResolver, entryPathFixer,
			shouldDownloadPredicate, updatedPaths);
	}
	
	/** @since 00.02.08 */
	protected final <V> void call(Event<CheckEvent, V> event) {
		eventRegistry.call(event, null);
	}
	
	/** @since 00.02.08 */
	protected final <V> void call(Event<CheckEvent, V> event, V value) {
		eventRegistry.call(event, value);
	}
	
	/** @since 00.02.08 */
	public abstract boolean check() throws Exception;
	
	@Override
	public <V> void addEventListener(Event<? extends CheckEvent, V> event, Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	@Override
	public <V> void removeEventListener(Event<? extends CheckEvent, V> event, Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
	
	/** @since 00.02.08 */
	private static final class OfRemoteFiles extends Updater {
		
		private final RemoteConfiguration cfgRemote;
		private final String remoteDirURL;
		private final Path dir;
		private final int timeout;
		private final FileChecker checker;
		private final CheckedBiFunction<String, Path, Path> callback;
		private final BiCallback<Path, String, String> urlResolver;
		private final CheckedCallback<Path, Path> entryPathFixer;
		private final BiPredicate<FileCheckerEntry, FileCheckerEntry> shouldDownloadPredicate;
		private final Collection<Path> updatedPaths;
		
		private OfRemoteFiles(RemoteConfiguration cfgRemote, String remoteDirURL, Path dir, int timeout,
				FileChecker checker, CheckedBiFunction<String, Path, Path> callback,
				BiCallback<Path, String, String> urlResolver, CheckedCallback<Path, Path> entryPathFixer,
		        BiPredicate<FileCheckerEntry, FileCheckerEntry> shouldDownloadPredicate,
		        Collection<Path> updatedPaths) {
			this.cfgRemote = cfgRemote;
			this.remoteDirURL = remoteDirURL;
			this.dir = dir;
			this.timeout = timeout;
			this.checker = checker;
			this.callback = callback;
			this.urlResolver = urlResolver;
			this.entryPathFixer = entryPathFixer;
			this.shouldDownloadPredicate = shouldDownloadPredicate;
			this.updatedPaths = updatedPaths;
		}
		
		@Override
		public boolean check() throws Exception {
			call(CheckEvent.BEGIN);
			
			boolean filesChanged = false;
			String webDir = urlConcat(remoteDirURL, cfgRemote.value(NAME_DATA));
			String lsPath = urlConcat(remoteDirURL, cfgRemote.value(NAME_LIST));
			FileChecker checkerWeb = FileChecker.parse(dir, content(lsPath, timeout));
			
			for(FileCheckerEntry entry : checkerWeb.entries()) {
				String webPath = urlResolver.call(entry.getPath(), webDir);
				Path entryPath = entryPathFixer.call(entry.getPath());
				FileCheckerEntry locEntry = checker.getEntry(entryPath);
				
				call(CheckEvent.COMPARE, entryPath.getFileName().toString());
				
				// Check whether to download the file
				if(shouldDownloadPredicate == null
						|| shouldDownloadPredicate.test(locEntry, entry)) {
					Path path = callback.apply(webPath, entryPath);
					
					if(path != null) {
						if(updatedPaths != null) {
							updatedPaths.add(path);
						}
						
						filesChanged = true;
					}
				}
			}
			
			call(CheckEvent.END);
			
			return filesChanged;
		}
	}
}