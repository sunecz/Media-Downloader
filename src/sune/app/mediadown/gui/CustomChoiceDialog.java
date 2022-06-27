package sune.app.mediadown.gui;

import java.util.List;

import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;
import sune.app.mediadown.gui.control.ScrollableComboBox;
import sune.app.mediadown.util.Reflection2;
import sune.app.mediadown.util.Utils;

public class CustomChoiceDialog<T> extends ChoiceDialog<T> {
	
	public CustomChoiceDialog() {
		this(null, (T) null, (T[]) null);
	}
	
	public CustomChoiceDialog(StringConverter<T> converter) {
		this(converter, (T) null, (T[]) null);
	}
	
	@SafeVarargs
	public CustomChoiceDialog(T defaultChoice, T... choices) {
		this(null, defaultChoice, choices);
	}
	
	@SafeVarargs
	public CustomChoiceDialog(StringConverter<T> converter, T defaultChoice, T... choices) {
		this(converter, defaultChoice, choices == null ? List.of() : List.of(choices));
	}
	
	public CustomChoiceDialog(T defaultChoice, List<T> choices) {
		this(null, defaultChoice, choices);
	}
	
	public CustomChoiceDialog(StringConverter<T> converter, T defaultChoice, List<T> choices) {
		super(defaultChoice, choices);
		initCustom(converter, defaultChoice, choices);
	}
	
	private final void initCustom(StringConverter<T> converter, T defaultChoice, List<T> choices) {
		ComboBox<T> oldComboBox = Reflection2.getField(ChoiceDialog.class, this, "comboBox");
		ComboBox<T> newComboBox = new ScrollableComboBox<>();
		Reflection2.setField(ChoiceDialog.class, this, "comboBox", newComboBox);
		newComboBox.setMinWidth(150);
		if(choices != null) newComboBox.getItems().addAll(Utils.deduplicate(choices));
		if(converter != null) newComboBox.setConverter(converter);
		newComboBox.setMaxWidth(Double.MAX_VALUE);
	    GridPane.setHgrow(newComboBox, Priority.ALWAYS);
	    GridPane.setFillWidth(newComboBox, true);
	    if(defaultChoice == null) {
			newComboBox.getSelectionModel().selectFirst();
	    } else {
			newComboBox.getSelectionModel().select(defaultChoice);
	    }
	    GridPane grid = (GridPane) getDialogPane().getContent();
	    grid.getChildren().remove(oldComboBox);
	    grid.add(newComboBox, 1, 0);
	}
}