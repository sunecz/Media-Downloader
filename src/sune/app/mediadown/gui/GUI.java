package sune.app.mediadown.gui;

import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.gui.window.ReportWindow;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.report.Report;
import sune.app.mediadown.report.Reporting;
import sune.app.mediadown.report.Reporting.ReportStatus;

/** @since 00.02.09 */
public final class GUI {
	
	// Forbid anyone to create an instance of this class
	private GUI() {
	}
	
	public static final void showReportWindow(Stage parent, Report.Builder builder,
			ReportWindow.Feature... features) {
		((ReportWindow) MediaDownloader.window(ReportWindow.NAME)).showWithFeatures(parent, builder, features);
	}
	
	public static final boolean report(Report report, boolean anonymize) {
		try {
			ReportStatus status = Reporting.send(report, anonymize);
			boolean success = status.isSuccess();
			Translation trr = MediaDownloader.translation().getTranslation("dialogs.report");
			
			if(success) {
				Translation tr = trr.getTranslation("success");
				Dialog.showInfo(tr.getSingle("title"), tr.getSingle("text"));
			} else {
				Translation tr = trr.getTranslation("error");
				String content = status.message();
				Dialog.showContentError(tr.getSingle("title"), tr.getSingle("text"), content);
			}
			
			return success;
		} catch(Exception ex) {
			MediaDownloader.error(ex);
		}
		
		return false;
	}
}