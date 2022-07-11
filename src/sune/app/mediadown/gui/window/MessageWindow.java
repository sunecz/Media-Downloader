package sune.app.mediadown.gui.window;

import java.util.List;
import java.util.stream.IntStream;

import javafx.collections.ListChangeListener.Change;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.message.Message;
import sune.app.mediadown.message.MessageList;
import sune.app.mediadown.message.MessageManager;
import sune.app.mediadown.os.OS;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.Utils;

/** @since 00.02.02 */
public class MessageWindow extends DraggableWindow<StackPane> {
	
	public static final String NAME = "message";
	
	private TabPane tabPane;
	
	public MessageWindow() {
		super(NAME, new StackPane(), 700.0, 600.0);
		initModality(Modality.APPLICATION_MODAL);
		tabPane = new TabPane();
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabPane.setTabDragPolicy(TabDragPolicy.FIXED);
		tabPane.getTabs().addListener((Change<? extends Tab> change) -> {
			if(change.next() && change.wasAdded()) {
				// Use reversed sequence so that the first item is selected last
				reversedIntRange(change.getFrom(), change.getTo()).forEach(this::selectTab);
			}
		});
		content.getChildren().add(tabPane);
		FXUtils.onWindowShow(this, () -> {
			// Show all given messages
			@SuppressWarnings("unchecked")
			List<Message> messages = (List<Message>) args.getOrDefault("messages", List.of());
			loadMessages(messages);
			// Center the window
			Stage parent = (Stage) args.get("parent");
			if((parent != null)) {
				centerWindow(parent);
			}
		});
	}
	
	/** @since 00.02.05 */
	private static final IntStream reversedIntRange(int from, int to) {
		return IntStream.range(from, to).map((i) -> from + (to - i - 1));
	}
	
	private final WebTab createTab(Message message) {
		return new WebTab(message);
	}
	
	/** @since 00.02.05 */
	private final void selectTab(int index) {
		tabPane.getSelectionModel().clearAndSelect(index);
	}
	
	private final void closeTab(WebTab tab) {
		tabPane.getTabs().remove(tab);
		// Check if any tabs are still present, if not, close the window
		if(tabPane.getTabs().isEmpty())
			close();
	}
	
	private final void loadMessages(List<Message> messages) {
		if(messages.isEmpty()) {
			// Nothing to show, close in the next iteration
			FXUtils.thread(this::close);
			return; // Do not continue
		}
		FXUtils.thread(() -> {
			tabPane.getTabs().setAll(messages.stream().map(this::createTab).toArray(Tab[]::new));
		});
	}
	
	private final class WebTab extends Tab {
		
		private final Message message;
		private final BorderPane pane;
		private final HBox boxBottom;
		private final Button btnShowInBrowser;
		private final Button btnMarkAsRead;
		private WebView view;
		
		public WebTab(Message message) {
			this.message = message;
			pane = new BorderPane();
			boxBottom = new HBox(5.0);
			btnShowInBrowser = new Button(translation.getSingle("buttons.show_in_browser"));
			btnMarkAsRead = new Button(translation.getSingle("buttons.mark_as_read"));
			Pane fill = new Pane();
			HBox.setHgrow(fill, Priority.ALWAYS);
			boxBottom.getChildren().addAll(fill, btnShowInBrowser, btnMarkAsRead);
			boxBottom.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
			pane.setBottom(boxBottom);
			btnShowInBrowser.setOnAction((e) -> showInBrowser());
			btnMarkAsRead.setOnAction((e) -> markAsRead());
			setContent(pane);
			setClosable(false);
			selectedProperty().addListener((o, ov, nv) -> {
				if(nv && view == null) {
					view = new WebView();
					view.getEngine().load(message.uri().toString());
					view.getEngine().titleProperty().addListener((o0, ov0, title) -> setText(title));
					pane.setCenter(view);
				}
			});
		}
		
		public void showInBrowser() {
			Utils.ignore(() -> OS.current().browse(message.uri()), MediaDownloader::error);
		}
		
		public void markAsRead() {
			MessageList local = Utils.ignore(() -> MessageManager.local(), MessageManager.empty());
			local.add(message);
			Utils.ignore(() -> MessageManager.updateLocal(), MediaDownloader::error);
			close();
		}
		
		public void close() {
			FXUtils.thread(() -> closeTab(this));
		}
	}
}