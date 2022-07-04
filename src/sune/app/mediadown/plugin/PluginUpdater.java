package sune.app.mediadown.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;

import sune.app.mediadown.Download;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.Shared;
import sune.app.mediadown.download.SingleFileDownloader;
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
		Version versionApp = MediaDownloader.VERSION;
		try(BufferedReader reader = new BufferedReader(new StringReader(response.content))) {
			String line;
			while((line = reader.readLine()) != null) {
				if((line.isEmpty())) continue;
				int index = line.indexOf(':');
				if((index >= 0)) {
					Version verApp = Version.fromString(line.substring(0, index));
					if((versionApp.compareTo(verApp) >= 0)) {
						versionPlugin = Version.fromString(line.substring(index + 1));
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
					                Version.fromString(plugin.version()))
					: Version.UNKNOWN; // Do not return null
	}
	
	public static final boolean hasNewerVersion(PluginFile file) {
		Version newest;
		return (newest = newestVersion(file)) != Version.UNKNOWN
					&& newest.compareTo(Version.fromString(file.getPlugin().instance().version())) > 0;
	}
	
	public static final String newestVersionURL(String baseURL) {
		return versionURL(baseURL, newestVersion(baseURL, Version.UNKNOWN));
	}
	
	public static final String newestVersionURL(PluginFile file) {
		return versionURL(file, newestVersion(file));
	}
	
	public static final String versionURL(String baseURL, Version version) {
		return version != Version.UNKNOWN
					? Utils.urlFixSlashes(Utils.urlConcat(baseURL, version.string()))
					: Version.UNKNOWN.string(); // Do not return null
	}
	
	public static final String versionURL(PluginFile file, Version version) {
		return versionURL(file.getPlugin().instance().updateBaseURL(), version);
	}
	
	public static final String versionURL(PluginFile file) {
		return versionURL(file, Version.fromString(file.getPlugin().instance().version()));
	}
	
	public static final Download update(String pluginURL, Path file) {
		return new Download() {
			
			private final GetRequest request;
			private final long size;
			private final SingleFileDownloader downloader;
			
			{
				request    = new GetRequest(Utils.url(pluginURL), Shared.USER_AGENT);
				size       = Utils.ignore(() -> Web.size(request.toHeadRequest()), -1L);
				downloader = new SingleFileDownloader(new TrackerManager());
			}
			
			@Override
			public void start() {
				downloader.start(request, file, this, size);
			}
			
			@Override
			public void stop() {
				downloader.stop();
			}
			
			@Override
			public void pause() {
				downloader.pause();
			}
			
			@Override
			public void resume() {
				downloader.resume();
			}
			
			@Override
			public boolean isStarted() {
				return downloader.isStarted();
			}
			
			@Override
			public boolean isRunning() {
				return downloader.isRunning();
			}
			
			@Override
			public boolean isPaused() {
				return downloader.isPaused();
			}
			
			@Override
			public boolean isDone() {
				return downloader.isDone();
			}
			
			@Override
			public boolean isStopped() {
				return downloader.isStopped();
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
		return hasNewerVersion(file) ? Utils.urlConcat(newestVersionURL(file), "plugin.jar") : null;
	}
	
	/** @since 00.01.27 */
	public static final Version checkVersion(PluginFile file) {
		return hasNewerVersion(file) ? newestVersion(file) : null;
	}
	
	// Forbid anyone to create an instance of this class
	private PluginUpdater() {
	}
}