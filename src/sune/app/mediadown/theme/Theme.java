package sune.app.mediadown.theme;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.resource.Extractable;
import sune.app.mediadown.resource.InputStreamResolver;
import sune.app.mediadown.update.Version;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;
import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDF;
import sune.util.ssdf2.SSDObject;

public class Theme implements Extractable {
	
	private static final String DEFAULT_LANGUAGE = MediaDownloader.Languages.defaultLanguageCode();
	
	private final String name;
	private final String path;
	private final Version version;
	private final Map<String, String> titles;
	private final List<String> files;
	private final boolean internal;
	private Path externalPath;
	
	private Theme(String name, String path, Version version, Map<String, String> titles, List<String> files, boolean internal) {
		this.name = Objects.requireNonNull(name);
		this.path = Objects.requireNonNull(path);
		this.version = Objects.requireNonNull(version);
		this.titles = ensureDefaultLanguage(titles, name);
		this.files = Collections.unmodifiableList(Objects.requireNonNull(files));
		this.internal = internal;
		this.externalPath = !internal ? Path.of(path) : null;
	}
	
	/** @since 00.02.07 */
	private static final Map<String, String> ensureDefaultLanguage(Map<String, String> titles, String name) {
		if(titles != null) {
			titles.computeIfAbsent(DEFAULT_LANGUAGE, (k) -> Utils.titlize(name));
			return Collections.unmodifiableMap(titles);
		}
		
		return Map.of(DEFAULT_LANGUAGE, Utils.titlize(name));
	}
	
	/** @since 00.02.07 */
	private static final boolean isStylesheet(String name) {
		return name.endsWith(".css");
	}
	
	/** @since 00.02.07 */
	public static final Builder newInternal() {
		return new Builder(true);
	}
	
	/** @since 00.02.07 */
	public static final Builder newExternal() {
		return new Builder(false);
	}
	
	/** @since 00.02.07 */
	public static final Theme ofDefault() {
		return ofLight();
	}
	
	/** @since 00.02.07 */
	public static final Theme ofLight() {
		return BuiltInThemes.ofLight();
	}
	
	/** @since 00.02.07 */
	public static final Theme ofDark() {
		return BuiltInThemes.ofDark();
	}
	
	private final String internalPath(String file) {
		return String.format("theme/%s/%s", name, file);
	}
	
	/** @since 00.02.07 */
	private final String fileURL(String name) {
		if(externalPath == null)
			return null; // Not extracted yet
		
		Path file = externalPath.resolve(name);
		if(!NIO.exists(file))
			return null; // Do not throw exception
		
		return Net.uri(file).toString();
	}
	
	@Override
	public void extract(Path dir, InputStreamResolver resolver) throws Exception {
		if(!internal) return; // External, already extracted
		
		Path root = dir.resolve(name).toAbsolutePath();
		
		// Check whether theme.ssdf exists, if not, delete all already existing files
		// to prevent possible future issues.
		if(NIO.exists(root)
				&& !NIO.exists(root.resolve("theme.ssdf"))) {
			NIO.deleteDir(root);
		}
		
		for(String file : files) {
			String internalPath = internalPath(file);
			Path externalPath = root.resolve(file);
			
			if(!NIO.exists(externalPath)) {
				// Ensure the style resource's parent directory
				NIO.createDir(externalPath.getParent());
				// Copy the style resource's bytes to the destination file
				NIO.copy(resolver.resolve(internalPath), externalPath);
			}
		}
		
		externalPath = root;
	}
	
	/** @since 00.02.07 */
	public boolean hasFile(String name) {
		return files.contains(name);
	}
	
	public boolean hasStylesheet(String name) {
		return isStylesheet(name) && hasFile(name);
	}
	
	/** @since 00.02.07 */
	public String file(String name) {
		return hasFile(name) ? fileURL(name) : null; // Do not throw exception
	}
	
	public String stylesheet(String name) {
		return hasStylesheet(name) ? fileURL(name) : null; // Do not throw exception
	}
	
