package sune.app.mediadown.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sune.app.mediadown.util.JSON;
import sune.app.mediadown.util.JSON.JSONCollection;
import sune.app.mediadown.util.JSON.JSONObject;
import sune.app.mediadown.util.NIO;

/** @since 00.02.09 */
public final class Manifest {
	
	private static final OpenOption[] WRITE_OPTIONS = {
		StandardOpenOption.CREATE,
		StandardOpenOption.WRITE,
		StandardOpenOption.TRUNCATE_EXISTING,
	};
	
	private final Map<String, ManagedVersion> versions;
	private final List<ManagedPath> paths;
	
	private Manifest(Map<String, ManagedVersion> versions, List<ManagedPath> paths) {
		this.versions = Objects.requireNonNull(versions);
		this.paths = Objects.requireNonNull(paths);
	}
	
	private static final List<ManagedPath> uniqueSortedList(List<ManagedPath> list) {
		Iterator<ManagedPath> it = list.iterator();
		
		if(!it.hasNext()) return list;
		ManagedPath prev = it.next();
		
		for(ManagedPath item; it.hasNext(); prev = item) {
			if(prev.compareTo(item = it.next()) >= 0) {
				return new ArrayList<>(new TreeSet<>(list));
			}
		}
		
		return list;
	}
	
	// Assumes both lists are sorted and contain unique paths.
	private static final List<ManagedPath> computeDeletedPaths(
		List<ManagedPath> newPaths,
		List<ManagedPath> oldPaths
	) {
		Iterator<ManagedPath> itNew = newPaths.iterator();
		Iterator<ManagedPath> itOld = oldPaths.iterator();
		
		if(!itNew.hasNext()) {
			return new ArrayList<>(oldPaths);
		}
		
		List<ManagedPath> list = new ArrayList<>();
		ManagedPath newPath = itNew.next();
		
		for(int cmp; itOld.hasNext();) {
			ManagedPath oldPath = itOld.next();
			while((cmp = newPath.compareTo(oldPath)) < 0 && itNew.hasNext()) newPath = itNew.next();
			if(cmp != 0) list.add(oldPath);
			if(cmp  < 0) while(itOld.hasNext()) list.add(itOld.next());
		}
		
		return list;
	}
	
	private static final ComponentChanges computeChangedComponents(
		Map<String, ManagedVersion> newVersions,
		Map<String, ManagedVersion> oldVersions,
		Predicate<ComponentChange> filter
	) {
		return new ComponentChanges(
			newVersions.entrySet().stream()
				.map((e) ->  new ComponentChange(
					e.getKey(),
					Optional.ofNullable(oldVersions.get(e.getKey()))
						.map(ManagedVersion::version)
						.orElse(null),
					e.getValue().version())
				)
				.filter(Objects.requireNonNull(filter))
				.collect(Collectors.toList())
		);
	}
	
	public static final Manifest empty() {
		return new Manifest(Map.of(), List.of());
	}
	
	public static final Manifest ofArtifacts(List<Artifact> artifacts) {
		Map<String, ManagedVersion> versions = (
			artifacts.stream()
				.collect(Collectors.toMap(
					Artifact::component,
					ManagedVersion::ofArtifact,
					(a, b) -> a,
					TreeMap::new
				))
		);
		
		List<ManagedPath> paths = uniqueSortedList(
			artifacts.stream()
				.map(ManagedPath::ofArtifact)
				.collect(Collectors.toList())
		);
		
		return new Manifest(versions, paths);
	}
	
