package sune.app.mediadown.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.module.ResolvedModule;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableViewSkinBase;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.exception.WrappedReportContextException;
import sune.app.mediadown.gui.GUI;
import sune.app.mediadown.gui.window.MainWindow;
import sune.app.mediadown.gui.window.ReportWindow;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.report.Report;
import sune.app.mediadown.report.Report.Reason;
import sune.app.mediadown.report.ReportContext;
import sune.app.mediadown.theme.Theme;
import sune.app.mediadown.util.Reflection2.InstanceCreationException;

public final class FXUtils {
	
	private static Consumer<Throwable> exceptionHandler;
	
	public static final void setExceptionHandler(Consumer<Throwable> handler) {
		exceptionHandler = handler;
	}
	
	private static final void handleException(Throwable throwable) {
		if((exceptionHandler != null))
			exceptionHandler.accept(throwable);
	}
	
	public static final void init() {
		init(() -> {});
	}
	
	public static final void init(Runnable r) {
		try {
			Platform.setImplicitExit(false);
			Platform.startup(r);
		} catch(Exception ex) {
			handleException(ex);
		}
	}
	
	public static final void exit() {
		Platform.exit();
	}
	
	public static final void thread(Runnable r) {
		if(!Platform.isFxApplicationThread()) {
			Platform.runLater(r);
		} else r.run();
	}
	
	public static final void enqueue(Runnable r) {
		Platform.runLater(r);
	}
	
	private static Runnable RUNNABLE_INIT_CHECK;
	public  static final boolean isInitialized() {
		if((RUNNABLE_INIT_CHECK == null)) {
			RUNNABLE_INIT_CHECK = (() -> {});
		}
		try {
			Platform.runLater(RUNNABLE_INIT_CHECK);
			return true;
		} catch(IllegalStateException ex) {
			// Ignore
		}
		return false;
	}
	
	public static final <T> void once(ReadOnlyProperty<T> property, ChangeListener<? super T> listener) {
		new FXProperty<>(property).once(listener);
	}
	
	public static final <T> void once(ReadOnlyProperty<T> property, InvalidationListener listener) {
		new FXProperty<>(property).once(listener);
	}
	
	public static final void centerWindow(Stage window) {
		centerWindow(window, 0.0, 0.0);
	}
	
	public static final void centerWindow(Stage window, double offX, double offY) {
		Rectangle2D screen = Screen.getPrimary().getVisualBounds();
		double px = 0.0;
		double py = 0.0;
		double pw = screen.getWidth();
		double ph = screen.getHeight();
		double ww = window.getWidth();
		double wh = window.getHeight();
		double sx = px + (pw - ww) * 0.5 + offX;
		double sy = py + (ph - wh) * 0.5 + offY;
		window.setX(sx);
		window.setY(sy);
	}
	
	public static final void centerWindow(Stage window, Stage parent) {
		centerWindow(window, parent, 0.0, 0.0);
	}
	
	public static final void centerWindow(Stage window, Stage parent, double offX, double offY) {
		double px = parent.getX();
		double py = parent.getY();
		double pw = parent.getWidth();
		double ph = parent.getHeight();
		double ww = window.getWidth();
		double wh = window.getHeight();
		double sx = px + (pw - ww) * 0.5 + offX;
		double sy = py + (ph - wh) * 0.5 + offY;
		window.setX(sx);
		window.setY(sy);
	}
	
	public static final void centerWindowOnShow(Stage window, Stage parent) {
		centerWindowOnShow(window, parent, 0.0, 0.0);
	}
	
	public static final void centerWindowOnShow(Stage window, Stage parent, double offX, double offY) {
		once(window.showingProperty(), (o, ov, nv) -> {
			if((nv && parent != null)) {
				centerWindow(window, parent, offX, offY);
			}
		});
	}
	
	public static final void showErrorWindow(String title, String message) {
		showErrorWindow(null, title, message);
	}
	
	public static final void showErrorWindow(Stage stage, String title, String message) {
		thread(() -> {
			Alert alert = new Alert(AlertType.ERROR);
			alert.initModality(Modality.APPLICATION_MODAL);
			if((stage != null))
				alert.initOwner(stage);
			alert.setHeaderText(null);
			alert.setContentText(message);
			alert.setTitle(title);
			setDialogIcon(alert, MediaDownloader.ICON);
			alert.getButtonTypes().clear();
			alert.getButtonTypes().add(ButtonType.OK);
			alert.showAndWait();
		});
	}
	
	/** @since 00.02.09 */
	private static final Image modenaIcon(String name) {
		ResolvedModule module = ModuleLayer.boot().configuration().findModule("javafx.controls").orElse(null);
		
		if(module == null) {
			return null;
		}
		
		URI moduleUri = module.reference().location().orElse(null);
		
		if(moduleUri == null) {
			return null;
		}
		
		String uri = "jar:" + moduleUri.toString() + "!/com/sun/javafx/scene/control/skin/modena/" + name;
		return new Image(uri);
	}
	
	/** @since 00.02.08 */
	private static final class ExceptionWindow {
		
		private static volatile ExceptionWindow instance;
		
		private volatile ExceptionAlert alert;
		private volatile boolean allowClear;
		
		private ExceptionWindow() {
			allowClear = true;
		}
		
