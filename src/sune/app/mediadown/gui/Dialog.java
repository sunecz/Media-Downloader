package sune.app.mediadown.gui;

import java.util.concurrent.Callable;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.gui.util.FXUtils;

public final class Dialog extends Alert {
	
	private final ButtonType buttonTypeResult;
	
	private Dialog(AlertType type, String title, String text) {
		super(type);
		setTitle(title);
		setContentText(text);
		setHeaderText(null);
		if((type == AlertType.CONFIRMATION)) {
			getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
			buttonTypeResult = ButtonType.YES;
		} else {
			getButtonTypes().setAll(ButtonType.OK);
			buttonTypeResult = ButtonType.OK;
		}
	}

	private boolean showThemed() {
		DialogPane content = getDialogPane();
		setDialogPane(new DialogPane());
		DraggableWindow<StackPane> window = new DraggableWindow<>("main", new StackPane(), 480.0, 180.0);
		window.setTitle(getTitle());
		window.getContent().getChildren().add(content);
		window.getContent().getStyleClass().add("themed-alert-content");

		Stage owner = javafx.stage.Window.getWindows().stream()
			.filter((w) -> w.isShowing() && w.isFocused() && w instanceof Stage)
			.map((w) -> (Stage) w).findFirst().orElse(null);

		if(owner != null) window.initOwner(owner);

		window.initModality(Modality.APPLICATION_MODAL);

		final ButtonType[] result = {null};

		for(ButtonType type : content.getButtonTypes()) {
			Button button = (Button) content.lookupButton(type);
			button.setDefaultButton(type == buttonTypeResult);
			button.setCancelButton(type == ButtonType.NO || type == ButtonType.OK);
			button.setOnAction((e) -> {
				result[0] = type;
				window.close();
			});
		}

		window.getScene().addEventFilter(KeyEvent.KEY_PRESSED, (e) -> {
			if(e.getCode() == KeyCode.ESCAPE) {
				window.close();
				e.consume();
			}
		});

		window.sizeToScene();
		if(owner != null) window.centerWindowOnShow(owner);
		window.showAndWait();
		return result[0] == buttonTypeResult;
	}
	
	/** @since 00.02.09 */
	private static final boolean tryRunInFX(Callable<Boolean> callGUI, Callable<Boolean> callNonGUI) {
		if(FXUtils.isInitialized()) {
			return FXUtils.fxTaskValue(callGUI);
		}
		
		try {
			return callNonGUI.call();
		} catch(Exception ex) {
			// Ignore
		}
		
		return false;
	}
	
	/** @since 00.02.09 */
	private static final boolean nonGUIError(String title, String text) {
		return nonGUIError(title, text, null);
	}
	
	/** @since 00.02.09 */
	private static final boolean nonGUIError(String title, String text, String content) {
		StringBuilder sb = new StringBuilder();
		sb.append(title).append('\n');
		sb.append(text).append('\n');
		
		if(content != null) {
			sb.append("<<<\n").append(content).append("\n>>>\n");
		}
		
		System.err.println(sb.toString());
		return false;
	}
	
	private static final boolean show0(AlertType type, String title, String text) {
		Dialog dialog = new Dialog(type, title, text);
		return dialog.showThemed();
	}
	
	private static final boolean showContent0(AlertType type, String title, String text, String content) {
		Dialog dialog = new Dialog(type, title, null);
		dialog.setHeaderText(text);
		TextArea area = new TextArea(content);
		area.setEditable(false);
		BorderPane pane = new BorderPane(area);
		pane.setPadding(new Insets(10.0));
		dialog.getDialogPane().setContent(pane);
		return dialog.showThemed();
	}
	
	public static final boolean showError(String title, String text) {
		return tryRunInFX(
			() -> show0(AlertType.ERROR, title, text),
			() -> nonGUIError(title, text)
		);
	}
	
	public static final boolean showContentError(String title, String text, String content) {
		return tryRunInFX(
			() -> showContent0(AlertType.ERROR, title, text, content),
			() -> nonGUIError(title, text, content)
		);
	}
	
	public static final boolean showWarning(String title, String text) {
		return tryRunInFX(
			() -> show0(AlertType.WARNING, title, text),
			() -> nonGUIError(title, text)
		);
	}
	
	public static final boolean showContentWarning(String title, String text, String content) {
		return tryRunInFX(
			() -> showContent0(AlertType.WARNING, title, text, content),
			() -> nonGUIError(title, text, content)
		);
	}
	
	public static final boolean showInfo(String title, String text) {
		return tryRunInFX(
			() -> show0(AlertType.INFORMATION, title, text),
			() -> nonGUIError(title, text)
		);
	}
	
	public static final boolean showContentInfo(String title, String text, String content) {
		return tryRunInFX(
			() -> showContent0(AlertType.INFORMATION, title, text, content),
			() -> nonGUIError(title, text, content)
		);
	}
	
	public static final boolean showPrompt(String title, String text) {
		return tryRunInFX(
			() -> show0(AlertType.CONFIRMATION, title, text),
			() -> nonGUIError(title, text)
		);
	}
}
