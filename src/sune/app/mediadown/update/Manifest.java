package sune.app.mediadown.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
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
		
		Mapping<String, ComponentRegistry> registries = Mapping.from(
			data.getCollection("registries"),
			ComponentRegistry::new
		);
		
		Mapping<String, String> components = Mapping.from(
			data.getCollection("components"),
			Function.identity()
		);
		
		Map<String, ManagedVersion> versions = (
			data.getCollection("versions").collectionsStream()
				.collect(Collectors.toMap(
					JSONCollection::name,
					(d) -> ManagedVersion.ofJSON(registries, components, d),
					(a, b) -> a,
					TreeMap::new
				))
		);
		
		List<ManagedPath> paths = uniqueSortedList(
			data.getCollection("paths").collectionsStream()
				.map((d) -> ManagedPath.ofJSON(registries, components, d))
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
		Mapping<String, String> registries = Mapping.from(
			Stream.concat(
				paths.stream().map(ManagedPath::registry),
				versions.values().stream().map(ManagedVersion::registry)
			),
			ComponentRegistry::endpointUri
		);
		
		Mapping<String, String> components = Mapping.from(
			Stream.concat(
				paths.stream().map(ManagedPath::component),
				versions.values().stream().map(ManagedVersion::component)
			),
			Function.identity()
		);
		
		Mapping<String, String> invRegistries = registries.inverse();
		Mapping<String, String> invComponents = components.inverse();
		
		Object[] objRegistries = registries.toArray();
		Object[] objComponents = components.toArray();
		
		Object[] objVersions = (
			versions.entrySet().stream()
				.flatMap((e) -> Stream.of(
					e.getKey(),
					JSONCollection.ofObject(
						"r", invRegistries.get(e.getValue().registry().endpointUri()),
						"c", invComponents.get(e.getValue().component()),
						"v", e.getValue().version()
					)
				))
				.toArray(Object[]::new)
		);
		
		Object[] objPaths = (
			paths.stream()
				.map((e) -> JSONCollection.ofObject(
					"r", invRegistries.get(e.registry().endpointUri()),
					"c", invComponents.get(e.component()),
					"p", e.path()
				))
				.toArray(Object[]::new)
		);
		
		JSONCollection data = JSONCollection.ofObject(
			"registries", JSONCollection.ofObject(objRegistries),
			"components", JSONCollection.ofObject(objComponents),
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
	
	public Manifest replaceComponents(Set<String> components, Manifest from) {
		Map<String, ManagedVersion> newVersions = (
			versions.entrySet().stream()
				.map((e) -> (
					!components.contains(e.getValue().component())
						? e
						: (
							!from.versions.containsKey(e.getValue().component())
								? null
								: Map.entry(e.getKey(), from.versions.get(e.getValue().component()))
						)
				))
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(
					Map.Entry::getKey,
					Map.Entry::getValue,
					(a, b) -> a,
					TreeMap::new
				))
		);
		
		Map<String, ManagedPath> mapPaths = (
			from.paths.stream()
				.collect(Collectors.toMap(
					ManagedPath::path,
					Function.identity(),
					(a, b) -> a,
					HashMap::new
				))
		);
		
		List<ManagedPath> newPaths = (
			paths.stream()
				.map((p) -> (
					!components.contains(p.component())
						? p
						: mapPaths.get(p.path())
				))
				.filter(Objects::nonNull)
				.collect(Collectors.toList())
		);
		
		return new Manifest(newVersions, newPaths);
	}
	
	private static final class Mapping<K, V> {
		
		private final Map<K, V> mapping;
		
		private Mapping(Map<K, V> mapping) {
			this.mapping = Objects.requireNonNull(mapping);
		}
		
		public static final <T> Mapping<String, T> from(JSONCollection data, Function<String, T> mapper) {
			return new Mapping<>(
				data.objectsStream()
				.collect(Collectors.toMap(
					JSONObject::name,
					(o) -> mapper.apply(o.stringValue()),
					(a, b) -> a,
					LinkedHashMap::new
				))
			);
		}
		
		public static final <T, R> Mapping<String, R> from(Stream<T> stream, Function<T, R> mapper) {
			return new Mapping<>(
				stream
					.distinct()
					.collect(
						LinkedHashMap::new,
						(m, v) -> m.put(String.valueOf(m.size()), mapper.apply(v)),
						Map::putAll
					)
			);
		}
		
		public V get(K key) {
			return mapping.get(key);
		}
		
		public Mapping<V, K> inverse() {
			return new Mapping<>(
				mapping.entrySet().stream()
					.collect(Collectors.toMap(
						Map.Entry::getValue,
						Map.Entry::getKey,
						(a, b) -> a,
						LinkedHashMap::new
					))
			);
		}
		
		public Object[] toArray() {
			return (
				mapping.entrySet().stream()
					.flatMap((e) -> Stream.of(e.getKey(), e.getValue()))
					.toArray(Object[]::new)
			);
		}
	}
	
	private static class ManagedItem<T extends Comparable<T>> implements Comparable<ManagedItem<T>> {
		
		protected final ComponentRegistry registry;
		protected final String component;
		protected final T value;
		
		protected ManagedItem(ComponentRegistry registry, String component, T value) {
			this.registry = Objects.requireNonNull(registry);
			this.component = Objects.requireNonNull(component);
			this.value = Objects.requireNonNull(value);
		}
		
		public ComponentRegistry registry() { return registry; }
		public String component() { return component; }
		
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
	
	public static final class ManagedVersion extends ManagedItem<String> {
		
		public ManagedVersion(ComponentRegistry registry, String component, String version) {
			super(registry, component, version);
		}
		
		public static final ManagedVersion ofArtifact(Artifact artifact) {
			return new ManagedVersion(artifact.registry(), artifact.component(), artifact.version());
		}
		
		public static final ManagedVersion ofJSON(
			Mapping<String, ComponentRegistry> registries,
			Mapping<String, String> components,
			JSONCollection data
		) {
			String version = Objects.requireNonNull(data.getString("v"));
			ComponentRegistry registry = Optional.ofNullable(data.getString("r")).map(registries::get).orElseThrow();
			String component = Optional.ofNullable(data.getString("c")).map(components::get).orElseThrow();
			return new ManagedVersion(registry, component, version);
		}
		
		public String version() { return value; }
	}
	
	public static final class ManagedPath extends ManagedItem<String> {
		
		public ManagedPath(ComponentRegistry registry, String component, String path) {
			super(registry, component, path);
		}
		
		public static final ManagedPath ofArtifact(Artifact artifact) {
			return new ManagedPath(artifact.registry(), artifact.component(), artifact.installPath());
		}
		
		public static final ManagedPath ofJSON(
			Mapping<String, ComponentRegistry> registries,
			Mapping<String, String> components,
			JSONCollection data
		) {
			String path = Objects.requireNonNull(data.getString("p"));
			ComponentRegistry registry = Optional.ofNullable(data.getString("r")).map(registries::get).orElseThrow();
			String component = Optional.ofNullable(data.getString("c")).map(components::get).orElseThrow();
			return new ManagedPath(registry, component, path);
		}
		
		public String path() { return value; }
	}
}