		public static final ExceptionWindow instance() {
			ExceptionWindow ref = instance;
			
			if(ref == null) {
				synchronized(ExceptionWindow.class) {
					ref = instance;
					
					if(ref == null) {
						instance = ref = new ExceptionWindow();
					}
				}
			}
			
			return ref;
		}
		
		private final void initialize() {
			if(alert != null) {
				return;
			}
			
			alert = new ExceptionAlert(AlertType.ERROR);
			alert.initModality(Modality.APPLICATION_MODAL);
			alert.setHeaderText(null);
			alert.setOnCloseRequest((e) -> clear());
		}
		
		private final void clear() {
			if(alert == null || !allowClear) {
				return;
			}
			
			alert.clearTabs();
		}
		
		private final void show() {
			if(alert.isShowing()) {
				return;
			}
			
			alert.showAndWait();
		}
		
		private final void add(ErrorItem item) {
			enqueue(() -> {
				initialize();
				alert.addTab(item);
				show();
			});
		}
		
		public void error(Throwable throwable) {
			if(throwable == null) {
				return;
			}
			
			add(new OfThrowable(throwable));
		}
		
		/** @since 00.02.09 */
		public void error(String message, String content) {
			if(message == null) {
				return;
			}
			
			add(new OfMessage(message, content));
		}
		
		public void refresh() {
			if(alert == null || !alert.isShowing()) {
				return;
			}
			
			allowClear = false;
			alert.close();
			allowClear = true;
			enqueue(this::show);
		}
		
		/** @since 00.02.09 */
		private static interface ExceptionAlertContext {
			
			Button createReportButton(ErrorItem item);
		}
		
		/** @since 00.02.09 */
		private static final class ExceptionAlert extends Alert implements ExceptionAlertContext {
			
			private final VBox pane;
			private final TabPane tabs;
			
			public ExceptionAlert(AlertType alertType) {
				super(alertType);
				pane = new VBox(5.0);
				tabs = new TabPane();
				pane.getChildren().addAll(tabs);
				getDialogPane().setContent(pane);
				VBox.setVgrow(tabs, Priority.ALWAYS);
				setDialogIcon(this, MediaDownloader.ICON);
				getButtonTypes().setAll(ButtonType.OK);
				setGraphic(null);
				initStylesheet();
				
				// Make the window resizable so that at least the user can resize it
				// and that way see the content, if the window is incorrectly sized.
				// See: https://github.com/javafxports/openjdk-jfx/issues/222
				// See: https://bugs.openjdk.org/browse/JDK-8179073
				setResizable(true);
				
				// Limit the size of the window. This should help when the window is
				// incorrectly sized on some Linux systems.
				Stage stage = alertStage(this);
				stage.setMinWidth(200.0);
				stage.setMinHeight(200.0);
			}
			
			private final void initStylesheet() {
				Theme currentTheme = MediaDownloader.theme();
				Scene scene = getDialogPane().getScene();
				scene.getStylesheets().add(currentTheme.stylesheet("general-component.css"));
			}
			
			@Override
			public Button createReportButton(ErrorItem item) {
				Translation tr = MediaDownloader.translation().getTranslation("windows.exception");
				Button btnReport = new Button(tr.getSingle("button.report"));
				
				btnReport.setOnAction((o) -> {
					Stage parent = MainWindow.getInstance();
					GUI.showReportWindow(parent, Report.Builders.ofError(
						item.throwable(), Reason.ERROR,
						item.reportContext()
					), ReportWindow.Feature.onlyReasons(Reason.ERROR));
				});
				
				return btnReport;
			}
			
			public void addTab(ErrorItem item) {
				tabs.getTabs().add(item.createTab(this, tabs));
			}
			
			public void clearTabs() {
				tabs.getTabs().clear();
			}
		}
		
		/** @since 00.02.09 */
		private static final class ErrorTab extends Tab {
			
			public ErrorTab(ErrorItem item, String text, Node content) {
				super(text, content);
				setClosable(false);
			}
		}
		
		/** @since 00.02.09 */
		private static interface ErrorItem {
			
			ErrorTab createTab(ExceptionAlertContext context, TabPane pane);
			ReportContext reportContext();
			Throwable throwable();
		}
		
		/** @since 00.02.09 */
		private static final class Common {
			
			private Common() {
			}
			
			public static final HBox createTabContentHeader(Button btnReport) {
				HBox wrapper = new HBox();
				Image icon = modenaIcon("dialog-error.png");
				
				if(icon != null) {
					ImageView iconView = new ImageView(icon);
					iconView.setFitWidth(25.0);
					iconView.setFitHeight(25.0);
					wrapper.getChildren().add(iconView);
				}
				
				HBox boxFill = new HBox();
				wrapper.getChildren().add(boxFill);
				HBox.setHgrow(boxFill, Priority.ALWAYS);
				
				wrapper.getChildren().add(btnReport);
				
				return wrapper;
			}
		}
		
		/** @since 00.02.09 */
		private static final class OfThrowable implements ErrorItem {
			
			private final Throwable throwable;
			
			public OfThrowable(Throwable throwable) {
				this.throwable = Objects.requireNonNull(throwable);
			}
			
			private final Throwable unwrappedThrowable() {
				if(throwable instanceof WrappedReportContextException) {
					WrappedReportContextException ex = (WrappedReportContextException) throwable;
					return ex.getCause();
				}
				
				return throwable;
			}
			
