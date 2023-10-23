package sune.app.mediadown.gui.control;

import java.util.Objects;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.image.ImageView;

/** @since 00.02.09 */
public abstract class IconTableCell<T, V> extends TableCell<T, V> {
	
	protected ImageView icon;
	
	protected IconTableCell() {
		getStyleClass().add("has-icon");
	}
	
	protected abstract ImageView iconView(V value);
	
	protected void update(V value) {
		if(getTableRow().getItem() == null) {
			return;
		}
		
		ImageView view = iconView(value);
		
		if(view == null) {
			return;
		}
		
		setGraphic(view);
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		icon = view;
	}
	
	protected void dispose() {
		if(!isInitialized()) {
			return;
		}
		
		icon = null;
	}
	
	protected boolean isInitialized() {
		return icon != null;
	}
	
	@Override
	protected void updateItem(V item, boolean empty) {
		if(Objects.equals(item, getItem()) && isInitialized()) {
			return;
		}
		
		super.updateItem(item, empty);
		
		if(item == null) {
			setText(null);
			setGraphic(null);
			dispose();
		} else {
			update(item);
		}
	}
}