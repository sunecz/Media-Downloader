package sune.app.mediadown.server;

import java.util.Collection;

import sune.app.mediadown.util.ObjectHolder;
import sune.app.mediadown.util.Utils;

public final class Servers {
	
	private static final ObjectHolder<String, Server> holder = new ObjectHolder<>();
	
	public static final void add(String name, Class<? extends Server> clazz) { holder.add(name, clazz); }
	public static final Server get(String name) { return holder.get(name); }
	public static final Collection<Server> all() { return holder.all(); }
	
	public static final Server getServerFromURL(String url) {
		return Utils.isValidURL(url)
					? holder.stream()
						.filter((o) -> o.isCompatibleURL(url))
						.findFirst().orElse(null)
					: null;
	}
	
	// Forbid anyone to create an instance of this class
	private Servers() {
	}
}