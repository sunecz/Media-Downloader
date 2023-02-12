package sune.app.mediadown.entity;

import java.net.URI;
import java.util.Collection;

import sune.app.mediadown.util.ObjectHolder;

public final class Servers {
	
	private static final ObjectHolder<String, Server> holder = new ObjectHolder<>();
	
	public static final void add(String name, Class<? extends Server> clazz) { holder.add(name, clazz); }
	public static final Server get(String name) { return holder.get(name); }
	public static final Collection<Server> all() { return holder.all(); }
	
	public static final Server fromURI(URI uri) {
		return holder.stream()
					.filter((o) -> o.isCompatibleURI(uri))
					.findFirst().orElse(null);
	}
	
	// Forbid anyone to create an instance of this class
	private Servers() {
	}
}