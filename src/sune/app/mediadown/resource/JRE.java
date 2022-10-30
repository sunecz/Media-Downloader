package sune.app.mediadown.resource;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import sune.app.mediadown.Shared;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.FileDownloader;
import sune.app.mediadown.download.InputStreamChannelFactory;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.resource.JRE.JREEvent;
import sune.app.mediadown.update.CheckListener;
import sune.app.mediadown.update.FileCheckListener;
import sune.app.mediadown.update.FileChecker;
import sune.app.mediadown.update.Requirements;
import sune.app.mediadown.update.Updater;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.PathSystem;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Web.GetRequest;

/** @since 00.02.02 */
public final class JRE implements EventBindable<JREEvent> {
	
	private static final Class<?> CLAZZ = JRE.class;
	private static final int TIMEOUT = 8000;
	
	private static final List<Pair<String, String>> supportedOS = Utils.toList(
		new Pair<>("win", "64"),
		new Pair<>("unx", "64"),
		new Pair<>("mac", "64")
	);
	
	private final EventRegistry<JREEvent> eventRegistry = new EventRegistry<>();
	
	private JRE() {
	}
	
	public static final JRE newInstance() {
		return new JRE();
	}
	
	private final void generateHashList(FileChecker checker, Path output, boolean checkRequirements)
			throws IOException {
		if(!checker.generate((path) -> true, checkRequirements, (path) -> true)) {
			throw new IllegalStateException("Unable to generate hash list.");
		}
		// Save the list of entries to a file
		NIO.save(output, checker.toString());
	}
	
	private final FileChecker fileChecker(Path dir, String version, Requirements requirements) throws IOException {
		FileChecker checker = new FileChecker.PrefixedFileChecker(dir, null, dir);
		if(NIO.exists(dir)) {
			Files.walk(dir).forEach((file) -> {
				Path path = file.toAbsolutePath();
				if(Files.isRegularFile(path)) {
					checker.addEntry(path, requirements, version);
				}
			});
		}
		return checker;
	}
	
	private final void generateHashList(String osName, String osArch, String version) throws IOException {
		Requirements requirements = Requirements.create(osName, osArch);
		String osInfo = requirements.toString().replace(";", "");
		String relativePath = "etc/jre/version/" + osInfo + "/" + version;
		Path currentDir = PathSystem.getPath(CLAZZ, "");
		FileChecker checker = fileChecker(currentDir.resolve(relativePath), version, requirements);
		generateHashList(checker, currentDir.resolve("jre." + osInfo + ".sha1"), false);
	}
	
	public final void generateHashLists(String version) throws IOException {
		for(Pair<String, String> os : supportedOS) {
			generateHashList(os.a, os.b, version);
		}
	}
	
	/** @since 00.02.07 */
	public final boolean check(Path dir, Path output, Requirements requirements, String version, Set<Path> visitedFiles,
			Predicate<Path> predicateComputeHash, Collection<Path> updatedPaths) throws Exception {
		String osInfo = requirements.toString().replace(";", "");
		String baseURL = "https://app.sune.tech/mediadown/jre/" + osInfo + "/" + version;
		FileChecker fileChecker = fileChecker(dir, version, requirements);
		fileChecker.generate((file) -> true, false, predicateComputeHash);
		ResourceChecker checker = new ResourceChecker(eventRegistry);
		return checker.check(baseURL, dir, output, fileChecker, true, true, visitedFiles, updatedPaths);
	}
	
