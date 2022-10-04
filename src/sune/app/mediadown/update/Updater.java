package sune.app.mediadown.update;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.BiPredicate;

import javafx.util.Callback;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.FileDownloader;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.update.FileChecker.FileCheckerEntry;
import sune.app.mediadown.util.BiCallback;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.ThrowableBiConsumer;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Web.GetRequest;

public final class Updater {
	
	private static final String NAME_CONFIG = "config";
	
	// Forbid anyone to create an instance of this class
	private Updater() {
	}
	
	private static final InputStream stream(String url, int timeout) throws IOException {
		return Utils.urlStream(url, timeout); // Forward call
	}
	
	private static final String content(String url, int timeout) {
		return Utils.ignore(() -> Utils.streamToString(Utils.urlStream(url, timeout)), (String) null); // Forward call
	}
	
	private static final String urlConcat(String... strings) {
		return Utils.urlConcat(strings); // Forward call
	}
	
	public static final boolean compare(String currentVersion, String newestVersion) {
		return compare(Version.of(currentVersion), Version.of(newestVersion));
	}
	
	/** @since 00.02.07 */
	public static final boolean compare(Version currentVersion, Version newestVersion) {
		return currentVersion.compareTo(newestVersion) < 0;
	}
	
	private static final boolean shouldDownloadEntry(FileCheckerEntry entry, String requiredHash) {
		// We do not know anything, rather download it just to be safe,
		// however this should not happen.
		if((entry == null))
			return true;
		String hash = entry.getHash();
		// This happens when the library is not present locally,
		// that can mean either:
		// (a) the file is just not present and is required,
		// (b) the file is not required by the current OS
		if((hash == null)) {
			Requirements requirements = entry.getRequirements();
			return requirements == Requirements.ANY
						|| requirements.equals(Requirements.CURRENT);
		}
		// File does exist, so just compare the hashes
		return !hash.equals(requiredHash);
	}
	
	public static final boolean checkRemoteFiles(RemoteConfiguration cfgRemote, String remoteDirURL, Path localDir, int timeout,
			CheckListener listener, FileChecker checker, Collection<Path> updatedPaths) throws IOException {
		boolean filesChanged = false;
		String webDir = urlConcat(remoteDirURL, cfgRemote.value("data"));
		String lsPath = urlConcat(remoteDirURL, cfgRemote.value("list"));
		FileChecker checkerWeb = FileChecker.parse(NIO.localPath(), content(lsPath, timeout));
		FileChecker checkerLoc = checker;
		
		for(FileCheckerEntry entry : checkerWeb.entries()) {
			Path webPath = localDir.relativize(entry.getPath());
			String webName = webPath.toString().replace('\\', '/');
			String webHash = entry.getHash();
			FileCheckerEntry locEntry = checkerLoc.getEntry(webPath);
			
			// Notify the listener, if needed
			if(listener != null)
				listener.compare(webName);
			
			// Check whether to download the file
			if(shouldDownloadEntry(locEntry, webHash)) {
				Path path = localDir.resolve(webName);
				
				FileDownloader downloader = new FileDownloader(new TrackerManager());
				GetRequest request = new GetRequest(Utils.url(urlConcat(webDir, webName)));
				try {
					downloader.start(request, path, DownloadConfiguration.ofDefault());
				} catch(Exception ex) {
					// Cannot throw Exception, so just translate it
					// TODO: Allow throwing Exception
					throw new IOException(ex);
				}
				
				if(updatedPaths != null) {
					updatedPaths.add(path);
				}
				
				filesChanged = true;
			}
		}
		
		return filesChanged;
	}
	
	public static final boolean checkRemoteFiles(RemoteConfiguration cfgRemote, String remoteDirURL, Path dir, int timeout,
			CheckListener listener, FileChecker checker, ThrowableBiConsumer<String, Path> callback,
			BiCallback<Path, String, String> urlResolver, Callback<Path, Path> entryPathFixer,
			BiPredicate<FileCheckerEntry, FileCheckerEntry> shouldDownloadPredicate, Collection<Path> updatedPaths)
			throws Exception {
		boolean filesChanged = false;
		String webDir = urlConcat(remoteDirURL, cfgRemote.value("data"));
		String lsPath = urlConcat(remoteDirURL, cfgRemote.value("list"));
		FileChecker checkerWeb = FileChecker.parse(dir, content(lsPath, timeout));
		
		for(FileCheckerEntry entry : checkerWeb.entries()) {
			String webPath = urlResolver.call(entry.getPath(), webDir);
			Path entryPath = entryPathFixer.call(entry.getPath());
			FileCheckerEntry locEntry = checker.getEntry(entryPath);
			
			// Notify the listener, if needed
			if(listener != null)
				listener.compare(entry.getPath().getFileName().toString());
			
			// Check whether to download the file
			if(shouldDownloadPredicate == null
					|| shouldDownloadPredicate.test(locEntry, entry)) {
				callback.accept(webPath, entryPath);
				
				if(updatedPaths != null) {
					updatedPaths.add(entryPath);
				}
				
				filesChanged = true;
			}
		}
		
		return filesChanged;
	}
	
	public static final boolean checkLibraries(String baseURL, Path dir, int timeout, CheckListener listener,
			FileChecker checker, Collection<Path> updatedPaths) throws IOException {
		RemoteConfiguration cfg = RemoteConfiguration.from(stream(urlConcat(baseURL, NAME_CONFIG), timeout));
		return checkRemoteFiles(cfg, baseURL, dir, timeout, listener, checker, updatedPaths);
	}
	
	public static final boolean checkResources(String baseURL, Path dir, int timeout, CheckListener listener,
			FileChecker checker, ThrowableBiConsumer<String, Path> callback,
			BiCallback<Path, String, String> urlResolver, Callback<Path, Path> entryPathFixer,
			BiPredicate<FileCheckerEntry, FileCheckerEntry> shouldDownloadPredicate, Collection<Path> updatedPaths)
			throws Exception {
		RemoteConfiguration cfg = RemoteConfiguration.from(stream(urlConcat(baseURL, NAME_CONFIG), timeout));
		return checkRemoteFiles(cfg, baseURL, dir, timeout, listener, checker, callback,
		                        urlResolver, entryPathFixer, shouldDownloadPredicate, updatedPaths);
	}
}