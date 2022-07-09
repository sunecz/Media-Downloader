package sune.app.mediadown.gui.form;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class FormBuilder {
	
	private final List<FormField<?>> fields = new ArrayList<>();
	private final List<Form> groups = new ArrayList<>();
	private final List<FormButton> buttons = new ArrayList<>();
	
	private Pane pane;
	private String name;
	private String title;
	private int row;
	
	private final void addFieldGUI(Form form, FormField<?> field) {
		Node node = field.render(form);
		Label lblTitle = new Label(field.title());
		lblTitle.setWrapText(true);
		form.paneFields().getChildren().addAll(lblTitle, node);
		GridPane.setConstraints(lblTitle, 0, row);
		GridPane.setConstraints(node, 1, row);
		GridPane.setHgrow(node, Priority.ALWAYS);
		++row;
	}
	
	private final void addGroupGUI(Form form, Form group) {
		form.paneFields().getChildren().add(group);
		GridPane.setConstraints(group, 0, row, 2, 1);
		++row;
	}
	
	private final void addButtonGUI(Form form, FormButton button) {
		button.setMinWidth(80.0);
		form.paneButtons().getChildren().add(button);
	}
	
	/** @since 00.02.07 */
	public void pane(Pane pane) {
		this.pane = Objects.requireNonNull(pane);
	}
	
	/** @since 00.02.07 */
	public void name(String name) {
		this.name = name;
	}
	
	/** @since 00.02.07 */
	public void title(String title) {
		this.title = title;
	}
	
	public void addField(FormField<?> field) {
		fields.add(Objects.requireNonNull(field));
	}
	
	public void addGroup(Form form) {
		groups.add(Objects.requireNonNull(form));
	}
	
	public void addButton(FormButton button) {
		buttons.add(Objects.requireNonNull(button));
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