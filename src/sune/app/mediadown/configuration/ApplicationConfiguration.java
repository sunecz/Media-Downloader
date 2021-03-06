package sune.app.mediadown.configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.gui.form.field.TextFieldMediaTitleFormat;
import sune.app.mediadown.gui.window.ConfigurationWindow;
import sune.app.mediadown.language.Language;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaTitleFormat;
import sune.app.mediadown.media.MediaTitleFormats;
import sune.app.mediadown.media.MediaTitleFormats.NamedMediaTitleFormat;
import sune.app.mediadown.resource.ResourceRegistry;
import sune.app.mediadown.theme.Theme;
import sune.app.mediadown.update.Version;
import sune.app.mediadown.update.VersionType;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Utils;
import sune.util.ssdf2.SSDCollection;

/** @since 00.02.04 */
public class ApplicationConfiguration extends Configuration implements ApplicationConfigurationAccessor {
	
	private static final String NAME = "application";
	
	private final Path path;
	
	private Version version;
	private Language language;
	private Theme theme;
	private boolean autoUpdateCheck;
	private int acceleratedDownload;
	private int parallelDownloads;
	private int parallelConversions;
	private boolean computeStreamSize;
	private int requestTimeout;
	private boolean checkResourcesIntegrity;
	private boolean plugins_autoUpdateCheck;
	/** @since 00.02.05 */
	private Path history_lastDirectory;
	/** @since 00.02.05 */
	private MediaFormat history_lastOpenFormat;
	/** @since 00.02.05 */
	private MediaFormat history_lastSaveFormat;
	/** @since 00.02.05 */
	private MediaTitleFormat naming_mediaTitleFormat;
	/** @since 00.02.05 */
	private String naming_customMediaTitleFormat;
	/** @since 00.02.07 */
	private boolean usePreReleaseVersions;
	/** @since 00.02.07 */
	private boolean autoEnableClipboardWatcher;
	
	private ApplicationConfiguration(Path path, String name, SSDCollection data, Map<String, ConfigurationProperty<?>> properties) {
		super(name, data, properties);
		this.path = Objects.requireNonNull(path);
		this.loadFields();
	}
	
