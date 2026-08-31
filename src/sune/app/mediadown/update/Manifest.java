package sune.app.mediadown.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
	
	private final Map<String, String> versions;
	private final List<String> paths;
	
	private Manifest(Map<String, String> versions, List<String> paths) {
		this.versions = Objects.requireNonNull(versions);
		this.paths = Objects.requireNonNull(paths);
	}
	
	private static final List<String> uniqueSortedList(List<String> list) {
		Iterator<String> it = list.iterator();
		
		if(!it.hasNext()) return list;
		String prev = it.next();
		
		for(String item; it.hasNext(); prev = item) {
			if(prev.compareTo(item = it.next()) >= 0) {
				return new ArrayList<>(new TreeSet<>(list));
			}
		}
		
		return list;
	}
	
	// Assumes both lists are sorted and contain unique paths.
	private static final List<String> computeDeletedPaths(
		List<String> newPaths,
		List<String> oldPaths
	) {
		Iterator<String> itNew = newPaths.iterator();
		Iterator<String> itOld = oldPaths.iterator();
		
		if(!itNew.hasNext()) {
			return new ArrayList<>(oldPaths);
		}
		
		List<String> list = new ArrayList<>();
		String newPath = itNew.next();
		
		for(int cmp; itOld.hasNext();) {
			String oldPath = itOld.next();
			while((cmp = newPath.compareTo(oldPath)) < 0 && itNew.hasNext()) newPath = itNew.next();
			if(cmp != 0) list.add(oldPath);
			if(cmp  < 0) while(itOld.hasNext()) list.add(itOld.next());
		}
		
		return list;
	}
	
	private static final ComponentChanges computeChangedComponents(
		Map<String, String> newVersions,
		Map<String, String> oldVersions,
		Predicate<ComponentChange> filter
	) {
		return new ComponentChanges(
			newVersions.entrySet().stream()
				.map((e) ->  new ComponentChange(e.getKey(), oldVersions.get(e.getKey()), e.getValue()))
				.filter(Objects.requireNonNull(filter))
				.collect(Collectors.toList())
		);
	}
	
	public static final Manifest empty() {
		return new Manifest(Map.of(), List.of());
	}
	
	public static final Manifest ofArtifacts(List<Artifact> artifacts) {
		Map<String, String> versions = (
			artifacts.stream()
				.collect(Collectors.toMap(
					Artifact::component,
					Artifact::version,
					(a, b) -> a,
					TreeMap::new
				))
		);
		
		List<String> paths = uniqueSortedList(
			artifacts.stream()
				.map(Artifact::installPath)
				.collect(Collectors.toList())
		);
		
		return new Manifest(versions, paths);
	}
	
	public static final Manifest ofLocal(Path path) throws IOException {
		if(!NIO.exists(path)) {
			return new Manifest(Map.of(), List.of());
		}
		
		JSONCollection data = JSON.read(path);
		
		Map<String, String> versions = (
			data.getCollection("versions").objectsStream()
				.collect(Collectors.toMap(
					JSONObject::name,
					JSONObject::stringValue,
					(a, b) -> a,
					TreeMap::new
				))
		);
		
		List<String> paths = uniqueSortedList(
			data.getCollection("paths").objectsStream()
				.map(JSONObject::stringValue)
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
	
	public List<String> deletedPaths(Manifest other) {
		return computeDeletedPaths(paths, Objects.requireNonNull(other).paths);
	}
	
	public void writeTo(Path path) throws IOException {
		Object[] objVersions = (
			versions.entrySet().stream()
				.flatMap((e) -> Stream.of(e.getKey(), e.getValue()))
				.toArray(Object[]::new)
		);
		
		Object[] objPaths = paths.toArray(Object[]::new);
		
		JSONCollection data = JSONCollection.ofObject(
			"versions", JSONCollection.ofObject(objVersions),
			"paths", JSONCollection.ofArray(objPaths)
		);
		
		Files.writeString(path, data.toString(), WRITE_OPTIONS);
	}
	
	public Manifest merge(Manifest other) {
		Map<String, String> mergedVersions = new TreeMap<>(versions);
		mergedVersions.putAll(other.versions);
		
		Set<String> mergedPaths = new TreeSet<>(paths);
		mergedPaths.addAll(other.paths);
		
		return new Manifest(mergedVersions, new ArrayList<>(mergedPaths));
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
}