			@Override
			public ErrorTab createTab(ExceptionAlertContext context, TabPane pane) {
				VBox wrapper = new VBox(5.0);
				wrapper.setPadding(new Insets(10.0));
				
				Button btnReport = context.createReportButton(this);
				HBox header = Common.createTabContentHeader(btnReport);
				wrapper.getChildren().addAll(header);
				
				Throwable throwable = unwrappedThrowable();
				String text = Utils.throwableToString(throwable);
				TextArea area = new TextArea(text);
				area.setEditable(false);
				wrapper.getChildren().add(area);
				VBox.setVgrow(area, Priority.ALWAYS);
				
				String title = throwable.getClass().getSimpleName();
				return new ErrorTab(this, title, wrapper);
			}
			
			@Override
			public ReportContext reportContext() {
				if(throwable instanceof WrappedReportContextException) {
					WrappedReportContextException ex = (WrappedReportContextException) throwable;
					return ex.context();
				}
				
				return ReportContext.none();
			}
			
			@Override
			public Throwable throwable() {
				return unwrappedThrowable();
			}
		}
		
		/** @since 00.02.09 */
		private static final class OfMessage implements ErrorItem {
			
			private final String message;
			private final String content;
			
			public OfMessage(String message, String content) {
				this.message = Objects.requireNonNull(message);
				this.content = content; // May be null
			}
			
			@Override
			public ErrorTab createTab(ExceptionAlertContext context, TabPane pane) {
				VBox wrapper = new VBox(5.0);
				wrapper.setPadding(new Insets(10.0));
				
				Button btnReport = context.createReportButton(this);
				HBox header = Common.createTabContentHeader(btnReport);
				wrapper.getChildren().addAll(header);
				
				TextArea label = new TextArea(message);
				label.setEditable(false);
				label.setPrefRowCount(1);
				double labelHeight = label.getFont().getSize() * 3.5;
				label.setPrefHeight(labelHeight);
				label.setMinHeight(labelHeight);
				wrapper.getChildren().add(label);
				
				if(content != null) {
					TextArea area = new TextArea(content);
					area.setEditable(false);
					wrapper.getChildren().add(area);
					VBox.setVgrow(area, Priority.ALWAYS);
				}
				
				String title = "Error";
				return new ErrorTab(this, title, wrapper);
			}
			
			@Override
			public ReportContext reportContext() {
				return ReportContext.none();
			}
			
			@Override
			public Throwable throwable() {
				String text = message;
				
				if(content != null) {
					text += "\n\n" + content;
				}
				
				return new Throwable(text);
			}
		}
	}
	
	public static final void showExceptionWindow(Throwable throwable) {
		ExceptionWindow.instance().error(throwable);
	}
	
	/** @since 00.02.09 */
	public static final void showExceptionWindow(String title, String message) {
		ExceptionWindow.instance().error(title, message);
	}
	
	/** @since 00.02.08 */
	public static final void refreshExceptionWindow() {
		ExceptionWindow.instance().refresh();
	}
	
	public static final void onWindowShow(Stage stage, Runnable action) {
		if((stage == null))
			throw new IllegalArgumentException("Stage cannot be null");
		if((action == null))
			throw new IllegalArgumentException("Action cannot be null");
		if((stage.isShowing())) {
			thread(action);
		} else {
			stage.showingProperty().addListener((o, ov, nv) -> {
				if((nv)) {
					action.run();
				}
			});
		}
	}
	
	/** @since 00.02.05 */
	public static final void onDialogShow(Dialog<?> dialog, Runnable action) {
		if(dialog == null)
			throw new IllegalArgumentException("Stage cannot be null");
		if(action == null)
			throw new IllegalArgumentException("Action cannot be null");
		if(dialog.isShowing()) {
			thread(action);
		} else {
			dialog.showingProperty().addListener((o, ov, nv) -> {
				if(nv) action.run();
			});
		}
	}
	
	public static final void onWindowShowOnce(Stage stage, Runnable action) {
		if((stage == null))
			throw new IllegalArgumentException("Stage cannot be null");
		if((action == null))
			throw new IllegalArgumentException("Action cannot be null");
		if((stage.isShowing())) {
			thread(action);
		} else {
			once(stage.showingProperty(), (o, ov, nv) -> {
				if((nv)) {
					action.run();
				}
			});
		}
	}
	
	public static final void onWindowShow(Node node, Runnable action) {
		if((node == null))
			throw new IllegalArgumentException("Node cannot be null");
		if((action == null))
			throw new IllegalArgumentException("Action cannot be null");
		Scene scene = node.getScene();
		if((scene != null)) {
			Stage stage = (Stage) scene.getWindow();
			if((stage != null)) {
				if((stage.isShowing())) {
					// ensure that the action is running on the JavaFX thread
					thread(action);
				} else {
					once(stage.showingProperty(), (o, ov, nv) -> {
						if((nv)) action.run();
					});
				}
			} else {
				once(scene.windowProperty(), (o, ov, nv) -> {
					if((nv != null)) {
						if((nv.isShowing())) {
							// ensure that the action is running on the JavaFX thread
							thread(action);
						} else {
							once(nv.showingProperty(), (o0, ov0, nv0) -> {
								if((nv0)) action.run();
							});
						}
					} else {
						// repeat the whole process
						onWindowShow(node, action);
					}
				});
			}
		} else {
			once(node.sceneProperty(), (o, ov, nv) -> {
				if((nv != null)) {
					Stage stage = (Stage) nv.getWindow();
					if((stage != null)) {
						if((stage.isShowing())) {
							// ensure that the action is running on the JavaFX thread
							thread(action);
						} else {
							once(stage.showingProperty(), (o0, ov0, nv0) -> {
								if((nv0)) action.run();
							});
						}
					} else {
						once(nv.windowProperty(), (o0, ov0, nv0) -> {
							if((nv0 != null)) {
								if((nv0.isShowing())) {
									// ensure that the action is running on the JavaFX thread
									thread(action);
								} else {
									once(nv0.showingProperty(), (o1, ov1, nv1) -> {
										if((nv1)) action.run();
									});
								}
							} else {
								// repeat the whole process
								onWindowShow(node, action);
							}
						});
					}
				} else {
					// repeat the whole process
					onWindowShow(node, action);
				}
			});
		}
	}
	
