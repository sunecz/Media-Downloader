package sune.app.mediadown.gui.form;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
	private final List<FormField<?>> fields;
	private final List<Form> groups;
	private final List<FormButton> buttons;
	
	private final Label lblTitle;
	private final GridPane paneFields;
	private final HBox paneButtons;
	
	public Form(Pane pane, String name, String title, List<FormField<?>> fields, List<Form> groups,
			List<FormButton> buttons) {
		this.pane = Objects.requireNonNull(pane);
		this.name = name;
		this.title = title;
		this.fields = Collections.unmodifiableList(Objects.requireNonNull(fields));
		this.groups = Collections.unmodifiableList(Objects.requireNonNull(groups));
		this.buttons = Collections.unmodifiableList(Objects.requireNonNull(buttons));
		lblTitle = new Label(title);
		lblTitle.getStyleClass().add("form-title");
		paneFields = new GridPane();
		paneFields.setHgap(10.0);
		paneFields.setVgap(10.0);
		paneFields.getStyleClass().add("form-fields");
		paneButtons = new HBox(5.0);
		paneButtons.setAlignment(Pos.CENTER_RIGHT);
		paneButtons.getStyleClass().add("form-buttons");
		if(title != null) {
			pane.getChildren().add(lblTitle);
		}
		pane.getChildren().addAll(paneFields, paneButtons);
		pane.getStyleClass().add("form");
		getChildren().add(pane);
	}
	
	/** @since 00.02.07 */
	public Pane pane() {
		return pane;
	}
	
	/** @since 00.02.07 */
	public Label labelTitle() {
		return lblTitle;
	}
	
	/** @since 00.02.07 */
	public GridPane paneFields() {
		return paneFields;
	}
	
	/** @since 00.02.07 */
	public HBox paneButtons() {
		return paneButtons;
	}
	
	/** @since 00.02.07 */
	public String name() {
		return name;
	}
	
	/** @since 00.02.07 */
	public String title() {
		return title;
	}
	
	/** @since 00.02.07 */
	public List<FormField<?>> fields() {
		return fields;
	}
	
	/** @since 00.02.07 */
	public List<Form> groups() {
		return groups;
	}
	
	/** @since 00.02.07 */
	public List<FormButton> buttons() {
		return buttons;
	}
}