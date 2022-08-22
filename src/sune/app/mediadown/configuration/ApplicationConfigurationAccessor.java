package sune.app.mediadown.configuration;

import java.nio.file.Path;

import sune.app.mediadown.language.Language;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaTitleFormat;
import sune.app.mediadown.theme.Theme;
import sune.app.mediadown.update.Version;
import sune.util.ssdf2.SSDCollection;

/** @since 00.02.04 */
public interface ApplicationConfigurationAccessor {
	
	// ----- Names of configuration groups
	/** @since 00.02.07 */
	public static final String GROUP_GENERAL = "general";
	/** @since 00.02.07 */
	public static final String GROUP_DOWNLOAD = "download";
	/** @since 00.02.07 */
	public static final String GROUP_CONVERSION = "conversion";
	/** @since 00.02.07 */
	public static final String GROUP_PLUGINS = "plugins";
	/** @since 00.02.07 */
	public static final String GROUP_NAMING = "naming";
	/** @since 00.02.07 */
	public static final String GROUP_OTHER = "other";
	
	// ----- Names of configuration properties
	public static final String PROPERTY_VERSION = "version";
	public static final String PROPERTY_LANGUAGE = "language";
	public static final String PROPERTY_THEME = "theme";
	public static final String PROPERTY_AUTO_UPDATE_CHECK = "autoUpdateCheck";
	public static final String PROPERTY_ACCELERATED_DOWNLOAD = "acceleratedDownload";
	public static final String PROPERTY_PARALLEL_DOWNLOADS = "parallelDownloads";
	public static final String PROPERTY_PARALLEL_CONVERSIONS = "parallelConversions";
	public static final String PROPERTY_COMPUTE_STREAM_SIZE = "computeStreamSize";
	public static final String PROPERTY_REQUEST_TIMEOUT = "requestTimeout";
	public static final String PROPERTY_CHECK_RESOURCES_INTEGRITY = "checkResourcesIntegrity";
	public static final String PROPERTY_PLUGINS_AUTO_UPDATE_CHECK = "plugins.autoUpdateCheck";
	/** @since 00.02.05 */
	public static final String PROPERTY_HISTORY_LAST_DIRECTORY = "history.lastDirectory";
	/** @since 00.02.05 */
	public static final String PROPERTY_HISTORY_LAST_OPEN_FORMAT = "history.lastOpenFormat";
	/** @since 00.02.05 */
	public static final String PROPERTY_HISTORY_LAST_SAVE_FORMAT = "history.lastSaveFormat";
	/** @since 00.02.05 */
	public static final String PROPERTY_NAMING_MEDIA_TITLE_FORMAT = "naming.mediaTitleFormat";
	/** @since 00.02.05 */
	public static final String PROPERTY_NAMING_CUSTOM_MEDIA_TITLE_FORMAT = "naming.customMediaTitleFormat";
	/** @since 00.02.07 */
	public static final String PROPERTY_USE_PRE_RELEASE_VERSIONS = "usePreReleaseVersions";
	/** @since 00.02.07 */
	public static final String PROPERTY_AUTO_ENABLE_CLIPBOARD_WATCHER = "autoEnableClipboardWatcher";
	
	Version version();
	Language language();
	Theme theme();
	boolean isAutoUpdateCheck();
	int acceleratedDownload();
	int parallelDownloads();
	int parallelConversions();
	boolean computeStreamSize();
	int requestTimeout();
	boolean isCheckResourcesIntegrity();
	boolean isPluginsAutoUpdateCheck();
	/** @since 00.02.05 */
	Path lastDirectory();
	/** @since 00.02.05 */
	MediaFormat lastOpenFormat();
	/** @since 00.02.05 */
	MediaFormat lastSaveFormat();
	/** @since 00.02.05 */
	MediaTitleFormat mediaTitleFormat();
	/** @since 00.02.05 */
	String customMediaTitleFormat();
	/** @since 00.02.07 */
	UsePreReleaseVersions usePreReleaseVersions();
	/** @since 00.02.07 */
	boolean autoEnableClipboardWatcher();
	
	SSDCollection data();
	
	/** @since 00.02.07 */
	public static enum UsePreReleaseVersions {
		
		ALWAYS, TILL_NEXT_RELEASE, NEVER;
	}
}