	/** @since 00.02.07 */
	public String name() {
		return name;
	}
	
	/** @since 00.02.07 */
	public String path() {
		return path;
	}
	
	/** @since 00.02.07 */
	public Path externalPath() {
		return externalPath;
	}
	
	/** @since 00.02.07 */
	public Version version() {
		return version;
	}
	
	/** @since 00.02.07 */
	public Map<String, String> titles() {
		return titles;
	}
	
	/** @since 00.02.07 */
	public String title(String languageCode) {
		return Optional.ofNullable(titles.get(languageCode)).orElseGet(this::title);
	}
	
	/** @since 00.02.07 */
	public String title() {
		return title(DEFAULT_LANGUAGE);
	}
	
	/** @since 00.02.07 */
	public List<String> files() {
		return files;
	}
	
	/** @since 00.02.07 */
	public boolean isInternal() {
		return internal;
	}
	
	@Override
	public String toString() {
		return title();
	}
	
	/** @since 00.02.07 */
	private static final class BuiltInThemes {
		
		private static final String BASE_RESOURCE = "/resources/theme/";
		private static final InputStreamResolver RESOLVER;
		
		static {
			RESOLVER = ((path) -> MediaDownloader.class.getResourceAsStream(BASE_RESOURCE + path));
		}
		
		private static Theme LIGHT;
		private static Theme DARK;
		
		// Forbid anyone to create an instance of this class
		private BuiltInThemes() {
		}
		
		private static final Theme loadInternal(String name) {
			return Ignore.defaultValue(() -> Reader.readInternal(name, RESOLVER), null, MediaDownloader::error);
		}
		
		public static final Theme ofLight() {
			return LIGHT == null ? (LIGHT = loadInternal("light")) : LIGHT;
		}
		
		public static final Theme ofDark() {
			return DARK == null ? (DARK = loadInternal("dark")) : DARK;
		}
	}
	
	/** @since 00.02.07 */
	public static final class Builder {
		
		private String name;
		private String path;
		private Version version;
		private Map<String, String> titles;
		private List<String> files;
		private boolean internal;
		
		private Builder(boolean internal) {
			this.internal = internal;
			this.titles = new LinkedHashMap<>();
			this.files = new ArrayList<>();
		}
		
		public Theme build() {
			return new Theme(name, path, version, titles, files, internal);
		}
		
		public void name(String name) {
			this.name = Objects.requireNonNull(name);
		}
		
		public void path(String path) {
			this.path = Objects.requireNonNull(path);
		}
		
		public void version(Version version) {
			this.version = Objects.requireNonNull(version);
		}
		
		public void titles(String... values) {
			titles.putAll(Utils.toMap((Object[]) Objects.requireNonNull(values)));
		}
		
		public void addTitle(String languageCode, String title) {
			titles(languageCode, title);
		}
		
		public void addTitles(String... values) {
			titles(values);
		}
		
		public void files(String... files) {
			this.files = List.of(Objects.requireNonNull(Utils.nonNullContent(files)));
		}
		
		public void files(List<String> files) {
			this.files.addAll(Objects.requireNonNull(Utils.nonNullContent(files)));
		}
		
		public void addFiles(String... files) {
			files(List.of(Objects.requireNonNull(Utils.nonNullContent(files))));
		}
		
		public void addFiles(List<String> files) {
			files(files);
		}
		
		public String name() {
			return name;
		}
		
		public String path() {
			return path;
		}
		
		public Version version() {
			return version;
		}
		
		public Map<String, String> titles() {
			return titles;
		}
		
		public List<String> files() {
			return files;
		}
	}
	
	/** @since 00.02.07 */
	public static final class Reader {
		
		// Forbid anyone to create an instance of this class
		private Reader() {
		}
		
		private static final String normalizePath(String path) {
			return path.replace('\\', '/');
		}
		
