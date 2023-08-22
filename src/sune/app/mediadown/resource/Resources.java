package sune.app.mediadown.resource;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.FileDownloader;
import sune.app.mediadown.download.InputStreamFactory;
import sune.app.mediadown.event.CheckEvent;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.FileCheckEvent;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.update.FileChecker;
import sune.app.mediadown.update.Requirements;
import sune.app.mediadown.update.Updater;
import sune.app.mediadown.update.Version;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Ref;
import sune.app.mediadown.util.Utils;

public final class Resources {
	
	private static final int TIMEOUT = 8000;
	private static final List<InternalResource> RESOURCES = new ArrayList<>();
	
	private static final Path PATH_RESOURCES = NIO.localPath("resources/binary");
	private static final Path PATH_ETC       = NIO.localPath("etc/binary");
	
	private static final String URL_BASE = "https://app.sune.tech/mediadown/res";
	
	// Forbid anyone to create an instance of this class
	private Resources() {
	}
	
	/** @since 00.02.08 */
	private static final Path download(URI uri, Path destination, StringReceiver receiver) throws Exception {
		Objects.requireNonNull(uri);
		Objects.requireNonNull(destination);
		
		// To be sure, delete the file first, so a fresh copy is downloaded.
		NIO.deleteFile(destination);
		
		FileDownloader downloader = new FileDownloader(new TrackerManager());
		downloader.setResponseStreamFactory(InputStreamFactory.GZIP.ofDefault());
		
		final String fileName = destination.getFileName().toString();
		final long minTime = 500000000L; // 500ms
		final Ref.Mutable<Long> lastTime = new Ref.Mutable<>(0L);
		
		downloader.addEventListener(DownloadEvent.BEGIN, (context) -> {
			if(receiver != null) {
				receiver.receive(String.format("Downloading %s...", fileName));
			}
		});
		
		downloader.addEventListener(DownloadEvent.UPDATE, (context) -> {
			if(receiver != null
					// Throttle to remove flickering
					&& System.nanoTime() - lastTime.get() >= minTime) {
				DownloadTracker tracker = (DownloadTracker) context.trackerManager().tracker();
				long current = tracker.current();
				long total = tracker.total();
				double percent = current * 100.0 / total;
				receiver.receive(String.format(Locale.US, "Downloading %s... %.2f%%", fileName, percent));
				lastTime.set(System.nanoTime());
			}
		});
		
		downloader.addEventListener(DownloadEvent.END, (context) -> {
			if(receiver != null) {
				receiver.receive(String.format("Downloading %s... done", fileName));
			}
		});
		
		Request request = Request.of(uri).GET();
		downloader.start(request, destination, DownloadConfiguration.ofDefault());
		
		return destination;
	}
	
	private static final Requirements requirements(String os) {
		Pair<String, String> pair = OSUtils.parse(os);
		return Requirements.create(pair.a, pair.b);
	}
	
	private static final boolean resourceExists(Path path) {
		return NIO.exists(PATH_RESOURCES.resolve(path.getFileName()));
	}
	
	public static final List<InternalResource> localResources() {
		return RESOURCES.stream()
			.filter((resource) -> requirements(resource.os()).equals(Requirements.CURRENT))
			.collect(Collectors.toList());
	}
	
	public static final void add(String name, String app, String version, String os) {
		RESOURCES.add(new InternalResource(name, app, version, os));
	}
	
