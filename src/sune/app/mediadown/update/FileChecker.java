package sune.app.mediadown.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;

import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.FileCheckEvent;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Regex;

public class FileChecker implements EventBindable<FileCheckEvent> {
	
	private static final String STRING_NEWLINE = "\n";
	
	private final Path dir;
	private final Map<Path, FileCheckerEntry> entries = new LinkedHashMap<>();
	private final EventRegistry<FileCheckEvent> eventRegistry = new EventRegistry<>();
	
	/** @since 00.02.08 */
	public FileChecker(Path dir) {
		this.dir = Objects.requireNonNull(dir).toAbsolutePath();
	}
	
	public static final FileChecker parse(Path dir, String string) {
		FileChecker checker = new FileChecker(dir);
		
		try(BufferedReader reader = new BufferedReader(new StringReader(string))) {
			for(String line; (line = reader.readLine()) != null;) {
				FileCheckerEntry entry;
				
				if((entry = FileCheckerEntry.parse(line)) == null)
					continue;
				
				checker.add(entry);
			}
		} catch(IOException ex) {
			// Ignore
		}
		
		return checker;
	}
	
	private final void add(FileCheckerEntry entry) {
		entries.put(entry.getPath(), entry);
	}
	
	protected Path relativePath(Path path) {
		return dir.relativize(path);
	}
	
	/** @since 00.02.08 */
	protected final <V> void call(Event<FileCheckEvent, V> event, V value) {
		eventRegistry.call(event, value);
	}
	
	public final void addEntry(Path path, Requirements requirements, String version) {
		add(new FileCheckerEntry(path.toAbsolutePath(), requirements, version, null));
	}
	
	/** @since 00.02.07 */
	public final void generate(Predicate<Path> filter, boolean checkRequirements, Predicate<Path> predicateComputeHash)
			throws Exception {
		for(Entry<Path, FileCheckerEntry> mapEntry : entries.entrySet()) {
			Path path = mapEntry.getKey();
			FileCheckerEntry entry = mapEntry.getValue();
			
			try {
				call(FileCheckEvent.BEGIN, path);
				
				if(!filter.test(path)) {
					continue;
				}
				
				Requirements requirements = entry.getRequirements();
				if(checkRequirements && requirements != Requirements.ANY
						&& !requirements.equals(Requirements.CURRENT)) {
					continue;
				}
				
				String hash = predicateComputeHash.test(path) ? Hash.sha1(path).toLowerCase() : null;
				Path relPath = relativePath(path);
				String version = entry.getVersion();
				FileCheckerEntry newEntry = new FileCheckerEntry(relPath, requirements, version, hash);
				mapEntry.setValue(newEntry);
				
				call(FileCheckEvent.UPDATE, new Pair<>(path, hash));
			} finally {
				call(FileCheckEvent.END, path);
			}
		}
	}
	
	@Override
	public <V> void addEventListener(Event<? extends FileCheckEvent, V> event, Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	@Override
	public <V> void removeEventListener(Event<? extends FileCheckEvent, V> event, Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
	
	public FileCheckerEntry getEntry(Path path) {
		return entries.get(path.toAbsolutePath());
	}
	
	public Collection<FileCheckerEntry> entries() {
		return Collections.unmodifiableCollection(entries.values());
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		for(FileCheckerEntry entry : entries.values()) {
			if(entry.getHash() == null) {
				continue;
			}
			
			builder.append(entry.toString());
			builder.append(STRING_NEWLINE);
		}
		
		return builder.toString();
	}
	
	public static final class FileCheckerEntry {
		
		private static final String STRING_DELIMITER = "|";
		
		private final Path path;
		private final Requirements requirements;
		private final String version;
		private final String hash;
		
		protected FileCheckerEntry(Path path, Requirements requirements, String version, String hash) {
			this.path = Objects.requireNonNull(path);
			this.requirements = Objects.requireNonNull(requirements);
			this.version = version;
			this.hash = hash;
		}
		
		public static final FileCheckerEntry parse(String line) {
			if(line == null || line.isEmpty()) {
				return null;
			}
			
			String[] parts = line.split(Regex.quote(STRING_DELIMITER));
			if(parts.length < 4) {
				return null;
			}
			
			String hash = parts[0];
			Requirements requirements = Requirements.parse(parts[1]);
			String version = parts[2];
			Path path = NIO.localPath(parts[3]).toAbsolutePath();
			
			return new FileCheckerEntry(path, requirements, version, hash);
		}
		
		public Path getPath() {
			return path;
		}
		
		public Requirements getRequirements() {
			return requirements;
		}
		
		public String getHash() {
			return hash;
		}
		
		public String getVersion() {
			return version;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(hash, path, requirements, version);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			FileCheckerEntry other = (FileCheckerEntry) obj;
			return Objects.equals(hash, other.hash)
			        && Objects.equals(path, other.path)
			        && Objects.equals(requirements, other.requirements)
			        && Objects.equals(version, other.version);
		}
		
		@Override
		public String toString() {
			return String.join("",
			    hash, STRING_DELIMITER,
				requirements.toString(), STRING_DELIMITER,
				version, STRING_DELIMITER,
				path.toString().replace('\\', '/')
			);
		}
	}
	
	public static final class PrefixedFileChecker extends FileChecker {
		
		private final Path dirPrefix;
		
		public PrefixedFileChecker(Path dir, Path dirPrefix) {
			super(dir);
			this.dirPrefix = dirPrefix;
		}
		
		@Override
		protected Path relativePath(Path path) {
			return dirPrefix.relativize(path);
		}
	}
}