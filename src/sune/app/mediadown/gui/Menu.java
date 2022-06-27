package sune.app.mediadown.gui;

import javafx.collections.ListChangeListener.Change;
import javafx.scene.control.MenuItem;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.language.Translation;

public class Menu extends javafx.scene.control.Menu {
	
	private final String name;
	private final Translation translation;
	
	public Menu(String name) {
		this.name = name;
		this.translation = MediaDownloader.translation().getTranslation("menus." + name);
		getItems().addListener((Change<? extends MenuItem> change) -> {
			if(!change.next()) return;
			if((change.wasAdded())) {
				for(MenuItem item : change.getAddedSubList()) {
					item.setText(translation.getSingle("items." + item.getText()));
				}
			}
		});
		setText(translation.getSingle("title"));
	}
	
	public String getName() {
		return name;
	}
}