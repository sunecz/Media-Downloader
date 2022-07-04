package sune.app.mediadown.resource;

import javafx.scene.image.Image;
import sune.app.mediadown.language.Language;
import sune.app.mediadown.registry.ResourceNamedRegistry;
import sune.app.mediadown.theme.Theme;

public final class ResourceRegistry {
	
	public static final ResourceNamedRegistry<Language> languages = new ResourceNamedRegistry<>();
	public static final ResourceNamedRegistry<Theme>    themes    = new ResourceNamedRegistry<>();
	public static final ResourceNamedRegistry<Image>    icons     = new ResourceNamedRegistry<>();
	public static final ResourceNamedRegistry<Image>    images    = new ResourceNamedRegistry<>();
	
	public static final Language language(String name) {
		return languages.getValue(name);
	}
	
	public static final Theme theme(String name) {
		return themes.getValue(name);
	}
	
	public static final Image icon(String name) {
		return icons.getValue(name);
	}
	
	public static final Image image(String name) {
		return images.getValue(name);
	}
	
	// Forbid anyone to create an instance of this class
	private ResourceRegistry() {
	}
}