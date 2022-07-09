package sune.app.mediadown.gui.form;

import java.util.Objects;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

public class FormButton extends Button {
	
	private final String name;
	private final String title;
	
	public FormButton(String name, String title, EventHandler<ActionEvent> action) {
		super(title);
		this.name = Objects.requireNonNull(name);
		this.title = Objects.requireNonNull(title);
		setOnAction(action);
	}
	
	/** @since 00.02.07 */
	public String name() {
		return name;
	}
	
	/** @since 00.02.07 */
	public String title() {
		return title;
	}
}