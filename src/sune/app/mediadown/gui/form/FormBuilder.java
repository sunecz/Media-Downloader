package sune.app.mediadown.gui.form;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class FormBuilder {
	
	private final List<FormField> fields = new ArrayList<>();
	private final List<Form> groups = new ArrayList<>();
	private final List<FormButton> buttons = new ArrayList<>();
	
	private Pane pane;
	private String name;
	private String title;
	private int row;
	
	public void setPane(Pane pane) {
		if((pane == null))
			throw new NullPointerException();
		this.pane = pane;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	private final void addFieldGUI(Form form, FormField field) {
		Node node = field.render(form);
		Label lblTitle = new Label(field.getTitle());
		lblTitle.setWrapText(true);
		form.getPaneFields().getChildren().addAll(lblTitle, node);
		GridPane.setConstraints(lblTitle, 0, row);
		GridPane.setConstraints(node, 1, row);
		GridPane.setHgrow(node, Priority.ALWAYS);
		++row;
	}
	
	private final void addGroupGUI(Form form, Form group) {
		form.getPaneFields().getChildren().add(group);
		GridPane.setConstraints(group, 0, row, 2, 1);
		++row;
	}
	
	private final void addButtonGUI(Form form, FormButton button) {
		button.setMinWidth(80.0);
		form.getPaneButtons().getChildren().add(button);
	}
	
	public void addField(FormField field) {
		if((field == null))
			throw new NullPointerException();
		fields.add(field);
	}
	
	public void addGroup(Form form) {
		if((form == null))
			throw new NullPointerException();
		groups.add(form);
	}
	
	public void addButton(FormButton button) {
		if((button == null))
			throw new NullPointerException();
		buttons.add(button);
	}
	
	public Form build() {
		Form form = new Form(pane, name, title, fields, groups, buttons);
		fields .forEach((i) -> addFieldGUI(form, i));
		groups .forEach((i) -> addGroupGUI(form, i));
		buttons.forEach((i) -> addButtonGUI(form, i));
		row = 0;
		return form;
	}
}