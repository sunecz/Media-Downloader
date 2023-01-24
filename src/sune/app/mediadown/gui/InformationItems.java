package sune.app.mediadown.gui;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.download.Downloader;
import sune.app.mediadown.download.Downloaders;
import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.engine.MediaEngines;
import sune.app.mediadown.gui.window.InformationWindow.InformationTab;
import sune.app.mediadown.gui.window.InformationWindow.Viewable;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.plugin.Plugin;
import sune.app.mediadown.plugin.PluginFile;
import sune.app.mediadown.plugin.PluginUpdater;
import sune.app.mediadown.plugin.Plugins;
import sune.app.mediadown.search.SearchEngine;
import sune.app.mediadown.search.SearchEngines;
import sune.app.mediadown.server.Server;
import sune.app.mediadown.server.Servers;
import sune.app.mediadown.update.Version;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Worker;

public final class InformationItems {
	
	// Forbid anyone to create an instance of this class
	private InformationItems() {
	}
	
	/** @since 00.02.08 */
	private static final <I, T> List<T> createItemsList(Collection<I> collection, Function<I, T> func) {
		return collection.stream().map(func).collect(Collectors.toList());
	}
	
	/** @since 00.01.26 */
	private static final class InfoPaneEntry {
		
		private final String title;
		private final Supplier<Node> value;
		private final Insets margin;
		
		public InfoPaneEntry(String title, Supplier<Node> value) {
			this(title, value, null);
		}
		
		public InfoPaneEntry(String title, Supplier<Node> value, Insets margin) {
			this.title = title;
			this.value = value;
			this.margin = margin;
		}
		
		public static final InfoPaneEntry label(String title, String text) {
			return text != null && !text.isEmpty()
						? new InfoPaneEntry(title, (() -> {
							Label label = new Label(text);
							label.setWrapText(true);
							return label;
						}))
						: null;
		}
		
		public static final InfoPaneEntry hyperlink(String title, String text, String url) {
			return text != null && !text.isEmpty() && url != null
						? new InfoPaneEntry(title, (() -> {
							Hyperlink link = new Hyperlink(text);
							link.setStyle("-fx-padding: 0");
							link.setOnAction((e) -> Utils.visitURL(url));
							return link;
						}))
						: null;
		}
		
		public static final InfoPaneEntry image(String title, Image image, Insets margin) {
			return image != null ? new InfoPaneEntry(title, (() -> new ImageView(image)), margin) : null;
		}
		
		public void addRow(GridPane pane, int row) {
			if((title == null)) {
				Node nodeValue = value.get();
				pane.getChildren().add(nodeValue);
				GridPane.setConstraints(nodeValue, 0, row, 2, 1);
				if((margin != null))
					GridPane.setMargin(nodeValue, margin);
			} else {
				Label lblTitle = new Label(title);
				Node nodeValue = value.get();
				lblTitle.setStyle("-fx-font-weight: bold");
				pane.getChildren().addAll(lblTitle, nodeValue);
				GridPane.setValignment(lblTitle, VPos.TOP);
				GridPane.setValignment(nodeValue, VPos.TOP);
				GridPane.setConstraints(lblTitle, 0, row);
				GridPane.setConstraints(nodeValue, 1, row);
			}
		}
	}
	
	/** @since 00.01.26 */
	private static final class InfoPane extends GridPane {
		
		private int row;
		
		public InfoPane() {
			setVgap(5);
			setHgap(10);
			setPrefWidth(200.0);
		}
		
		public void addEntry(InfoPaneEntry entry) {
			if((entry != null))
				entry.addRow(this, row++);
		}
		
		public void add(Pane pane) {
			getChildren().add(pane);
			GridPane.setConstraints(pane, 0, row++, 2, 1);
		}
	}
	
	public static final class ItemMediaEngine implements Viewable {
		
		private final MediaEngine engine;
		
		public ItemMediaEngine(MediaEngine engine) {
			this.engine = engine;
		}
		
		public static final List<ItemMediaEngine> items() {
			return createItemsList(MediaEngines.all(), ItemMediaEngine::new);
		}
		
		@Override
		public Pane infoPane(InformationTab<?> tab) {
			Translation translation = tab.content().translation();
			InfoPane pane = new InfoPane();
			pane.addEntry(InfoPaneEntry.image(null, engine.icon(), new Insets(0, 0, 10, 0)));
			pane.addEntry(InfoPaneEntry.hyperlink(translation.getSingle("labels.url"), engine.url(), engine.url()));
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.title"), engine.title()));
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.version"), engine.version()));
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.author"), engine.author()));
			return pane;
		}
		
