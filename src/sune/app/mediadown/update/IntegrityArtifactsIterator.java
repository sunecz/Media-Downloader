package sune.app.mediadown.update;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** @since 00.02.09 */
public final class IntegrityArtifactsIterator extends ArtifactsIteratorBase {
	
	private final Predicate<Artifact> skipArtifactFilter;
	private final ArtifactChecker checker;
	private final Set<String> unchangedComponents;
	
	public IntegrityArtifactsIterator(
		List<Artifact> artifacts,
		Predicate<Artifact> skipArtifactFilter,
		ArtifactChecker checker,
		Set<String> unchangedComponents
	) {
		super(artifacts);
		this.skipArtifactFilter = skipArtifactFilter;
		this.checker = checker;
		this.unchangedComponents = unchangedComponents;
	}
	
	@Override
	protected final boolean isArtifactOk(Artifact artifact) throws IOException {
		if(artifact.component().equals("application")) {
			System.out.println(skipArtifactFilter.test(artifact));
			System.out.println(unchangedComponents.contains(artifact.component()));
			System.out.println(checker.checkExistanceOnly(artifact));
			System.out.println(checker.check(artifact));
		}
		
		return skipArtifactFilter.test(artifact)
					|| (
						unchangedComponents.contains(artifact.component())
							? checker.checkExistanceOnly(artifact) == ArtifactCheckResult.OK
							: checker.check(artifact) == ArtifactCheckResult.OK
					);
	}
}
