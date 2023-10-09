package sune.app.mediadown.gui;

import java.util.Map;

import javafx.scene.layout.Pane;

public class DialogWindow<T extends Pane, R> extends DraggableWindow<T> {
	
	protected R theResult;
	
	public DialogWindow(String winName, T pane) {
		super("dialogs." + winName, pane);
	}
	
	public DialogWindow(String winName, T pane, double width, double height) {
		super("dialogs." + winName, pane, width, height);
	}
	
	protected void setResult(R result) {
		theResult = result;
	}
	
	@Override
	public DialogWindow<T, R> setArgs(Map<String, Object> args) {
		@SuppressWarnings("unchecked")
		DialogWindow<T, R> win = (DialogWindow<T, R>) super.setArgs(args);
		return win;
	}
	
	@Override
	public DialogWindow<T, R> setArgs(Object... args) {
		@SuppressWarnings("unchecked")
		DialogWindow<T, R> win = (DialogWindow<T, R>) super.setArgs(args);
		return win;
	}
	
	public R showAndGet() {
		showAndWait();
		return theResult;
	}
}