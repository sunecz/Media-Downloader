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

import sune.app.mediadown.Shared;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.FileDownloader;
import sune.app.mediadown.download.InputStreamChannelFactory;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.update.CheckListener;
import sune.app.mediadown.update.FileCheckListener;
import sune.app.mediadown.update.FileChecker;
import sune.app.mediadown.update.Requirements;
import sune.app.mediadown.update.Updater;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Property;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Web.GetRequest;

public final class Resources {
	
	private static final int TIMEOUT = 8000;
	private static final List<InternalResource> RESOURCES = new ArrayList<>();
	
	private static final Path PATH_RESOURCES = NIO.localPath("resources/binary");
	private static final Path PATH_ETC       = NIO.localPath("etc/binary");
	
	private static final String URL_BASE = "https://app.sune.tech/mediadown/res";
	
	// Forbid anyone to create an instance of this class
	private Resources() {
	}
	
	private static final CheckListener checkListener(StringReceiver receiver) {
		return new CheckListener() {
			
			@Override
			public void begin() {
				if((receiver != null))
					receiver.receive("Checking resources...");
			}
			
			@Override
			public void compare(String name) {
				if((receiver != null))
					receiver.receive(String.format("Checking resource %s...", name));
			}
			
			@Override
			public void end() {
				if((receiver != null))
					receiver.receive("Checking resources... done");
			}
			
			@Override
			public FileCheckListener fileCheckListener() {
				return new FileCheckListener() {
					
					@Override
					public void begin(Path dir) {
						if((receiver != null))
							receiver.receive(String.format("Checking %s...", dir.getFileName().toString()));
					}
					
					@Override
					public void update(Path file, String hash) {
						if((receiver != null))
							receiver.receive(String.format("Checking %s...%s", file.getFileName().toString(), hash != null ? " done" : ""));
					}
					
					@Override
					public void end(Path dir) {
						if((receiver != null))
							receiver.receive(String.format("Checking %s... done", dir.getFileName().toString()));
					}
					
					@Override
					public void error(Exception ex) {
						if((receiver != null))
							receiver.receive(String.format("Error: %s", ex.getMessage()));
					}
				};
			}
		};
	}
	
	/** @since 00.02.08 */
	private static final void download(URI uri, Path destination, StringReceiver receiver) throws Exception {
		Objects.requireNonNull(uri);
		Objects.requireNonNull(destination);
		
		// To be sure, delete the file first, so a fresh copy is downloaded.
		NIO.deleteFile(destination);
		
		FileDownloader downloader = new FileDownloader(new TrackerManager());
		downloader.setResponseChannelFactory(InputStreamChannelFactory.GZIP.ofDefault());
		
		final String fileName = destination.getFileName().toString();
		final long minTime = 500000000L; // 500ms
		final Property<Long> lastTime = new Property<>(0L);
		
		downloader.addEventListener(DownloadEvent.BEGIN, (d) -> {
			if(receiver != null) {
				receiver.receive(String.format("Downloading %s...", fileName));
			}
		});
		
		downloader.addEventListener(DownloadEvent.UPDATE, (pair) -> {
			if(receiver != null
					// Throttle to remove flickering
					&& System.nanoTime() - lastTime.getValue() >= minTime) {
				DownloadTracker tracker = (DownloadTracker) pair.b.tracker();
				long current = tracker.current();
				long total = tracker.total();
				double percent = current * 100.0 / total;
				receiver.receive(String.format(Locale.US, "Downloading %s... %.2f%%", fileName, percent));
				lastTime.setValue(System.nanoTime());
			}
		});
		
		downloader.addEventListener(DownloadEvent.END, (d) -> {
			if(receiver != null) {
				receiver.receive(String.format("Downloading %s... done", fileName));
			}
		});
		
		GetRequest request = new GetRequest(Utils.url(uri), Shared.USER_AGENT);
		downloader.start(request, destination, DownloadConfiguration.ofDefault());
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
		if(!NIO.exists(PATH_RESOURCES)) {
			NIO.createDir(PATH_RESOURCES);
		}
		
		Updater.checkResources(URL_BASE, PATH_RESOURCES, TIMEOUT,
			checkListener(receiver), localFileChecker(predicateComputeHash),
			(url, file) -> download(Utils.uri(url), file, receiver),
			(file, webDir) -> Utils.urlConcat(webDir, NIO.localPath().relativize(file).toString().replace('\\', '/')),
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
		
		// Make sure all the binaries are executable (Unix systems)
		if(!OSUtils.isWindows()) {
			for(InternalResource resource : localResources()) {
				NIO.chmod(PATH_RESOURCES.resolve(OSUtils.getExecutableName(resource.name())), 7, 7, 7);
			}
		}
	}
	
	/** @since 00.02.07 */
	public static final FileChecker etcFileChecker(String dirName, Predicate<Path> predicateComputeHash) {
		Path dir = PATH_ETC.resolve(dirName);
		FileChecker checker = new FileChecker.PrefixedFileChecker(dir.getParent(), null, dir);
		
		for(InternalResource resource : RESOURCES) {
			checker.addEntry(dir.resolve(resource.os())
			                    .resolve(resource.app())
			                    .resolve(resource.version())
			                    .resolve(OSUtils.getExecutableName(resource.name())),
			                 requirements(resource.os()),
			                 resource.version());
		}
		
		return checker.generate((path) -> true, false, predicateComputeHash) ? checker : null;
	}
	
	/** @since 00.02.07 */
	public static final FileChecker localFileChecker(Predicate<Path> predicateComputeHash) {
		FileChecker checker = new FileChecker.PrefixedFileChecker(PATH_RESOURCES, null, PATH_RESOURCES);
		
		for(InternalResource resource : localResources()) {
			checker.addEntry(PATH_RESOURCES.resolve(OSUtils.getExecutableName(resource.name())),
			                 requirements(resource.os()),
			                 resource.version());
		}
		
		return checker.generate((path) -> true, true, predicateComputeHash) ? checker : null;
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