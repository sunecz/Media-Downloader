package sune.app.mediadown.update;

import java.util.List;

/** @since 00.02.09 */
public class ArtifactsState {
	
	protected final List<Artifact> artifacts;
	protected final Manifest manifest;
	
	public ArtifactsState(List<Artifact> artifacts, Manifest manifest) {
		this.artifacts = artifacts;
		this.manifest = manifest;
	}
	
	public List<Artifact> artifacts() {
		return artifacts;
	}
	
	public Manifest manifest() {
		return manifest;
	}
}
