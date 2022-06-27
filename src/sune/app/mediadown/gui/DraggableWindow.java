package sune.app.mediadown.gui;

import java.util.Objects;

import javafx.event.EventTarget;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
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

public class DraggableWindow<T extends Pane> extends Window<StackPane> {
	
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
	
	public void centerWindow(Stage parent) {
		FXUtils.centerWindow(this, parent, 0.0, -4.0);
	}
	
	public void centerWindowOnShow(Stage parent) {
		FXUtils.centerWindowOnShow(this, parent, 0.0, -4.0);
	}
	
	public void fitSizeToContent() {
		FXUtils.once(scene::addPostLayoutPulseListener,
			scene::removePostLayoutPulseListener,
			() -> {
				double height = wrapper.getHeight();
				Insets padding = pane.getPadding();
				height += padding.getTop();
				height += padding.getBottom();
				setHeight(height);
			});
	}
	
	public void centerWindowAfterLayoutUpdate() {
		FXUtils.once(scene::addPostLayoutPulseListener,
			scene::removePostLayoutPulseListener,
			() -> {
				Stage parent;
				if((parent = (Stage) args.get("parent")) != null) {
					centerWindow(parent);
				}
			});
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
		private static final double HEADER_MIN_HEIGHT = 30.0;
		private static final Insets RESIZE_INSETS = new Insets(INNER_INSETS + 5.0);
		
		private final Stage stage;
		private final Insets border;
		
		private double mx;
		private double my;
		private double sx, sy, sw, sh;
		
		private Direction dragDirection;
		private boolean isCursorChanged;
		
		private Resizer(Stage stage, Insets border) {
			this.stage = Objects.requireNonNull(stage);
			this.border = Objects.requireNonNull(border);
			stage.resizableProperty().addListener((o, ov, nv) -> updateResizable(nv));
			updateResizable(stage.isResizable());
			FXUtils.onWindowShowOnce(stage, () -> {
				Dimension2D min = minSize((Region) stage.getScene().getRoot());
				if(stage.getMinWidth()  <= 0.0) stage.setMinWidth (min.getWidth()  + 2.0 * INNER_INSETS);
				if(stage.getMinHeight() <= 0.0) stage.setMinHeight(min.getHeight() + 3.0 * INNER_INSETS + HEADER_MIN_HEIGHT);
			});
		}
		
		private static final Dimension2D minSize(Region node) {
			double minWidth = 0.0, minHeight = 0.0;
			for(Node child : node.getChildrenUnmodifiable()) {
				if(child instanceof Region) {
					Region r = (Region) child;
					double minW = r.minWidth(-1.0);
					double minH = r.minHeight(-1.0);
					if(minW <= 0.0 || minH <= 0.0) {
						Dimension2D min = minSize(r);
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
		
		public static final Resizer create(Stage stage) {
			return create(stage, RESIZE_INSETS);
		}
		
		public static final Resizer create(Stage stage, Insets border) {
			return new Resizer(stage, border);
		}
		
		private final void updateResizable(boolean newValue) {
			if(newValue) addListeners(); else removeListeners();
		}
		
		private final void addListeners() {
			stage.addEventFilter(MouseEvent.MOUSE_PRESSED, this::mousePressed);
			stage.addEventFilter(MouseEvent.MOUSE_RELEASED, this::mouseReleased);
			stage.addEventFilter(MouseEvent.MOUSE_MOVED, this::mouseMoved);
			stage.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);
		}
		
		private final void removeListeners() {
			stage.removeEventFilter(MouseEvent.MOUSE_PRESSED, this::mousePressed);
			stage.removeEventFilter(MouseEvent.MOUSE_RELEASED, this::mouseReleased);
			stage.removeEventFilter(MouseEvent.MOUSE_MOVED, this::mouseMoved);
			stage.removeEventFilter(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);
		}
		
		private final Bounds stageBounds() {
			return stage.getScene().getRoot().getBoundsInParent();
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
				bounds.getHeight() - border.getTop() - border.getBottom()
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
			for(int i = 0; i < 4; ++i)
				args[i] += matrix[i] * delta[i];
			return args;
		}
		
		private final void loadStageArgs() {
			sx = stage.getX();
			sy = stage.getY();
			sw = stage.getWidth();
			sh = stage.getHeight();
		}
		
		private final void saveStageArgs(double[] args) {
			double w = Math.max(stage.getMinWidth(),  Math.min(args[2], stage.getMaxWidth()));
			double h = Math.max(stage.getMinHeight(), Math.min(args[3], stage.getMaxHeight()));
			if(w == args[2]) stage.setX(args[0]);
			if(h == args[3]) stage.setY(args[1]);
			stage.setWidth(w);
			stage.setHeight(h);
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
			return scaledRound(value, stage.getRenderScaleX());
		}
		
		private final double scaledRoundY(double value) {
			return scaledRound(value, stage.getRenderScaleY());
		}
		
		private final void mousePressed(MouseEvent event) {
			if(!isDragTarget(event.getTarget())) return;
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
					stage.getScene().setCursor(direction.cursor());
					inDragBorder = true;
					isCursorChanged = true;
				}
			}
			if(!inDragBorder && isCursorChanged) {
				stage.getScene().setCursor(Cursor.DEFAULT);
			}
		}
		
		private final void mouseDragged(MouseEvent event) {
			if(dragDirection == null) return;
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
				if(Objects.requireNonNull(matrix).length != 4)
					throw new IllegalArgumentException();
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