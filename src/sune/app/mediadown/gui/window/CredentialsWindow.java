package sune.app.mediadown.gui.window;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.authentication.Credentials;
import sune.app.mediadown.authentication.CredentialsManager;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.gui.GUI.CredentialsRegistry;
import sune.app.mediadown.gui.GUI.CredentialsRegistry.CredentialsEntry;
import sune.app.mediadown.gui.control.IconTableCell;
import sune.app.mediadown.gui.util.FXUtils;

/** @since 00.02.09 */
public class CredentialsWindow extends DraggableWindow<VBox> {
	
	public static final String NAME = "credentials";
	
	private final TableView<CredentialsEntryItem> table;
	private final HBox boxButtons;
	private final Button btnEdit;
	
	public CredentialsWindow() {
		super(NAME, new VBox(5.0), 400.0, 350.0);
		initModality(Modality.APPLICATION_MODAL);
		
		table = new TableView<>();
		TableColumn<CredentialsEntryItem, String> columnIcon = columnIcon("");
		TableColumn<CredentialsEntryItem, String> columnTitle = columnTitle(tr("table.column.title"));
		table.getColumns().add(columnIcon);
		table.getColumns().add(columnTitle);
		columnTitle.setSortType(SortType.ASCENDING);
		table.setRowFactory(this::createTableRow);
		table.getSortOrder().add(columnTitle);
		table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		table.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> itemSelected(nv));
		table.setMaxWidth(Double.MAX_VALUE);
		table.setPlaceholder(new Label(tr("table.placeholder")));
		
		boxButtons = new HBox(5.0);
		btnEdit = new Button(tr("button.edit"));
		btnEdit.setOnAction((e) -> editSelectedCredentials());
		HBox fill = new HBox();
		HBox.setHgrow(fill, Priority.ALWAYS);
		boxButtons.getChildren().addAll(fill, btnEdit);
		btnEdit.setDisable(true);
		
		content.getChildren().addAll(table, boxButtons);
		content.setPadding(new Insets(10.0));
		
		FXUtils.onWindowShow(this, () -> {
			// Center the window
			Stage parent = (Stage) args.get("parent");
			
			if(parent != null) {
				centerWindow(parent);
			}
			
			loadItems();
		});
	}
	
	private final void loadItems() {
		table.getItems().addAll(
			CredentialsRegistry.entries().stream()
				.map(CredentialsEntryItem::new)
				.collect(Collectors.toList())
		);
		table.sort();
	}
	
	private final void itemSelected(CredentialsEntryItem item) {
		btnEdit.setDisable(item == null);
	}
	
	private final <T> TableRow<T> createTableRow(TableView<T> table) {
		TableRow<T> row = new TableRow<>();
		row.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> {
			if(e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) { // Double-click
				editSelectedCredentials();
			}
		});
		return row;
	}
	
	private final void editSelectedCredentials() {
		CredentialsEntryItem item = table.getSelectionModel().getSelectedItem();
		
		if(item == null) {
			return;
		}
		
		CredentialsEditDialogWindow window = MediaDownloader.window(CredentialsEditDialogWindow.NAME);
		Credentials credentials = window.showAndGet(this, item.entry());
		
		if(credentials == null) {
			return; // Cancelled
		}
		
		try {
			CredentialsManager cm = CredentialsManager.instance();
			cm.set(item.entry().name(), credentials);
		} catch(IOException ex) {
			MediaDownloader.error(ex);
		}
	}
	
	private static final TableColumn<CredentialsEntryItem, String> columnIcon(String title) {
		TableColumn<CredentialsEntryItem, String> column = new TableColumn<>(title);
		column.setCellValueFactory(new PropertyValueFactory<>("icon"));
		column.setCellFactory((col) -> new CredentialsEntryIconTableCell());
		column.setPrefWidth(24.0);
		column.setResizable(false);
		column.setReorderable(false);
		column.setSortable(false);
		return column;
	}
	
	private static final TableColumn<CredentialsEntryItem, String> columnTitle(String title) {
		TableColumn<CredentialsEntryItem, String> column = new TableColumn<>(title);
		column.setCellValueFactory(new PropertyValueFactory<>("title"));
		column.setPrefWidth(280.0);
		column.setReorderable(false);
		return column;
	}
	
	private static final class CredentialsEntryIconTableCell extends IconTableCell<CredentialsEntryItem, String> {
		
		@Override
		protected ImageView iconView(String value) {
			Image image = getTableRow().getItem().icon();
			
			if(image == null) {
				return null;
			}
			
			ImageView view = new ImageView(image);
			view.setFitWidth(24.0);
			view.setFitHeight(24.0);
			return view;
		}
	}
	
	protected static final class CredentialsEntryItem {
		
		private final CredentialsEntry entry;
		
		private StringProperty iconProperty;
		private StringProperty titleProperty;
		
		public CredentialsEntryItem(CredentialsEntry entry) {
			this.entry = Objects.requireNonNull(entry);
		}
		
		public StringProperty iconProperty() {
			return iconProperty == null
						? iconProperty = new SimpleStringProperty(name())
						: iconProperty;
		}
		
		public StringProperty titleProperty() {
			return titleProperty == null
						? titleProperty = new SimpleStringProperty(title())
						: titleProperty;
		}
		
		public CredentialsEntry entry() { return entry; }
		public String name() { return entry.name(); }
		public String title() { return entry.title(); }
		public Image icon() { return entry.icon(); }
	}
}