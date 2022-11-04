package sune.app.mediadown;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sune.app.mediadown.util.FXTask;

// Since this window is shown well before anything else is initialized,
// we must use only basic methods and components.
public final class StartupWindow extends Stage {
	
	private final Scene scene;
	private final StackPane pane;
	private final StackPane paneBack;
	private final StackPane paneTitle;
	private final BorderPane content;
	private final VBox boxBottom;
	private final Label lblStatus;
	private final ProgressBar prgbar;
	
	private int current;
	private double total = Double.NaN;
	
	public StartupWindow(String title, int total) {
		initStyle(StageStyle.UNDECORATED);
		pane      = new StackPane();
		scene     = new Scene(pane, 600.0, 400.0);
		paneBack  = new StackPane();
		paneTitle = new StackPane();
		content   = new BorderPane();
		boxBottom = new VBox();
		lblStatus = new Label("Initializing window...");
		prgbar    = new ProgressBar();
		String[] words = title.split("\\s+");
		for(int i = 0, l = words.length; i < l; ++i) {
			Label lblTitle = new Label(words[i].toUpperCase());
			lblTitle.getStyleClass().add("lbl-title");
			lblTitle.setId("lbl-title-" + i);
			paneTitle.getChildren().add(lblTitle);
		}
		pane.getChildren().addAll(paneBack, paneTitle, content);
		boxBottom.getChildren().addAll(lblStatus, prgbar);
		content.setBottom(boxBottom);
		paneBack .setId("background");
		paneTitle.setId("pane-title");
		lblStatus.setId("lbl-status");
		boxBottom.setId("box-bottom");
		scene.setCursor(Cursor.WAIT);
		scene.getStylesheets().add(stylesheet("window-startup.css"));
		setScene(scene);
		getIcons().setAll(MediaDownloader.ICON);
		setTitle(title);
		setTotal(total);
	}
	
	static { loadFonts(); }
	private static final void loadFonts() {
		String path = "/resources/font/hp-simplified.woff";
		try(InputStream stream = StartupWindow.class.getResourceAsStream(path)) {
			if((Font.loadFont(stream, 0.0) == null))
				throw new IllegalStateException("Invalid or unsupported font.");
		} catch(IOException ex) {
			throw new IllegalStateException("Error while reading a font file.");
		}
	}
	
	private static final String stylesheet(String name) {
		return StartupWindow.class.getResource("/resources/theme/" + name).toExternalForm();
	}
	
	private static final void fxThread(Runnable r) {
		if(!Platform.isFxApplicationThread()) Platform.runLater(r); else r.run();
	}
	
	private static final <T> FXTask<T> fxTask(Callable<T> callable) {
		FXTask<T> task = new FXTask<>(callable);
		fxThread(task);
		return task;
	}
	
	private static final <T> T fxTaskValue(Callable<T> callable) {
		try {
			return fxTask(callable).get();
		} catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	public final void update(String text) {
		fxThread(() -> {
			setProgress(++current / total);
			setText(text);
		});
	}
	
	public final void setText(String text) {
		fxThread(() -> lblStatus.setText(text));
	}
	
	public final void setTotal(int value) {
		total = value;
		fxThread(() -> prgbar.setProgress(current / total));
	}
	
	public final void setProgress(double value) {
		fxThread(() -> prgbar.setProgress(value));
	}
	
	public final double getProgress() {
		return fxTaskValue(prgbar::getProgress);
	}
}