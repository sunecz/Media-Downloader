package sune.app.mediadown.update;

import java.nio.file.Path;
import java.util.Objects;

import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.FileDownloader;
import sune.app.mediadown.download.InputStreamFactory;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.util.NIO;

/** @since 00.02.09 */
public class ArtifactDownloader extends FileDownloader {
	
	protected final DownloadTracker tracker;
	protected final Path root;
	
	public ArtifactDownloader(TrackerManager trackerManager, Path root) {
		super(trackerManager);
		this.root = Objects.requireNonNull(root);
		this.tracker = new DownloadTracker();
		setTracker(tracker);
	}
	
	protected Path artifactPath(Artifact artifact) {
		return root.resolve(artifact.installPath());
	}
	
	public long download(Artifact artifact) throws Exception {
		Objects.requireNonNull(artifact);
		Path destination = artifactPath(artifact);
		
		// To be sure, delete the file first, so a fresh copy is downloaded.
		NIO.deleteFile(destination);
		NIO.createDir(destination.getParent());
		
		switch(artifact.encoding()) {
			case GZIP:
				setResponseStreamFactory(InputStreamFactory.GZIP.ofDefault());
				break;
		}
		
		tracker.reset();
		
		long bytes = start(
			Request.of(artifact.uri()).GET(),
			destination,
			DownloadConfiguration.ofDefault()
		);
		
		if(artifact.executable()) {
			NIO.makeExecutable(destination);
		}
		
		return bytes;
	}
}
