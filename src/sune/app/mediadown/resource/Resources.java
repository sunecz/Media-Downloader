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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import sune.app.mediadown.Shared;
import sune.app.mediadown.update.CheckListener;
import sune.app.mediadown.update.FileCheckListener;
import sune.app.mediadown.update.FileChecker;
import sune.app.mediadown.update.FileChecker.Requirements;
import sune.app.mediadown.update.FileDownloadListener;
import sune.app.mediadown.update.Updater;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Web;
import sune.app.mediadown.util.Web.HeadRequest;

public final class Resources {
	
	public static interface StringReceiver {
		
		void receive(String text);
	}
	
	public static interface ResourceDownloadListener {
		
		void begin();
		void progress(long current, long total);
		void end();
	}
	
	private static final class InternalResource {
		
		private final String name;
		private final String app;
		private final String version;
		private final String os;
		
		public InternalResource(String name, String app, String version, String os) {
			if((name == null || app == null || version == null || os == null ||
					name.isEmpty() || app.isEmpty() || version.isEmpty() || os.isEmpty()))
				throw new IllegalArgumentException();
			this.name = name;
			this.app = app;
			this.version = version;
			this.os = os;
		}
		
		public String getName() {
			return name;
		}
		
		public String getApp() {
			return app;
		}
		
		public String getVersion() {
			return version;
		}
		
		public String getOS() {
			return os;
		}
	}
	
	private static final int TIMEOUT = 8000;
	private static final List<InternalResource> RESOURCES = new ArrayList<>();
	
	private static final Path PATH_RESOURCES = NIO.localPath("resources/binary");
	private static final Path PATH_ETC       = NIO.localPath("etc/binary");
	
	private static final String URL_BASE = "https://app.sune.tech/mediadown/res";
	
	private static final ResourceDownloadListener downloadListener(String fileName, StringReceiver receiver) {
		return new ResourceDownloadListener() {
			
			private static final long minTime = 500000000L; // 500ms
			private long lastTime;
			
			@Override
			public void begin() {
				if((receiver != null))
					receiver.receive(String.format("Downloading %s...", fileName));
			}
			
			@Override
			public void progress(long current, long total) {
				// remove flickering
				if((System.nanoTime() - lastTime >= minTime)) {
					double percent = current / (double) total * 100.0;
					if((receiver != null))
						receiver.receive(String.format(Locale.US, "Downloading %s... %.2f%%", fileName, percent));
					lastTime = System.nanoTime();
				}
			}
			
			@Override
			public void end() {
				if((receiver != null))
					receiver.receive(String.format("Downloading %s... done", fileName));
			}
		};
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
			
			@Override
			public FileDownloadListener fileDownloadListener() {
				return null; // Not used
			}
		};
	}
	
	private static final class CompressedDownloadByteChannel implements ReadableByteChannel {
		
		private static final int DEFAULT_BUFFER_SIZE = 8192;
		
		// The original channel
		private final ReadableByteChannel channel;
		// Download progress information
		private final long total;
		private long current;
		// The listener to which pass the information
		private final ResourceDownloadListener listener;
		
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
				current += read;
				if((listener != null))
					listener.progress(current, total);
				return read;
			}
		}
		
		public CompressedDownloadByteChannel(InputStream stream, long total, ResourceDownloadListener listener)
				throws IOException {
			this.channel  = Channels.newChannel(new GZIPInputStream(new UIS(stream), DEFAULT_BUFFER_SIZE));
			this.total    = total;
			this.listener = listener;
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
	
	private static final ReadableByteChannel channel(URL url, ResourceDownloadListener listener)
			throws Exception {
		long total = Web.size(new HeadRequest(url, Shared.USER_AGENT));
		return new CompressedDownloadByteChannel(url.openStream(), total, listener);
	}
	
	private static final void download(URL url, Path dest, ResourceDownloadListener listener)
			throws Exception {
		if((url == null || dest == null))
			throw new IllegalArgumentException();
		// To be sure, delete the file first, so a fresh copy is downloaded.
		NIO.deleteFile(dest);
		try(ReadableByteChannel dbc = channel(url, listener);
			FileChannel         fch = FileChannel.open(dest, CREATE, WRITE)) {
			// Notify the listener, if needed
			if((listener != null))
				listener.begin();
			// Actually download the file
			fch.transferFrom(dbc, 0L, Long.MAX_VALUE);
			// Notify the listener, if needed
			if((listener != null))
				listener.end();
		}
	}
	
	private static final Requirements requirements(String os) {
		Pair<String, String> pair = OSUtils.parse(os);
		return Requirements.create(pair.a, pair.b);
	}
	
	private static final List<InternalResource> localResources() {
		return RESOURCES.stream()
			.filter((resource) -> requirements(resource.getOS()).equals(Requirements.CURRENT))
			.collect(Collectors.toList());
	}
	
	public static final void add(String name, String app, String version, String os) {
		RESOURCES.add(new InternalResource(name, app, version, os));
	}
	
	private static final boolean resourceExists(Path path) {
		return NIO.exists(PATH_RESOURCES.resolve(path.getFileName()));
	}
	
	public static final void ensureResources(StringReceiver receiver, boolean checkIntegrity)
			throws Exception {
		if(!NIO.exists(PATH_RESOURCES)) NIO.createDir(PATH_RESOURCES);
		Updater.checkResources(URL_BASE, PATH_RESOURCES, TIMEOUT,
			checkListener(receiver), localFileChecker(checkIntegrity),
			(url, file) -> download(Utils.url(url), file, downloadListener(file.getFileName().toString(), receiver)),
			(file, webDir) -> Utils.urlConcat(webDir, NIO.localPath().relativize(file).toString().replace('\\', '/')),
			(file) -> PATH_RESOURCES.resolve(file.getFileName()),
			(entryLoc, entryWeb) -> {
				if((entryLoc == null)) return false;
				Requirements requirements = entryWeb.getRequirements();
				return (requirements == Requirements.ANY
							|| requirements.equals(Requirements.CURRENT))
						&& (entryLoc.getVersion().equals(entryWeb.getVersion())) // Version check
						&& ((checkIntegrity // Muse be called before getHash()
								&& !entryLoc.getHash().equals(entryWeb.getHash()))
							|| !resourceExists(entryLoc.getPath()));
			});
		// Make sure all the binaries are executable (Unix systems)
		if(!OSUtils.isWindows()) {
			for(InternalResource resource : localResources()) {
				NIO.chmod(PATH_RESOURCES.resolve(resource.getName()), 7, 7, 7);
			}
		}
	}
	
	public static final FileChecker etcFileChecker(boolean computeHashes) {
		Path dir = PATH_ETC.resolve("original");
		FileChecker checker = new FileChecker.PrefixedFileChecker(dir.getParent(), null, dir);
		for(InternalResource resource : RESOURCES) {
			checker.addEntry(dir.resolve(resource.getOS())
			                    .resolve(resource.getApp())
			                    .resolve(resource.getVersion())
			                    .resolve(resource.getName()),
			                 requirements(resource.getOS()),
			                 resource.getVersion());
		}
		return checker.generate((path) -> true, false, computeHashes) ? checker : null;
	}
	
	public static final FileChecker localFileChecker(boolean computeHashes) {
		FileChecker checker = new FileChecker.PrefixedFileChecker(PATH_RESOURCES, null, PATH_RESOURCES);
		for(InternalResource resource : localResources()) {
			checker.addEntry(PATH_RESOURCES.resolve(resource.getName()),
			                 requirements(resource.getOS()),
			                 resource.getVersion());
		}
		return checker.generate((path) -> true, true, computeHashes) ? checker : null;
	}
}