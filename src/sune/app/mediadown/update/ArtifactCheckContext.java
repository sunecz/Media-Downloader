package sune.app.mediadown.update;

import java.io.IOException;
import java.nio.file.Path;

/** @since 00.02.09 */
public interface ArtifactCheckContext {
	
	Artifact artifact();
	ArtifactCheckResult result();
	IOException exception();
	Path root();
}
