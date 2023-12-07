package sune.app.mediadown.gui;

import java.util.Objects;

import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.EventTarget;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.Utils;

public class DraggableWindow<T extends Pane> extends Window<StackPane> {
	
	protected static final Insets OUTER_INSETS = new Insets(20.0);
	
	protected VBox wrapper;
	protected HBox header;
	protected T content;
	
	protected Label lblHeaderTitle;
	protected Button btnHeaderClose;
	
	private double posX;
	private double posY;
	
	public DraggableWindow(String winName, T content, double width, double height, Paint paint) {
		super(winName, new StackPane(), width, height, paint);
		initWindow(content);
	}

	public DraggableWindow(String winName, T content, double width, double height) {
		super(winName, new StackPane(), width, height);
		initWindow(content);
	}

	public DraggableWindow(String winName, T content) {
		super(winName, new StackPane());
		initWindow(content);
	}
	
	private final void initWindow(T contentPane) {
		initStyle(StageStyle.TRANSPARENT);
		Resizer.create(this);
		scene.setFill(Color.TRANSPARENT);
		wrapper = new VBox();
		wrapper.setId("pane-wrapper");
		header         = new HBox();
		lblHeaderTitle = new Label(getTitle());
		SVGPath svgCross = new SVGPath();
		svgCross.setContent("M 10.5 10.5 L 20.5 20.5 M 20.5 10.5 L 10.5 20.5");
		svgCross.setStroke(Color.BLACK);
		svgCross.setStrokeWidth(0.5);
		svgCross.setId("button-cross");
		btnHeaderClose = new Button("", svgCross);
		titleProperty().addListener((o, ov, nv) -> {
			lblHeaderTitle.setText(nv);
		});
		btnHeaderClose.setOnAction((e) -> windowClose());
		lblHeaderTitle.setId("label-window-title");
		btnHeaderClose.setId("button-window-close");
		btnHeaderClose.setPrefWidth(30.0);
		btnHeaderClose.setPrefHeight(30.0);
		HBox boxHeaderFill = new HBox();
		boxHeaderFill.setId("pane-header-fill");
		HBox.setHgrow(boxHeaderFill, Priority.ALWAYS);
		header.addEventHandler(MouseEvent.MOUSE_PRESSED, (e) -> {
			if((e.getButton() != MouseButton.PRIMARY))
				return;
			posX = e.getSceneX();
			posY = e.getSceneY();
		});
		header.addEventHandler(MouseEvent.MOUSE_DRAGGED, (e) -> {
			if((e.getButton() != MouseButton.PRIMARY))
				return;
			setX(e.getScreenX() - posX);
			setY(e.getScreenY() - posY);
		});
		header.getChildren().addAll(lblHeaderTitle, boxHeaderFill, btnHeaderClose);
		header.setId("pane-header");
		content = contentPane;
		content.setId("pane-content");
		VBox.setVgrow(contentPane, Priority.ALWAYS);
		wrapper.getChildren().addAll(header, content);
		pane.getChildren().add(wrapper);
		pane.getStyleClass().add("draggable-window-pane");
		setResizable(true);
	}
	
