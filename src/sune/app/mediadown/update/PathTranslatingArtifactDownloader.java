package sune.app.mediadown.update;

import java.nio.file.Path;
import java.util.Objects;

import sune.app.mediadown.event.tracker.TrackerManager;

/** @since 00.02.09 */
public class PathTranslatingArtifactDownloader extends ArtifactDownloader {
	
	protected final PathTranslator translator;
	
	public PathTranslatingArtifactDownloader(
		TrackerManager trackerManager,
		Path root,
		PathTranslator translator
	) {
		super(trackerManager, root);
		this.translator = Objects.requireNonNull(translator);
	}
	
	@Override
	protected Path artifactPath(Artifact artifact) {
		return root.resolve(translator.translate(artifact.installPath()));
	}
}
