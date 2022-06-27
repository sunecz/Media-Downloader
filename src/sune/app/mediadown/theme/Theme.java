package sune.app.mediadown.theme;

import java.nio.file.Path;
import java.nio.file.Paths;

import sune.app.mediadown.resource.Extractable;
import sune.app.mediadown.resource.InputStreamResolver;
import sune.app.mediadown.resource.Resource;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Utils;

public class Theme implements Extractable {
	
	private static final Path  DIRECTORY   = Paths.get(Resource.resolve("theme/"));
	private static final Theme THEME_DARK  = new DarkTheme();
	private static final Theme THEME_LIGHT = new LightTheme();
	
	private final String name;
	private final String[] styles;
	private final Path path;
	
	public Theme(String name, String... styles) {
		this.name = name;
		this.styles = styles;
		this.path = DIRECTORY.resolve(name + '/');
	}
	
	public static final Theme getLight() {
		return THEME_LIGHT;
	}
	
	public static final Theme getDark() {
		return THEME_DARK;
	}
	
	public static final Theme getDefault() {
		return THEME_LIGHT;
	}
	
	private final String internalPath(String style) {
		return String.format("theme/%s/%s", name, style);
	}
	
	@Override
	public void extract(Path dir, InputStreamResolver resolver) throws Exception {
		Path dirTheme = dir.resolve(name);
		NIO.createDir(dirTheme);
		for(String style : styles) {
			String internalPath = internalPath(style);
			Path fileStyle = dirTheme.resolve(style);
			if(!NIO.exists(fileStyle)) {
				// Ensure the style resource's parent directory
				NIO.createDir(fileStyle.getParent());
				// Copy the style resource's bytes to the destination file
				NIO.copy(resolver.resolve(internalPath), fileStyle);
			}
		}
	}
	
	public boolean hasStylesheet(String name) {
		return Utils.indexOf(name, styles) >= 0;
	}
	
	public String stylesheet(String name) {
		if(!hasStylesheet(name))
			return null; // Do not throw exception
		Path file = path.resolve(name);
		if(!NIO.exists(file))
			return null; // Do not throw exception
		return Utils.toURL(file);
	}
	
	public String getName() {
		return name;
	}
	
	public Path getPath() {
		return path;
	}
	
	public String[] getStyles() {
		return styles;
	}
	
	@Override
	public String toString() {
		return Utils.titlize(name);
	}
}