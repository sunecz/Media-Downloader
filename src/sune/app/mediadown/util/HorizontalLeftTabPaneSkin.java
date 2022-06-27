package sune.app.mediadown.util;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Custom tab pane skin for left-positioned horizontal tabs. Uses the original
 * {@linkplain TabPaneSkin}.
 * May break in the future due to the use of Java Reflection.
 * @author Sune
 */
public final class HorizontalLeftTabPaneSkin extends TabPaneSkin {
	
	private final Class<?> clazzTPS;
	private final Class<?> clazzTHA;
	private final StackPane tabHeaderArea;
	private final Rectangle headerClip;
	private final StackPane headersRegion;
	private final List<StackPane> tabContentRegions;
	private final double spacer;
	
	public HorizontalLeftTabPaneSkin(TabPane control) throws Exception {
		super(control);
		clazzTPS = TabPaneSkin.class;
		clazzTHA = Class.forName(TabPaneSkin.class.getCanonicalName() + "$TabHeaderArea");
		tabHeaderArea = (StackPane) Reflection3.getFieldValue(this, TabPaneSkin.class, "tabHeaderArea");
		spacer = Reflection3.getFieldValueDouble(null, TabPaneSkin.class, "SPACER");
		@SuppressWarnings("unchecked")
		List<StackPane> _tabContentRegions
			= (List<StackPane>) Reflection3.getFieldValue(this, TabPaneSkin.class, "tabContentRegions");
		tabContentRegions = _tabContentRegions;
		headerClip = new Rectangle();
		headersRegion = new StackPane() {
			
			@Override
			protected double computePrefWidth(double height) {
				double width = 0.0;
				for(Node child : getChildren()) {
					if((child.isVisible())) {
						width = Math.max(width, child.prefWidth(height));
					}
				}
				return snapSizeX(width) + snappedLeftInset() + snappedRightInset();
			}
			
			@Override
			protected double computePrefHeight(double width) {
				double height = 0.0;
				for(Node child : getChildren()) {
					if((child.isVisible())) {
						height += child.prefHeight(width);
					}
				}
				return snapSizeY(height) + snappedTopInset() + snappedBottomInset();
			}
			
			@Override
			protected void layoutChildren() {
				double tabBackgroundWidth = snapSizeX(prefWidth(-1)) + spacer;
				// Since the layout Y value is already set at this point and we have to negate it to position
				// the header tabs at the [0, 0] relative to the header, we can just negate it and start from
				// there.
				double tabY = -getLayoutY();
				double tabX = -getLayoutX();
				// Position the header tabs starting from [0, 0] relative to the header
				for(Node node : getChildren()) {
					double nodeHeight = snapSizeY(node.prefHeight(-1));
					node.resize(tabBackgroundWidth, nodeHeight);
					node.relocate(tabX, tabY);
					tabY += nodeHeight;
				}
			}
		};
		StackPane prevHeaderBackground = getField(tabHeaderArea, clazzTHA, "headerBackground");
		headersRegion.getStyleClass().setAll("headers-region");
		headersRegion.setClip(null); // Disable clipping
		setField(tabHeaderArea, clazzTHA, "headerClip", headerClip);
		setField(tabHeaderArea, clazzTHA, "headersRegion", headersRegion);
		tabHeaderArea.getChildren().setAll(prevHeaderBackground, headersRegion);
		tabHeaderArea.setClip(null); // Disable clipping
		setupReordering();
	}
	
	@SuppressWarnings("unchecked")
	private final <T> T getField(Object object, Class<?> clazz, String fieldName)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		return (T) Reflection3.getFieldValue(object, clazz, fieldName);
	}
	
	private final void setField(Object object, Class<?> clazz, String fieldName, Object value)
			throws NoSuchFieldException,
				   SecurityException,
				   IllegalArgumentException,
				   IllegalAccessException {
		Reflection3.setFieldValue(object, clazz, fieldName, value);
	}
	
	private final void setupReordering()
			throws NoSuchMethodException,
				   SecurityException,
				   NoSuchFieldException,
				   IllegalAccessException,
				   InvocationTargetException,
				   IllegalArgumentException {
		Reflection3.invoke(this, clazzTPS, "setupReordering", new Class<?>[] { StackPane.class }, headersRegion);
	}
	
	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		super.layoutChildren(x, y, w, h);
		double headerWidth = snapSizeX(tabHeaderArea.prefWidth(-1));
		tabHeaderArea.getTransforms().clear();
		tabHeaderArea.resize(headerWidth, h + 1 /* graphical fix */);
		tabHeaderArea.relocate(x, y);
		// Must also resize and relocate the contents of the tab pane
		double contentWidth = w - headerWidth;
		double contentHeight = h;
		double contentStartX = headerWidth;
		double contentStartY = 0.0;
		for(StackPane tabContent : tabContentRegions) {
			tabContent.resize(contentWidth, contentHeight);
			tabContent.relocate(contentStartX, contentStartY);
			tabContent.setAlignment(Pos.TOP_LEFT);
			tabContent.setClip(null); // Disable clipping
		}
	}
}