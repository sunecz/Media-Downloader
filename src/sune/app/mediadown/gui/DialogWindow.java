package sune.app.mediadown.gui;

import java.util.LinkedHashMap;
import java.util.Map;

import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import sune.app.mediadown.gui.window.MainWindow;
import sune.app.mediadown.util.Utils;

public class DialogWindow<T extends Pane, R> extends Window<T> {
	
	protected final Map<String, Object> args = new LinkedHashMap<>();
	protected R theResult;
	
	public DialogWindow(String winName, T pane) {
		super("dialogs." + winName, pane);
	}
	
	public DialogWindow(String winName, T pane, double width, double height) {
		super("dialogs." + winName, pane, width, height);
	}
	
	protected final void prepare(javafx.stage.Window owner, Modality modality) {
		if((owner == null))
			owner = MainWindow.getInstance();
		initOwner(owner);
		if((modality == null))
			modality = Modality.APPLICATION_MODAL;
		initModality(modality);
		showingProperty().addListener((o, ov, nv) -> {
			if((nv)) {
				initWithArgs();
				requestFocus();
			}
		});
	}
	
	protected void initWithArgs() {
		// by default do nothing
	}
	
	protected void setResult(R result) {
		theResult = result;
	}
	
	public DialogWindow<T, R> setArgs(Map<String, Object> args) {
		this.args.clear();
		this.args.putAll(args);
		return this;
	}
	
	public DialogWindow<T, R> setArgs(Object... args) {
		setArgs(Utils.<String, Object>toMap(args));
		return this;
	}
	
	public R showAndGet() {
		showAndWait();
		return theResult;
	}
}