		@Override
		public String toString() {
			return engine.toString();
		}
	}
	
	public static final class ItemDownloader implements Viewable {
		
		private final Downloader downloader;
		
		public ItemDownloader(Downloader downloader) {
			this.downloader = downloader;
		}
		
		public static final List<ItemDownloader> items() {
			return createItemsList(Downloaders.all(), ItemDownloader::new);
		}
		
		@Override
		public Pane infoPane(InformationTab<?> tab) {
			Translation translation = tab.content().translation();
			InfoPane pane = new InfoPane();
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.title"), downloader.title()));
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.version"), downloader.version()));
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.author"), downloader.author()));
			return pane;
		}
		
		@Override
		public String toString() {
			return downloader.toString();
		}
	}
	
	public static final class ItemServer implements Viewable {
		
		private final Server server;
		
		public ItemServer(Server server) {
			this.server = server;
		}
		
		public static final List<ItemServer> items() {
			return createItemsList(Servers.all(), ItemServer::new);
		}
		
		@Override
		public Pane infoPane(InformationTab<?> tab) {
			Translation translation = tab.content().translation();
			InfoPane pane = new InfoPane();
			pane.addEntry(InfoPaneEntry.image(null, server.icon(), new Insets(0, 0, 10, 0)));
			pane.addEntry(InfoPaneEntry.hyperlink(translation.getSingle("labels.url"), server.url(), server.url()));
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.title"), server.title()));
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.version"), server.version()));
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.author"), server.author()));
			return pane;
		}
		
		@Override
		public String toString() {
			return server.toString();
		}
	}
	
	/** @since 00.01.17 */
	public static final class ItemSearchEngine implements Viewable {
		
		private final SearchEngine engine;
		
		public ItemSearchEngine(SearchEngine engine) {
			this.engine = engine;
		}
		
		public static final List<ItemSearchEngine> items() {
			return createItemsList(SearchEngines.all(), ItemSearchEngine::new);
		}
		
		@Override
		public Pane infoPane(InformationTab<?> tab) {
			Translation translation = tab.content().translation();
			InfoPane pane = new InfoPane();
			pane.addEntry(InfoPaneEntry.image(null, engine.getIcon(), new Insets(0, 0, 10, 0)));
			pane.addEntry(InfoPaneEntry.hyperlink(translation.getSingle("labels.url"), engine.getURL(), engine.getURL()));
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.title"), engine.getTitle()));
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.version"), engine.getVersion()));
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.author"), engine.getAuthor()));
			return pane;
		}
		
		@Override
		public String toString() {
			return engine.toString();
		}
	}
	
	public static final class ItemPlugin implements Viewable {
		
		// Just one Worker instance for all plugin items
		private static final Worker worker = Worker.createWorker(1);
		
		private final PluginFile plugin;
		
		public ItemPlugin(PluginFile plugin) {
			this.plugin = plugin;
		}
		
		public static final List<ItemPlugin> items() {
			return createItemsList(Plugins.allLoaded(), ItemPlugin::new);
		}
		
		private static final String getPluginName(Plugin plugin) {
			return plugin.name();
		}
		
		private static final String getPluginTitle(Plugin plugin) {
			return MediaDownloader.translation().getSingle(plugin.title());
		}
		
		private final Pane getUpdatePane(Translation translation) {
			HBox pane = new HBox(5);
			Label lblInfo = new Label(translation.getSingle("checking_new_version"));
			worker.submit(() -> {
				Version newVersion;
				String text = (newVersion = PluginUpdater.checkVersion(plugin)) != null
						? translation.getSingle("new_version_available", "version", PluginUpdater.pluginVersionString(newVersion))
						: translation.getSingle("no_new_version_available");
				FXUtils.thread(() -> lblInfo.setText(text));
			});
			pane.getChildren().add(lblInfo);
			pane.setAlignment(Pos.CENTER_LEFT);
			return pane;
		}
		
		public PluginFile getPlugin() {
			return plugin;
		}
		
		@Override
		public Pane infoPane(InformationTab<?> tab) {
			Translation translation = tab.content().translation();
			Plugin thePlugin = plugin.getPlugin().instance();
			InfoPane pane = new InfoPane();
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.name"), getPluginName(thePlugin)));
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.title"), getPluginTitle(thePlugin)));
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.version"), thePlugin.version()));
			pane.addEntry(InfoPaneEntry.label(translation.getSingle("labels.author"), thePlugin.author()));
			pane.add(getUpdatePane(translation.getTranslation("labels.update")));
			return pane;
		}
		
		@Override
		public String toString() {
			return getPluginTitle(plugin.getPlugin().instance());
		}
	}
}