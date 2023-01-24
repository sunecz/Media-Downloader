package sune.app.mediadown.gui.window;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

public class InformationWindow extends DraggableWindow<StackPane> {
	
	public static final String NAME = "information";
	
	private TabPane tabPane;
	
	public InformationWindow() {
		super(NAME, new StackPane(), 600.0, 500.0);
		initModality(Modality.APPLICATION_MODAL);
		
		tabPane = new TabPane();
		tabPane.setSide(Side.LEFT);
		tabPane.setSkin(Ignore.call(() -> new HorizontalLeftTabPaneSkin(tabPane)));
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabPane.getSelectionModel().selectedItemProperty().addListener((o, ov, tab) -> {
			if(tab == null) {
				return;
			}
			
			Utils.<InformationTab<?>>cast(tab).content().doInit(this);
		});
		
		content.getChildren().add(tabPane);
		
		FXUtils.onWindowShow(this, () -> {
			// Add the tab panes before centering the window
			List<TabContent<?>> tabs = Utils.cast(args.get("tabs"));
			tabPane.getTabs().setAll(createTabs(tabs));
			
			// Center the window
			Stage parent = (Stage) args.get("parent");
			if(parent != null) {
				centerWindow(parent);
			}
		});
	}
	
	/** @since 00.02.08 */
	@SuppressWarnings("unchecked")
	private final List<Tab> createTabs(List<TabContent<?>> contents) {
		return contents.stream().map(InformationTab::new).collect(Collectors.toList());
	}
	
	/** @since 00.02.08 */
	public <T extends Viewable> InformationTab<T> selectedTab() {
		return Utils.cast(tabPane.getSelectionModel().getSelectedItem());
	}
	
	private static final class TabContentPane<T extends Viewable> extends GridPane {
		
		private final InformationTab<T> tab;
		private ListView<T> list;
		private StackPane paneTop;
		private StackPane paneBottom;
		private Label lblPlaceholder;
		
		public TabContentPane(InformationTab<T> tab, List<T> items) {
			this.tab = tab;
			list = new ListView<>();
			paneTop = new StackPane();
			paneBottom = new StackPane();
			
			InformationWindow window = MediaDownloader.window(InformationWindow.NAME);
			Translation tr = window.getTranslation();
			lblPlaceholder = new Label(tr.getSingle("labels.no_item_selected"));
			
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
			if(newItem != null) {
				Pane paneInfo = newItem.infoPane(tab);
				paneBottom.getChildren().setAll(paneInfo);
			} else {
				emptyItems();
			}
		}
		
		/** @since 00.02.08 */
		public final ListView<T> list() {
			return list;
		}
	}
	
	public static interface Viewable {
		
		/** @since 00.02.08 */
		Pane infoPane(InformationTab<?> tab);
	}
	
	public static class TabContent<T extends Viewable> {
		
		private final String title;
		private final List<T> items;
		private final Translation translation;
		
		private Consumer<InformationWindow> actionOnInit;
		private boolean isInited;
		
		public TabContent(Translation translation, List<T> items) {
			this.title = translation.getSingle("title");
			this.items = items;
			this.translation = translation;
		}
		
		public void setOnInit(Consumer<InformationWindow> action) {
			this.actionOnInit = action;
		}
		
		public void doInit(InformationWindow window) {
			if(actionOnInit == null || isInited) {
				return;
			}
			
			actionOnInit.accept(window);
			isInited = true;
		}
		
		/** @since 00.02.08 */
		public String title() {
			return title;
		}
		
		/** @since 00.02.08 */
		public List<T> items() {
			return items;
		}
		
		/** @since 00.02.08 */
		public Translation translation() {
			return translation;
		}
	}
	
	public static class InformationTab<T extends Viewable> extends Tab {
		
		private final TabContent<T> content;
		
		public InformationTab(TabContent<T> content) {
			super(content.title());
			this.content = content;
			setContent(new TabContentPane<>(this, content.items()));
		}
		
		/** @since 00.02.08 */
		public ListView<T> list() {
			return Utils.<TabContentPane<T>>cast(getContent()).list();
		}
		
		/** @since 00.02.08 */
		public TabContent<T> content() {
			return content;
		}
	}
}