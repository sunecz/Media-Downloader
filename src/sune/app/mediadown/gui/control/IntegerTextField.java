package sune.app.mediadown.gui.control;

public class IntegerTextField extends FixedTextField {
	
	private static final int DEFAULT_VALUE = 0;
	
	public IntegerTextField() {
		this(DEFAULT_VALUE);
	}
	
	public IntegerTextField(int value) {
		super(Integer.toString(value));
		textProperty().addListener((o, oldValue, newValue) -> {
			if(!newValue.matches("\\d*")) {
				setText(newValue.replaceAll("[^\\d]", ""));
			}
		});
	}
	
	public void setValue(int value) {
		setText(Integer.toString(value));
	}
	
	public int getValue() {
		String text = getText();
		return !text.isEmpty() ? Integer.parseInt(text) : DEFAULT_VALUE;
	}
}