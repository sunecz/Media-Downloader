package sune.app.mediadown.gui.control;

import javafx.scene.Group;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.control.skin.TextInputControlSkin;
import javafx.scene.layout.Region;
import javafx.scene.shape.Path;
import sune.app.mediadown.util.Reflection2;

public class FixedTextField extends TextField {
	
	private static final class FixedTextFieldSkin extends TextFieldSkin {
		
		public FixedTextFieldSkin(TextField textField) {
			super(textField);
			// Fixes the caret position, so that it is sharper; it is achieved by removing subpixel accuracy
			Path caretPath = Reflection2.getField(TextInputControlSkin.class, this, "caretPath");
			caretPath.setStrokeWidth(1.0);
			caretPath.layoutBoundsProperty().addListener((o, ov, nv) -> {
				double xfix = -(nv.getMinX() % 1.0);
				if((nv.getMinX() >= ((Region) caretPath.getParent().getParent()).getWidth())) {
					Group parent = (Group) caretPath.getParent();
					xfix -= parent.getLayoutBounds().getMinX() % 1.0;
					caretPath.setTranslateX(xfix % 1.0 + 0.5); // In range (-0.5, 0.5>
				} else {
					caretPath.setTranslateX(xfix + 0.5); // In range (-0.5, 0.5>
				}
			});
		}
	}
	
	public FixedTextField() {
		super();
		init();
	}
	
	public FixedTextField(String text) {
		super(text);
		init();
	}
	
	private final void init() {
		setSkin(new FixedTextFieldSkin(this));
	}
}