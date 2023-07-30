package sune.app.mediadown.gui.window;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.media.AudioMediaBase;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaContainer;
import sune.app.mediadown.media.MediaQuality;
import sune.app.mediadown.media.MediaType;
import sune.app.mediadown.media.SubtitlesMedia;
import sune.app.mediadown.media.VideoMediaBase;
import sune.app.mediadown.util.ClipboardUtils;
import sune.app.mediadown.util.FXUtils;

/** @since 00.02.05 */
public class MediaInfoWindow extends DraggableWindow<StackPane> {
	
	public static final String NAME = "media_info";
	
	private static final Map<MediaType, MediaInfoLoader> loaders = new LinkedHashMap<>();
	
	static {
		registerLoader(MediaType.VIDEO, new VideoMediaInfoLoader());
		registerLoader(MediaType.AUDIO, new AudioMediaInfoLoader());
		registerLoader(MediaType.SUBTITLES, new SubtitlesMediaInfoLoader());
	}
	
	private TreeTableView<MediaInfoRow> view;
	
	public MediaInfoWindow() {
		super(NAME, new StackPane(), 600.0, 500.0);
		initModality(Modality.APPLICATION_MODAL);
		FXUtils.onWindowShow(this, () -> {
			Media media = (Media) args.get("media");
			if(media != null) loadMedia(media);
			// Center the window
			Stage parent = (Stage) args.get("parent");
			if((parent != null)) {
				centerWindow(parent);
			}
		});
	}
	
	private static final MediaInfoLoader defaultMediaInfoLoader() {
		return new BasicMediaInfoLoader();
	}
	
	public static final void registerLoader(MediaType mediaType, MediaInfoLoader loader) {
		loaders.put(Objects.requireNonNull(mediaType), Objects.requireNonNull(loader));
	}
	
	public static final void unregisterLoader(MediaType mediaType) {
		loaders.remove(Objects.requireNonNull(mediaType));
	}
	
	private static final String rowToString(MediaInfoRow row, int titleMaxSize, String indent) {
		return row.type() == MediaInfoRow.RowType.CHILD
					? (titleMaxSize <= 0
							? String.format("%s%s: %s", indent, row.title(), row.value())
							: String.format("%s%-" + titleMaxSize + "s: %s", indent, row.title(), row.value()))
					: (titleMaxSize <= 0
							? String.format("%s%s", indent, row.title())
							: String.format("%s%-" + titleMaxSize + "s", indent, row.title()));
	}
	
	private static final String rowToStringOnlyName(MediaInfoRow row, String indent) {
		return String.format("%s%s", indent, row.title());
	}
	
	private static final String rowToStringOnlyValue(MediaInfoRow row, String indent) {
		return String.format("%s%s", indent, row.value());
	}
	
	private static final String itemToString(TreeItem<MediaInfoRow> item, int titleMaxSize, String indent) {
		StringBuilder builder = new StringBuilder();
		MediaInfoRow row = item.getValue();
		builder.append(rowToString(row, titleMaxSize, indent));
		if(row.type() == MediaInfoRow.RowType.PARENT) {
			builder.append(": [\n");
			String childIndent = "    " + indent;
			int childrenTitleMaxSize = item.getChildren().stream()
					.map((i) -> i.getValue().title().length())
					.max(Comparator.naturalOrder())
					.orElse(0);
			item.getChildren().stream()
				.map((i) -> itemToString(i, childrenTitleMaxSize, childIndent))
				.forEach(builder::append);
			builder.append(indent).append("]");
		}
		builder.append("\n");
		return builder.toString();
	}
	
	private static final String itemToStringFunction(TreeItem<MediaInfoRow> item, String indent,
			BiFunction<MediaInfoRow, String, String> function) {
		StringBuilder builder = new StringBuilder();
		MediaInfoRow row = item.getValue();
		builder.append(function.apply(row, indent));
		if(row.type() == MediaInfoRow.RowType.PARENT) {
			builder.append("\n");
			String childIndent = "    " + indent;
			item.getChildren().stream()
				.map((i) -> itemToStringFunction(i, childIndent, function))
				.forEach(builder::append);
			builder.append(indent);
		}
		builder.append("\n");
		return builder.toString();
	}
	
