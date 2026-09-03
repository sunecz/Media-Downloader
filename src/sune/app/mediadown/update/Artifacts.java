package sune.app.mediadown.update;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import sune.app.mediadown.MediaDownloader.Common;
import sune.app.mediadown.update.Manifest.ManagedPath;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public final class Artifacts {
	
	private final Environment environment;
	private final Channel channel;
	
	private Manifest localManifest;
	private ArtifactsState state;
	
	private Function<Artifacts, ArtifactsIterator> iteratorSupplier;
	
	private Artifacts(
		Environment environment,
		Channel channel,
		Function<Artifacts, ArtifactsIterator> iteratorSupplier
	) {
		this.environment = Objects.requireNonNull(environment);
		this.channel = Objects.requireNonNull(channel);
		this.iteratorSupplier = Objects.requireNonNull(iteratorSupplier);
	}
	
	private final void load(Manifest localManifest, List<ComponentRegistry> registries) throws Exception {
		Objects.requireNonNull(localManifest);
		Objects.requireNonNull(registries);
		
		List<Artifact> allArtifacts = new ArrayList<>();
		Manifest manifest = Manifest.empty();
		
		for(ComponentRegistry registry : registries) {
			try {
				List<Artifact> artifacts = registry.artifacts(environment, channel);
				manifest = manifest.merge(Manifest.ofArtifacts(artifacts));
				allArtifacts.addAll(artifacts);
			} catch(Exception ex) {
				Manifest keepManifest = localManifest.subManifest(registry);
				manifest = manifest.merge(keepManifest);
			}
		}
		
		this.localManifest = localManifest;
		this.state = new ArtifactsState(allArtifacts, manifest);
	}
	
	public List<Artifact> download(ArtifactDownloader downloader) throws Exception {
		ArtifactsIterator iterator = iteratorSupplier.apply(this);
		List<Artifact> downloaded = new ArrayList<>();
		
		for(Artifact artifact : Utils.iterable(iterator)) {
			if(downloader.isStopped()) break;
			if(downloader.download(artifact) > 0L) downloaded.add(artifact);
		}
		
		return downloaded;
	}
	
	public Manifest.ComponentChanges changedComponents() {
		return state.manifest().changedComponents(localManifest);
	}
	
	public Manifest.ComponentChanges unchangedComponents() {
		return state.manifest().unchangedComponents(localManifest);
	}
	
	public List<ManagedPath> deletedPaths() {
		return state.manifest().deletedPaths(localManifest);
	}
	
	public Manifest remoteManifest() {
		return state.manifest();
	}
	
	public List<Artifact> artifacts() {
		return state.artifacts();
	}
	
	public static final Builder builderOf(Channel channel) {
		return new Builder(Common.rootPath(), Environment.ofCurrent(), channel);
	}
	
	public static final Builder builderOf(Path root, Environment environment, Channel channel) {
		return new Builder(root, environment, channel);
	}
	
	public static final class Builder {
		
		private static final Predicate<Artifact> NO_SKIP_FILTER = (a) -> false;
		
		private final Path root;
		private final Environment environment;
		private final Channel channel;
		
		private Predicate<Artifact> skipArtifactFilter;
		private Function<Artifacts, ArtifactsIterator> iteratorSupplier;
		
		private Builder(Path root, Environment environment, Channel channel) {
			this.root = root;
			this.environment = environment;
			this.channel = channel;
		}
		
		public Builder noIntegrityCheck() {
			iteratorSupplier = (artifacts) -> (
				new ArtifactsIteratorNoIntegrity(
					artifacts.artifacts(),
					root,
					skipArtifactFilter,
					artifacts.unchangedComponents().components()
				)
			);
			
			return this;
		}
		
		public Builder withIntegrityCheck(Function<Artifacts, ArtifactChecker> supplier) {
			iteratorSupplier = (artifacts) -> (
				new ArtifactsIteratorWithIntegrity(
					artifacts.artifacts(),
					supplier.apply(artifacts),
					skipArtifactFilter
				)
			);
			
			return this;
		}
		
		public Builder skipArtifactFilter(Predicate<Artifact> skipArtifactFilter) {
			this.skipArtifactFilter = skipArtifactFilter;
			return this;
		}
		
		public Artifacts build(Manifest localManifest, List<ComponentRegistry> registries) throws Exception {
			if(iteratorSupplier == null) {
				noIntegrityCheck();
			}
			
			if(skipArtifactFilter == null) {
				skipArtifactFilter = NO_SKIP_FILTER;
			}
			
			Artifacts artifacts = new Artifacts(environment, channel, iteratorSupplier);
			artifacts.load(localManifest, registries);
			return artifacts;
		}
		
		public Path root() { return root; }
		public Environment environment() { return environment; }
		public Channel channel() { return channel; }
	}
}
