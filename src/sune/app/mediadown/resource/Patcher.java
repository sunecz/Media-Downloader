package sune.app.mediadown.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import sune.app.mediadown.event.PatchEvent;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.net.Web.Response;
import sune.app.mediadown.resource.Patcher.ResourceList.Entry;
import sune.app.mediadown.update.Hash;
import sune.app.mediadown.update.Requirements;
import sune.app.mediadown.update.Version;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public final class Patcher implements EventBindable<EventType>, PatchContext {
	
	private final EventRegistry<EventType> eventRegistry = new EventRegistry<>();
	
	private final ResourceList localList;
	private final boolean allowNullLocalEntry;
	private final boolean checkIntegrity;
	private final boolean useCompressedStreams;
	
	private FileDownloader downloader;
	
	private volatile ResourceList.Entry entryRemote;
	private volatile ResourceList.Entry entryLocal;
	private volatile Exception exception;
	
	private Patcher(ResourceList localList, boolean allowNullLocalEntry, boolean checkIntegrity,
			boolean useCompressedStreams) {
		this.localList = Objects.requireNonNull(localList);
		this.allowNullLocalEntry = allowNullLocalEntry;
		this.checkIntegrity = checkIntegrity;
		this.useCompressedStreams = useCompressedStreams;
	}
	
	private final FileDownloader downloader() {
		if(downloader == null) {
			downloader = new FileDownloader(new TrackerManager());
			eventRegistry.bindAll(downloader, DownloadEvent.values());
			
			if(useCompressedStreams) {
				downloader.setResponseChannelFactory(InputStreamChannelFactory.GZIP.ofDefault());
			}
		}
		
		return downloader;
	}
	
	private final URI entryRemoteUri(ResourceList remoteList, ResourceList.Entry remote) {
		return remoteList.entryUri(remote);
	}
	
	private final Path entryLocalPath(ResourceList.Entry local, ResourceList.Entry remote) {
		return Path.of(localList.entryUri(local == null ? remote : local));
	}
	
	private final boolean canDownloadEntry(ResourceList.Entry remote, ResourceList.Entry local) {
		if(local == null) {
			// Entry does not exist, download it, if needed
			return !allowNullLocalEntry;
		}
		
		if(!remote.version().equals(local.version())) {
			// Not the version we want, no need to download it
			return false;
		}
		
		Requirements requirements = remote.requirements();
		
		if(requirements.equals(Requirements.ANY)
				&& requirements.equals(Requirements.CURRENT)) {
			// Not for the current OS, no need to download it
			return false;
		}
		
		Path fileLocal = entryLocalPath(local, null);
		
		if(!Files.exists(fileLocal)) {
			// File is required, download it
			return true;
		}
		
		if(checkIntegrity
				&& !remote.hash().equalsIgnoreCase(local.hash())) {
			// Hashes mismatch, download it
			return true;
		}
		
		// All seems good, no need to download it
		return false;
	}
	
	private final void downloadEntry(URI uri, Path file) throws Exception {
		if(Files.exists(file)) Files.delete(file);
		Files.createDirectories(file.getParent());
		FileDownloader downloader = downloader();
		downloader.start(Request.of(uri).GET(), file, DownloadConfiguration.ofDefault());
		((DownloadTracker) downloader.trackerManager().tracker()).reset();
	}
	
	private static final Builder ofDefault(ResourceList localList) {
		return builder(localList).checkIntegrity(true);
	}
	
	public static final Patcher of(ResourceList localList) {
		return ofDefault(localList).build();
	}
	
	public static final Patcher ofCompressed(ResourceList localList) {
		return ofDefault(localList).useCompressedStreams(true).build();
	}
	
	public static final Builder builder() {
		return new Builder();
	}
	
	public static final Builder builder(ResourceList localList) {
		return new Builder(localList);
	}
	
	public final List<ResourceList.Entry> patch(ResourceList remoteList) throws Exception {
		eventRegistry.call(PatchEvent.BEGIN, this);
		
		try {
			List<ResourceList.Entry> updated = new ArrayList<>();
			
			for(ResourceList.Entry remote : remoteList.entries()) {
				ResourceList.Entry local = localList.entry(remote.relativePath());
				
				entryRemote = remote;
				entryLocal = local;
				eventRegistry.call(PatchEvent.UPDATE, this);
				
				if(!canDownloadEntry(remote, local)) {
					continue;
				}
				
				downloadEntry(entryRemoteUri(remoteList, remote), entryLocalPath(local, remote));
				updated.add(remote);
			}
			
			return List.copyOf(updated);
		} catch(Exception ex) {
			exception = ex;
			eventRegistry.call(PatchEvent.ERROR, this);
			throw ex; // Rethrow
		} finally {
			eventRegistry.call(PatchEvent.END, this);
		}
	}
	
	@Override
	public <V> void addEventListener(Event<? extends EventType, V> event, Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	@Override
	public <V> void removeEventListener(Event<? extends EventType, V> event, Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
	
	@Override
	public Entry remoteEntry() {
		return entryRemote;
	}
	
	@Override
	public Entry localEntry() {
		return entryLocal;
	}
	
	@Override
	public Exception exception() {
		return exception;
	}
	
	public static final class ResourceList {
		
		private static final int CHAR_SEPARATOR = '|';
		
		private final URI uri;
		private final URI baseUri;
		private final Map<String, Entry> entries;
		
		private ResourceList(URI uri, URI baseUri, Map<String, Entry> entries) {
			this.uri = Objects.requireNonNull(uri);
			this.baseUri = Objects.requireNonNull(baseUri);
			this.entries = Objects.requireNonNull(entries);
		}
		
		private static final Map<String, Entry> entriesMap(List<Entry> entries) {
			return Objects.requireNonNull(entries).stream()
						.collect(Collectors.toUnmodifiableMap(Entry::relativePath, Function.identity()));
		}
		
		private static final ResourceList of(URI uri, URI baseUri, List<Entry> entries) {
			return new ResourceList(uri, baseUri, entriesMap(entries));
		}
		
		public static final ResourceList ofRemote(URI uri, URI baseUri, List<Entry> entries) {
			return of(uri, baseUri, entries);
		}
		
		public static final ResourceList ofRemote(Request request, URI baseUri) throws Exception {
			return parser().parse(request, baseUri);
		}
		
		public static final ResourceList ofRemote(URI uri, URI baseUri) throws Exception {
			return parser().parse(Request.of(uri).GET(), baseUri);
		}
		
		public static final ResourceList ofLocal(Path directory, Path rootDirectory, List<Entry> entries) throws Exception {
			return of(directory.toUri(), rootDirectory.toUri(), entries);
		}
		
		public static final ResourceList ofLocal(Path directory, Path rootDirectory, Version version) throws Exception {
			return generator().generate(directory, rootDirectory, version);
		}
		
		public static final Parser parser() {
			return new Parser();
		}
		
		public static final Generator generator() {
			return new Generator();
		}
		
		public Entry entry(String path) {
			return entries.get(path);
		}
		
		public URI entryUri(Entry entry) {
			return baseUri.resolve(entry.relativePath());
		}
		
		public URI uri() { return uri; }
		public URI baseUri() { return baseUri; }
		public List<Entry> entries() { return List.copyOf(entries.values()); }
		
		public static final class Entry {
			
			private final String hash;
			private final Requirements requirements;
			private final Version version;
			private final String relativePath;
			
			private Entry(String hash, Requirements requirements, Version version, String relativePath) {
				this.hash = Objects.requireNonNull(hash);
				this.requirements = Objects.requireNonNull(requirements);
				this.version = Objects.requireNonNull(version);
				this.relativePath = Objects.requireNonNull(relativePath);
			}
			
			public String hash() { return hash; }
			public Requirements requirements() { return requirements; }
			public Version version() { return version; }
			public String relativePath() { return relativePath; }
			
			@Override
			public String toString() {
				return String.format(
					"%s%c%s%c%s%c%s",
					hash,
					CHAR_SEPARATOR,
					requirements.toString(),
					CHAR_SEPARATOR,
					version,
					CHAR_SEPARATOR,
					relativePath
				);
			}
		}
		
		public static final class Parser {
			
			private Parser() {
			}
			
			private static final int next(String string, int offset, int separator) {
				int next; return (next = string.indexOf(separator, offset)) < 0 ? string.length() : next;
			}
			
			private static final String ensureContainedRelativePath(String path) {
				path = path.strip();
				
				if(path.startsWith("/")) {
					path = path.substring(1);
				}
				
				path = path.replace("./", "");
				path = path.replace("../", "");
				
				return path;
			}
			
			private final Entry parseLine(String line) {
				int offset = 0, len = line.length();
				
				String strHash = line.substring(offset, offset = next(line, offset + 1, CHAR_SEPARATOR));
				if(offset == len) return null; // Invalid line
				String strRequirements = line.substring(offset + 1, offset = next(line, offset + 1, CHAR_SEPARATOR));
				if(offset == len) return null; // Invalid line
				String strVersion = line.substring(offset + 1, offset = next(line, offset + 1, CHAR_SEPARATOR));
				if(offset == len) return null; // Invalid line
				String strPath = line.substring(offset + 1, offset = next(line, offset + 1, CHAR_SEPARATOR));
				if(offset <  len) return null; // Invalid line
				
				Requirements requirements = Requirements.parse(strRequirements);
				Version version = Version.of(strVersion);
				
				return new Entry(strHash, requirements, version, ensureContainedRelativePath(strPath));
			}
			
			public ResourceList parse(Request request, URI baseUri) throws Exception {
				try(Response.OfStream response = Web.requestStream(request)) {
					return parse(response.uri(), baseUri, response.stream());
				}
			}
			
			public ResourceList parse(URI uri, URI baseUri, InputStream stream) throws IOException {
				try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Shared.CHARSET))) {
					return parse(uri, baseUri, reader);
				}
			}
			
			public ResourceList parse(URI uri, URI baseUri, BufferedReader reader) throws IOException {
				Objects.requireNonNull(reader);
				List<Entry> entries = new ArrayList<>();
				
				for(String line; (line = reader.readLine()) != null;) {
					// Skip empty lines, do not stop
					if(line.isBlank()) {
						continue;
					}
					
					Entry entry = parseLine(line);
					
					// Skip invalid entries
					if(entry == null) {
						continue;
					}
					
					entries.add(entry);
				}
				
				return ResourceList.of(uri, baseUri, entries);
			}
		}
		
		public static final class Generator {
			
			private Generator() {
			}
			
			public ResourceList generate(Path directory, Path rootDirectory, Version version) throws IOException {
				directory = directory.toAbsolutePath();
				List<Entry> entries = new Walker(directory, rootDirectory, version).walk();
				return ResourceList.of(directory.toUri(), rootDirectory.toUri(), entries);
			}
			
			private static final class Walker {
				
				private final Path directory;
				private final Path rootDirectory;
				private final Version version;
				
				private Walker(Path directory, Path rootDirectory, Version version) {
					this.directory = Objects.requireNonNull(directory);
					this.rootDirectory = Objects.requireNonNull(rootDirectory);
					this.version = Objects.requireNonNull(version);
				}
				
				private final Entry createEntry(Path file) throws IOException {
					if(!Files.isRegularFile(file)) {
						return null;
					}
					
					String hash = Hash.sha1Checked(file).toLowerCase();
					String path = rootDirectory.relativize(file).toString().replace('\\', '/');
					
					return new Entry(hash, Requirements.CURRENT, version, path);
				}
				
				public List<Entry> walk() throws IOException {
					List<Entry> entries = new ArrayList<>();
					
					if(!Files.isDirectory(directory)) {
						return entries;
					}
					
					for(Path file : Utils.iterable(Files.walk(directory).iterator())) {
						Entry entry = createEntry(file.toAbsolutePath());
						
						// Skip invalid entries
						if(entry == null) {
							continue;
						}
						
						entries.add(entry);
					}
					
					return entries;
				}
			}
		}
	}
	
	public static final class Builder {
		
		private ResourceList localList;
		private boolean allowNullLocalEntry;
		private boolean checkIntegrity;
		private boolean useCompressedStreams;
		
		private Builder() {
		}
		
		private Builder(ResourceList localList) {
			this.localList = localList;
		}
		
		public Patcher build() {
			return new Patcher(localList, allowNullLocalEntry, checkIntegrity, useCompressedStreams);
		}
		
		public Builder localList(ResourceList localList) {
			this.localList = localList;
			return this;
		}
		
		public Builder allowNullLocalEntry(boolean allowNullLocalEntry) {
			this.allowNullLocalEntry = allowNullLocalEntry;
			return this;
		}
		
		public Builder checkIntegrity(boolean checkIntegrity) {
			this.checkIntegrity = checkIntegrity;
			return this;
		}
		
		public Builder useCompressedStreams(boolean useCompressedStreams) {
			this.useCompressedStreams = useCompressedStreams;
			return this;
		}
		
		public ResourceList localList() {
			return localList;
		}
		
		public boolean allowNullLocalEntry() {
			return allowNullLocalEntry;
		}
		
		public boolean checkIntegrity() {
			return checkIntegrity;
		}
		
		public boolean useCompressedStreams() {
			return useCompressedStreams;
		}
	}
}