package sune.app.mediadown.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;

public class FileChecker {
	
	public static final class Requirements {
		
		private static final String STRING_DELIMITER = ";";
		private static final String STRING_ANY_OS_NAME = "***";
		private static final String STRING_ANY_OS_ARCH = "**";
		
		public static final Requirements CURRENT = new Requirements(OSUtils.getSystemName(), OSUtils.getSystemArch());
		public static final Requirements ANY = new Requirements(null, null);
		
		private final String osName;
		private final String osArch;
		
		private Requirements(String osName, String osArch) {
			this.osName = osName;
			this.osArch = osArch;
		}
		
		public static final Requirements create(String osName, String osArch) {
			if((osName == null || osName.isEmpty() || osArch == null || osArch.isEmpty()))
				throw new IllegalArgumentException();
			return new Requirements(osName, osArch);
		}
		
		public static final Requirements parse(String value) {
			if((value == null || value.isEmpty()))
				throw new IllegalArgumentException("Invalid Requirement string");
			String[] parts = value.split(Pattern.quote(STRING_DELIMITER));
			if((parts.length != 2))
				throw new IllegalArgumentException("Invalid Requirement string");
			String osName = parts[0];
			String osArch = parts[1];
			// Special value
			if((osName.equals(STRING_ANY_OS_NAME)
					&& osArch.equals(STRING_ANY_OS_ARCH)))
				return ANY;
			// Normal value
			return create(osName, osArch);
		}
		
		/** @since 00.02.02 */
		public static final Requirements parseNoDelimiter(String value) {
			if(value == null || value.isEmpty())
				throw new IllegalArgumentException("Invalid Requirement string");
			if(value.length() != 5)
				throw new IllegalArgumentException("Invalid Requirement string");
			String osName = value.substring(0, 3);
			String osArch = value.substring(3, 5);
			// Special value
			if((osName.equals(STRING_ANY_OS_NAME)
					&& osArch.equals(STRING_ANY_OS_ARCH)))
				return ANY;
			// Normal value
			return create(osName, osArch);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((osArch == null) ? 0 : osArch.hashCode());
			result = prime * result + ((osName == null) ? 0 : osName.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			Requirements other = (Requirements) obj;
			if(osArch == null) {
				if(other.osArch != null)
					return false;
			} else if(!osArch.equals(other.osArch))
				return false;
			if(osName == null) {
				if(other.osName != null)
					return false;
			} else if(!osName.equals(other.osName))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return String.format("%s%s%s", osName == null ? STRING_ANY_OS_NAME : osName,
				STRING_DELIMITER, osArch == null ? STRING_ANY_OS_ARCH : osArch);
		}
	}
	
	public static final class FileCheckerEntry {
		
		private static final String STRING_DELIMITER = "|";
		
		private final Path path;
		private final Requirements requirements;
		private final String version;
		private final String hash;
		
		protected FileCheckerEntry(Path path, Requirements requirements, String version, String hash) {
			if((path == null || requirements == null))
				throw new IllegalArgumentException();
			this.path = path;
			this.requirements = requirements;
			this.version = version;
			this.hash = hash;
		}
		
		public static final FileCheckerEntry parse(String line) {
			if((line == null || line.isEmpty())) return null;
			String[] parts = line.split(Pattern.quote(STRING_DELIMITER));
			if((parts.length < 4)) return null;
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
			final int prime = 31;
			int result = 1;
			result = prime * result + ((hash == null) ? 0 : hash.hashCode());
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			result = prime * result + ((requirements == null) ? 0 : requirements.hashCode());
			result = prime * result + ((version == null) ? 0 : version.hashCode());
			return result;
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
			if(hash == null) {
				if(other.hash != null)
					return false;
			} else if(!hash.equals(other.hash))
				return false;
			if(path == null) {
				if(other.path != null)
					return false;
			} else if(!path.equals(other.path))
				return false;
			if(requirements == null) {
				if(other.requirements != null)
					return false;
			} else if(!requirements.equals(other.requirements))
				return false;
			if(version == null) {
				if(other.version != null)
					return false;
			} else if(!version.equals(other.version))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return String.join("",
			    hash, STRING_DELIMITER,
				requirements.toString(), STRING_DELIMITER,
				version, STRING_DELIMITER,
				path.toString().replace('\\', '/'));
		}
	}
	
	public static final class PrefixedFileChecker extends FileChecker {
		
		private final Path dirPrefix;
		
		public PrefixedFileChecker(Path dir, FileCheckListener listener, Path dirPrefix) {
			super(dir, listener);
			this.dirPrefix = dirPrefix;
		}
		
		@Override
		protected Path relativePath(Path path) {
			return dirPrefix.relativize(path);
		}
	}
	
	private static final String STRING_NEWLINE = "\n";
	
	private final Path dir;
	private final FileCheckListener listener;
	private final Map<Path, FileCheckerEntry> entries = new LinkedHashMap<>();
	
	public FileChecker(Path dir, FileCheckListener listener) {
		if((dir == null))
			throw new IllegalArgumentException();
		this.dir = dir.toAbsolutePath();
		this.listener = listener;
	}
	
	protected Path relativePath(Path path) {
		return dir.relativize(path);
	}
	
	public static final FileChecker parse(Path dir, String string) {
		FileChecker checker = new FileChecker(dir, null);
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
	
	public final void addEntry(Path path, Requirements requirements, String version) {
		add(new FileCheckerEntry(path.toAbsolutePath(), requirements, version, null));
	}
	
	public final boolean generate(Predicate<Path> filter, boolean checkRequirements, boolean computeHashes) {
		try {
			// Notify the listener, if needed
			if((listener != null))
				listener.begin(dir);
			// Loop through all the added entries
			for(Iterator<Entry<Path, FileCheckerEntry>> it = entries.entrySet().iterator();
					it.hasNext();) {
				Entry<Path, FileCheckerEntry> mapEntry = it.next();
				Path path = mapEntry.getKey();
				FileCheckerEntry entry = mapEntry.getValue();
				if(!filter.test(path)) continue;
				// Notify the listener, if needed
				if((listener != null))
					listener.update(path, null);
				Requirements requirements = entry.getRequirements();
				if((checkRequirements && requirements != Requirements.ANY
						&& !requirements.equals(Requirements.CURRENT)))
					continue;
				String hash = computeHashes ? Hash.sha1(path).toLowerCase() : null;
				Path relPath = relativePath(path);
				String version = entry.getVersion();
				FileCheckerEntry newEntry = new FileCheckerEntry(relPath, requirements, version, hash);
				mapEntry.setValue(newEntry);
				// Notify the listener, if needed
				if((listener != null))
					listener.update(path, hash);
			}
			// Notify the listener, if needed
			if((listener != null))
				listener.end(dir);
			// Successfully walked
			return true;
		} catch(Exception ex) {
			// Notify the listener, if needed
			if((listener != null))
				listener.error(ex);
		}
		// An error has occured
		return false;
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
			if((entry.getHash() == null))
				continue;
			builder.append(entry.toString());
			builder.append(STRING_NEWLINE);
		}
		return builder.toString();
	}
}