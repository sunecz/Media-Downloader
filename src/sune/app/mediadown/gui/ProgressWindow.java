package sune.app.mediadown.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import sune.app.mediadown.Disposables;
import sune.app.mediadown.util.FXFix;
import sune.app.mediadown.util.FXUtils;

public class ProgressWindow extends Window<StackPane> {
	
	private static final String WINDOW_NAME = "progress";
	
	public static interface ProgressAction {
		
		void action(ProgressListener listener);
		void cancel();
		
		default void cancelled() {
			// Do nothing
		}
	}
	
	public static interface ProgressListener {
		
		public static final double PROGRESS_INDETERMINATE = ProgressBar.INDETERMINATE_PROGRESS;
		public static final double PROGRESS_NONE = 0.0;
		public static final double PROGRESS_DONE = 1.0;
		
		void setText(String text);
		void setProgress(double progress);
	}
	
	private final class InternalAction implements Runnable {
		
		private final ProgressAction action;
		private final AtomicBoolean cancelled = new AtomicBoolean();
		
		public InternalAction(ProgressAction action) {
			if((action == null))
				throw new IllegalArgumentException("Action cannot be null");
			this.action = action;
		}
		
		@Override
		public void run() {
			runningAction_add(this);
			action.action(listener);
			runningAction_remove(this);
			if((cancelled.get())) {
				// Call the internal method
				action.cancelled();
			}
		}
		
		public void cancel() {
			// Call the internal method
			action.cancel();
			cancelled.set(true);
		}
	}
	
	private final List<InternalAction> running = new ArrayList<>();
	private final ExecutorService executor = Executors.newFixedThreadPool(1);
	
	private final ProgressListener listener;
	
	private Stage parent;
	private VBox boxContent;
	private VBox boxInfo;
	private Label lblText;
	private ProgressBar progressBar;
	
	private HBox boxButtons;
	private Button btnCancel;
	
	private ProgressWindow() {
		super(WINDOW_NAME, new StackPane(), 400.0, 150.0);
		initStyle(StageStyle.TRANSPARENT);
		initModality(Modality.APPLICATION_MODAL);
		scene.setFill(Color.TRANSPARENT);
		boxContent  = new VBox();
		boxInfo     = new VBox();
		lblText     = new Label();
		progressBar = new ProgressBar();
		boxButtons  = new HBox();
		btnCancel   = new Button(translation.getSingle("buttons.cancel"));
		btnCancel.setOnAction((e) -> cancelActions(null));
		lblText.setWrapText(true);
		boxInfo.getChildren().addAll(lblText, progressBar);
		boxButtons.getChildren().add(btnCancel);
		boxContent.getChildren().addAll(boxInfo, boxButtons);
		boxButtons.setId("pane-buttons");
		boxContent.setId("pane-content");
		// JavaFX 9 introduced a new bug that if a progress bar is added to a scene AFTER the scene
		// is added to a stage, then the progress bar will NOT be animated when in the indeterminate
		// state. The bug was fixed in JavaFX 12 but was not yet backported to JavaFX 11, so we need to
		// use a workaround. The procedure to fix this issue is taken from
		// https://github.com/javafxports/openjdk-jfx/pull/342/commits/b6c85cfc415eb90586ac2e475702d0ebbdcf527c
		// and the fix is implemented as such so it does not require any other action.
		FXFix.fixProgressBar(progressBar);
		pane.getChildren().add(boxContent);
		VBox.setVgrow(boxInfo, Priority.ALWAYS);
		VBox.setMargin(progressBar, new Insets(10.0, 0, 10.0, 0));
		setOnCloseRequest(this::cancelActions);
		setResizable(false);
		FXUtils.onWindowShow(this, () -> {
			if((parent != null))
				FXUtils.centerWindow(this, parent);
		});
		listener = new ProgressListener() {
			
			@Override
			public void setText(String text) {
				FXUtils.thread(() -> lblText.setText(text));
			}
			
			@Override
			public void setProgress(double progress) {
				FXUtils.thread(() -> progressBar.setProgress(progress));
			}
		};
	}
	
	private final void runningAction_add(InternalAction action) {
		reset(); // When it is needed to implement parallel actions, this has to be removed
		running.add(action);
		FXUtils.thread(() -> {
			if(!isShowing())
				// Ensure the window is visible
				show();
		});
	}
	
	private final void runningAction_remove(InternalAction action) {
		running.remove(action);
		FXUtils.thread(() -> {
			if((running.isEmpty()))
				// Ensure the window is closed
				close();
		});
	}
	
	private final void internal_submitAction(ProgressAction action) {
		executor.submit(new InternalAction(action));
	}
	
	private final void setParent(Stage parent) {
		this.parent = parent;
	}
	
	private final void reset() {
		FXUtils.thread(() -> lblText.setText(""));
	}
	
	private final void addAction(ProgressAction action) {
		internal_submitAction(action);
	}
	
	private final void cancelActions(WindowEvent event) {
		if(!running.isEmpty()) {
			for(InternalAction action : running) {
				action.cancel();
			}
			// Do not close the window itself
			if((event != null))
				event.consume();
		} else {
			// Otherwise close the window
			close();
		}
	}
	
	private static ProgressWindow instance;
	private static final ProgressWindow instance() {
		if((instance == null)) {
			instance = FXUtils.fxTaskValue(ProgressWindow::new);
			Disposables.add(instance.executor::shutdownNow);
		}
		return instance;
	}
	
	public static final ProgressWindow submitAction(Stage parent, ProgressAction action) {
		if((action == null))
			throw new IllegalArgumentException("Action cannot be null");
		ProgressWindow instance = instance();
		instance.setParent(parent); // When it is needed to implement parallel actions, this has to be removed
		instance.addAction(action);
		return instance;
	}
}