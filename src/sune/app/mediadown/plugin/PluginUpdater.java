package sune.app.mediadown.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.regex.Pattern;

import sune.app.mediadown.Download;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.Shared;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.FileDownloader;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.update.Version;
import sune.app.mediadown.update.VersionType;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;
import sune.app.mediadown.util.Web;
import sune.app.mediadown.util.Web.GetRequest;
import sune.app.mediadown.util.Web.StringResponse;

public final class PluginUpdater {
	
	/** @since 00.02.08 */
	private static final Pattern REGEX_ONLY_DIGITS = Pattern.compile("^\\d+$");
	/** @since 00.02.08 */
	private static final Pattern REGEX_VERSION_VALUE = Pattern.compile("-(\\d+)$");
	
	/** @since 00.02.08 */
	private static final String fixPluginVersionString(Version version) {
		return REGEX_VERSION_VALUE.matcher(version.string())
					.replaceFirst((r) -> String.format("-%04d", Integer.valueOf(r.group(1))));
	}
	
	/** @since 00.02.08 */
	public static final Version pluginVersion(String string) {
		// Prefix the given version string with zero release version and treat
		// the whole version string as a value, rather than a major version.
		// This is done due to the transition to the new format of plugin versions
		// that are now prefixed with the minimum application version with which
		// they are compatible with. This solves compatibility issues with
		// comparison between the old format versions and the new ones.
		if(REGEX_ONLY_DIGITS.matcher(string).matches()) {
			string = "00.00.00-" + string;
		}
		
		return Version.of(string);
	}
	
	public static final Version newestVersion(String baseURL, Version versionPlugin) {
		if((versionPlugin == null)) return Version.UNKNOWN;
		if((baseURL == null || baseURL.isEmpty())) return versionPlugin;
		String configURL = Utils.urlFixSlashes(Utils.urlConcat(baseURL, "config"));
		StringResponse response;
		try {
			response = Web.request(new GetRequest(Utils.url(configURL), Shared.USER_AGENT));
		} catch(Exception ex) {
			// Request failed, nothing else to do
			return versionPlugin;
		}
		Version localAppVersion = MediaDownloader.VERSION.release();
		try(BufferedReader reader = new BufferedReader(new StringReader(response.content))) {
			String line;
			while((line = reader.readLine()) != null) {
				if((line.isEmpty())) continue;
				int index = line.indexOf(':');
				if((index >= 0)) {
					Version remoteAppVersion = Version.of(line.substring(0, index)).release();
					if((localAppVersion.compareTo(remoteAppVersion) >= 0)) {
						versionPlugin = pluginVersion(line.substring(index + 1));
					} else break;
				}
			}
		} catch(IOException ex) {
			// Ignore
		}
		return versionPlugin;
	}
	
	public static final Version newestVersion(PluginFile file) {
		Plugin plugin = file.getPlugin().instance();
		return plugin.updatable()
					? newestVersion(plugin.updateBaseURL(), pluginVersion(plugin.version()))
					: Version.UNKNOWN; // Do not return null
	}
	
	public static final boolean isNewestVersion(PluginFile file) {
		Version newest;
		return (newest = newestVersion(file)) == Version.UNKNOWN
					|| newest.compareTo(pluginVersion(file.getPlugin().instance().version())) == 0;
	}
	
	/** @since 00.02.08 */
	public static final boolean isNewerVersionAvailable(PluginFile file) {
		Version newest;
		return (newest = newestVersion(file)) == Version.UNKNOWN
					|| newest.compareTo(pluginVersion(file.getPlugin().instance().version())) > 0;
	}
	
	public static final String newestVersionURL(String baseURL) {
		return versionURL(baseURL, newestVersion(baseURL, Version.UNKNOWN));
	}
	
	public static final String newestVersionURL(PluginFile file) {
		return versionURL(file, newestVersion(file));
	}
	
	/** @since 00.02.07 */
	public static final String pluginVersionString(Version version) {
		if(version == Version.UNKNOWN) {
			return version.string();
		}
		
		// Treat the old format versions the same way as before
		if(version.type() == VersionType.RELEASE
				&& version.major() == 0
				&& version.minor() == 0
				&& version.patch() == 0) {
			return String.format("%04d", version.value());
		}
		
		return fixPluginVersionString(version);
	}
	
	public static final String versionURL(String baseURL, Version version) {
		return Utils.urlFixSlashes(Utils.urlConcat(baseURL, pluginVersionString(version)));
	}
	
	public static final String versionURL(PluginFile file, Version version) {
		return versionURL(file.getPlugin().instance().updateBaseURL(), version);
	}
	
	public static final String versionURL(PluginFile file) {
		return versionURL(file, pluginVersion(file.getPlugin().instance().version()));
	}
	
	public static final Download update(String pluginURL, Path file) {
		return new Download() {
			
			private final FileDownloader downloader = new FileDownloader(new TrackerManager());
			
			@Override
			public void start() throws Exception {
				GetRequest request = new GetRequest(Utils.url(pluginURL), Shared.USER_AGENT);
				long size = Ignore.defaultValue(() -> Web.size(request.toHeadRequest()), -1L);
				downloader.start(request, file, new DownloadConfiguration(size));
			}
			
			@Override
			public void stop() throws Exception {
				downloader.stop();
			}
			
			@Override
			public void pause() throws Exception {
				downloader.pause();
			}
			
			@Override
			public void resume() throws Exception {
				downloader.resume();
			}
			
			@Override
			public boolean isRunning() {
				return downloader.isRunning();
			}
			
			@Override
			public boolean isDone() {
				return downloader.isDone();
			}
			
			@Override
			public boolean isStarted() {
				return downloader.isStarted();
			}
			
			@Override
			public boolean isPaused() {
				return downloader.isPaused();
			}
			
			@Override
			public boolean isStopped() {
				return downloader.isStopped();
			}
			
			@Override
			public boolean isError() {
				return downloader.isError();
			}
			
			@Override
			public <V> void addEventListener(Event<? extends DownloadEvent, V> event, Listener<V> listener) {
				downloader.addEventListener(event, listener);
			}
			
			@Override
			public <V> void removeEventListener(Event<? extends DownloadEvent, V> event, Listener<V> listener) {
				downloader.removeEventListener(event, listener);
			}
		};
	}
	
	public static final String check(PluginFile file) {
		return isNewerVersionAvailable(file) ? Utils.urlConcat(newestVersionURL(file), "plugin.jar") : null;
	}
	
	/** @since 00.01.27 */
	public static final Version checkVersion(PluginFile file) {
		return isNewerVersionAvailable(file) ? newestVersion(file) : null;
	}
	
	// Forbid anyone to create an instance of this class
	private PluginUpdater() {
	}
}