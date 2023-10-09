package sune.app.mediadown.gui.control;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.image.ImageView;

/** @since 00.02.09 */
public abstract class IconTableCell<T> extends TableCell<T, String> {
	
	protected ImageView icon;
	
	protected IconTableCell() {
		getStyleClass().add("has-icon");
	}
	
	protected abstract ImageView iconView();
	
	protected void initialize() {
		if(isInitialized()) {
			return;
		}
		
		if(getTableRow().getItem() == null) {
			return;
		}
		
		ImageView view = iconView();
		
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
	
	protected void value(String value) {
		initialize();
	}
	
	@Override
	protected void updateItem(String item, boolean empty) {
		if(item == getItem() && isInitialized()) {
			return;
		}
		
		super.updateItem(item, empty);
		
		if(item == null) {
			setText(null);
			setGraphic(null);
			dispose();
		} else {
			value(item);
		}
	}
}