	/** @since 00.01.27 */
	public static final <T> FXTask<T> fxTask(Runnable runnable) {
		FXTask<T> task = new FXTask<>(runnable);
		thread(task);
		return task;
	}
	
	/** @since 00.01.27 */
	public static final <T> FXTask<T> fxTask(Runnable runnable, T result) {
		FXTask<T> task = new FXTask<>(runnable, result);
		thread(task);
		return task;
	}
	
	public static final <T> FXTask<T> fxTask(Callable<T> callable) {
		FXTask<T> task = new FXTask<>(callable);
		thread(task);
		return task;
	}
	
	/** @since 00.01.27 */
	@SuppressWarnings("unchecked")
	public static final <T> T fxTaskValue(Runnable runnable) {
		try {
			return (T) fxTask(runnable).get();
		} catch(Exception ex) {
			handleException(ex);
		}
		return null;
	}
	
	/** @since 00.01.27 */
	public static final <T> T fxTaskValue(Runnable runnable, T result) {
		try {
			return fxTask(runnable, result).get();
		} catch(Exception ex) {
			handleException(ex);
		}
		return null;
	}
	
	public static final <T> T fxTaskValue(Callable<T> callable) {
		try {
			return fxTask(callable).get();
		} catch(Exception ex) {
			handleException(ex);
		}
		return null;
	}
	
