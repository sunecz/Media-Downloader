package sune.app.mediadown.util;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.configuration.ApplicationConfiguration;
import sune.app.mediadown.media.MediaFormat;

/** @since 00.02.05 */
public final class Choosers {
	
	// Forbid anyone to create an instance of this class
	private Choosers() {
	}
	
	private static final void updateConfigurationProperty(String name, Object value) {
		ApplicationConfiguration configuration = MediaDownloader.configuration();
		Utils.ignore(() -> configuration.writer().set(name, value).save(configuration.path()),
		             MediaDownloader::error);
	}
	
	private static final void updateLastDirectory(Path path) {
		updateConfigurationProperty(
			ApplicationConfiguration.PROPERTY_HISTORY_LAST_DIRECTORY,
			(NIO.isDirectory(path) ? path : path.getParent()).toAbsolutePath().toString().replace('\\', '/')
		);
	}
	
	private static final void updateLastOpenFormat(Path path) {
		updateConfigurationProperty(
			ApplicationConfiguration.PROPERTY_HISTORY_LAST_OPEN_FORMAT,
			MediaFormat.fromPath(path).name()
		);
	}
	
	private static final void updateLastSaveFormat(Path path) {
		updateConfigurationProperty(
			ApplicationConfiguration.PROPERTY_HISTORY_LAST_SAVE_FORMAT,
			MediaFormat.fromPath(path).name()
		);
	}
	
	private static final Path maybeUpdateHistory(OfFile.ChooserAccessor<?> accessor, Path path, boolean saveMode) {
		if(path != null) {
			updateLastDirectory(path);
			if(saveMode) updateLastSaveFormat(path);
			else         updateLastOpenFormat(path);
		}
		return path;
	}
	
	private static final List<Path> maybeUpdateHistory(OfFile.ChooserAccessor<?> accessor, List<Path> paths) {
		if(paths != null) {
			updateLastDirectory(paths.stream().map(Path::getParent).distinct().findFirst().get());
			updateLastOpenFormat(paths.get(0)); // Only open (multiple) mode
		}
		return paths;
	}
	
	private static final Path maybeUpdateHistory(OfDirectory.ChooserAccessor<?> accessor, Path path) {
		if(path != null) {
			updateLastDirectory(path);
		}
		return path;
	}
	
	private static final Path lastDirectory() {
		return MediaDownloader.configuration().lastDirectory();
	}
	
	private static final MediaFormat lastOpenFormat() {
		return MediaDownloader.configuration().lastOpenFormat();
	}
	
	private static final MediaFormat lastSaveFormat() {
		return MediaDownloader.configuration().lastSaveFormat();
	}
	
	public static final class OfFile {
		
		// Forbid anyone to create an instance of this class
		private OfFile() {
		}
		
		public static final Builder builder() {
			return new Builder(false);
		}
		
		public static final Builder configuredBuilder() {
			Builder builder = new Builder(true);
			
			Path lastDir = lastDirectory();
			if(lastDir != null) {
				builder.directory(lastDir);
			}
			
			return builder;
		}
		
		public static final class Chooser extends ChooserAccessor<Chooser> {
			
			private final Window parent;
			private final boolean isConfigured;
			
			protected Chooser(ChooserAccessor<?> accessor, Window parent, boolean isConfigured) {
				super(accessor);
				this.parent = parent;
				this.isConfigured = isConfigured;
			}
			
			private final void selectConfiguredFormat(boolean saveMode) {
				MediaFormat lastFormat = saveMode ? lastSaveFormat() : lastOpenFormat();
				if(!lastFormat.is(MediaFormat.UNKNOWN)) {
					selectedFilter(ExtensionFilters.extensionFilter(lastFormat));
				}
			}
			
			public Path showOpen() {
				if(isConfigured) {
					selectConfiguredFormat(false);
				}
				
				return maybeUpdateHistory(
					this,
					Optional.ofNullable(chooser.showOpenDialog(parent))
						.map(File::toPath)
						.orElse(null),
					false
				);
			}
			
			public List<Path> showOpenMultiple() {
				if(isConfigured) {
					selectConfiguredFormat(false);
				}
				
				return maybeUpdateHistory(
					this,
					Optional.ofNullable(chooser.showOpenMultipleDialog(parent))
						.map(List::stream)
						.orElseGet(Stream::of)
						.map(File::toPath)
						.collect(Collectors.toUnmodifiableList())
				);
			}
			
			public Path showSave() {
				if(isConfigured) {
					selectConfiguredFormat(true);
				}
				
				return maybeUpdateHistory(
					this,
					Optional.ofNullable(chooser.showSaveDialog(parent))
						.map(File::toPath)
						.orElse(null),
					true
				);
			}
		}
		
		public static final class Builder extends ChooserAccessor<Builder> {
			
			private Window parent;
			private final boolean isConfigured;
			
			protected Builder(boolean isConfigured) {
				super(new FileChooser());
				this.isConfigured = isConfigured;
			}
			
			public Chooser build() {
				return new Chooser(this, parent, isConfigured);
			}
			
			public Builder parent(Window parent) {
				this.parent = parent;
				return this;
			}
			
			public Window parent() {
				return parent;
			}
		}
		
		private static class ChooserAccessor<T extends ChooserAccessor<T>> {
			
			protected final FileChooser chooser;
			protected String title;
			protected String fileName;
			protected List<ExtensionFilter> filters;
			protected ExtensionFilter selectedFilter;
			protected Path directory;
			