	private final ContextMenu newContextMenu() {
		ContextMenu menu = new ContextMenu();
		
		MenuItem copyDataText = new MenuItem(translation.getSingle("context_menu.copy_data_text"));
		copyDataText.setOnAction((e) -> {
			StringBuilder builder = new StringBuilder();
			List<TreeItem<MediaInfoRow>> items = view.getSelectionModel().getSelectedItems();
			boolean hasAnyParentItems = items.stream()
					.filter((i) -> i.getValue().type() == MediaInfoRow.RowType.PARENT)
					.findAny().isPresent();
			
			if(!hasAnyParentItems) {
				int titleMaxSize = items.stream()
						.map((i) -> i.getValue().title().length())
						.max(Comparator.naturalOrder())
						.orElse(0);
				items.stream()
					.map((i) -> itemToString(i, titleMaxSize, ""))
					.forEach(builder::append);
			} else {
				items.stream()
					.map((i) -> itemToString(i, 0, "") + "\n")
					.forEach(builder::append);
			}
			
			ClipboardUtils.copy(builder.toString());
		});
		
		MenuItem copyDataTextOnlyName = new MenuItem(translation.getSingle("context_menu.copy_data_text_only_name"));
		copyDataTextOnlyName.setOnAction((e) -> {
			StringBuilder builder = new StringBuilder();
			List<TreeItem<MediaInfoRow>> items = view.getSelectionModel().getSelectedItems();
			items.stream()
				.map((i) -> itemToStringFunction(i, "", MediaInfoWindow::rowToStringOnlyName))
				.forEach(builder::append);
			ClipboardUtils.copy(builder.toString());
		});
		
		MenuItem copyDataTextOnlyValue = new MenuItem(translation.getSingle("context_menu.copy_data_text_only_value"));
		copyDataTextOnlyValue.setOnAction((e) -> {
			StringBuilder builder = new StringBuilder();
			List<TreeItem<MediaInfoRow>> items = view.getSelectionModel().getSelectedItems();
			items.stream()
				.map((i) -> itemToStringFunction(i, "", MediaInfoWindow::rowToStringOnlyValue))
				.forEach(builder::append);
			ClipboardUtils.copy(builder.toString());
		});
		
		menu.getItems().addAll(copyDataText, copyDataTextOnlyName, copyDataTextOnlyValue);
		
		return menu;
	}
	
	private final void loadMedia(Media media) {
		TreeItem<MediaInfoRow> root = loadMediaInformation(media);
		view = new TreeTableView<>(root);
		String titleTitle = translation.getSingle("column.title");
		String titleValue = translation.getSingle("column.value");
		TreeTableColumn<MediaInfoRow, String> columnTitle = new TreeTableColumn<>(titleTitle);
		TreeTableColumn<MediaInfoRow, String> columnValue = new TreeTableColumn<>(titleValue);
		columnTitle.setCellValueFactory((r) -> new SimpleStringProperty(r.getValue().getValue().title()));
		columnValue.setCellValueFactory((r) -> new SimpleStringProperty(r.getValue().getValue().value()));
		columnTitle.setPrefWidth(100);
		columnValue.setPrefWidth(350);
		columnTitle.setSortable(false);
		columnValue.setSortable(false);
		columnTitle.setReorderable(false);
		columnValue.setReorderable(false);
		view.getColumns().add(columnTitle);
		view.getColumns().add(columnValue);
		FXUtils.oncePostLayout(scene, () -> FXUtils.Table.resizeColumnToFitContentAllRows(columnTitle));
		view.setPlaceholder(new Label(translation.getSingle("no_items")));
		view.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		view.setContextMenu(newContextMenu());
		content.getChildren().add(view);
	}
	
	private final TreeItem<MediaInfoRow> loadMediaInformation(Media media) {
		TreeItem<MediaInfoRow> item = Optional.ofNullable(loaders.get(media.type()))
				.orElseGet(MediaInfoWindow::defaultMediaInfoLoader)
				.load(media);
		
		if(media.isContainer()) {
			MediaContainer container = (MediaContainer) media;
			item.getChildren().addAll(container.media().stream()
			                                .map(this::loadMediaInformation)
			                                .collect(Collectors.toList()));
		}
		
		item.setExpanded(true);
		return item;
	}
	
	public static final class MediaInfoRow {
		
		private final RowType type;
		private final String title;
		private final String value;
		
		private MediaInfoRow(RowType type, String title, String value) {
			this.type = Objects.requireNonNull(type);
			this.title = title;
			this.value = value;
		}
		
		public static final MediaInfoRow ofParent(String title) {
			return new MediaInfoRow(RowType.PARENT, Objects.requireNonNull(title), null);
		}
		
		public static final MediaInfoRow ofChild(String title, String value) {
			return new MediaInfoRow(RowType.CHILD, Objects.requireNonNull(title), Objects.requireNonNull(value));
		}
		
		public RowType type() {
			return type;
		}
		
		public String title() {
			return title;
		}
		
		public String value() {
			return value;
		}
		
		public static enum RowType {
			PARENT, CHILD;
		}
	}
	
	public static interface MediaInfoLoader {
		