	public static final Manifest ofLocal(Path path) throws IOException {
		if(!NIO.exists(path)) {
			return new Manifest(Map.of(), List.of());
		}
		
		JSONCollection data = JSON.read(path);
		
		Map<String, ComponentRegistry> registries = (
			data.getCollection("registries").objectsStream()
				.collect(Collectors.toMap(
					JSONObject::name,
					(o) -> new ComponentRegistry(o.stringValue()),
					(a, b) -> a,
					LinkedHashMap::new
				))
		);
		
		Map<String, ManagedVersion> versions = (
			data.getCollection("versions").collectionsStream()
				.collect(Collectors.toMap(
					JSONCollection::name,
					(d) -> ManagedVersion.ofJSON(registries, d),
					(a, b) -> a,
					TreeMap::new
				))
		);
		
		List<ManagedPath> paths = uniqueSortedList(
			data.getCollection("paths").collectionsStream()
				.map((d) -> ManagedPath.ofJSON(registries, d))
				.collect(Collectors.toList())
		);
		
		return new Manifest(versions, paths);
	}
	
	public ComponentChanges changedComponents(Manifest other) {
		return computeChangedComponents(
			versions,
			Objects.requireNonNull(other).versions,
			(c) -> !c.newVersion().equals(c.oldVersion())
		);
	}
	
	public ComponentChanges unchangedComponents(Manifest other) {
		return computeChangedComponents(
			versions,
			Objects.requireNonNull(other).versions,
			(c) -> c.newVersion().equals(c.oldVersion())
		);
	}
	
	public List<ManagedPath> deletedPaths(Manifest other) {
		return computeDeletedPaths(paths, Objects.requireNonNull(other).paths);
	}
	
	public void writeTo(Path path) throws IOException {
		Map<String, String> registries = (
			paths.stream()
				.map(ManagedPath::registry)
				.distinct()
				.collect(
					LinkedHashMap::new,
					(m, v) -> m.put(String.valueOf(m.size()), v.endpointUri()),
					Map::putAll
				)
		);
		
		Map<String, String> invRegistries = (
			registries.entrySet().stream()
				.collect(Collectors.toMap(
					Map.Entry::getValue,
					Map.Entry::getKey,
					(a, b) -> a,
					LinkedHashMap::new
				))
		);
		
		Object[] objRegistries = (
			registries.entrySet().stream()
				.flatMap((e) -> Stream.of(e.getKey(), e.getValue()))
				.toArray(Object[]::new)
		);
		
		Object[] objVersions = (
			versions.entrySet().stream()
				.flatMap((e) -> Stream.of(
					e.getKey(),
					JSONCollection.ofObject(
						"r", invRegistries.get(e.getValue().registry().endpointUri()),
						"v", e.getValue().version()
					)
				))
				.toArray(Object[]::new)
		);
		
		Object[] objPaths = (
			paths.stream()
				.map((e) -> JSONCollection.ofObject(
					"r", invRegistries.get(e.registry().endpointUri()),
					"p", e.path()
				))
				.toArray(Object[]::new)
		);
		
		JSONCollection data = JSONCollection.ofObject(
			"registries", JSONCollection.ofObject(objRegistries),
			"versions", JSONCollection.ofObject(objVersions),
			"paths", JSONCollection.ofArray(objPaths)
		);
		
		Files.writeString(path, data.toString(true), WRITE_OPTIONS);
	}
	
	public Manifest merge(Manifest other) {
		Map<String, ManagedVersion> mergedVersions = new TreeMap<>(versions);
		mergedVersions.putAll(other.versions);
		
		Set<ManagedPath> mergedPaths = new TreeSet<>(paths);
		mergedPaths.addAll(other.paths);
		
		return new Manifest(mergedVersions, new ArrayList<>(mergedPaths));
	}
	
	public Manifest subManifest(ComponentRegistry registry) {
		Map<String, ManagedVersion> subVersions = (
			versions.entrySet().stream()
				.filter((e) -> e.getValue().registry().equals(registry))
				.collect(Collectors.toMap(
					Map.Entry::getKey,
					Map.Entry::getValue,
					(a, b) -> a,
					TreeMap::new
				))
		);
		
		List<ManagedPath> subPaths = (
			paths.stream()
				.filter((p) -> p.registry().equals(registry))
				.collect(Collectors.toList())
		);
		
		return new Manifest(subVersions, subPaths);
	}
	
	public static final class ComponentChange {
		