			protected ChooserAccessor(ChooserAccessor<?> accessor) {
				this.chooser = accessor.chooser;
				this.title = accessor.title;
				this.fileName = accessor.fileName;
				this.filters = accessor.filters;
				this.selectedFilter = accessor.selectedFilter;
				this.directory = accessor.directory;
			}
			
			protected ChooserAccessor(FileChooser chooser) {
				this.chooser = Objects.requireNonNull(chooser);
			}
			
			@SuppressWarnings("unchecked")
			private final T self() {
				return (T) this;
			}
			
			private final ExtensionFilter obtainExtensionFilter() {
				if(filters == null || filters.isEmpty())
					return null;
				
				ExtensionFilter filter = selectedFilter;
				if(filter != null) {
					if(filters.contains(selectedFilter)) {
						filter = selectedFilter;
					} else {
						// Try to match using the extensions of the extension filters
						List<String> extensions = filter.getExtensions();
						filter = null; // Must reset first
						for(ExtensionFilter ef : filters) {
							if(Utils.equalsNoOrder(extensions, ef.getExtensions())) {
								filter = ef; break;
							}
						}
					}
				} else {
					// Select extension filter based on the given file name or just use the first one
					filter = filters.get(0);
					if(fileName != null && !fileName.isEmpty()) {
						String ext = Utils.fileType(fileName);
						if(ext != null) {
							filter = filters.stream()
								.filter((f) -> f.getExtensions().stream()
								                   .map(Utils::fileType)
								                   .anyMatch(ext::equalsIgnoreCase))
								.findFirst().orElse(filter);
						}
					}
				}
				
				return filter;
			}
			
			private final void selectExtensionFilter() {
				ExtensionFilter filter = obtainExtensionFilter();
				if(filter != null) {
					chooser.setSelectedExtensionFilter(filter);
				}
			}
			
			public T title(String title) {
				this.title = title;
				chooser.setTitle(title);
				return self();
			}
			
			public T fileName(String fileName) {
				this.fileName = fileName;
				chooser.setInitialFileName(fileName);
				selectExtensionFilter();
				return self();
			}
			
			public T filters(ExtensionFilter... filters) {
				return filters(List.of(filters));
			}
			
			public T filters(List<ExtensionFilter> filters) {
				this.filters = filters;
				if(filters != null && !filters.isEmpty()) {
					chooser.getExtensionFilters().setAll(filters);
					selectExtensionFilter();
				}
				return self();
			}
			
			public T selectedFilter(ExtensionFilter selectedFilter) {
				this.selectedFilter = selectedFilter;
				selectExtensionFilter();
				return self();
			}
			
			public T directory(Path directory) {
				this.directory = directory;
				if(directory != null) {
					chooser.setInitialDirectory(directory.toFile());
				}
				return self();
			}
			
			public String title() {
				return title;
			}
			
			public String fileName() {
				return fileName;
			}
			
			public List<ExtensionFilter> filters() {
				return filters;
			}
			
			public ExtensionFilter selectedFilter() {
				return selectedFilter;
			}
			
			public Path directory() {
				return directory;
			}
			
			public FileChooser chooser() {
				return chooser;
			}
		}
	}
	
	public static final class OfDirectory {
		
		// Forbid anyone to create an instance of this class
		private OfDirectory() {
		}
		
		public static final Builder builder() {
			return new Builder();
		}
		
		public static final Builder configuredBuilder() {
			Builder builder = new Builder();
			
			Path lastDir = lastDirectory();
			if(lastDir != null) {
				builder.directory(lastDir);
			}
			
			return builder;
		}
		
		public static final class Chooser extends ChooserAccessor<Chooser>  {
			
			private final Window parent;
			
			protected Chooser(ChooserAccessor<?> accessor, Window parent) {
				super(accessor);
				this.parent = parent;
			}
			
			public Path show() {
				return maybeUpdateHistory(
					this,
					Optional.ofNullable(chooser.showDialog(parent))
						.map(File::toPath)
						.orElse(null)
				);
			}
		}
		
		public static final class Builder extends ChooserAccessor<Builder> {
			
			private Window parent;
			
			protected Builder() {
				super(new DirectoryChooser());
			}
			
			public Chooser build() {
				return new Chooser(this, parent);
			}
			
			public Builder parent(Window parent) {
				this.parent = parent;
				return this;
			}
			
			public Window parent() {
				return parent;
			}
		}
		
		private static class ChooserAccessor<T extends ChooserAccessor<T>> {
			
			protected final DirectoryChooser chooser;
			protected String title;
			protected Path directory;
			
			protected ChooserAccessor(ChooserAccessor<?> accessor) {
				this.chooser = accessor.chooser;
				this.title = accessor.title;
				this.directory = accessor.directory;
			}
			
			protected ChooserAccessor(DirectoryChooser chooser) {
				this.chooser = Objects.requireNonNull(chooser);
			}
			
			@SuppressWarnings("unchecked")
			private final T self() {
				return (T) this;
			}
			
			public T title(String title) {
				this.title = title;
				chooser.setTitle(title);
				return self();
			}
			
			public T directory(Path directory) {
				this.directory = directory;
				if(directory != null) {
					chooser.setInitialDirectory(directory.toFile());
				}
				return self();
			}
			
			public String title() {
				return title;
			}
			
			public Path directory() {
				return directory;
			}
			
			public DirectoryChooser chooser() {
				return chooser;
			}
		}
	}
}