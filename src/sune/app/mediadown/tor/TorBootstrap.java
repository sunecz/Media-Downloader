package sune.app.mediadown.tor;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import sune.app.mediadown.MediaDownloader.Versions.VersionEntryAccessor;
import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.PatchEvent;
import sune.app.mediadown.resource.Patcher;
import sune.app.mediadown.resource.Resources;
import sune.app.mediadown.update.RemoteConfiguration;
import sune.app.mediadown.update.Requirements;
import sune.app.mediadown.update.Version;
import sune.app.mediadown.util.NIO;

/** @since 00.02.10 */
public final class TorBootstrap implements EventBindable<EventType> {
	
	private static final Version torVersion = Version.of("0.4.7-14");
	private static final VarLoader<TorBootstrap> instance = VarLoader.of(TorBootstrap::new);
	
	private final EventRegistry<EventType> eventRegistry = new EventRegistry<>();
	private final AtomicBoolean wasRun = new AtomicBoolean();
	
	// Forbid anyone to create an instance of this class
	private TorBootstrap() {
	}
	
	public static TorBootstrap instance() {
		return instance.value();
	}
	
	private final void checkResources(boolean checkIntegrity) throws Exception {
		Version versionRemote = torVersion;
		Path directory = NIO.localPath("resources/binary/tor");
		Patcher.ResourceList localList = Patcher.ResourceList.ofLocal(directory, NIO.localPath(), torVersion);
		URI uri = Resources.baseUri("tor", versionRemote, Requirements.CURRENT);
		RemoteConfiguration config = RemoteConfiguration.of(uri.resolve("config"));
		URI uriList = uri.resolve(config.value("list"));
		URI uriData = uri.resolve(config.value("data") + '/');
		Patcher.ResourceList remoteList = Patcher.ResourceList.ofRemote(uriList, uriData);
		
		VersionEntryAccessor version = VersionEntryAccessor.of("res_tor");
		Version versionLocal = version.get();
		checkIntegrity = checkIntegrity || versionLocal == Version.UNKNOWN || !versionLocal.equals(torVersion);
		
		Patcher.Builder builder = Patcher.builder(localList);
		builder.useCompressedStreams(true);
		builder.checkIntegrity(checkIntegrity);
		
		Patcher patcher = builder.build();
		eventRegistry.bindAll(patcher, PatchEvent.values());
		eventRegistry.bindAll(patcher, DownloadEvent.values());
		patcher.patch(remoteList);
		version.set(versionRemote);
	}
	
	public final void bootstrap(boolean checkIntegrity) throws Exception {
		// Return, if already run, otherwise set to true
		if(!wasRun.compareAndSet(false, true)) {
			return;
		}
		
		checkResources(checkIntegrity);
	}
	
	@Override
	public <V> void addEventListener(Event<? extends EventType, V> event, Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	@Override
	public <V> void removeEventListener(Event<? extends EventType, V> event, Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
}