package sune.app.mediadown.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;

import sune.app.mediadown.Download;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.Shared;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.FileDownloader;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.update.Version;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Web;
import sune.app.mediadown.util.Web.GetRequest;
import sune.app.mediadown.util.Web.StringResponse;

public final class PluginUpdater {
	
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
						versionPlugin = Version.of(line.substring(index + 1));
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
					? newestVersion(plugin.updateBaseURL(),
					                Version.of(plugin.version()))
					: Version.UNKNOWN; // Do not return null
	}
	
	public static final boolean isNewestVersion(PluginFile file) {
		Version newest;
		return (newest = newestVersion(file)) == Version.UNKNOWN
					|| newest.compareTo(Version.of(file.getPlugin().instance().version())) == 0;
	}
	
	public static final String newestVersionURL(String baseURL) {
		return versionURL(baseURL, newestVersion(baseURL, Version.UNKNOWN));
	}
	
	public static final String newestVersionURL(PluginFile file) {
		return versionURL(file, newestVersion(file));
	}
	
	/** @since 00.02.07 */
	public static final String pluginVersionString(Version version) {
		return version == Version.UNKNOWN ? version.string() : String.format("%04d", version.major());
	}
	
	public static final String versionURL(String baseURL, Version version) {
		return Utils.urlFixSlashes(Utils.urlConcat(baseURL, pluginVersionString(version)));
	}
	
	public static final String versionURL(PluginFile file, Version version) {
		return versionURL(file.getPlugin().instance().updateBaseURL(), version);
	}
	
	public static final String versionURL(PluginFile file) {
		return versionURL(file, Version.of(file.getPlugin().instance().version()));
	}
	
	public static final Download update(String pluginURL, Path file) {
		return new Download() {
			
			private final FileDownloader downloader = new FileDownloader(new TrackerManager());
			
			@Override
			public void start() throws Exception {
				GetRequest request = new GetRequest(Utils.url(pluginURL), Shared.USER_AGENT);
				long size = Utils.ignore(() -> Web.size(request.toHeadRequest()), -1L);
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
			public <E> void addEventListener(EventType<DownloadEvent, E> type, Listener<E> listener) {
				downloader.addEventListener(type, listener);
			}
			
			@Override
			public <E> void removeEventListener(EventType<DownloadEvent, E> type, Listener<E> listener) {
				downloader.removeEventListener(type, listener);
			}
		};
	}
	
	public static final String check(PluginFile file) {
		return !isNewestVersion(file) ? Utils.urlConcat(newestVersionURL(file), "plugin.jar") : null;
	}
	
	/** @since 00.01.27 */
	public static final Version checkVersion(PluginFile file) {
		return !isNewestVersion(file) ? newestVersion(file) : null;
	}
	
	// Forbid anyone to create an instance of this class
	private PluginUpdater() {
	}
}