package sune.app.mediadown.theme;

public final class LightTheme extends Theme {
	
	private static final String   NAME   = "light";
	private static final String[] STYLES = {
		"general-component.css",
		"window-progress.css",
		"window-information.css",
		"window-configuration.css",
		"window-search.css",
	};
	
	public LightTheme() {
		super(NAME, STYLES);
	}
}