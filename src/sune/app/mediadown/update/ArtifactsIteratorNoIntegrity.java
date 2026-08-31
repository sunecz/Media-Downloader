package sune.app.mediadown.update;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import sune.app.mediadown.util.NIO;

/** @since 00.02.09 */
public final class ArtifactsIteratorNoIntegrity extends ArtifactsIteratorBase {
	
	private final Path root;
	private final Predicate<Artifact> skipArtifactFilter;
	private final Set<String> unchangedComponents;
	
	public ArtifactsIteratorNoIntegrity(
		List<Artifact> artifacts,
		Path root,
		Predicate<Artifact> skipArtifactFilter,
		Set<String> unchangedComponents
	) {
		super(artifacts);
		this.root = root;
		this.skipArtifactFilter = skipArtifactFilter;
		this.unchangedComponents = unchangedComponents;
	}
	
	@Override
	protected final boolean isArtifactOk(Artifact artifact) throws IOException {
		return skipArtifactFilter.test(artifact)
					|| (
						unchangedComponents.contains(artifact.component())
							&& NIO.isRegularFile(root.resolve(artifact.installPath()))
					);
	}
}