		TreeItem<MediaInfoRow> load(Media media);
	}
	
	private static class BasicMediaInfoLoader implements MediaInfoLoader {
		
		protected <T> TreeItem<MediaInfoRow> newParent(String title) {
			return new TreeItem<>(MediaInfoRow.ofParent(title));
		}
		
		protected <T> TreeItem<MediaInfoRow> newChild(String title, T value) {
			return new TreeItem<>(MediaInfoRow.ofChild(title, Objects.toString(value)));
		}
		
		/** @since 00.02.09 */
		protected <T> TreeItem<MediaInfoRow> newChildOfList(String title, List<T> value) {
			if(value.isEmpty()) {
				return newChild(title, "EMPTY");
			}
			
			TreeItem<MediaInfoRow> parent = newParent(title);
			
			for(int i = 0, l = value.size(); i < l; ++i) {
				addChildren(parent, newChild(String.valueOf(i), value.get(i)));
			}
			
			return parent;
		}
		
		/** @since 00.02.09 */
		@SuppressWarnings("unchecked")
		protected <T> TreeItem<MediaInfoRow> newChildOfAny(String title, T value) {
			if(value instanceof List) {
				return newChildOfList(title, (List<T>) value);
			}
			
			return newChild(title, value);
		}
		
		protected final String rootName(Media media) {
			return String.format("%s::%s", media.isContainer() ? "MediaContainer" : "Media", media.type().name());
		}
		
		@SafeVarargs
		protected final void addChildren(TreeItem<MediaInfoRow> parent, TreeItem<MediaInfoRow>... children) {
			parent.getChildren().addAll(children);
		}
		
		protected final void addChildren(TreeItem<MediaInfoRow> parent, List<TreeItem<MediaInfoRow>> children) {
			parent.getChildren().addAll(children);
		}
		
		protected void loadBeforeMetadata(Media media, TreeItem<MediaInfoRow> root) {
			// Do nothing by default
		}
		
		/** @since 00.02.09 */
		protected String textOfQuality(MediaQuality quality) {
			return MediaQuality.removeNameSuffix(quality.name());
		}
		
		/** @since 00.02.09 */
		protected String textOfSize(long size) {
			return size < 0L ? "UNKNOWN" : String.valueOf(size);
		}
		
		@Override
		public TreeItem<MediaInfoRow> load(Media media) {
			TreeItem<MediaInfoRow> root = newParent(rootName(media));
			addChildren(root,
				newChild("source", media.source()),
				newChild("uri", media.uri()),
				newChild("type", media.type()),
				newChild("format", media.format()),
				newChild("quality", textOfQuality(media.quality())),
				newChild("size", textOfSize(media.size())),
				newChild("isContainer", media.isContainer()),
				newChild("isSingle", media.isSingle()),
				newChild("isSegmented", media.isSegmented()),
				newChild("isSolid", media.isSolid())
			);
			
			loadBeforeMetadata(media, root);
			
			TreeItem<MediaInfoRow> metadataRoot = newParent("metadata");
			addChildren(
				metadataRoot,
				media.metadata().data().entrySet().stream()
					.map((e) -> newChildOfAny(e.getKey(), e.getValue()))
					.collect(Collectors.toList())
			);
			addChildren(root, metadataRoot);
			
			return root;
		}
	}
	
	private static final class VideoMediaInfoLoader extends BasicMediaInfoLoader {
		
		@Override
		protected void loadBeforeMetadata(Media media, TreeItem<MediaInfoRow> root) {
			VideoMediaBase video = (VideoMediaBase) media;
			addChildren(root,
				newChild("resolution", video.resolution()),
				newChild("duration", video.duration()),
				newChildOfList("codecs", video.codecs()),
				newChild("bandwidth", video.bandwidth()),
				newChild("frameRate", video.frameRate())
			);
		}
	}
	
	private static final class AudioMediaInfoLoader extends BasicMediaInfoLoader {
		
		@Override
		protected void loadBeforeMetadata(Media media, TreeItem<MediaInfoRow> root) {
			AudioMediaBase audio = (AudioMediaBase) media;
			addChildren(root,
				newChild("language", audio.language()),
				newChild("duration", audio.duration()),
				newChildOfList("codecs", audio.codecs()),
				newChild("bandwidth", audio.bandwidth()),
				newChild("sampleRate", audio.sampleRate())
			);
		}
	}
	
	private static final class SubtitlesMediaInfoLoader extends BasicMediaInfoLoader {
		
		@Override
		protected void loadBeforeMetadata(Media media, TreeItem<MediaInfoRow> root) {
			SubtitlesMedia subtitles = (SubtitlesMedia) media;
			addChildren(root,
				newChild("language", subtitles.language())
			);
		}
	}
}