	public static final ApplicationConfiguration.Builder builder(Path path) {
		ApplicationConfiguration.Builder builder = new ApplicationConfiguration.Builder(path);
		
		// ----- Hidden
		builder.addProperty(ConfigurationProperty.ofString(PROPERTY_VERSION).asHidden(true));
		builder.addProperty(ConfigurationProperty.ofArray("removeAtInit").asHidden(true));
		
		// ----- General
		builder.addProperty(ConfigurationProperty.ofType(PROPERTY_LANGUAGE, Language.class)
			.inGroup(GROUP_GENERAL)
			.withFactory(() -> Stream.concat(List.of("auto").stream(),
			                                 ResourceRegistry.languages.values().stream()
			                                        .map(Language::getName))
	                                 .collect(Collectors.toList()))
			.withTransformer((v) -> v.getName(),
			                 (s) -> ResourceRegistry.languages.values().stream()
			                            .filter((l) -> l.getName().equals(s))
			                            .findFirst().orElse(null)));
		builder.addProperty(ConfigurationProperty.ofType(PROPERTY_THEME, Theme.class)
			.inGroup(GROUP_GENERAL)
			.withFactory(() -> ResourceRegistry.themes.values().stream()
			                        .map(Theme::getName)
			                        .collect(Collectors.toList()))
			.withTransformer((v) -> v.getName(),
			                 (s) -> ResourceRegistry.themes.values().stream()
			                            .filter((t) -> t.getName().equals(s))
			                            .findFirst().orElse(null)));
		builder.addProperty(ConfigurationProperty.ofBoolean(PROPERTY_AUTO_UPDATE_CHECK).inGroup(GROUP_GENERAL).withDefaultValue(true));
		builder.addProperty(ConfigurationProperty.ofBoolean(PROPERTY_CHECK_RESOURCES_INTEGRITY).inGroup(GROUP_GENERAL).withDefaultValue(true));
		builder.addProperty(ConfigurationProperty.ofBoolean(PROPERTY_USE_PRE_RELEASE_VERSIONS)
			.inGroup(GROUP_GENERAL)
			.withDefaultValue(MediaDownloader.version().type() != VersionType.RELEASE));

		// ----- Download
		builder.addProperty(ConfigurationProperty.ofInteger(PROPERTY_ACCELERATED_DOWNLOAD).inGroup(GROUP_DOWNLOAD).withDefaultValue(1));
		builder.addProperty(ConfigurationProperty.ofInteger(PROPERTY_PARALLEL_DOWNLOADS).inGroup(GROUP_DOWNLOAD).withDefaultValue(1));
		builder.addProperty(ConfigurationProperty.ofBoolean(PROPERTY_COMPUTE_STREAM_SIZE).inGroup(GROUP_DOWNLOAD).withDefaultValue(true));
		builder.addProperty(ConfigurationProperty.ofInteger(PROPERTY_REQUEST_TIMEOUT).inGroup(GROUP_DOWNLOAD).withDefaultValue(5000));
		
		// ----- Conversion
		builder.addProperty(ConfigurationProperty.ofInteger(PROPERTY_PARALLEL_CONVERSIONS).inGroup(GROUP_CONVERSION).withDefaultValue(1));
		
		// ----- Plugins
		builder.addProperty(ConfigurationProperty.ofBoolean(PROPERTY_PLUGINS_AUTO_UPDATE_CHECK)
			.inGroup(GROUP_PLUGINS).withDefaultValue(true));
		
		// ----- History
		builder.addProperty(ConfigurationProperty.ofType(PROPERTY_HISTORY_LAST_DIRECTORY, Path.class)
			.asHidden(true)
			.withTransformer(Path::toString, NIO::pathOrNull));
		builder.addProperty(ConfigurationProperty.ofType(PROPERTY_HISTORY_LAST_OPEN_FORMAT, MediaFormat.class)
			.asHidden(true)
			.withDefaultValue(MediaFormat.UNKNOWN.name())
			.withTransformer(MediaFormat::name, MediaFormat::fromName));
		builder.addProperty(ConfigurationProperty.ofType(PROPERTY_HISTORY_LAST_SAVE_FORMAT, MediaFormat.class)
			.asHidden(true)
			.withDefaultValue(MediaFormat.UNKNOWN.name())
			.withTransformer(MediaFormat::name, MediaFormat::fromName));
		
		// ----- Naming
		builder.addProperty(ConfigurationProperty.ofType(PROPERTY_NAMING_MEDIA_TITLE_FORMAT, NamedMediaTitleFormat.class)
			.inGroup(GROUP_NAMING)
			.withFactory(() -> Stream.concat(MediaTitleFormats.all().keySet().stream(), Stream.of("custom"))
			                         .collect(Collectors.toList()))
			.withTransformer((v) -> v.name(),
			                 (s) -> Optional.ofNullable(MediaTitleFormats.ofName(s))
			                                .map((v) -> new NamedMediaTitleFormat(s, v))
			                                .orElse(null))
			.withDefaultValue(MediaTitleFormats.defaultName()));
		builder.addProperty(ConfigurationProperty.ofString(PROPERTY_NAMING_CUSTOM_MEDIA_TITLE_FORMAT)
			.inGroup(GROUP_NAMING));
		
		// ----- Other
		builder.addProperty(ConfigurationProperty.ofBoolean(PROPERTY_AUTO_ENABLE_CLIPBOARD_WATCHER)
			.inGroup(GROUP_OTHER)
			.withDefaultValue(false));
		
		// Register custom configuration fields that will be display in the Configuration window
		ConfigurationWindow.registerFormField(PROPERTY_NAMING_CUSTOM_MEDIA_TITLE_FORMAT,
		                                      TextFieldMediaTitleFormat::new);
		
		return builder;
	}
	
