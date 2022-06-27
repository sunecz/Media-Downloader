package sune.app.mediadown.theme;

public final class DarkTheme extends Theme {
	
	private static final String   NAME   = "dark";
	private static final String[] STYLES = {
		"general-component.css",
		"window-progress.css",
		"window-information.css",
		"window-configuration.css",
		"window-search.css",
	};
	
	public DarkTheme() {
		super(NAME, STYLES);
	}
}