package sune.app.mediadown.gui;

import java.util.LinkedHashMap;
import java.util.Map;

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
	
	static final double DEFAULT_WIDTH  = 200.0;
	static final double DEFAULT_HEIGHT = 200.0;
	
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
		initStylesheet();
	}
	
	private final void initStylesheet() {
		Theme currentTheme = MediaDownloader.theme();
		// init general styles (these must exist)
		scene.getStylesheets().add(currentTheme.stylesheet("general-component.css"));
		// init window-specific style
		String style;
		if((style = currentTheme.stylesheet("window-" + name + ".css")) != null) {
			scene.getStylesheets().add(style);
		}
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