	@Override
	public <V> void addEventListener(Event<? extends JREEvent, V> event, Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	@Override
	public <V> void removeEventListener(Event<? extends JREEvent, V> event, Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
	
	public static class EventContext<C> {
		
		private final C context;
		
		public EventContext(C context) {
			this.context = context;
		}
		
		public C context() {
			return context;
		}
	}
	
	public static final class DownloadEventContext<C> extends EventContext<C> {
		
		/** @since 00.02.08 */
		private final URI uri;
		private final Path path;
		private final DownloadTracker tracker;
		
		public DownloadEventContext(C context, URI uri, Path path, DownloadTracker tracker) {
			super(context);
			this.uri = Objects.requireNonNull(uri);
			this.path = Objects.requireNonNull(path);
			this.tracker = Objects.requireNonNull(tracker);
		}
		
		/** @since 00.02.08 */
		public URI uri() {
			return uri;
		}
		
		public Path path() {
			return path;
		}
		
		public DownloadTracker tracker() {
			return tracker;
		}
	}
	
	public static final class CheckEventContext<C> extends EventContext<C> {
		
		private final String name;
		
		public CheckEventContext(C context, String name) {
			super(context);
			this.name = name;
		}
		
		public String name() {
			return name;
		}
	}
	
	public static final class ErrorEventContext<C> extends EventContext<C> {
		
		private final Exception exception;
		
		public ErrorEventContext(C context, Exception exception) {
			super(context);
			this.exception = exception;
		}
		
		public Exception exception() {
			return exception;
		}
	}
	
	public static final class JREEvent implements EventType {
		
		public static final Event<JREEvent, CheckEventContext<JRE>>    CHECK           = new Event<>();
		public static final Event<JREEvent, DownloadEventContext<JRE>> DOWNLOAD_BEGIN  = new Event<>();
		public static final Event<JREEvent, DownloadEventContext<JRE>> DOWNLOAD_UPDATE = new Event<>();
		public static final Event<JREEvent, DownloadEventContext<JRE>> DOWNLOAD_END    = new Event<>();
		public static final Event<JREEvent, ErrorEventContext<JRE>>    ERROR           = new Event<>();
		
		private static Event<JREEvent, ?>[] values;
		
		// Forbid anyone to create an instance of this class
		private JREEvent() {
		}
		
		public static final Event<JREEvent, ?>[] values() {
			if(values == null) {
				values = Utils.array(
					CHECK,
					DOWNLOAD_BEGIN, DOWNLOAD_UPDATE, DOWNLOAD_END,
					ERROR
				);
			}
			
			return values;
		}
	}
	
	private final class ResourceChecker {
		
		private final TrackerManager manager = new TrackerManager();
		private final EventRegistry<JREEvent> eventRegistry;
		
		public ResourceChecker(EventRegistry<JREEvent> eventRegistry) {
			this.eventRegistry = eventRegistry;
		}
		
		/** @since 00.02.08 */
		private final void download(URI uri, Path destination) throws Exception {
			Objects.requireNonNull(uri);
			Objects.requireNonNull(destination);
			
			// To be sure, delete the file first, so a fresh copy is downloaded.
			NIO.deleteFile(destination);
			NIO.createDir(destination.getParent());
			
			FileDownloader downloader = new FileDownloader(manager);
			downloader.setResponseChannelFactory(InputStreamChannelFactory.GZIP.ofDefault());
			
			DownloadTracker tracker = new DownloadTracker();
			downloader.setTracker(tracker);
			
			DownloadEventContext<JRE> context
				= new DownloadEventContext<>(JRE.this, uri, destination, tracker);
			
			downloader.addEventListener(DownloadEvent.BEGIN, (d) -> {
				eventRegistry.call(JREEvent.DOWNLOAD_BEGIN, context);
			});
			
			downloader.addEventListener(DownloadEvent.UPDATE, (pair) -> {
				eventRegistry.call(JREEvent.DOWNLOAD_UPDATE, context);
			});
			
			downloader.addEventListener(DownloadEvent.END, (d) -> {
				eventRegistry.call(JREEvent.DOWNLOAD_END, context);
			});
			
			GetRequest request = new GetRequest(Utils.url(uri), Shared.USER_AGENT);
			downloader.start(request, destination, DownloadConfiguration.ofDefault());
		}
		
		private final Path ensurePathInDirectory(Path file, Path dir, boolean resolve) {
			if(file.isAbsolute()) {
				file = dir.relativize(file);
			}
			List<Path> paths = new ArrayList<>();
			int skip = 0;
			for(Path part : file) {
				if(skip > 0) { --skip; continue; }
				String name = part.getFileName().toString();
				if(name.equals("."))  { continue; }
				if(name.equals("..")) { ++skip; continue; }
				paths.add(part);
			}
			Path path = file;
			if(!paths.isEmpty()) {
				path = paths.stream().reduce((a, b) -> a.resolve(b)).get();
				if(resolve) path = dir.resolve(path);
			}
			return path;
		}
		
		/** @since 00.02.05 */
		private final boolean check(String baseURL, Path baseDirOld, Path baseDirNew, FileChecker checker, boolean checkIntegrity,
				boolean allowNullLocalEntry, Set<Path> visitedFiles, Collection<Path> updatedPaths) throws Exception {
			if(!NIO.exists(baseDirNew)) NIO.createDir(baseDirNew);
			Path localPath = PathSystem.getPath(CLAZZ, "");
			return Updater.checkResources(baseURL, baseDirOld, TIMEOUT, checkListener(), checker,
				(url, file) -> download(Utils.uri(url), ensurePathInDirectory(baseDirOld.relativize(file), baseDirNew, true)),
				(file, webDir) -> Utils.urlConcat(webDir, ensurePathInDirectory(localPath.relativize(file), baseDirOld, false).toString().replace('\\', '/')),
				(file) -> ensurePathInDirectory(localPath.relativize(file), baseDirOld, true),
				(entryLoc, entryWeb) -> {
					visitedFiles.add(ensurePathInDirectory(localPath.relativize(entryWeb.getPath()), baseDirOld, true));
					if(entryLoc == null) return allowNullLocalEntry;
					Requirements requirements = entryWeb.getRequirements();
					return (requirements == Requirements.ANY
								|| requirements.equals(Requirements.CURRENT))
							&& (entryLoc.getVersion().equals(entryWeb.getVersion())) // Version check
							&& ((checkIntegrity // Muse be called before getHash()
									&& !entryLoc.getHash().equals(entryWeb.getHash()))
								|| !NIO.exists(ensurePathInDirectory(entryLoc.getPath(), baseDirOld, true)));
				}, updatedPaths);
		}
		
		private final CheckListener checkListener() {
			return new CheckListener() {
				
				@Override
				public void compare(String name) {
					eventRegistry.call(JREEvent.CHECK, new CheckEventContext<>(JRE.this, name));
				}
				
				@Override public void begin() {}
				@Override public void end() {}
				@Override public FileCheckListener fileCheckListener() { return null; /* Not used */ }
			};
		}
	}
}