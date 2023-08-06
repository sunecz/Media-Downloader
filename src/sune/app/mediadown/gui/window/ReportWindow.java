package sune.app.mediadown.gui.window;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.gui.GUI;
import sune.app.mediadown.report.ContactInformation;
import sune.app.mediadown.report.Report;
import sune.app.mediadown.report.Reporting;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.JSON.JSONCollection;

/** @since 00.02.09 */
public class ReportWindow extends DraggableWindow<VBox> {
	
	public static final String NAME = "report";
	
	private final Label lblEmail;
	private final TextField txtEmail;
	private final Label lblRawData;
	private final TextArea txtRawData;
	private final CheckBox chbAnonymizeData;
	private final Button btnSend;
	
	private volatile Report.Builder report;
	
	public ReportWindow() {
		super(NAME, new VBox(5.0), 700.0, 500.0);
		initModality(Modality.APPLICATION_MODAL);
		
		VBox boxContactInformation = new VBox(5.0);
		lblEmail = new Label(translation.getSingle("label.email"));
		txtEmail = new TextField();
		txtEmail.focusedProperty().addListener((o, ov, nv) -> updateData());
		txtEmail.textProperty().addListener((o, ov, nv) -> updateData());
		boxContactInformation.getChildren().addAll(lblEmail, txtEmail);
		
		VBox boxRawData = new VBox(5.0);
		lblRawData = new Label(translation.getSingle("label.raw_data"));
		txtRawData = new TextArea();
		txtRawData.setFont(Font.font("monospaced", txtRawData.getFont().getSize()));
		txtRawData.setEditable(false);
		boxContactInformation.getChildren().addAll(lblRawData, txtRawData);
		
		VBox boxAnonymizeData = new VBox(5.0);
		chbAnonymizeData = new CheckBox(translation.getSingle("label.anonymize_data"));
		chbAnonymizeData.setOnAction((e) -> updateData());
		boxAnonymizeData.getChildren().addAll(chbAnonymizeData);
		
		HBox boxBottom = new HBox(5.0);
		btnSend = new Button(translation.getSingle("button.send"));
		btnSend.setOnAction((e) -> sendData());
		
		HBox boxFill = new HBox();
		HBox.setHgrow(boxFill, Priority.ALWAYS);
		boxBottom.getChildren().addAll(boxFill, btnSend);
		content.getChildren().addAll(boxContactInformation, boxRawData, boxAnonymizeData, boxBottom);
		content.setPadding(new Insets(10));
		btnSend.setMinWidth(80);
		boxBottom.setAlignment(Pos.CENTER_LEFT);
		VBox.setVgrow(txtRawData, Priority.ALWAYS);
		VBox.setVgrow(boxContactInformation, Priority.ALWAYS);
		
		FXUtils.onWindowShow(this, () -> {
			Stage parent = (Stage) args.get("parent");
			
			if(parent != null) {
				centerWindow(parent);
			}
			
			report = (Report.Builder) args.get("report_builder");
			
			if(report != null) {
				loadData();
			}
		});
	}
	
	private final void updateContactInformation() {
		ContactInformation contact = null;
		String email = txtEmail.getText();
		
		if(!email.isBlank()) {
			contact = new ContactInformation(email);
		}
		
		report.contact(contact);
	}
	
	private final void updatePayload() {
		updateContactInformation();
		
		boolean anonymize = chbAnonymizeData.isSelected();
		JSONCollection payload = Reporting.payload(report.build(), anonymize);
		
		txtRawData.setText(payload.toString(false));
	}
	
	private final void loadData() {
		ContactInformation contact = report.contact();
		
		if(contact != null) {
			txtEmail.setText(contact.email());
		}
		
		boolean anonymize = chbAnonymizeData.isSelected();
		JSONCollection payload = Reporting.payload(report.build(), anonymize);
		
		txtRawData.setText(payload.toString(false));
	}
	
	private final void updateData() {
		updatePayload();
	}
	
	private final void clearData() {
		txtEmail.setText("");
		txtRawData.setText("");
		chbAnonymizeData.setSelected(false);
	}
	
	private final void sendData() {
		updateContactInformation();
		boolean anonymize = chbAnonymizeData.isSelected();
		
		if(GUI.report(report.build(), anonymize)) {
			clearData();
			close();
		}
	}
}