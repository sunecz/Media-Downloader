package sune.app.mediadown.update;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

/** @since 00.02.09 */
public final class StrictIntegrityArtifactsIterator extends ArtifactsIteratorBase {
	
	private final Predicate<Artifact> skipArtifactFilter;
	private final ArtifactChecker checker;
	
	public StrictIntegrityArtifactsIterator(
		List<Artifact> artifacts,
		Predicate<Artifact> skipArtifactFilter,
		ArtifactChecker checker
	) {
		super(artifacts);
		this.skipArtifactFilter = skipArtifactFilter;
		this.checker = checker;
	}
	
	@Override
	protected final boolean isArtifactOk(Artifact artifact) throws IOException {
		return skipArtifactFilter.test(artifact)
					|| checker.check(artifact) == ArtifactCheckResult.OK;
	}
}
