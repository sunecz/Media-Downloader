package sune.app.mediadown.update;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/** @since 00.02.09 */
abstract class ArtifactsIteratorBase implements ArtifactsIterator {
	
	protected final Iterator<Artifact> iterator;
	protected Artifact next;
	
	public ArtifactsIteratorBase(List<Artifact> artifacts) {
		this.iterator = artifacts.iterator();
	}
	
	protected abstract boolean isArtifactOk(Artifact artifact) throws IOException;
	
	protected final boolean probe() throws IOException {
		next = null;
		
		for(Artifact artifact; iterator.hasNext();) {
			artifact = iterator.next();
			
			if(!isArtifactOk(artifact)) {
				next = artifact;
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public boolean hasNext() {
		try {
			return probe();
		} catch(IOException ex) {
			return false;
		}
	}
	
	@Override
	public Artifact next() {
		return next;
	}
}
