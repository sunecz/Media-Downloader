package sune.app.mediadown.gui.form;

import javafx.scene.Node;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

public abstract class FormField {
	
	private final String name;
	private final String title;
	
	public FormField(String name, String title) {
		this.name = name;
		this.title = title;
	}
	
	public abstract Node render(Form form);
	public abstract void setValue(SSDValue value, SSDType type);
	public abstract Object getValue();
	
	public String getName() {
		return name;
	}
	
	public String getTitle() {
		return title;
	}
}