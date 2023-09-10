package sune.app.mediadown.tor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import sune.app.mediadown.concurrent.VarLoader;

/** @since 00.02.09 */
public final class TorStream {
	
	private int id;
	private Status status;
	private int circuitId;
	private Set<String> targets;
	private Map<String, String> attributes;
	
	private TorStream(int id, Status status, int circuitId, String target) {
		this.id = id;
		this.status = Objects.requireNonNull(status);
		this.circuitId = circuitId;
		this.targets = new LinkedHashSet<>(List.of(Objects.requireNonNull(target)));
		this.attributes = null;
	}
	
	public static final TorStream create(int id, Status status, int circuitId, String target) {
		return new TorStream(id, status, circuitId, target);
	}
	
	// package-private
	void id(int id) {
		this.id = id;
	}
	
	// package-private
	void status(Status status) {
		this.status = Objects.requireNonNull(status);
	}
	
	// package-private
	void circuitId(int circuitId) {
		this.circuitId = circuitId;
	}
	
	// package-private
	void target(String target) {
		this.targets = new LinkedHashSet<>(List.of(Objects.requireNonNull(target)));
	}
	
	// package-private
	void addTarget(String target) {
		this.targets.add(target);
	}
	
	// package-private
	void attributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
	
	// package-private
	void addAttributes(Map<String, String> attributes) {
		if(this.attributes == null) {
			this.attributes = new HashMap<>();
		}
		
		this.attributes.putAll(attributes);
	}
	
	public int id() { return id; }
	public Status status() { return status; }
	public int circuitId() { return circuitId; }
	public Set<String> targets() { return Set.copyOf(targets); }
	
	public String attribute(String name) { return attributes == null ? null : attributes.get(name); }
	public Map<String, String> attributes() { return attributes == null ? Map.of() : Map.copyOf(attributes); }
	
	// Tor control-spec.txt, section 4.1.2.
	public static enum Status {
		
		NEW, NEWRESOLVE, REMAP, SENTCONNECT, SENTRESOLVE,
		SUCCEEDED, FAILED, CLOSED, DETACHED, CONTROLLER_WAIT,
		XOFF_SENT, XOFF_RECV, XON_SENT, XON_RECV, UNKNOWN;
		
		private static final VarLoader<Status[]> values = VarLoader.of(Status::values);
		
		public static final Status of(String name) {
			return Arrays.stream(values.value())
				.filter((e) -> e.name().equalsIgnoreCase(name))
				.findFirst().orElse(UNKNOWN);
		}
	}
}