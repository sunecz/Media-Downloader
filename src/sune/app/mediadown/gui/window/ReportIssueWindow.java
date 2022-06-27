package sune.app.mediadown.gui.window;

import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.util.FXUtils;

/** @since 00.02.02 */
public class ReportIssueWindow extends DraggableWindow<VBox> {
	
	// TODO: Create the Report issue window
	
	public static final String NAME = "report_issue";
	
	public ReportIssueWindow() {
		super(NAME, new VBox(5), 500.0, 160.0);
		initModality(Modality.APPLICATION_MODAL);

		FXUtils.onWindowShow(this, () -> {
			Stage parent = (Stage) args.get("parent");
			if((parent != null)) {
				centerWindow(parent);
			}
		});
	}
}