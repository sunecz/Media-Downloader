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

import sune.app.mediadown.Defaults;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.conversion.ConversionProvider;
import sune.app.mediadown.conversion.Conversions;
import sune.app.mediadown.language.Language;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaTitleFormat;
import sune.app.mediadown.media.MediaTitleFormats;
import sune.app.mediadown.media.MediaTitleFormats.NamedMediaTitleFormat;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.resource.ResourceRegistry;
import sune.app.mediadown.theme.Theme;
import sune.app.mediadown.update.Version;
import sune.app.mediadown.update.VersionType;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Utils.Ignore;
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
	/** @since 00.02.08 */
	private int requestConnectTimeout;
	/** @since 00.02.08 */
	private int requestReadTimeout;
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
	private UsePreReleaseVersions usePreReleaseVersions;
	/** @since 00.02.07 */
	private boolean autoEnableClipboardWatcher;
	/** @since 00.02.09 */
	private ConversionProvider conversionProvider;
	
	private ApplicationConfiguration(Path path, String name, SSDCollection data, Map<String, ConfigurationProperty<?>> properties) {
		super(name, data, properties);
		this.path = Objects.requireNonNull(path);
		this.loadFields();
	}
	
	public static final ApplicationConfiguration.Builder builder(Path path) {
		ApplicationConfiguration.Builder builder = new ApplicationConfiguration.Builder(path);
		
		// ----- Hidden
		builder.addProperty(ConfigurationProperty.ofString(PROPERTY_VERSION).asHidden(true)
			.withDefaultValue(MediaDownloader.version().toString()));
		builder.addProperty(ConfigurationProperty.ofArray(PROPERTY_REMOVE_AT_INIT).asHidden(true));
		
		// ----- General
		builder.addProperty(ConfigurationProperty.ofType(PROPERTY_LANGUAGE, Language.class)
			.inGroup(GROUP_GENERAL)
			.withFactory(() -> Stream.concat(List.of("auto").stream(),
			                                 ResourceRegistry.languages.values().stream()
			                                        .map(Language::name))
	                                 .collect(Collectors.toList()))
			.withTransformer((v) -> v.name(),
			                 (s) -> ResourceRegistry.languages.values().stream()
			                            .filter((l) -> l.name().equals(s))
			                            .findFirst().orElse(null))
			.withDefaultValue("auto"));
		builder.addProperty(ConfigurationProperty.ofType(PROPERTY_THEME, Theme.class)
			.inGroup(GROUP_GENERAL)
			.withFactory(() -> ResourceRegistry.themes.values().stream()
			                        .map(Theme::name)
			                        .collect(Collectors.toList()))
			.withTransformer((v) -> v.name(),
			                 (s) -> ResourceRegistry.themes.values().stream()
			                            .filter((t) -> t.name().equals(s))
			                            .findFirst().orElse(null))
			.withDefaultValue(Theme.ofDefault().name()));
		builder.addProperty(ConfigurationProperty.ofBoolean(PROPERTY_AUTO_UPDATE_CHECK)
			.inGroup(GROUP_GENERAL)
			.withDefaultValue(true));
		builder.addProperty(ConfigurationProperty.ofBoolean(PROPERTY_CHECK_RESOURCES_INTEGRITY)
			.inGroup(GROUP_GENERAL)
			.withDefaultValue(false));
		builder.addProperty(ConfigurationProperty.ofType(PROPERTY_USE_PRE_RELEASE_VERSIONS, UsePreReleaseVersions.class)
			.inGroup(GROUP_GENERAL)
			.withFactory(() -> Stream.concat(
			                       // Compatibility with old boolean values
			                       Stream.of("true", "false"),
			                       Stream.of(UsePreReleaseVersions.values())
			                             .map(Enum::name)
			                   ).collect(Collectors.toList()))
			.withTransformer(Enum::name,
			                 UsePreReleaseVersions::of)
			.withDefaultValue((MediaDownloader.version().type() != VersionType.RELEASE
			                      ? UsePreReleaseVersions.ALWAYS
			                      : UsePreReleaseVersions.NEVER
			                  ).name()));
		
		// ----- Download
		builder.addProperty(ConfigurationProperty.ofInteger(PROPERTY_ACCELERATED_DOWNLOAD)
			.inGroup(GROUP_DOWNLOAD)
			.withDefaultValue(4)
			.withOrder(10));
		builder.addProperty(ConfigurationProperty.ofInteger(PROPERTY_PARALLEL_DOWNLOADS)
			.inGroup(GROUP_DOWNLOAD)
			.withDefaultValue(3)
			.withOrder(20));
		builder.addProperty(ConfigurationProperty.ofBoolean(PROPERTY_COMPUTE_STREAM_SIZE)
			.inGroup(GROUP_DOWNLOAD)
			.withDefaultValue(true)
			.withOrder(30));
		builder.addProperty(ConfigurationProperty.ofInteger(PROPERTY_REQUEST_CONNECT_TIMEOUT)
			.inGroup(GROUP_DOWNLOAD)
			.withDefaultValue(Web.defaultConnectTimeout().toMillis())
			.withOrder(40));
		builder.addProperty(ConfigurationProperty.ofInteger(PROPERTY_REQUEST_READ_TIMEOUT)
			.inGroup(GROUP_DOWNLOAD)
			.withDefaultValue(Web.defaultReadTimeout().toMillis())
			.withOrder(50));
		
		// ----- Conversion
		builder.addProperty(ConfigurationProperty.ofInteger(PROPERTY_PARALLEL_CONVERSIONS)
			.inGroup(GROUP_CONVERSION)
			.withDefaultValue(1));
		builder.addProperty(ConfigurationProperty.ofType(PROPERTY_CONVERSION_PROVIDER, ConversionProvider.class)
			.inGroup(GROUP_CONVERSION)
			.withFactory(Conversions.Providers.registry()::allNames)
			.withTransformer(ConversionProvider::name, Conversions.Providers::ofName)
			.withDefaultValue(Defaults.CONVERSION_PROVIDER_NAME));
		
		// ----- Plugins
		builder.addProperty(ConfigurationProperty.ofBoolean(PROPERTY_PLUGINS_AUTO_UPDATE_CHECK)
			.inGroup(GROUP_PLUGINS)
			.withDefaultValue(true));
		
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
		
		return builder;
	}
	
	/** @since 00.02.05 */
	private final void loadFields() {
		version = Version.of(stringValue(PROPERTY_VERSION));
		autoUpdateCheck = booleanValue(PROPERTY_AUTO_UPDATE_CHECK);
		acceleratedDownload = intValue(PROPERTY_ACCELERATED_DOWNLOAD);
		parallelDownloads = intValue(PROPERTY_PARALLEL_DOWNLOADS);
		parallelConversions = intValue(PROPERTY_PARALLEL_CONVERSIONS);
		computeStreamSize = booleanValue(PROPERTY_COMPUTE_STREAM_SIZE);
		requestConnectTimeout = intValue(PROPERTY_REQUEST_CONNECT_TIMEOUT);
		requestReadTimeout = intValue(PROPERTY_REQUEST_READ_TIMEOUT);
		checkResourcesIntegrity = booleanValue(PROPERTY_CHECK_RESOURCES_INTEGRITY);
		plugins_autoUpdateCheck = booleanValue(PROPERTY_PLUGINS_AUTO_UPDATE_CHECK);
		language = Optional.ofNullable(ResourceRegistry.language(stringValue(PROPERTY_LANGUAGE)))
				           .orElseGet(() -> ResourceRegistry.language("english"));
		theme = Optional.ofNullable(ResourceRegistry.theme(stringValue(PROPERTY_THEME)))
				        .orElseGet(Theme::ofDefault);
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
				                                .map((s) -> Ignore.call(() -> MediaTitleFormat.of(s)) != null ? s : null)
				                                .orElse(null);
		
		// Register the custom media title format, if present
		if(naming_customMediaTitleFormat != null && !naming_customMediaTitleFormat.isEmpty()) {
			Ignore.callVoid(() -> MediaTitleFormats.register("custom", MediaTitleFormat.of(naming_customMediaTitleFormat)));
		}
		
		naming_mediaTitleFormat = Optional.<NamedMediaTitleFormat>ofNullable(typeValue(PROPERTY_NAMING_MEDIA_TITLE_FORMAT))
				                          .map(NamedMediaTitleFormat::format)
				                          .orElseGet(MediaTitleFormats::ofDefault);
		
		usePreReleaseVersions = UsePreReleaseVersions.of(stringValue(PROPERTY_USE_PRE_RELEASE_VERSIONS));
		autoEnableClipboardWatcher = booleanValue(PROPERTY_AUTO_ENABLE_CLIPBOARD_WATCHER);
		conversionProvider = Conversions.Providers.ofName(stringValue(PROPERTY_CONVERSION_PROVIDER));
	}
	
	/** @since 00.02.07 */
	@Override
	public boolean reload() {
		loadFields();
		return true;
	}
	
	/** @since 00.02.05 */
	@Override
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
	
	/** @since 00.02.08 */
	@Override
	public int requestConnectTimeout() {
		return requestConnectTimeout;
	}
	
	/** @since 00.02.08 */
	@Override
	public int requestReadTimeout() {
		return requestReadTimeout;
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
	public UsePreReleaseVersions usePreReleaseVersions() {
		return usePreReleaseVersions;
	}
	
	/** @since 00.02.07 */
	@Override
	public boolean autoEnableClipboardWatcher() {
		return autoEnableClipboardWatcher;
	}
	
	/** @since 00.02.09 */
	@Override
	public ConversionProvider conversionProvider() {
		return conversionProvider;
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
			return Version.of(accessor().stringValue(PROPERTY_VERSION));
		}
		
		@Override
		public Language language() {
			return Optional.ofNullable(ResourceRegistry.language(accessor().stringValue(PROPERTY_LANGUAGE)))
					       .orElseGet(() -> ResourceRegistry.language("english"));
		}
		
		@Override
		public Theme theme() {
			return Optional.ofNullable(ResourceRegistry.theme(accessor().stringValue(PROPERTY_THEME)))
					       .orElseGet(Theme::ofDefault);
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
		
		/** @since 00.02.08 */
		@Override
		public int requestConnectTimeout() {
			return accessor().intValue(PROPERTY_REQUEST_CONNECT_TIMEOUT);
		}
		
		/** @since 00.02.08 */
		@Override
		public int requestReadTimeout() {
			return accessor().intValue(PROPERTY_REQUEST_READ_TIMEOUT);
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
		public UsePreReleaseVersions usePreReleaseVersions() {
			return UsePreReleaseVersions.of(accessor().stringValue(PROPERTY_USE_PRE_RELEASE_VERSIONS));
		}
		
		/** @since 00.02.07 */
		@Override
		public boolean autoEnableClipboardWatcher() {
			return accessor().booleanValue(PROPERTY_AUTO_ENABLE_CLIPBOARD_WATCHER);
		}
		
		/** @since 00.02.09 */
		@Override
		public ConversionProvider conversionProvider() {
			return Conversions.Providers.ofName(accessor().stringValue(PROPERTY_CONVERSION_PROVIDER));
		}
		
		@Override
		public SSDCollection data() {
			return accessor().data();
		}
		
		/** @since 00.02.07 */
		@Override
		public boolean reload() {
			// Do nothing
			return true;
		}
		
		/** @since 00.02.07 */
		@Override
		public Path path() {
			return path;
		}
	}
}