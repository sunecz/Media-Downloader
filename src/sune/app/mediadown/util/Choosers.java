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
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.05 */
public final class Choosers {
	
	// Forbid anyone to create an instance of this class
	private Choosers() {
	}
	
	/** @since 00.02.08 */
	private static final MediaFormat selectedMediaFormat(SelectedItem item) {
		return item.extension().getExtensions().stream()
					.map(MediaFormat::fromName)
					.filter((f) -> !f.is(MediaFormat.UNKNOWN))
					.findFirst().orElse(MediaFormat.UNKNOWN);
	}
	
	private static final void updateConfigurationProperty(String name, Object value) {
		ApplicationConfiguration configuration = MediaDownloader.configuration();
		Ignore.callVoid(() -> configuration.writer().set(name, value).save(configuration.path()),
		                MediaDownloader::error);
	}
	
	private static final void updateLastDirectory(Path path) {
		updateConfigurationProperty(
			ApplicationConfiguration.PROPERTY_HISTORY_LAST_DIRECTORY,
			(NIO.isDirectory(path) ? path : path.getParent()).toAbsolutePath().toString().replace('\\', '/')
		);
	}
	
	private static final void updateLastOpenFormat(SelectedItem item) {
		updateConfigurationProperty(
			ApplicationConfiguration.PROPERTY_HISTORY_LAST_OPEN_FORMAT,
			selectedMediaFormat(item).name()
		);
	}
	
	private static final void updateLastSaveFormat(SelectedItem item) {
		updateConfigurationProperty(
			ApplicationConfiguration.PROPERTY_HISTORY_LAST_SAVE_FORMAT,
			selectedMediaFormat(item).name()
		);
	}
	
	private static final SelectedItem maybeUpdateHistory(OfFile.ChooserAccessor<?> accessor, SelectedItem item,
			boolean saveMode) {
		if(item != null) {
			updateLastDirectory(item.path());
			if(saveMode) updateLastSaveFormat(item);
			else         updateLastOpenFormat(item);
		}
		
		return item;
	}
	
	private static final List<SelectedItem> maybeUpdateHistory(OfFile.ChooserAccessor<?> accessor,
			List<SelectedItem> items) {
		if(items != null) {
			updateLastDirectory(
				items.stream()
				     .map(SelectedItem::path)
				     .map(Path::getParent)
				     .distinct()
				     .findFirst().get()
			);
			updateLastOpenFormat(items.get(0)); // Only open (multiple) mode
		}
		
		return items;
	}
	
	private static final SelectedItem maybeUpdateHistory(OfDirectory.ChooserAccessor<?> accessor, SelectedItem item) {
		if(item != null) {
			updateLastDirectory(item.path());
		}
		
		return item;
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
			
			/** @since 00.02.08 */
			private final SelectedItem createItem(Path path) {
				return SelectedItem.ofFile(path, chooser.getSelectedExtensionFilter());
			}
			
			public SelectedItem showOpen() {
				if(isConfigured) {
					selectConfiguredFormat(false);
				}
				
				return maybeUpdateHistory(
					this,
					Optional.ofNullable(chooser.showOpenDialog(parent))
						.map(File::toPath)
						.map(this::createItem)
						.orElse(null),
					false
				);
			}
			
			public List<SelectedItem> showOpenMultiple() {
				if(isConfigured) {
					selectConfiguredFormat(false);
				}
				
				return maybeUpdateHistory(
					this,
					Optional.ofNullable(chooser.showOpenMultipleDialog(parent))
						.map(List::stream)
						.orElseGet(Stream::of)
						.map(File::toPath)
						.map(this::createItem)
						.collect(Collectors.toUnmodifiableList())
				);
			}
			
			public SelectedItem showSave() {
				if(isConfigured) {
					selectConfiguredFormat(true);
				}
				
				return maybeUpdateHistory(
					this,
					Optional.ofNullable(chooser.showSaveDialog(parent))
						.map(File::toPath)
						.map(this::createItem)
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
						String ext = Utils.OfPath.info(fileName).extension();
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
			
			public SelectedItem show() {
				return maybeUpdateHistory(
					this,
					Optional.ofNullable(chooser.showDialog(parent))
						.map(File::toPath)
						.map(SelectedItem::ofDirectory)
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
	
	/** @since 00.02.08 */
	public static final class SelectedItem {
		
		private final Path path;
		private final ExtensionFilter extension;
		
		private SelectedItem(Path path, ExtensionFilter extension) {
			this.path = Objects.requireNonNull(path);
			this.extension = extension;
		}
		
		private static final String cleanExtension(String extension) {
			return extension.replaceFirst("^\\*\\.", "");
		}
		
		public static final SelectedItem ofFile(Path path, ExtensionFilter extension) {
			return new SelectedItem(path, Objects.requireNonNull(extension));
		}
		
		public static final SelectedItem ofDirectory(Path path) {
			return new SelectedItem(path, null);
		}
		
		public Path path() {
			return path;
		}
		
		public Path pathWithExtension() {
			if(extension == null) {
				return path;
			}
			
			List<String> extensions = extension.getExtensions();
			String fileName = path.getFileName().toString();
			String lowerCaseFileName = fileName.toLowerCase();
			
			// Keep the explicit extension, if present
			for(String ext : extensions) {
				if(lowerCaseFileName.endsWith('.' + cleanExtension(ext).toLowerCase())) {
					return path;
				}
			}
			
			// Otherwise, append the first extension
			String suffix = '.' + cleanExtension(extensions.get(0));
			return path.resolveSibling(fileName + suffix);
		}
		
		public ExtensionFilter extension() {
			return extension;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(extension != null ? extension.getExtensions() : extension, path);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			SelectedItem other = (SelectedItem) obj;
			return Objects.equals(
				      extension != null ?       extension.getExtensions() :       extension,
				other.extension != null ? other.extension.getExtensions() : other.extension
			) && Objects.equals(path, other.path);
		}
	}
}