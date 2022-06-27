package sune.app.mediadown.gui.form;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

public class FormButton extends Button {
	
	private final String name;
	private final String title;
	
	public FormButton(String name, String title, EventHandler<ActionEvent> action) {
		super(title);
		if((name == null || title == null))
			throw new NullPointerException();
		this.name = name;
		this.title = title;
		setOnAction(action);
	}
	
	public String getName() {
		return name;
	}
	
	public String getTitle() {
		return title;
	}
}