		private final String component;
		private final String oldVersion;
		private final String newVersion;
		
		public ComponentChange(String component, String oldVersion, String newVersion) {
			this.component = component;
			this.oldVersion = oldVersion;
			this.newVersion = newVersion;
		}
		
		public String component() { return component; }
		public String oldVersion() { return oldVersion; }
		public String newVersion() { return newVersion; }
	}
	
	public static final class ComponentChanges {
		
		private final List<ComponentChange> changes;
		private final Set<String> components;
		
		public ComponentChanges(List<ComponentChange> changes) {
			this.changes = changes;
			this.components = (
				changes.stream()
					.map(ComponentChange::component)
					.collect(Collectors.toCollection(TreeSet::new))
			);
		}
		
		public static final ComponentChanges empty() {
			return new ComponentChanges(List.of());
		}
		
		public ComponentChanges removeAll(Set<String> components) {
			if(components.isEmpty()) {
				return this;
			}
			
			return new ComponentChanges(
				changes.stream()
					.filter((c) -> !components.contains(c.component()))
					.collect(Collectors.toList())
			);
		}
		
		public boolean has(String component) { return components.contains(component); }
		public List<ComponentChange> changes() { return changes; }
		public Set<String> components() { return Set.copyOf(components); }
	}
	
	private static class ManagedItem<T extends Comparable<T>> implements Comparable<ManagedItem<T>> {
		
		protected final ComponentRegistry registry;
		protected final T value;
		
		protected ManagedItem(ComponentRegistry registry, T value) {
			this.registry = Objects.requireNonNull(registry);
			this.value = Objects.requireNonNull(value);
		}
		
		public ComponentRegistry registry() { return registry; }
		
		@Override
		public int hashCode() {
			return value.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj == this) return true;
			if(!(obj instanceof ManagedItem)) return false;
			@SuppressWarnings("unchecked")
			ManagedItem<T> other = (ManagedItem<T>) obj;
			return value.equals(other.value);
		}
		
		@Override
		public int compareTo(ManagedItem<T> other) {
			return value.compareTo(other.value);
		}
	}
	
	public static final class ManagedVersion extends ManagedItem<String> {
		
		public ManagedVersion(ComponentRegistry registry, String version) {
			super(registry, version);
		}
		
		public static final ManagedVersion ofArtifact(Artifact artifact) {
			return new ManagedVersion(artifact.registry(), artifact.version());
		}
		
		public static final ManagedVersion ofJSON(
			Map<String, ComponentRegistry> registries,
			JSONCollection data
		) {
			String version = data.getString("v");
			
			if(version == null) {
				throw new IllegalArgumentException("Empty version");
			}
			
			String registryId = data.getString("r");
			
			if(registryId == null) {
				throw new IllegalArgumentException("Invalid registry");
			}
			
			ComponentRegistry registry = registries.get(registryId);
			
			if(registry == null) {
				throw new IllegalArgumentException("Invalid registry");
			}
			
			return new ManagedVersion(registry, version);
		}
		
		public String version() { return value; }
	}
	
	public static final class ManagedPath extends ManagedItem<String> {
		
		public ManagedPath(ComponentRegistry registry, String path) {
			super(registry, path);
		}
		
		public static final ManagedPath ofArtifact(Artifact artifact) {
			return new ManagedPath(artifact.registry(), artifact.installPath());
		}
		
		public static final ManagedPath ofJSON(
			Map<String, ComponentRegistry> registries,
			JSONCollection data
		) {
			String path = data.getString("p");
			
			if(path == null) {
				throw new IllegalArgumentException("Empty path");
			}
			
			String registryId = data.getString("r");
			
			if(registryId == null) {
				throw new IllegalArgumentException("Invalid registry");
			}
			
			ComponentRegistry registry = registries.get(registryId);
			
			if(registry == null) {
				throw new IllegalArgumentException("Invalid registry");
			}
			
			return new ManagedPath(registry, path);
		}
		
		public String path() { return value; }
	}
}