	/** @since 00.02.07 */
	public static final void ensureResources(StringReceiver receiver, Predicate<Path> predicateComputeHash,
			Collection<Path> updatedPaths) throws Exception {
		NIO.createDir(PATH_RESOURCES);
		
		FileChecker checker = localFileChecker(predicateComputeHash);
		Updater updater = Updater.ofResources(URL_BASE, PATH_RESOURCES, TIMEOUT, checker,
			(url, file) -> download(Net.uri(url), file, receiver),
			(file, webDir) -> Net.uriConcat(webDir, NIO.localPath().relativize(file).toString().replace('\\', '/')),
			(file) -> PATH_RESOURCES.resolve(file.getFileName()),
			(entryLoc, entryWeb) -> {
				if((entryLoc == null)) return false;
				Requirements requirements = entryWeb.getRequirements();
				return (requirements == Requirements.ANY
							|| requirements.equals(Requirements.CURRENT))
						&& (entryLoc.getVersion().equals(entryWeb.getVersion())) // Version check
						&& ((predicateComputeHash.test(entryLoc.getPath()) // Muse be called before getHash()
								&& !entryLoc.getHash().equals(entryWeb.getHash()))
							|| !resourceExists(entryLoc.getPath()));
			}, updatedPaths);
		
		updater.addEventListener(CheckEvent.BEGIN, (v) -> {
			if(receiver != null) {
				receiver.receive("Checking resources...");
			}
		});
		
		updater.addEventListener(CheckEvent.END, (v) -> {
			if(receiver != null) {
				receiver.receive("Checking resources... done");
			}
		});
		
		updater.addEventListener(CheckEvent.COMPARE, (name) -> {
			if(receiver != null) {
				receiver.receive(String.format("Checking resource %s...", name));
			}
		});
		
		checker.addEventListener(FileCheckEvent.BEGIN, (path) -> {
			if(receiver != null) {
				receiver.receive(String.format("Checking %s...", path.getFileName().toString()));
			}
		});
		
		checker.addEventListener(FileCheckEvent.END, (path) -> {
			if(receiver != null) {
				receiver.receive(String.format("Checking %s... done", path.getFileName().toString()));
			}
		});
		
		checker.addEventListener(FileCheckEvent.UPDATE, (pair) -> {
			if(receiver != null) {
				receiver.receive(String.format(
					"Checking %s...%s",
					pair.a.getFileName().toString(),
					pair.b != null ? " done" : ""
				));
			}
		});
		
		checker.addEventListener(FileCheckEvent.ERROR, (ex) -> {
			if(receiver != null) {
				receiver.receive(String.format("Error: %s", ex.getMessage()));
			}
		});
		
		if(updater.check()) {
			// Make sure all the binaries are executable (Unix systems)
			if(!OSUtils.isWindows()) {
				for(InternalResource resource : localResources()) {
					NIO.chmod(PATH_RESOURCES.resolve(OSUtils.getExecutableName(resource.name())), 7, 7, 7);
				}
			}
		}
	}
	
	/** @since 00.02.07 */
	public static final FileChecker etcFileChecker(String dirName, Predicate<Path> predicateComputeHash)
			throws Exception {
		Path dir = PATH_ETC.resolve(dirName);
		FileChecker checker = new FileChecker.PrefixedFileChecker(dir.getParent(), dir);
		
		for(InternalResource resource : RESOURCES) {
			checker.addEntry(dir.resolve(resource.os())
			                    .resolve(resource.app())
			                    .resolve(resource.version())
			                    .resolve(OSUtils.getExecutableName(resource.name())),
			                 requirements(resource.os()),
			                 resource.version());
		}
		
		checker.generate((path) -> true, false, predicateComputeHash);
		return checker;
	}
	
	/** @since 00.02.07 */
	public static final FileChecker localFileChecker(Predicate<Path> predicateComputeHash) throws Exception {
		FileChecker checker = new FileChecker.PrefixedFileChecker(PATH_RESOURCES, PATH_RESOURCES);
		
		for(InternalResource resource : localResources()) {
			checker.addEntry(PATH_RESOURCES.resolve(OSUtils.getExecutableName(resource.name())),
			                 requirements(resource.os()),
			                 resource.version());
		}
		
		checker.generate((path) -> true, true, predicateComputeHash);
		return checker;
	}
	
	/** @since 00.02.09 */
	public static final URI baseUri(String name, Version version, Requirements requirements) {
		return Net.uri(Utils.format(
			"https://app.sune.tech/mediadown/res/%{name}s/%{version}s/%{os}s/",
			"name", name,
			"version", version.toString(),
			"os", requirements.toCompactString()
		));
	}
	
	public static interface StringReceiver {
		
		void receive(String text);
	}
	
	public static final class InternalResource {
		
		private final String name;
		private final String app;
		private final String version;
		private final String os;
		
		public InternalResource(String name, String app, String version, String os) {
			this.name = checkString(name);
			this.app = checkString(app);
			this.version = checkString(version);
			this.os = checkString(os);
		}
		
		/** @since 00.02.07 */
		private static final String checkString(String string) {
			if(string == null || string.isEmpty())
				throw new IllegalArgumentException();
			
			return string;
		}
		
		/** @since 00.02.07 */
		public String name() {
			return name;
		}
		
		/** @since 00.02.07 */
		public String app() {
			return app;
		}
		
		/** @since 00.02.07 */
		public String version() {
			return version;
		}
		
		/** @since 00.02.07 */
		public String os() {
			return os;
		}
	}
}