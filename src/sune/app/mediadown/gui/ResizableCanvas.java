package sune.app.mediadown.gui;

import javafx.scene.canvas.Canvas;

/** @since 00.02.09 */
public class ResizableCanvas extends Canvas {
	
	public ResizableCanvas() {
		super();
	}
	
	public ResizableCanvas(double width, double height) {
		super(width, height);
	}
	
	protected void draw() {
		// Do nothing, by default
	}
	
	@Override
	public void resize(double width, double height) {
		setWidth(width);
		setHeight(height);
		draw();
	}
	
	@Override
	public double minWidth(double height) {
		return 1.0;
	}
	
	@Override
	public double minHeight(double width) {
		return 1.0;
	}
	
	@Override
	public double maxWidth(double height) {
		return Double.MAX_VALUE;
	}
	
	@Override
	public double maxHeight(double width) {
		return Double.MAX_VALUE;
	}
	
	@Override
	public double prefWidth(double height) {
		return minWidth(height);
	}
	
	@Override
	public double prefHeight(double width) {
		return minHeight(width);
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
}