	private final void windowClose() {
		fireEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSE_REQUEST));
		close();
	}
	
	/** @since 00.02.09 */
	protected void setCloseButtonVisible(boolean value) {
		btnHeaderClose.setVisible(value);
	}
	
	public void centerWindow(Stage parent) {
		FXUtils.centerWindow(this, parent, 0.0, -4.0);
	}
	
	public void centerWindowOnShow(Stage parent) {
		FXUtils.centerWindowOnShow(this, parent, 0.0, -4.0);
	}
	
	public void centerWindowAfterLayoutUpdate() {
		FXUtils.once(
			scene::addPostLayoutPulseListener,
			scene::removePostLayoutPulseListener,
			() -> {
				Stage parent;
				if((parent = (Stage) args.get("parent")) != null) {
					centerWindow(parent);
				}
			}
		);
	}
	
	public VBox getWrapper() {
		return wrapper;
	}
	
	public HBox getHeader() {
		return header;
	}
	
	public T getContent() {
		return content;
	}
	
	private static final class Resizer {
		
		private static final double INNER_INSETS = 20.0;
		private static final Insets DEFAULT_RESIZE_INSETS = new Insets(INNER_INSETS + 5.0);
		
		private final DraggableWindow<? extends Pane> window;
		private final Insets border;
		
		private double mx;
		private double my;
		private double sx, sy, sw, sh;
		
		private Direction dragDirection;
		private boolean isCursorChanged;
		
		private boolean isSetWidth = false;
		private boolean isSetHeight = false;
		
		private Resizer(DraggableWindow<? extends Pane> window, Insets border) {
			this.window = Objects.requireNonNull(window);
			this.border = Objects.requireNonNull(border);
			
			window.resizableProperty().addListener((o, ov, nv) -> updateResizable(nv));
			updateResizable(window.isResizable());
			
			FXUtils.onWindowShow(window, () -> {
				addUpdateListener(window.content);
				updateMinSize();
			});
		}
		
		private final void addUpdateListener(Node node) {
			if(!(node instanceof Parent)) {
				return;
			}
			
			Parent parent = (Parent) node;
			parent.getProperties().computeIfAbsent("updateListener", (k) -> {
				ListChangeListener<? super Node> listener = ((Change<? extends Node> change) -> {
					while(change.next()) {
						change.getAddedSubList().forEach(Resizer.this::addUpdateListener);
						change.getRemoved().forEach(Resizer.this::removeUpdateListener);
					}
					
					updateMinSize();
				});
				
				parent.getChildrenUnmodifiable().addListener(listener);
				return listener;
			});
		}
		
		private final void removeUpdateListener(Node node) {
			if(!(node instanceof Parent)) {
				return;
			}
			
			Parent parent = (Parent) node;
			ListChangeListener<? super Node> listener = Utils.cast(parent.getProperties().remove("updateListener"));
			
			if(listener != null) {
				ObservableList<Node> children = parent.getChildrenUnmodifiable();
				children.forEach(this::removeUpdateListener);
				children.removeListener(listener);
			}
		}
		
		private final void updateMinSize() {
			if(!window.isShowing()) {
				return;
			}
			
			Dimension2D min = minContentSize(window.content);
			Insets insets = window.content.getInsets();
			double headerHeight = window.header.getHeight();
			
			if(isSetWidth || window.getMinWidth() <= 0.0) {
				double outer = OUTER_INSETS.getLeft() + OUTER_INSETS.getRight();
				double inner = insets.getLeft() + insets.getRight();
				window.setMinWidth(min.getWidth() + inner + outer);
				isSetWidth = true;
			}
			
			if(isSetHeight || window.getMinHeight() <= 0.0) {
				double outer = OUTER_INSETS.getTop() + OUTER_INSETS.getBottom();
				double inner = insets.getTop() + insets.getBottom();
				window.setMinHeight(min.getHeight() + inner + outer + headerHeight);
				isSetHeight = true;
			}
		}
		
		private static final Dimension2D minContentSize(Region node) {
			double minWidth = 0.0, minHeight = 0.0;
			
			if(node instanceof Region) {
				Region r = (Region) node;
				double minW = r.minWidth (Region.USE_COMPUTED_SIZE);
				double minH = r.minHeight(Region.USE_COMPUTED_SIZE);
				minWidth  = Math.max(minWidth,  minW);
				minHeight = Math.max(minHeight, minH);
			}
			
			if(minWidth <= 0.0 || minHeight <= 0.0) {
				for(Node child : node.getChildrenUnmodifiable()) {
					if(!(child instanceof Region)) {
						continue;
					}
					
					Region r = (Region) child;
					double minW = r.minWidth (Region.USE_COMPUTED_SIZE);
					double minH = r.minHeight(Region.USE_COMPUTED_SIZE);
					
					if(minW <= 0.0 || minH <= 0.0) {
						Dimension2D min = minContentSize(r);
						minW = Math.max(minW, min.getWidth());
						minH = Math.max(minH, min.getHeight());
					}
					
					minWidth  = Math.max(minWidth,  minW);
					minHeight = Math.max(minHeight, minH);
				}
			}
			
			return new Dimension2D(minWidth, minHeight);
		}
		
		private static final double scaledRound(double value, double scale) {
	        return Math.round(value * scale) / scale;
	    }
		
		public static final Resizer create(DraggableWindow<? extends Pane> window) {
			return create(window, DEFAULT_RESIZE_INSETS);
		}
		
		public static final Resizer create(DraggableWindow<? extends Pane> window, Insets resizeInsets) {
			return new Resizer(window, resizeInsets);
		}
		
		private final void updateResizable(boolean newValue) {
			if(newValue) addListeners(); else removeListeners();
		}
		
		private final void addListeners() {
			window.addEventFilter(MouseEvent.MOUSE_PRESSED, this::mousePressed);
			window.addEventFilter(MouseEvent.MOUSE_RELEASED, this::mouseReleased);
			window.addEventFilter(MouseEvent.MOUSE_MOVED, this::mouseMoved);
			window.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);
		}
		
		private final void removeListeners() {
			window.removeEventFilter(MouseEvent.MOUSE_PRESSED, this::mousePressed);
			window.removeEventFilter(MouseEvent.MOUSE_RELEASED, this::mouseReleased);
			window.removeEventFilter(MouseEvent.MOUSE_MOVED, this::mouseMoved);
			window.removeEventFilter(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);
		}
		
		private final Bounds stageBounds() {
			return window.getScene().getRoot().getBoundsInParent();
		}
		
		private final Point2D mousePoint(MouseEvent event) {
			return new Point2D(event.getSceneX(), event.getSceneY());
		}
		
		private final Point2D mousePointScreen(MouseEvent event) {
			return new Point2D(event.getScreenX(), event.getScreenY());
		}
		
		private final Rectangle2D innerRect(Bounds bounds) {
			return new Rectangle2D(
				bounds.getMinX()   + border.getLeft(),
				bounds.getMinY()   + border.getTop(),
				bounds.getWidth()  - border.getLeft() - border.getRight(),
				bounds.getHeight() - border.getTop()  - border.getBottom()
			);
		}
		
		private final boolean isInDragBorder(Bounds bounds, Point2D mouse) {
			return bounds.contains(mouse) && !innerRect(bounds).contains(mouse);
		}
		
		private final Direction mouseDragDirection(Bounds bounds, Point2D mouse) {
			Rectangle2D rect = innerRect(bounds);
			
			if(mouse.getY() < rect.getMinY()) {
				if(mouse.getX() <  rect.getMinX()) return Direction.NORTH_WEST; else
				if(mouse.getX() >= rect.getMaxX()) return Direction.NORTH_EAST;
				else                               return Direction.NORTH;
			} else if(mouse.getY() >= rect.getMaxY()) {
				if(mouse.getX() <  rect.getMinX()) return Direction.SOUTH_WEST; else
				if(mouse.getX() >= rect.getMaxX()) return Direction.SOUTH_EAST;
				else                               return Direction.SOUTH;
			} else {
				if(mouse.getX() <  rect.getMinX()) return Direction.WEST; else
				if(mouse.getX() >= rect.getMaxX()) return Direction.EAST;
				else                               return Direction.NONE;
			}
		}
		
		private final boolean isDragTarget(EventTarget target) {
			String id;
			return target instanceof Node && (id = ((Node) target).getId()) != null
						&& (id.equals("pane-wrapper") || id.equals("pane-content")
								|| id.equals("pane-header-fill") || id.equals("pane-header"));
		}
		
		private final double[] transform(double[] matrix, double[] args, double[] delta) {
			for(int i = 0; i < 4; ++i) {
				args[i] += matrix[i] * delta[i];
			}
			
			return args;
		}
		
		private final void loadStageArgs() {
			sx = window.getX();
			sy = window.getY();
			sw = window.getWidth();
			sh = window.getHeight();
		}
		
		private final void saveStageArgs(double[] args) {
			double w = Math.max(window.getMinWidth(),  Math.min(args[2], window.getMaxWidth()));
			double h = Math.max(window.getMinHeight(), Math.min(args[3], window.getMaxHeight()));
			
			if(w == args[2]) window.setX(args[0]);
			if(h == args[3]) window.setY(args[1]);
			
			window.setWidth(w);
			window.setHeight(h);
		}
		
		private final double[] stageArgs() {
			return new double[] { sx, sy, sw, sh };
		}
		
		private final double[] mouseDelta(MouseEvent event) {
			Point2D mouse = mousePointScreen(event);
			double dx = scaledRoundX(mouse.getX() - mx);
			double dy = scaledRoundY(mouse.getY() - my);
			return new double[] { dx, dy, dx, dy };
		}
		
		private final double scaledRoundX(double value) {
			return scaledRound(value, window.getRenderScaleX());
		}
		
		private final double scaledRoundY(double value) {
			return scaledRound(value, window.getRenderScaleY());
		}
		
		private final void mousePressed(MouseEvent event) {
			if(!isDragTarget(event.getTarget())) {
				return;
			}
			
			Bounds bounds = stageBounds();
			Point2D mouse = mousePoint(event);
			
			if(isInDragBorder(bounds, mouse)) {
				Point2D mouseScreen = mousePointScreen(event);
				mx = scaledRoundX(mouseScreen.getX());
				my = scaledRoundY(mouseScreen.getY());
				dragDirection = mouseDragDirection(bounds, mouse);
				loadStageArgs();
			}
		}
		
		private final void mouseReleased(MouseEvent event) {
			dragDirection = null;
		}
		
		private final void mouseMoved(MouseEvent event) {
			boolean inDragBorder = false;
			
			if(isDragTarget(event.getTarget())) {
				Bounds bounds = stageBounds();
				Point2D mouse = mousePoint(event);
				
				if(isInDragBorder(bounds, mouse)) {
					Direction direction = mouseDragDirection(bounds, mouse);
					window.getScene().setCursor(direction.cursor());
					inDragBorder = true;
					isCursorChanged = true;
				}
			}
			
			if(!inDragBorder && isCursorChanged) {
				window.getScene().setCursor(Cursor.DEFAULT);
			}
		}
		
		private final void mouseDragged(MouseEvent event) {
			if(dragDirection == null) {
				return;
			}
			
			saveStageArgs(transform(dragDirection.matrix(), stageArgs(), mouseDelta(event)));
			event.consume();
		}
		
		private static enum Direction {
			
			//         cursor            sx   sy   sw   sh
			NORTH     (Cursor.N_RESIZE,  0.0, 1.0,  0.0, -1.0),
			SOUTH     (Cursor.S_RESIZE,  0.0, 0.0,  0.0,  1.0),
			WEST      (Cursor.W_RESIZE,  1.0, 0.0, -1.0,  0.0),
			EAST      (Cursor.E_RESIZE,  0.0, 0.0,  1.0,  0.0),
			NORTH_WEST(Cursor.NW_RESIZE, 1.0, 1.0, -1.0, -1.0),
			NORTH_EAST(Cursor.NE_RESIZE, 0.0, 1.0,  1.0, -1.0),
			SOUTH_WEST(Cursor.SW_RESIZE, 1.0, 0.0, -1.0,  1.0),
			SOUTH_EAST(Cursor.SE_RESIZE, 0.0, 0.0,  1.0,  1.0),
			NONE      (Cursor.DEFAULT,   0.0, 0.0,  0.0,  0.0);
			
			private final Cursor cursor;
			private final double[] matrix;
			
			private Direction(Cursor cursor, double... matrix) {
				if(Objects.requireNonNull(matrix).length != 4) {
					throw new IllegalArgumentException();
				}
				
				this.cursor = Objects.requireNonNull(cursor);
				this.matrix = matrix;
			}
			
			public Cursor cursor() {
				return cursor;
			}
			
			public double[] matrix() {
				return matrix;
			}
		}
	}
}