package sune.app.mediadown.gui.window;

import java.io.IOException;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.authentication.Credentials;
import sune.app.mediadown.authentication.CredentialsManager;
import sune.app.mediadown.gui.DialogWindow;
import sune.app.mediadown.gui.GUI.CredentialsRegistry;
import sune.app.mediadown.gui.GUI.CredentialsRegistry.CredentialsEntry;
import sune.app.mediadown.gui.GUI.CredentialsRegistry.CredentialsType;
import sune.app.mediadown.gui.util.FXUtils;

/** @since 00.02.09 */
public final class CredentialsEditDialogWindow extends DialogWindow<VBox, Credentials> {
	
	public static final String NAME = "credentials_edit";
	
	private CredentialsType<?> type;
	private Pane entryPane;
	
	private final HBox boxButtons;
	private final Button btnSave;
	private final Button btnCancel;
	
	public CredentialsEditDialogWindow() {
		super(NAME, new VBox(5.0), 350.0, 250.0);
		initModality(Modality.APPLICATION_MODAL);
		setCloseButtonVisible(false);
		
		boxButtons = new HBox(5.0);
		btnSave = new Button(tr("button.save"));
		btnCancel = new Button(tr("button.cancel"));
		
		btnSave.setOnAction((e) -> save());
		btnCancel.setOnAction((e) -> cancel());
		
		HBox fill = new HBox();
		HBox.setHgrow(fill, Priority.ALWAYS);
		boxButtons.getChildren().addAll(fill, btnSave, btnCancel);
		content.setPadding(new Insets(10.0));
		
		setOnCloseRequest((e) -> dispose());
		
		FXUtils.onWindowShow(this, () -> {
			setResult(null);
			updateData((CredentialsEntry) args.get("entry"));
			
			// Center the window
			Stage parent = (Stage) args.get("parent");
			
			if(parent != null) {
				centerWindow(parent);
			}
		});
	}
	
	private final void save() {
		setResult(type.save(entryPane));
		close();
	}
	
	private final void dispose() {
		type = null;
		entryPane = null;
	}
	
	private final void cancel() {
		close();
	}
	
	@SuppressWarnings("unchecked")
	private final <T extends Credentials> Pane initEntryPane(CredentialsManager cm, String name) throws IOException {
		T credentials = (T) cm.get(name);
		CredentialsType<T> type = (CredentialsType<T>) CredentialsRegistry.typeOf(credentials);
		
		if(type == null) {
			throw new IllegalStateException(
				"Unregistered credentials type '" + credentials.getClass().getName() + "'"
			);
		}
		
		this.type = type;
		Pane entryPane = type.create();
		entryPane.getStyleClass().add("credentials-pane");
		type.load(entryPane, credentials);
		return entryPane;
	}
	
	private final void updateData(CredentialsEntry entry) {
		Objects.requireNonNull(entry);
		String name = entry.name();
		setTitle(tr("title", "title", entry.title()));
		
		try {
			CredentialsManager cm = CredentialsManager.instance();
			entryPane = initEntryPane(cm, name);
			content.getChildren().setAll(entryPane, boxButtons);
			VBox.setVgrow(entryPane, Priority.ALWAYS);
		} catch(IOException ex) {
			MediaDownloader.error(ex);
		}
	}
	
	public final Credentials showAndGet(Stage parent, CredentialsEntry entry) {
		return setArgs("parent", parent, "entry", entry).showAndGet();
	}
}