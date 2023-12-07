package sune.app.mediadown.gui.control;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import sune.app.mediadown.resource.ResourceRegistry;

/** @since 00.02.09 */
public class PasswordFieldPane extends GridPane {
	
	private final PasswordField passwordControl;
	private final TextField textControl;
	private final ViewToggleButton toggleButton;
	
	public PasswordFieldPane() {
		passwordControl = new PasswordField();
		textControl = new TextField();
		toggleButton = new ViewToggleButton();
		passwordControl.textProperty().bindBidirectional(textControl.textProperty());
		textControl.setVisible(false);
		toggleButton.setOnAction(this::toggleControls);
		getChildren().addAll(passwordControl, textControl, toggleButton);
		setHgap(3.0);
		GridPane.setHgrow(passwordControl, Priority.ALWAYS);
		GridPane.setHgrow(textControl, Priority.ALWAYS);
		GridPane.setConstraints(passwordControl, 0, 0);
		GridPane.setConstraints(textControl, 0, 0);
		GridPane.setConstraints(toggleButton, 1, 0);
	}
	
	private final void toggleControls(ActionEvent event) {
		boolean textVisible = toggleButton.isSelected();
		boolean passwordVisible = !textVisible;
		passwordControl.setVisible(passwordVisible);
		textControl.setVisible(textVisible);
	}
	
	public void setText(String text) {
		passwordControl.setText(text);
		textControl.setText(text);
	}
	
	public String getText() {
		return passwordControl.getText();
	}
	
	public PasswordField passwordControl() {
		return passwordControl;
	}
	
	public TextField textControl() {
		return textControl;
	}
	
	public ToggleButton toggleButton() {
		return toggleButton;
	}
	
	private static final class ViewToggleButton extends ToggleButton {
		
		private static final Image SHOW = ResourceRegistry.icon("show.png");
		private static final Image HIDE = ResourceRegistry.icon("hide.png");
		
		public ViewToggleButton() {
			selectedProperty().addListener((o, ov, nv) -> setGraphic(imageView()));
			setGraphic(imageView());
			setPadding(new Insets(4.0));
		}
		
		private final ImageView imageView() {
			ImageView view = new ImageView(isSelected() ? HIDE : SHOW);
			view.setFitWidth(16.0);
			view.setFitHeight(16.0);
			return view;
		}
	}
}