	public static final void setDialogIcon(Dialog<?> dialog, Image icon) {
		((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().setAll(icon);
	}
	
	public static final class Table {
		
		public static final void addRowCSSClass(TableView<?> table, int index, String classCSS) {
			(new RowManipulator(table, index, (row) -> row.getStyleClass().add(classCSS))).start();
		}
		
		public static final void removeRowCSSClass(TableView<?> table, int index, String classCSS) {
			(new RowManipulator(table, index, (row) -> row.getStyleClass().remove(classCSS))).start();
		}
		
		/** @since 00.02.05 */
		public static final void resizeColumnToFitContent(TableColumn<?, ?> column) {
			ColumnResizer.resizeColumnToFitContent(column);
		}
		
		/** @since 00.02.05 */
		public static final void resizeColumnToFitContent(TreeTableColumn<?, ?> column) {
			ColumnResizer.resizeColumnToFitContent(column);
		}
		
		/** @since 00.02.05 */
		public static final void resizeColumnToFitContentAllRows(TableColumn<?, ?> column) {
			ColumnResizer.resizeColumnToFitContentAllRows(column);
		}
		
		/** @since 00.02.05 */
		public static final void resizeColumnToFitContentAllRows(TreeTableColumn<?, ?> column) {
			ColumnResizer.resizeColumnToFitContentAllRows(column);
		}
		
		private static final class RowManipulator {
			
			TableView<?> theTable;
			InvalidationListener theListener;
			int theIndex;
			Consumer<Node> theAction;
			
			RowManipulator(TableView<?> table, int index, Consumer<Node> action) {
				if((table == null || index < 0 || action == null))
					throw new IllegalArgumentException();
				theTable = table;
				theListener = ((o) -> doAction());
				theIndex = index;
				theAction = action;
			}
			
			void doAction() {
				if((theIndex >= theTable.getItems().size()))
					return;
				Node theRow = Utils.loopTo(theTable.lookupAll(".table-row-cell").iterator(), theIndex);
				if((theRow!= null)) {
					theAction.accept(theRow);
					end();
				} else {
					enqueue(() -> doAction());
				}
			}
			
			void start() {
				thread(() -> {
					theTable.needsLayoutProperty().addListener(theListener);
				});
			}
			
			void end() {
				thread(() -> {
					theTable.needsLayoutProperty().removeListener(theListener);
				});
			}
		}
		
		/** @since 00.02.05 */
		private static final class ColumnResizer {
			
			private static final int DEFAULT_RESIZE_COLUMN_NUM_ROWS = 30;
			
			private static final MethodHandle mh_getColumnHeaderFor;
			private static final MethodHandle mh_resizeColumnToFitContent;
			
			static {
				MethodHandles.Lookup lookup = MethodHandles.lookup();
				MethodHandle _mh_getColumnHeaderFor;
				MethodHandle _mh_resizeColumnToFitContent;
				
				try {
					Method method = TableHeaderRow.class.getDeclaredMethod(
						"getColumnHeaderFor",
						TableColumnBase.class
					);
					Reflection.setAccessible(method, true);
					_mh_getColumnHeaderFor = lookup.unreflect(method);
				} catch(NoSuchMethodException
							| SecurityException
							| NoSuchFieldException
							| IllegalArgumentException
							| IllegalAccessException ex) {
					throw new IllegalStateException(ex);
				}
				
				try {
					Method method = TableColumnHeader.class.getDeclaredMethod(
						"resizeColumnToFitContent",
						int.class
					);
					Reflection.setAccessible(method, true);
					_mh_resizeColumnToFitContent = lookup.unreflect(method);
				} catch(NoSuchMethodException
						| SecurityException
						| NoSuchFieldException
						| IllegalArgumentException
						| IllegalAccessException ex) {
					throw new IllegalStateException(ex);
				}
				
				mh_getColumnHeaderFor = _mh_getColumnHeaderFor;
				mh_resizeColumnToFitContent = _mh_resizeColumnToFitContent;
			}
			
			private ColumnResizer() {
			}
			
			private static final int countItems(TreeItem<?> item) {
				int count = 0;
				for(TreeItem<?> child : item.getChildren()) {
					count += countItems(child) + 1;
				}
				return count;
			}
			
			private static final int countItems(TableView<?> table) {
				return table.getItems().size();
			}
			
			private static final int countItems(TreeTableView<?> table) {
				return countItems(table.getRoot()) + (table.isShowRoot() ? 1 : 0);
			}
			
			public static final void resizeColumnToFitContent(TableColumn<?, ?> column) {
				TableView<?> table = column.getTableView();
				resizeColumnToFitContent((TableViewSkinBase<?, ?, ?, ?, ?>) table.getSkin(), column,
				                         DEFAULT_RESIZE_COLUMN_NUM_ROWS);
			}
			
			public static final void resizeColumnToFitContent(TreeTableColumn<?, ?> column) {
				TreeTableView<?> table = column.getTreeTableView();
				resizeColumnToFitContent((TableViewSkinBase<?, ?, ?, ?, ?>) table.getSkin(), column,
				                         DEFAULT_RESIZE_COLUMN_NUM_ROWS);
			}
			
			public static final void resizeColumnToFitContentAllRows(TableColumn<?, ?> column) {
				TableView<?> table = column.getTableView();
				resizeColumnToFitContent((TableViewSkinBase<?, ?, ?, ?, ?>) table.getSkin(), column, countItems(table));
			}
			
			public static final void resizeColumnToFitContentAllRows(TreeTableColumn<?, ?> column) {
				TreeTableView<?> table = column.getTreeTableView();
				resizeColumnToFitContent((TableViewSkinBase<?, ?, ?, ?, ?>) table.getSkin(), column, countItems(table));
			}
			
			public static final void resizeColumnToFitContent(TableViewSkinBase<?, ?, ?, ?, ?> skin,
					TableColumnBase<?, ?> column, int numRows) {
				Object headerRow = skin.getSkinnable().lookup("TableHeaderRow");
				
				if(headerRow == null) {
					return; // Ignore, nothing we can do
				}
				
				try {
					Object columnHeader = mh_getColumnHeaderFor.invoke(headerRow, column);
					
					if(columnHeader == null) {
						return; // Ignore, nothing we can do
					}
					
					mh_resizeColumnToFitContent.invoke(columnHeader, -1);
				} catch(Throwable ex) {
					throw new RuntimeException(ex);
				}
			}
		}
	}
	
	public static final ImageView toGraphics(Image image, double width) {
		return toGraphics(image, width, 0.0);
	}
	
	public static final ImageView toGraphics(Image image, double width, double height) {
		ImageView view = new ImageView(image);
		double ratio = image.getWidth() / image.getHeight();
		double fitw  = width;
		double fith  = height;
		if((fitw > 0.0)) {
			if((fith <= 0.0)) {
				// fit height is not set
				fith = fitw / ratio;
			}
		} else if((fith > 0.0)) {
			fitw = fith * ratio;
		}
		view.setFitWidth(fitw);
		view.setFitHeight(fith);
		return view;
	}
	
	/** @since 00.01.27 */
	private static final class CollectionChanges {
		
		private static final class ListenerHolder<A, B> {
			
			public final A collection;
			public final B listener;
			
			public ListenerHolder(A collection, B listener) {
				this.collection = collection;
				this.listener = listener;
			}
			
			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((collection == null) ? 0 : collection.hashCode());
				result = prime * result + ((listener == null) ? 0 : listener.hashCode());
				return result;
			}
			
			@Override
			public boolean equals(Object obj) {
				if(this == obj)
					return true;
				if(obj == null)
					return false;
				if(getClass() != obj.getClass())
					return false;
				ListenerHolder<?, ?> other = (ListenerHolder<?, ?>) obj;
				if(collection == null) {
					if(other.collection != null)
						return false;
				} else if(!collection.equals(other.collection))
					return false;
				if(listener == null) {
					if(other.listener != null)
						return false;
				} else if(!listener.equals(other.listener))
					return false;
				return true;
			}
		}
		
		// Don't do generics here, since we control all the input and don't require it
		private static final Map<Integer, List<ListenerHolder<Object, Object>>> reflected = new HashMap<>();
		
		private static final int getObjectID(Object object) {
			return System.identityHashCode(object);
		}
		
		private static final List<ListenerHolder<Object, Object>> listSupplier(Object key) {
			return new ArrayList<>();
		}
		
		private static final List<ListenerHolder<Object, Object>> get(Object collection) {
			return reflected.computeIfAbsent(getObjectID(collection), CollectionChanges::listSupplier);
		}
		
		private static final <F, T, L> L addReflected(F from, T to, L listener) {
			get(from).add(new ListenerHolder<>(to, listener));
			return listener;
		}
		
		private static final <F, L> List<L> removeReflected(F from) {
			List<ListenerHolder<Object, Object>> listeners = get(from);
			@SuppressWarnings("unchecked")
			List<L> casted = listeners.stream().map((l) -> (L) l.listener).collect(Collectors.toList());
			listeners.clear();
			return casted;
		}
		
		private static final <F, T, L> L removeReflected(F from, T to) {
			for(Iterator<ListenerHolder<Object, Object>> it = get(from).iterator();
					it.hasNext();) {
				ListenerHolder<Object, Object> holder = it.next();
				if((holder.collection == to)) {
					it.remove(); // Remove the holder
					@SuppressWarnings("unchecked")
					L casted = (L) holder.listener;
					return casted;
				}
			}
			return null;
		}
		
		public static final <T> void reflect(ObservableList<T> from, List<T> to) {
			from.addListener(addReflected(from, to, (ListChangeListener<T>) (change) -> {
				while(change.next()) {
					if(change.wasAdded()) thread(() -> to.addAll(change.getAddedSubList()));
					if(change.wasRemoved()) thread(() -> to.removeAll(change.getRemoved()));
				}
			}));
		}
		
		public static final <T> void reflect(ObservableSet<T> from, Set<T> to) {
			from.addListener(addReflected(from, to, (SetChangeListener<T>) (change) -> {
				if(change.wasAdded()) thread(() -> to.add(change.getElementAdded()));
				if(change.wasRemoved()) thread(() -> to.remove(change.getElementRemoved()));
			}));
		}
		
		public static final <K, V> void reflect(ObservableMap<K, V> from, Map<K, V> to) {
			from.addListener(addReflected(from, to, (MapChangeListener<K, V>) (change) -> {
				if(change.wasAdded()) thread(() -> to.put(change.getKey(), change.getValueAdded()));
				if(change.wasRemoved()) thread(() -> to.remove(change.getKey(), change.getValueRemoved()));
			}));
		}
		
		public static final <T> void unreflect(ObservableList<T> from) {
			for(ListChangeListener<T> listener
					: CollectionChanges.<ObservableList<T>, ListChangeListener<T>>removeReflected(from))
				from.removeListener(listener);
		}
		
		public static final <T> void unreflect(ObservableList<T> from, List<T> to) {
			ListChangeListener<T> listener = removeReflected(from, to);
			if((listener != null)) from.removeListener(listener);
		}
		
		public static final <T> void unreflect(ObservableSet<T> from, Set<T> to) {
			SetChangeListener<T> listener = removeReflected(from, to);
			if((listener != null)) from.removeListener(listener);
		}
		
		public static final <T> void unreflect(ObservableSet<T> from) {
			for(SetChangeListener<T> listener
					: CollectionChanges.<ObservableSet<T>, SetChangeListener<T>>removeReflected(from))
				from.removeListener(listener);
		}
		
		public static final <K, V> void unreflect(ObservableMap<K, V> from, Map<K, V> to) {
			MapChangeListener<K, V> listener = removeReflected(from, to);
			if((listener != null)) from.removeListener(listener);
		}
		
		public static final <K, V> void unreflect(ObservableMap<K, V> from) {
			for(MapChangeListener<K, V> listener
					: CollectionChanges.<ObservableMap<K, V>, MapChangeListener<K, V>>removeReflected(from))
				from.removeListener(listener);
		}
	}
	
	/** @since 00.01.27 */
	public static final <T> void reflectChanges(ObservableList<T> from, List<T> to) {
		CollectionChanges.reflect(from, to);
	}
	
	/** @since 00.01.27 */
	public static final <T> void reflectChanges(ObservableSet<T> from, Set<T> to) {
		CollectionChanges.reflect(from, to);
	}
	
	/** @since 00.01.27 */
	public static final <K, V> void reflectChanges(ObservableMap<K, V> from, Map<K, V> to) {
		CollectionChanges.reflect(from, to);
	}
	
	/** @since 00.01.27 */
	public static final <T> void unreflectChanges(ObservableList<T> from, List<T> to) {
		CollectionChanges.unreflect(from, to);
	}
	
	/** @since 00.01.27 */
	public static final <T> void unreflectChanges(ObservableList<T> from) {
		CollectionChanges.unreflect(from);
	}
	
	/** @since 00.01.27 */
	public static final <T> void unreflectChanges(ObservableSet<T> from, Set<T> to) {
		CollectionChanges.unreflect(from, to);
	}
	
	/** @since 00.01.27 */
	public static final <T> void unreflectChanges(ObservableSet<T> from) {
		CollectionChanges.unreflect(from);
	}
	
	/** @since 00.01.27 */
	public static final <K, V> void unreflectChanges(ObservableMap<K, V> from, Map<K, V> to) {
		CollectionChanges.unreflect(from, to);
	}
	
	/** @since 00.01.27 */
	public static final <K, V> void unreflectChanges(ObservableMap<K, V> from) {
		CollectionChanges.unreflect(from);
	}
	
	/** @since 00.02.00 */
	@SuppressWarnings("unchecked")
	private static final <L, T> List<L> getListeners(String fieldPrefix, ReadOnlyProperty<T> property)
			throws Exception {
		List<L> listeners = new ArrayList<>();
		Field fieldHelper = null, fieldValue = null;
		try {
			Class<?> clazzBase = property.getClass().getSuperclass();
			fieldHelper = clazzBase.getDeclaredField("helper");
			Reflection.setAccessible(fieldHelper, true);
			Object helper = fieldHelper.get(property);
			Class<?> clazzHelper = helper.getClass();
			if((clazzHelper.getSimpleName().equals("Generic"))) {
				fieldValue = clazzHelper.getDeclaredField(fieldPrefix + "Listeners");
				Reflection.setAccessible(fieldValue, true);
				listeners.addAll(Arrays.asList((L[]) fieldValue.get(helper)));
			} else {
				fieldValue = clazzHelper.getDeclaredField("listener");
				Reflection.setAccessible(fieldValue, true);
				listeners.add((L) fieldValue.get(helper));
			}
		} finally {
			if((fieldHelper != null)) Reflection.setAccessible(fieldHelper, false);
			if((fieldValue  != null)) Reflection.setAccessible(fieldValue,  false);
		}
		return listeners;
	}
	
	/** @since 00.02.00 */
	public static final <T> List<ChangeListener<? super T>> getChangeListeners(ReadOnlyProperty<T> property)
			throws Exception {
		return getListeners("change", property);
	}
	
	/** @since 00.02.00 */
	public static final <T> List<ChangeListener<? super T>> getInvalidationListeners(ReadOnlyProperty<T> property)
			throws Exception {
		return getListeners("invalidation", property);
	}
	
	/** @since 00.02.00 */
	public static abstract class Once<L> {
		
		private final Map<Long, L> listeners = new HashMap<>();
		private final Consumer<L> methodAdd;
		private final Consumer<L> methodRemove;
		
		public Once(Consumer<L> methodAdd, Consumer<L> methodRemove) {
			this.methodAdd = methodAdd;
			this.methodRemove = methodRemove;
		}
		
		protected abstract L createListener(long id, L listener);
		
		protected final void add(L listener) {
			final long id = System.nanoTime();
			L newListener = createListener(id, listener);
			methodAdd.accept(newListener);
			listeners.put(id, newListener);
		}
		
		protected final void remove(long id) {
			methodRemove.accept(listeners.get(id));
			listeners.remove(id);
		}
		
		public final void addListener(L listener) {
			add(listener);
		}
		
		public final void removeListener(L listener) {
			remove(listeners.entrySet().stream()
			            .filter((e) -> e.getValue() == listener)
			            .findFirst()
			            .orElseThrow()
			            .getKey());
		}
		
		public static final class OnceRunnable extends Once<Runnable> {
			
			public OnceRunnable(Consumer<Runnable> methodAdd, Consumer<Runnable> methodRemove) {
				super(methodAdd, methodRemove);
			}
			
			@Override
			protected Runnable createListener(long id, Runnable listener) {
				return (() -> { listener.run(); remove(id); });
			}
		}
		
		public static final class OnceInvalidationListener extends Once<InvalidationListener> {
			
			public OnceInvalidationListener(Consumer<InvalidationListener> methodAdd,
					Consumer<InvalidationListener> methodRemove) {
				super(methodAdd, methodRemove);
			}
			
			@Override
			protected InvalidationListener createListener(long id, InvalidationListener listener) {
				return ((o) -> { listener.invalidated(o); remove(id); });
			}
		}
		
		public static final class OnceChangeListener<T> extends Once<ChangeListener<T>> {
			
			public OnceChangeListener(Consumer<ChangeListener<T>> methodAdd,
					Consumer<ChangeListener<T>> methodRemove) {
				super(methodAdd, methodRemove);
			}
			
			@Override
			protected ChangeListener<T> createListener(long id, ChangeListener<T> listener) {
				return ((o, ov, nv) -> { listener.changed(o, ov, nv); remove(id); });
			}
		}
		
		/** @since 00.02.09 */
		public static final class OnceListChangeListener<T> extends Once<ListChangeListener<T>> {
			
			public OnceListChangeListener(Consumer<ListChangeListener<T>> methodAdd,
					Consumer<ListChangeListener<T>> methodRemove) {
				super(methodAdd, methodRemove);
			}
			
			@Override
			protected ListChangeListener<T> createListener(long id, ListChangeListener<T> listener) {
				return ((c) -> { listener.onChanged(c); remove(id); });
			}
		}
		
		/** @since 00.02.09 */
		public static final class OnceSetChangeListener<T> extends Once<SetChangeListener<T>> {
			
			public OnceSetChangeListener(Consumer<SetChangeListener<T>> methodAdd,
					Consumer<SetChangeListener<T>> methodRemove) {
				super(methodAdd, methodRemove);
			}
			
			@Override
			protected SetChangeListener<T> createListener(long id, SetChangeListener<T> listener) {
				return ((c) -> { listener.onChanged(c); remove(id); });
			}
		}
		
		/** @since 00.02.09 */
		public static final class OnceMapChangeListener<K, V> extends Once<MapChangeListener<K, V>> {
			
			public OnceMapChangeListener(Consumer<MapChangeListener<K, V>> methodAdd,
					Consumer<MapChangeListener<K, V>> methodRemove) {
				super(methodAdd, methodRemove);
			}
			
			@Override
			protected MapChangeListener<K, V> createListener(long id, MapChangeListener<K, V> listener) {
				return ((c) -> { listener.onChanged(c); remove(id); });
			}
		}
	}
	
	/** @since 00.02.00 */
	public static final void once(Consumer<Runnable> methodAdd, Consumer<Runnable> methodRemove, Runnable listener) {
		(new Once.OnceRunnable(methodAdd, methodRemove)).addListener(listener);
	}
	
	/** @since 00.02.00 */
	public static final void once(Consumer<InvalidationListener> methodAdd,
			Consumer<InvalidationListener> methodRemove, InvalidationListener listener) {
		(new Once.OnceInvalidationListener(methodAdd, methodRemove)).addListener(listener);
	}
	
	/** @since 00.02.00 */
	public static final <T> void once(Consumer<ChangeListener<T>> methodAdd,
			Consumer<ChangeListener<T>> methodRemove, ChangeListener<T> listener) {
		(new Once.OnceChangeListener<>(methodAdd, methodRemove)).addListener(listener);
	}
	
	/** @since 00.02.09 */
	public static final <T> void once(Consumer<ListChangeListener<T>> methodAdd,
			Consumer<ListChangeListener<T>> methodRemove, ListChangeListener<T> listener) {
		(new Once.OnceListChangeListener<>(methodAdd, methodRemove)).addListener(listener);
	}
	
	/** @since 00.02.09 */
	public static final <T> void once(Consumer<SetChangeListener<T>> methodAdd,
			Consumer<SetChangeListener<T>> methodRemove, SetChangeListener<T> listener) {
		(new Once.OnceSetChangeListener<>(methodAdd, methodRemove)).addListener(listener);
	}
	
	/** @since 00.02.09 */
	public static final <K, V> void once(Consumer<MapChangeListener<K, V>> methodAdd,
			Consumer<MapChangeListener<K, V>> methodRemove, MapChangeListener<K, V> listener) {
		(new Once.OnceMapChangeListener<>(methodAdd, methodRemove)).addListener(listener);
	}
	
	/** @since 00.02.05 */
	public static final void oncePreLayout(Scene scene, Runnable listener) {
		once(scene::addPreLayoutPulseListener, scene::removePreLayoutPulseListener, listener);
	}
	
	/** @since 00.02.05 */
	public static final void oncePostLayout(Scene scene, Runnable listener) {
		once(scene::addPostLayoutPulseListener, scene::removePostLayoutPulseListener, listener);
	}
	
	/** @since 00.02.05 */
	public static final void oncePreLayout(Stage stage, Runnable listener) {
		Scene scene;
		if((scene = stage.getScene()) != null) {
			once(scene::addPreLayoutPulseListener, scene::removePreLayoutPulseListener, listener);
		} else {
			once(stage.sceneProperty(), (o, ov, nv) -> oncePreLayout(stage, listener));
		}
	}
	
	/** @since 00.02.05 */
	public static final void oncePostLayout(Stage stage, Runnable listener) {
		Scene scene;
		if((scene = stage.getScene()) != null) {
			once(scene::addPostLayoutPulseListener, scene::removePostLayoutPulseListener, listener);
		} else {
			once(stage.sceneProperty(), (o, ov, nv) -> oncePostLayout(stage, listener));
		}
	}
	
	/** @since 00.02.06 */
	private static Application dummyApplication;
	
	/** @since 00.02.06 */
	private static final Application dummyApplication() {
		if(dummyApplication == null) {
			dummyApplication = new Application() {
				
				@Override
				public void start(Stage primaryStage) throws Exception {
					// Do nothing
				}
			};
		}
		return dummyApplication;
	}
	
	/** @since 00.02.06 */
	private static HostServices hostServices;
	
	/** @since 00.02.06 */
	public static final boolean openURI(String uri) {
		Objects.requireNonNull(uri);
		if(hostServices == null) {
			// The constructor is not publicly accessible, so just use reflection.
			// For the Application argument just use the dummy Application instance,
			// this may cause problems with other HostServices methods, however
			// the showDocument one does not use the Application instance in any way,
			// at least not currently in JavaFX 11, so we should be okay.
			try {
				hostServices = Reflection2.newInstance(
					HostServices.class,
					new Class[] { Application.class },
					dummyApplication()
				);
			} catch(InstanceCreationException ex) {
				return false;
			}
		}
		hostServices.showDocument(uri);
		// Return true even though we don't know if it was really successful
		return true;
	}
	
	/** @since 00.02.06 */
	public static final boolean openURI(URI uri) {
		return openURI(Objects.requireNonNull(uri).toString());
	}
	
	/** @since 00.02.09 */
	public static final Stage alertStage(Alert alert) {
		Objects.requireNonNull(alert);
		Class<?> internalClass = Reflection2.getClass("javafx.scene.control.HeavyweightDialog");
		return (Stage) Reflection2.getField(
			internalClass, Reflection2.getField(Dialog.class, alert, "dialog"), "stage"
		);
	}
	
	// Forbid anyone to create an instance of this class
	private FXUtils() {
	}
}