	/** @since 00.02.05 */
	private final void loadFields() {
		version = Version.fromString(stringValue(PROPERTY_LANGUAGE));
		autoUpdateCheck = booleanValue(PROPERTY_AUTO_UPDATE_CHECK);
		acceleratedDownload = intValue(PROPERTY_ACCELERATED_DOWNLOAD);
		parallelDownloads = intValue(PROPERTY_PARALLEL_DOWNLOADS);
		parallelConversions = intValue(PROPERTY_PARALLEL_CONVERSIONS);
		computeStreamSize = booleanValue(PROPERTY_COMPUTE_STREAM_SIZE);
		requestTimeout = intValue(PROPERTY_REQUEST_TIMEOUT);
		checkResourcesIntegrity = booleanValue(PROPERTY_CHECK_RESOURCES_INTEGRITY);
		plugins_autoUpdateCheck = booleanValue(PROPERTY_PLUGINS_AUTO_UPDATE_CHECK);
		language = Optional.ofNullable(ResourceRegistry.language(stringValue(PROPERTY_LANGUAGE)))
				           .orElseGet(() -> ResourceRegistry.language("english"));
		theme = Optional.ofNullable(ResourceRegistry.theme(stringValue(PROPERTY_THEME)))
				        .orElseGet(() -> Theme.getDefault());
		history_lastDirectory = Optional.<Path>ofNullable(typeValue(PROPERTY_HISTORY_LAST_DIRECTORY))
				                        .filter(Files::isDirectory)
				                        .orElse(null);
		history_lastOpenFormat = Optional.<MediaFormat>ofNullable(typeValue(PROPERTY_HISTORY_LAST_OPEN_FORMAT))
				                         .orElse(MediaFormat.UNKNOWN);
		history_lastSaveFormat = Optional.<MediaFormat>ofNullable(typeValue(PROPERTY_HISTORY_LAST_SAVE_FORMAT))
				                         .orElse(MediaFormat.UNKNOWN);
		
		// Initialize the custom media title format first, so we can register it before getting
		// the actual media title format that will be used throughout the program.
		naming_customMediaTitleFormat = Optional.ofNullable(stringValue(PROPERTY_NAMING_CUSTOM_MEDIA_TITLE_FORMAT))
				                                .map((s) -> Utils.ignore(() -> MediaTitleFormat.of(s)) != null ? s : null)
				                                .orElse(null);
		
		// Register the custom media title format, if present
		if(naming_customMediaTitleFormat != null && !naming_customMediaTitleFormat.isEmpty()) {
			Utils.ignore(() -> MediaTitleFormats.register("custom", MediaTitleFormat.of(naming_customMediaTitleFormat)));
		}
		
		naming_mediaTitleFormat = Optional.<NamedMediaTitleFormat>ofNullable(typeValue(PROPERTY_NAMING_MEDIA_TITLE_FORMAT))
				                          .map(NamedMediaTitleFormat::format)
				                          .orElseGet(MediaTitleFormats::ofDefault);
		
		usePreReleaseVersions = booleanValue(PROPERTY_USE_PRE_RELEASE_VERSIONS);
		autoEnableClipboardWatcher = booleanValue(PROPERTY_AUTO_ENABLE_CLIPBOARD_WATCHER);
	}
	
	/** @since 00.02.05 */
	public Path path() {
		return path;
	}
	
	@Override
	public Version version() {
		return version;
	}
	
	@Override
	public Language language() {
		return language;
	}
	
	@Override
	public Theme theme() {
		return theme;
	}
	
	@Override
	public boolean isAutoUpdateCheck() {
		return autoUpdateCheck;
	}
	
	@Override
	public int acceleratedDownload() {
		return acceleratedDownload;
	}
	
	@Override
	public int parallelDownloads() {
		return parallelDownloads;
	}
	
	@Override
	public int parallelConversions() {
		return parallelConversions;
	}
	
	@Override
	public boolean computeStreamSize() {
		return computeStreamSize;
	}
	
	@Override
	public int requestTimeout() {
		return requestTimeout;
	}
	
	@Override
	public boolean isCheckResourcesIntegrity() {
		return checkResourcesIntegrity;
	}
	
	@Override
	public boolean isPluginsAutoUpdateCheck() {
		return plugins_autoUpdateCheck;
	}
	
	/** @since 00.02.05 */
	@Override
	public Path lastDirectory() {
		return history_lastDirectory;
	}
	
	/** @since 00.02.05 */
	@Override
	public MediaFormat lastOpenFormat() {
		return history_lastOpenFormat;
	}
	
	/** @since 00.02.05 */
	@Override
	public MediaFormat lastSaveFormat() {
		return history_lastSaveFormat;
	}
	
	/** @since 00.02.05 */
	@Override
	public MediaTitleFormat mediaTitleFormat() {
		return naming_mediaTitleFormat;
	}
	
	/** @since 00.02.05 */
	@Override
	public String customMediaTitleFormat() {
		return naming_customMediaTitleFormat;
	}
	
	/** @since 00.02.07 */
	@Override
	public boolean usePreReleaseVersions() {
		return usePreReleaseVersions;
	}
	
	/** @since 00.02.07 */
	@Override
	public boolean autoEnableClipboardWatcher() {
		return autoEnableClipboardWatcher;
	}
	
