package sune.app.mediadown.transformer;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.util.ObjectHolder;
import sune.app.mediadown.util.Opt;

/** @since 00.02.09 */
public final class Transformers {
	
	private static final ObjectHolder<String, Transformer> holder = new ObjectHolder<>();
	
	// Forbid anyone to create an instance of this class
	private Transformers() {
	}
	
	public static final void add(String name, Class<? extends Transformer> clazz) { holder.add(name, clazz); }
	public static final Transformer get(String name) { return holder.get(name); }
	public static final Collection<Transformer> all() { return holder.all(); }
	
	public static final Transformer from(ResolvedMedia media) {
		return holder.stream()
			.filter((o) -> o.isUsable(media))
			.findFirst()
			.orElseGet(DefaultTransformer::instance);
	}
	
	public static final List<Transformer> allFrom(ResolvedMedia media) {
		return (
			Opt.of(
				holder.stream()
					.filter((o) -> o.isUsable(media))
					.collect(Collectors.toList())
			)
			.ifFalse(List::isEmpty)
			.orElseGet(() -> List.of(DefaultTransformer.instance()))
		);
	}
}