		private static final void readEmptyInfoNoFiles(String path, Builder builder) {
			String name = Utils.OfPath.baseName(path);
			String title = Utils.titlize(name);
			builder.name(name.toLowerCase());
			builder.version(Version.UNKNOWN);
			builder.titles(DEFAULT_LANGUAGE, title);
		}
		
		private static final void readInfo(InputStream stream, Builder builder) {
			SSDCollection info = SSDF.read(stream);
			
			String name = info.getDirectString("name").toLowerCase();
			builder.name(name);
			builder.version(Version.of(info.getDirectString("version")));
			
			builder.titles(
				Utils.stream(info.getDirectCollection("title").objectsIterable())
				     .flatMap((o) -> Stream.of(o.getName(), o.stringValue()))
				     .toArray(String[]::new)
			);
			
			builder.files(Stream.concat(
				Stream.of("theme.ssdf"), // Always include the theme.ssdf file
				Utils.stream(info.getDirectCollection("files").objectsIterable())
				     .map(SSDObject::stringValue)
			).toArray(String[]::new));
		}
		
		private static final void readFiles(Path path, Builder builder) throws IOException {
			builder.files(
				Files.walk(path)
				     .filter(Files::isRegularFile)
				     .map((p) -> path.relativize(p).toString())
				     .collect(Collectors.toList())
			);
		}
		
		public static final Theme readInternal(String path, InputStreamResolver resolver) throws IOException {
			String strPath = normalizePath(path);
			
			Builder builder = newInternal();
			builder.path(strPath);
			
			String infoPath = strPath + "/theme.ssdf";
			try(InputStream stream = resolver.resolve(infoPath)) {
				// Support themes that does not have the theme.ssdf file
				if(stream != null) {
					readInfo(stream, builder);
				} else {
					readEmptyInfoNoFiles(strPath, builder);
					readFiles(Path.of(Net.uri(Theme.class.getResource(strPath))), builder);
				}
			}
			
			return builder.build();
		}
		
		public static final Theme readExternal(Path path) throws IOException {
			Path absPath = path.toAbsolutePath();
			String strPath = normalizePath(absPath.toString());
			
			Builder builder = newExternal();
			builder.path(strPath);
			
			Path infoPath = absPath.resolve("theme.ssdf");
			// Support themes that does not have the theme.ssdf file
			if(NIO.exists(infoPath)) {
				try(InputStream stream = Files.newInputStream(infoPath, StandardOpenOption.READ)) {
					readInfo(stream, builder);
				}
			} else {
				readEmptyInfoNoFiles(strPath, builder);
				readFiles(absPath, builder);
			}
			
			return builder.build();
		}
	}
	
	/** @since 00.02.07 */
	public static final class Writer {
		
		// Forbid anyone to create an instance of this class
		private Writer() {
		}
		
		private static final void writeInfoNoTitle(Theme theme, SSDCollection info) {
			info.setDirect("name", theme.name());
			info.setDirect("version", theme.version().toString());
			
			SSDCollection files = SSDCollection.emptyArray();
			theme.files().forEach((f) -> files.add(f));
		}
		
		public static final SSDCollection writeInternal(Theme theme, InputStreamResolver resolver) throws IOException {
			SSDCollection info = SSDCollection.empty();
			writeInfoNoTitle(theme, info);
			
			String infoPath = theme.path() + "/theme.ssdf";
			SSDCollection oldInfo = SSDF.read(resolver.resolve(infoPath));
			SSDCollection title = oldInfo.getDirectCollection("title").copy();
			info.setDirect("title", title);
			
			return info;
		}
		
		public static final SSDCollection writeExternal(Theme theme) throws IOException {
			SSDCollection info = SSDCollection.empty();
			writeInfoNoTitle(theme, info);
			
			Path infoPath = theme.externalPath().resolve("theme.ssdf");
			SSDCollection oldInfo = SSDF.read(Files.newInputStream(infoPath, StandardOpenOption.READ));
			SSDCollection title = oldInfo.getDirectCollection("title").copy();
			info.setDirect("title", title);
			
			return info;
		}
	}
}