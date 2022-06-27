package sune.app.mediadown.resource;

import javafx.scene.image.Image;
import sune.app.mediadown.language.Language;
import sune.app.mediadown.registry.NamedRegistry;
import sune.app.mediadown.registry.SimpleNamedRegistry;
import sune.app.mediadown.theme.Theme;

public final class ResourceRegistry {
	
	public static final NamedRegistry<Language> languages = new SimpleNamedRegistry<>();
	public static final NamedRegistry<Theme>    themes    = new SimpleNamedRegistry<>();
	public static final NamedRegistry<Image>    icons     = new SimpleNamedRegistry<>();
	public static final NamedRegistry<Image>    images    = new SimpleNamedRegistry<>();
	
	public static final Language language(String name) {
		return languages.get(name);
	}
	
	public static final Theme theme(String name) {
		return themes.get(name);
	}
	
	public static final Image icon(String name) {
		return icons.get(name);
	}
	
	public static final Image image(String name) {
		return images.get(name);
	}
	
	// Forbid anyone to create an instance of this class
	private ResourceRegistry() {
	}
}