package sune.app.mediadown.gui.window;

import java.util.Arrays;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.HorizontalLeftTabPaneSkin;
import sune.app.mediadown.util.Utils.Ignore;

public class InformationWindow extends DraggableWindow<StackPane> {
	
	public static final String NAME = "information";
	
	public static interface Viewable {
		
		Pane getInfoPane(InformationTab<?> tab);
	}
	
	public static class TabContent<T extends Viewable> {
		
		private final String title;
		private final T[] items;
		private final Translation translation;
		
		private Consumer<InformationWindow> actionOnInit;
		private boolean isInited;
		
		public TabContent(Translation translation, T[] items) {
			this.title = translation.getSingle("title");
			this.items = items;
			this.translation = translation;
		}
		
		public void setOnInit(Consumer<InformationWindow> action) {
			this.actionOnInit = action;
		}
		
		public void doInit(InformationWindow window) {
			if((actionOnInit != null && !isInited)) {
				actionOnInit.accept(window);
				isInited = true;
			}
		}
		
		public String getTitle() {
			return title;
		}
		
		public T[] getItems() {
			return items;
		}
		
		public Translation getTranslation() {
			return translation;
		}
	}
	
	public static class InformationTab<T extends Viewable> extends Tab {
		
		private final TabContent<T> content;
		
		public InformationTab(TabContent<T> content) {
			super(content.getTitle());
			this.content = content;
			setContent(new TabContentPane<>(this, content.getItems()));
		}
		
		@SuppressWarnings("unchecked")
		public ListView<T> getList() {
			return ((TabContentPane<T>) getContent()).getList();
		}
		
		public TabContent<T> getTabContent() {
			return content;
		}
	}
	
	private static final class TabContentPane<T extends Viewable> extends GridPane {
		
		private final InformationTab<T> tab;
		private ListView<T> list;
		private StackPane paneTop;
		private StackPane paneBottom;
		private Label lblPlaceholder;
		
		public TabContentPane(InformationTab<T> tab, T[] items) {
			this.tab   = tab;
			list       = new ListView<>();
			paneTop    = new StackPane();
			paneBottom = new StackPane();
			InformationWindow window = MediaDownloader.window(InformationWindow.NAME);
			Translation translation = window.getTranslation();
			lblPlaceholder = new Label(translation.getSingle("labels.no_item_selected"));
			list.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> selectItem(ov, nv));
			list.setMaxWidth(Double.MAX_VALUE);
			paneTop.getChildren().add(list);
			paneTop.setPadding(new Insets(10, 10, 10, 0));
			paneBottom.setPadding(new Insets(0, 10, 10, 0));
			paneBottom.setId("pane-info");
			getChildren().addAll(paneTop, paneBottom);
			GridPane.setConstraints(paneTop, 0, 0);
			GridPane.setConstraints(paneBottom, 0, 1);
			GridPane.setHgrow(paneTop, Priority.ALWAYS);
			GridPane.setHgrow(paneBottom, Priority.ALWAYS);
			GridPane.setVgrow(paneTop, Priority.ALWAYS);
			list.getItems().setAll(items);
			emptyItems();
		}
		
		private final void emptyItems() {
			paneBottom.getChildren().setAll(lblPlaceholder);
		}
		
		private final void selectItem(T oldItem, T newItem) {
			if((newItem != null)) {
				Pane paneInfo = newItem.getInfoPane(tab);
				paneBottom.getChildren().setAll(paneInfo);
			} else {
				emptyItems();
			}
		}
		
		public final ListView<T> getList() {
			return list;
		}
	}
	
	private TabPane tabPane;
	
	public InformationWindow() {
		super(NAME, new StackPane(), 600.0, 500.0);
		initModality(Modality.APPLICATION_MODAL);
		tabPane = new TabPane();
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabPane.getSelectionModel().selectedItemProperty().addListener((o, ov, tab) -> {
			if((tab == null)) return;
			((InformationTab<?>) tab).getTabContent().doInit(this);
		});
		tabPane.setSide(Side.LEFT);
		Ignore.callVoid(() -> tabPane.setSkin(new HorizontalLeftTabPaneSkin(tabPane)));
		content.getChildren().add(tabPane);
		FXUtils.onWindowShow(this, () -> {
			// Add the tab panes before centering the window
			TabContent<?>[] tabs = (TabContent[]) args.get("tabs");
			tabPane.getTabs().setAll(toTabs(tabs));
			// Center the window
			Stage parent = (Stage) args.get("parent");
			if((parent != null)) {
				centerWindow(parent);
			}
		});
	}
	
	private final Tab[] toTabs(TabContent<?>[] contents) {
		return Arrays.asList(contents)
					 .stream()
					 .map((content) -> new InformationTab<>(content))
					 .toArray(Tab[]::new);
	}
	
	public <T extends Viewable> InformationTab<T> getSelectedTab() {
		@SuppressWarnings("unchecked")
		InformationTab<T> tab = (InformationTab<T>) tabPane.getSelectionModel().getSelectedItem();
		return tab;
	}
}