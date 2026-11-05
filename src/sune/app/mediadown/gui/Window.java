package sune.app.mediadown.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.theme.Theme;
import sune.app.mediadown.util.Utils;

public class Window<T extends Pane> extends Stage {

	private static final Set<Window<?>> windows = Collections.newSetFromMap(new WeakHashMap<>());
	
	static final double DEFAULT_WIDTH  = 200.0;
	static final double DEFAULT_HEIGHT = 200.0;

	private final List<String> themeStylesheets = new ArrayList<>();
	
	protected final Map<String, Object> args = new LinkedHashMap<>();
	
	protected final String name;
	protected final Scene scene;
	protected final T pane;
	
	protected final Translation translation;
	
	public Window(String winName, T root) {
		this(winName, root, DEFAULT_WIDTH, DEFAULT_HEIGHT, Color.WHITE);
	}
	
	public Window(String winName, T root, double width, double height) {
		this(winName, root, width, height, Color.WHITE);
	}
	
	public Window(String winName, T root, double width, double height, Paint paint) {
		name  = winName.replace('.', '-');
		pane  = root;
		scene = new Scene(root, width, height, paint);
		translation = MediaDownloader.translation().getTranslation("windows." + winName);
		pane.setId("window-" + name);
		setScene(scene);
		setTitle(translation.getSingle("title"));
		getIcons().setAll(MediaDownloader.ICON);
		applyTheme(MediaDownloader.theme());
		scene.getStylesheets().add(Window.class.getResource("/resources/theme/window-chrome.css").toExternalForm());
		windows.add(this);
	}
	
	private final void applyTheme(Theme theme) {
		scene.getStylesheets().removeAll(themeStylesheets);
		themeStylesheets.clear();
		themeStylesheets.add(theme.stylesheet("general-component.css"));

		String windowStylesheet = theme.stylesheet("window-" + name + ".css");
		if(windowStylesheet != null) {
			themeStylesheets.add(windowStylesheet);
		}

		scene.getStylesheets().addAll(0, themeStylesheets);
	}

	public static void refreshTheme() {
		Theme theme = MediaDownloader.theme();
		for(Window<?> window : new ArrayList<>(windows)) {
			window.applyTheme(theme);
			window.scene.getRoot().applyCss();
		}
	}
	
	/** @since 00.02.08 */
	protected final String tr(String name, Object... args) {
		return translation.getSingle(name, args);
	}
	
	/** @since 00.02.08 */
	protected final Translation trtr(String name) {
		return translation.getTranslation(name);
	}
	
	public Window<T> setArgs(Map<String, Object> args) {
		this.args.clear();
		this.args.putAll(args);
		return this;
	}
	
	public Window<T> setArgs(Object... args) {
		setArgs(Utils.<String, Object>toMap(args));
		return this;
	}
	
	public void show(Stage parent) {
		args.put("parent", parent);
		show();
	}
	
	public void showAndWait(Stage parent) {
		args.put("parent", parent);
		showAndWait();
	}
	
	public T getPane() {
		return pane;
	}
	
	public String getName() {
		return name;
	}
	
	public Translation getTranslation() {
		return translation;
	}
}
