package sune.app.mediadown.update;

import java.util.Collections;
import java.util.Iterator;

/** @since 00.02.09 */
public interface ArtifactsIterator extends Iterator<Artifact> {
	
	static ArtifactsIterator empty() {
		return (ArtifactsIterator) Collections.<Artifact>emptyIterator();
	}
}
