package sune.app.mediadown.gui.form.field;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.app.mediadown.gui.form.FormFieldType;
import sune.app.mediadown.language.Translation;
import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDNode;
import sune.util.ssdf2.SSDObject;

/** @since 00.02.09 */
public class TableField<T> extends FormField<T> {
	
	private final VBox parent;
	private final TableView<TableItem> control;
	private final HBox paneButtons;
	private final Button btnAdd;
	private final Button btnRemove;
	
	public TableField(T property, String name, String title, boolean showNames) {
		super(property, FormFieldType.TABLE, name, title);
		parent = new VBox(5.0);
		
		control = new TableView<>();
		control.setMinSize(0.0, 0.0);
		control.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		control.setPrefSize(0.0, 150.0);
		control.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		control.setEditable(true);
		
		Translation tr = translation();
		
		if(showNames) {
			TableColumn<TableItem, String> columnName = new TableColumn<>("Name");
			columnName.setText(tr.getSingle("columns.name"));
			columnName.setCellValueFactory(new PropertyValueFactory<>("name"));
			columnName.setCellFactory(TextFieldTableCell.forTableColumn());
			columnName.setOnEditCommit((e) -> e.getRowValue().setName(e.getNewValue()));
			control.getColumns().add(columnName);
		}
		
		TableColumn<TableItem, String> columnValue = new TableColumn<>("Value");
		columnValue.setText(tr.getSingle("columns.value"));
		columnValue.setCellValueFactory(new PropertyValueFactory<>("value"));
		columnValue.setCellFactory(TextFieldTableCell.forTableColumn());
		columnValue.setOnEditCommit((e) -> e.getRowValue().setValue(e.getNewValue()));
		control.getColumns().add(columnValue);
		
		paneButtons = new HBox(5.0);
		btnAdd = new Button(tr.getSingle("buttons.add"));
		btnRemove = new Button(tr.getSingle("buttons.remove"));
		
		btnAdd.setOnAction((e) -> {
			List<TableItem> items = control.getItems();
			int max = items.stream()
				.mapToInt((o) -> {
					try { return Integer.parseInt(o.name()); }
					catch(NumberFormatException ex) { return -1; }
				})
				.max().orElse(-1);
			
			control.getItems().add(new TableItem(
				String.valueOf(max + 1),
				tr.getSingle("columns.value")
			));
		});
		
		btnRemove.setOnAction((e) -> {
			int index = control.getSelectionModel().getSelectedIndex();
			
			if(index < 0 || index >= control.getItems().size()) {
				return;
			}
			
			control.getItems().remove(index);
		});
		
		paneButtons.getChildren().addAll(btnAdd, btnRemove);
		paneButtons.setAlignment(Pos.CENTER_RIGHT);
		
		control.getStyleClass().add("column-count-" + control.getColumns().size());
		parent.getChildren().addAll(control, paneButtons);
	}
	
	@Override
	public Node render(Form form) {
		return parent;
	}
	
	@Override
	public void value(SSDNode node) {
		if(!node.isCollection()) {
			throw new IllegalStateException("Not a collection");
		}
		
		SSDCollection collection = (SSDCollection) node;
		List<SSDObject> objects = List.copyOf(collection.objects());
		List<TableItem> items;
		
		switch(collection.getType()) {
			case ARRAY:
				items = IntStream.range(0, objects.size())
					.mapToObj((i) -> new TableItem(String.valueOf(i), objects.get(i).getFormattedValue().stringValue()))
					.collect(Collectors.toList());
				break;
			case OBJECT:
				items = objects.stream()
					.map((o) -> new TableItem(o.getName(), o.getFormattedValue().stringValue()))
					.collect(Collectors.toList());
				break;
			default:
				throw new IllegalStateException("Unsupported collection type: " + collection.getType()); // Should not happen
		}
		
		control.getItems().setAll(items);
	}
	
	@Override
	public Object value() {
		return control.getItems().stream()
					.collect(Collectors.toMap(
						TableItem::name,
						TableItem::value,
						(a, b) -> a,
						LinkedHashMap::new
					));
	}
	
	public static final class TableItem {
		
		private String name;
		private String value;
		
		private TableItem(String name, String value) {
			this.name = name;
			this.value = value;
		}
		
		public String getName() { return name(); } // GUI
		public String getValue() { return value(); } // GUI
		public void setName(String name) { this.name = name; } // GUI
		public void setValue(String value) { this.value = value; } // GUI
		
		public String name() { return name; }
		public String value() { return value; }
	}
}
