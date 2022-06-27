package sune.app.mediadown.gui.form;

import java.util.Collections;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class Form extends VBox {
	
	private final Pane pane;
	private final String name;
	private final String title;
	private final List<FormField> fields;
	private final List<Form> groups;
	private final List<FormButton> buttons;
	
	private final Label lblTitle;
	private final GridPane paneFields;
	private final HBox paneButtons;
	
	public Form(Pane pane, String name, String title, List<FormField> fields, List<Form> groups,
			List<FormButton> buttons) {
		if((pane == null || fields == null || groups == null || buttons == null))
			throw new IllegalArgumentException();
		this.pane = pane;
		this.name = name;
		this.title = title;
		this.fields = Collections.unmodifiableList(fields);
		this.groups = Collections.unmodifiableList(groups);
		this.buttons = Collections.unmodifiableList(buttons);
		lblTitle = new Label(title);
		lblTitle.getStyleClass().add("form-title");
		paneFields = new GridPane();
		paneFields.setHgap(10.0);
		paneFields.setVgap(10.0);
		paneFields.getStyleClass().add("form-fields");
		paneButtons = new HBox(5.0);
		paneButtons.setAlignment(Pos.CENTER_RIGHT);
		paneButtons.getStyleClass().add("form-buttons");
		if((title != null)) {
			pane.getChildren().add(lblTitle);
		}
		pane.getChildren().addAll(paneFields, paneButtons);
		pane.getStyleClass().add("form");
		getChildren().add(pane);
	}
	
	public Pane getPane() {
		return pane;
	}
	
	public Label getLabelTitle() {
		return lblTitle;
	}
	
	public GridPane getPaneFields() {
		return paneFields;
	}

	public HBox getPaneButtons() {
		return paneButtons;
	}
	
	public String getName() {
		return name;
	}
	
	public String getTitle() {
		return title;
	}
	
	public List<FormField> getFields() {
		return fields;
	}
	
	public List<Form> getGroups() {
		return groups;
	}
	
	public List<FormButton> getButtons() {
		return buttons;
	}
}