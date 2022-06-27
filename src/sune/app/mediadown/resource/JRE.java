package sune.app.mediadown.resource;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.IEventType;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.update.CheckListener;
import sune.app.mediadown.update.FileCheckListener;
import sune.app.mediadown.update.FileChecker;
import sune.app.mediadown.update.FileChecker.Requirements;
import sune.app.mediadown.update.FileDownloadListener;
import sune.app.mediadown.update.Updater;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.PathSystem;
import sune.app.mediadown.util.UserAgent;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Web;
import sune.app.mediadown.util.Web.HeadRequest;

/** @since 00.02.02 */
public final class JRE {
	
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
		if(!checker.generate((path) -> true, checkRequirements, true)) {
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
	
	/** @since 00.02.05 */
	public final boolean check(Path dir, Path output, Requirements requirements, String version, Set<Path> visitedFiles)
			throws Exception {
		String osInfo = requirements.toString().replace(";", "");
		String baseURL = "https://app.sune.tech/mediadown/jre/" + osInfo + "/" + version;
		FileChecker fileChecker = fileChecker(dir, version, requirements);
		fileChecker.generate((file) -> true, false, true);
		ResourceChecker checker = new ResourceChecker(eventRegistry);
		return checker.check(baseURL, dir, output, fileChecker, true, true, visitedFiles);
	}
	
	public final <T> void addEventListener(EventType<JREEvent, T> type, Listener<T> listener) {
		eventRegistry.add(type, listener);
	}
	
	public final <T> void removeEventListener(EventType<JREEvent, T> type, Listener<T> listener) {
		eventRegistry.remove(type, listener);
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
		
		private final URL url;
		private final Path path;
		private final DownloadTracker tracker;

		public DownloadEventContext(C context, URL url, Path path, DownloadTracker tracker) {
			super(context);
			this.url = url;
			this.path = path;
			this.tracker = tracker;
		}
		
		public URL url() {
			return url;
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
	
	public static final class JREEvent implements IEventType {
		
		public static final EventType<JREEvent, CheckEventContext<JRE>>    CHECK           = new EventType<>();
		public static final EventType<JREEvent, DownloadEventContext<JRE>> DOWNLOAD_BEGIN  = new EventType<>();
		public static final EventType<JREEvent, DownloadEventContext<JRE>> DOWNLOAD_UPDATE = new EventType<>();
		public static final EventType<JREEvent, DownloadEventContext<JRE>> DOWNLOAD_END    = new EventType<>();
		public static final EventType<JREEvent, ErrorEventContext<JRE>>    ERROR           = new EventType<>();
		
		private static final EventType<JREEvent, ?>[] VALUES = Utils.array(
			CHECK,
			DOWNLOAD_BEGIN, DOWNLOAD_UPDATE, DOWNLOAD_END,
			ERROR
		);
		
		public static final EventType<JREEvent, ?>[] values() {
			return VALUES;
		}
		
		// Forbid anyone to create an instance of this class
		private JREEvent() {
		}
	}
	
	private final class ResourceChecker {
		
		private final TrackerManager manager = new TrackerManager();
		private final EventRegistry<JREEvent> eventRegistry;
		
		public ResourceChecker(EventRegistry<JREEvent> eventRegistry) {
			this.eventRegistry = eventRegistry;
		}
		
		private final ReadableByteChannel channel(DownloadEventContext<JRE> context, URL url) throws Exception {
			long total = Web.size(new HeadRequest(url, UserAgent.CHROME));
			return new DownloadByteChannel(context, url.openStream(), total);
		}
		
		private final void download(URL url, Path dest) throws Exception {
			if(url == null || dest == null)
				throw new IllegalArgumentException();
			DownloadTracker tracker = new DownloadTracker(0L, false);
			tracker.setTrackerManager(manager);
			DownloadEventContext<JRE> context
				= new DownloadEventContext<>(JRE.this, url, dest, tracker);
			// To be sure, delete the file first, so a fresh copy is downloaded.
			NIO.deleteFile(dest);
			NIO.createDir(dest.getParent());
			try(ReadableByteChannel dbc = channel(context, url);
				FileChannel         fch = FileChannel.open(dest, CREATE, WRITE)) {
				// Notify the listener, if needed
				eventRegistry.call(JREEvent.DOWNLOAD_BEGIN, context);
				// Actually download the file
				fch.transferFrom(dbc, 0L, Long.MAX_VALUE);
				// Notify the listener, if needed
				eventRegistry.call(JREEvent.DOWNLOAD_END, context);
			}
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
				boolean allowNullLocalEntry, Set<Path> visitedFiles) throws Exception {
			if(!NIO.exists(baseDirNew)) NIO.createDir(baseDirNew);
			Path localPath = PathSystem.getPath(CLAZZ, "");
			return Updater.checkResources(baseURL, baseDirOld, TIMEOUT, checkListener(), checker,
				(url, file) -> download(Utils.url(url), ensurePathInDirectory(baseDirOld.relativize(file), baseDirNew, true)),
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
				});
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
				@Override public FileDownloadListener fileDownloadListener() { return null; /* Not used */ }
			};
		}
		
		private final class DownloadByteChannel implements ReadableByteChannel {
			
			// The original channel
			private final ReadableByteChannel channel;
			private final DownloadEventContext<JRE> context;
			
			// Underlying input stream implementation
			private final class UIS extends InputStream {
				
				private final InputStream stream;
				
				public UIS(InputStream stream) {
					this.stream = stream;
				}
				
				@Override
				public int read() throws IOException {
					return stream.read();
				}
				
				@Override
				public int read(byte[] buf, int off, int len) throws IOException {
					// Call the underlying method
					int read = stream.read(buf, off, len);
					context.tracker().update(read);
					eventRegistry.call(JREEvent.DOWNLOAD_UPDATE, context);
					return read;
				}
			}
			
			public DownloadByteChannel(DownloadEventContext<JRE> context, InputStream stream, long total)
					throws IOException {
				this.channel = Channels.newChannel(new UIS(stream));
				this.context = context;
				this.context.tracker().updateTotal(total);
			}
			
			@Override
			public boolean isOpen() {
				return channel.isOpen();
			}
			
			@Override
			public void close() throws IOException {
				channel.close();
			}
			
			@Override
			public int read(ByteBuffer dst) throws IOException {
				return channel.read(dst);
			}
		}
	}
}