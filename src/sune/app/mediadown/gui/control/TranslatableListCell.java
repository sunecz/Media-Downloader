package sune.app.mediadown.gui.control;

import java.util.Objects;
import java.util.function.Function;

import javafx.scene.control.ListCell;
import sune.app.mediadown.language.Translation;

/** @since 00.02.09 */
public class TranslatableListCell<T> extends ListCell<T> {
	
	private final Translation translation;
	private final Function<T, String> valueToName;
	
	public TranslatableListCell(Translation translation, Function<T, String> valueToName) {
		this.translation = Objects.requireNonNull(translation);
		this.valueToName = Objects.requireNonNull(valueToName);
	}
	
	private final String translate(T item) {
		return translation.getSingle(valueToName.apply(item));
	}
	
	@Override
	protected void updateItem(T item, boolean empty) {
		super.updateItem(item, empty);
		
		if(!empty) {
			setText(translate(item));
		} else {
			setText(null);
			setGraphic(null);
		}
	}
}