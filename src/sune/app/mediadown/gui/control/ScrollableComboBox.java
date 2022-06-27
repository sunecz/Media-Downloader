package sune.app.mediadown.gui.control;

import javafx.scene.control.ComboBox;
import javafx.scene.control.SelectionModel;
import javafx.scene.input.ScrollEvent;

public class ScrollableComboBox<T> extends ComboBox<T> {
	
	public ScrollableComboBox() {
		addEventFilter(ScrollEvent.SCROLL, (e) -> {
			if((e.getDeltaY() > 0.0)) prevItem();
			else                      nextItem();
		});
	}
	
	private final void selectItemRelative(int offset) {
		SelectionModel<T> model = getSelectionModel();
		int curIndex = model.getSelectedIndex();
		int newIndex = curIndex + offset;
		if((newIndex < 0 || newIndex >= getItems().size()))
			return;
		model.clearAndSelect(newIndex);
	}
	
	private final void prevItem() {
		selectItemRelative(-1);
	}
	
	private final void nextItem() {
		selectItemRelative(+1);
	}
}