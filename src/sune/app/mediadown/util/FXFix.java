package sune.app.mediadown.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.stage.Window;
import sune.app.mediadown.util.unsafe.Reflection;

/** @since 00.02.00 */
public final class FXFix {
	
	private static final Map<ProgressBar, ProgressBarFixer> progressBarFixers = new HashMap<>();
	
	private static final class ProgressBarFixer {
		
		private final ChangeListener<Boolean> windowShowingChangedListener;
		
		public ProgressBarFixer(ProgressBar progressBar) {
			Field field = Reflection.getField(Node.class, "windowShowingChangedListener");
			windowShowingChangedListener = Reflection.getValue(field, progressBar);
			progressBar.sceneProperty().addListener((o, ov, nv) -> fix(ov, nv));
		}
		
		public final void fix(Scene oldScene, Scene newScene) {
			if(windowShowingChangedListener == null) {
				return; // Nothing to do
			}
			
			if((oldScene != null)) {
				Window win = oldScene.windowProperty().get();
				if((win != null)) {
					win.showingProperty().removeListener(windowShowingChangedListener);
				}
			}
			if((newScene != null)) {
				Window win = newScene.windowProperty().get();
				if((win != null)) {
					win.showingProperty().addListener(windowShowingChangedListener);
				}
			}
		}
	}
	
	public static final void fixProgressBar(ProgressBar progressBar) {
		progressBarFixers.computeIfAbsent(progressBar, (k) -> new ProgressBarFixer(k));
	}
	
	// Forbid anyone to create an instance of this class
	private FXFix() {
	}
}