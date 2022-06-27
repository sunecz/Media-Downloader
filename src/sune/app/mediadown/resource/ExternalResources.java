package sune.app.mediadown.resource;

import static java.nio.file.StandardOpenOption.READ;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.scene.image.Image;
import sune.app.mediadown.language.Language;
import sune.app.mediadown.theme.Theme;
import sune.app.mediadown.util.CheckedConsumer;
import sune.app.mediadown.util.MimeType;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.UncheckedException;
import sune.app.mediadown.util.Utils;
import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDF;

public final class ExternalResources {
	
	private static final class ImageResource {
		
		public final Path  file;
		public final Image image;
		public ImageResource(Path file, Image image) {
			this.file  = file;
			this.image = image;
		}
	}
	
	private static final <T> T usingStream(Path file, Function<InputStream, T> action) throws IOException {
		try(InputStream stream = Files.newInputStream(file, READ)) {
			return action.apply(stream);
		}
	}
	
	private static final <T> Map<String, T> scan(Path folder, Predicate<Path> filter, Function<Path, T> action,
			Function<T, String> keyMapper, Function<T, T> valueMapper) throws IOException {
		return Files.list(folder)
					.filter(filter)
					.map(action)
					.filter((i) -> i != null)
					.collect(Collectors.toMap(keyMapper, valueMapper,
					                          (e0, e1) -> e0,
					                          LinkedHashMap::new));
	}
	
	private static final String string(Path path) {
		return path.toString().replace('\\', '/');
	}
	
	private static final String[] content(Path folder, Predicate<Path> filter) throws IOException {
		return Files.walk(folder).filter((p) -> Files.isRegularFile(p) && filter.test(p))
					.map((file) -> string(folder.relativize(file)))
					.toArray(String[]::new);
	}
	
	public static final Map<String, Language> findLanguages(Path folder) throws IOException {
		if(!NIO.exists(folder)) return new HashMap<>(); // Do not return null
		Path parent = folder.getParent();
		return scan(folder, (file) -> file.getFileName().toString().endsWith(".ssdf"), (file) -> {
			try {
				return usingStream(file, (stream) -> {
					Path path = file.toAbsolutePath();
					String stringPath = string(parent.relativize(file));
					Language language = null;
					try {
						language = Language.from(stringPath, stream);
					} catch(Exception ex0) {
						// In some cases there may be some basic information missing from the file,
						// so just try to insert it automatically, if possible.
						try { FileFixer.fixLanguageFile(path); }
						catch(Exception ex1) { throw new UncheckedException(ex1); }
						finally {
							// File has been successfully fixed, try to load it again.
							try { language = usingStream(path, (newStream) -> Language.from(stringPath, newStream)); }
							catch(IOException ex2) { throw new UncheckedException(ex2); }
						}
					}
					return language;
				});
			} catch(IOException ex) {
				throw new UncheckedException(ex);
			}
		}, (item) -> item.getName(), (item) -> item);
	}
	
	public static final Map<String, Theme> findThemes(Path folder) throws IOException {
		if(!NIO.exists(folder)) return new HashMap<>(); // Do not return null
		return scan(folder, (file) -> Files.isDirectory(file),
			(file) -> {
				try {
					return new Theme(file.getFileName().toString(),
					                    content(file, (f) -> f.getFileName().toString().endsWith(".css")));
				} catch(IOException ex) {
					throw new UncheckedException(ex);
				}
			},
			(item) -> item.getName(), (item) -> item);
	}
	
	private static final Map<String, ImageResource> findImages0(Path folder) throws IOException {
		if(!NIO.exists(folder)) return new HashMap<>(); // Do not return null
		return Files.walk(folder)
					.filter((path) -> Files.isRegularFile(path) && MimeType.isImage(path))
					.map((file) -> {
						try {
							return usingStream(file, (stream) -> new ImageResource(file, new Image(stream)));
						} catch(IOException ex) {
							throw new UncheckedException(ex);
						}
					})
					.collect(Collectors.toMap((item) -> string(folder.relativize(item.file)),
					                          (item) -> item,
					                          (e0, e1) -> e0,
					                          LinkedHashMap::new));
	}
	
	public static final Map<String, Image> findIcons(Path folder) throws IOException {
		return findImages(folder);
	}
	
	public static final Map<String, Image> findImages(Path folder) throws IOException {
		return findImages0(folder).entrySet().stream()
								  .collect(Collectors.toMap((item) -> item.getKey(),
								                            (item) -> item.getValue().image,
								                            (e0, e1) -> e0,
								                            LinkedHashMap::new));
	}
	
	/** @since 00.02.02 */
	private static final class FileFixer {
		
		private static final String languageCodeFromName(String name) {
			return Utils.ignore(() -> Stream.of(Locale.getAvailableLocales())
			                                .filter((locale) -> locale.getDisplayLanguage(Locale.ENGLISH).equalsIgnoreCase(name))
			                                .findFirst().get().getISO3Language(),
			                    (String) null);
		}
		
		private static final CheckedConsumer<Path> reversableAction(CheckedConsumer<Path> action) {
			return ((path) -> {
				Path tempPath = path.resolveSibling(path.getFileName().toString() + ".tmp");
				NIO.copy(Files.newInputStream(path, StandardOpenOption.READ), tempPath);
				Exception exception = null;
				try { action.accept(path); } catch(Exception ex) { exception = ex; }
				finally {
					if(exception != null) {
						NIO.move_force(tempPath, path);
						throw exception;
					} else {
						NIO.delete(tempPath);
					}
				}
			});
		}
		
		private static final void doReversableAction(Path path, CheckedConsumer<Path> action) throws Exception {
			reversableAction(action).accept(path);
		}
		
		public static final void fixLanguageFile(Path path) throws Exception {
			doReversableAction(path, (p) -> {
				try(InputStream stream = Files.newInputStream(path, StandardOpenOption.READ)) {
					SSDCollection ssdf = SSDF.read(stream);
					if(!ssdf.hasDirectString("name")) // Fix missing name
						ssdf.setDirect("name", Utils.fileNameNoType(path.getFileName().toString()));
					if(!ssdf.hasDirectString("version")) // Fix missing version
						ssdf.setDirect("version", "0000");
					if(!ssdf.hasDirectString("title")) // Fix missing title
						ssdf.setDirect("title", ssdf.getDirectString("name"));
					if(!ssdf.hasDirectString("code")) { // Fix missing code
						String code = languageCodeFromName(ssdf.getDirectString("name"));
						if(code != null) ssdf.setDirect("code", code);
					}
					NIO.save(path, ssdf.toString());
				}
			});
		}
	}
	
	// Forbid anyone to create an instance of this class
	private ExternalResources() {
	}
}