	public static final class Builder extends Configuration.Builder implements ApplicationConfigurationAccessor {
		
		private final Path path;
		
		public Builder(Path path) {
			super(NAME);
			this.path = Objects.requireNonNull(path);
		}
		
		@Override
		public Configuration build() {
			Map<String, ConfigurationProperty<?>> builtProperties = new LinkedHashMap<>();
			SSDCollection data = data(builtProperties);
			return new ApplicationConfiguration(path, name, data, builtProperties);
		}
		
		@Override
		public Version version() {
			return Version.fromString(accessor().stringValue(PROPERTY_VERSION));
		}
		
		@Override
		public Language language() {
			return Optional.ofNullable(ResourceRegistry.language(accessor().stringValue(PROPERTY_LANGUAGE)))
					       .orElseGet(() -> ResourceRegistry.language("english"));
		}
		
		@Override
		public Theme theme() {
			return Optional.ofNullable(ResourceRegistry.theme(accessor().stringValue(PROPERTY_THEME)))
					       .orElseGet(() -> Theme.getDefault());
		}
		
		@Override
		public boolean isAutoUpdateCheck() {
			return accessor().booleanValue(PROPERTY_AUTO_UPDATE_CHECK);
		}
		
		@Override
		public int acceleratedDownload() {
			return accessor().intValue(PROPERTY_ACCELERATED_DOWNLOAD);
		}
		
		@Override
		public int parallelDownloads() {
			return accessor().intValue(PROPERTY_PARALLEL_DOWNLOADS);
		}
		
		@Override
		public int parallelConversions() {
			return accessor().intValue(PROPERTY_PARALLEL_CONVERSIONS);
		}
		
		@Override
		public boolean computeStreamSize() {
			return accessor().booleanValue(PROPERTY_COMPUTE_STREAM_SIZE);
		}
		
		@Override
		public int requestTimeout() {
			return accessor().intValue(PROPERTY_REQUEST_TIMEOUT);
		}
		
		@Override
		public boolean isCheckResourcesIntegrity() {
			return accessor().booleanValue(PROPERTY_CHECK_RESOURCES_INTEGRITY);
		}
		
		@Override
		public boolean isPluginsAutoUpdateCheck() {
			return accessor().booleanValue(PROPERTY_PLUGINS_AUTO_UPDATE_CHECK);
		}
		
		/** @since 00.02.05 */
		@Override
		public Path lastDirectory() {
			return Optional.ofNullable(accessor().stringValue(PROPERTY_HISTORY_LAST_DIRECTORY))
						   .map(Path::of).filter(Files::isDirectory).orElse(null);
		}
		
		/** @since 00.02.05 */
		@Override
		public MediaFormat lastOpenFormat() {
			return Optional.ofNullable(accessor().stringValue(PROPERTY_HISTORY_LAST_OPEN_FORMAT))
						   .map(MediaFormat::fromName).orElse(MediaFormat.UNKNOWN);
		}
		
		/** @since 00.02.05 */
		@Override
		public MediaFormat lastSaveFormat() {
			return Optional.ofNullable(accessor().stringValue(PROPERTY_HISTORY_LAST_SAVE_FORMAT))
						   .map(MediaFormat::fromName).orElse(MediaFormat.UNKNOWN);
		}
		
		/** @since 00.02.05 */
		@Override
		public MediaTitleFormat mediaTitleFormat() {
			return Optional.ofNullable(accessor().stringValue(PROPERTY_NAMING_MEDIA_TITLE_FORMAT))
					       .map(MediaTitleFormats::ofName).orElseGet(MediaTitleFormats::ofDefault);
		}
		
		/** @since 00.02.05 */
		@Override
		public String customMediaTitleFormat() {
			return accessor().stringValue(PROPERTY_NAMING_CUSTOM_MEDIA_TITLE_FORMAT);
		}
		
		/** @since 00.02.07 */
		@Override
		public boolean usePreReleaseVersions() {
			return accessor().booleanValue(PROPERTY_USE_PRE_RELEASE_VERSIONS);
		}
		
		/** @since 00.02.07 */
		@Override
		public boolean autoEnableClipboardWatcher() {
			return accessor().booleanValue(PROPERTY_AUTO_ENABLE_CLIPBOARD_WATCHER);
		}
		
		@Override
		public SSDCollection data() {
			return accessor().data();
		}
	}
}