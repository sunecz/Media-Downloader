package sune.app.mediadown.gui.window;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.gui.Dialog;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.gui.GUI;
import sune.app.mediadown.gui.Window;
import sune.app.mediadown.gui.control.TranslatableListCell;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.report.ContactInformation;
import sune.app.mediadown.report.Report;
import sune.app.mediadown.report.Report.Reason;
import sune.app.mediadown.report.Reporting;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.JSON.JSONCollection;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public class ReportWindow extends DraggableWindow<VBox> {
	
	public static final String NAME = "report";
	
	private final Label lblReason;
	private final ComboBox<Reason> chbReason;
	private final LabelOptionalField lblEmail;
	private final TextField txtEmail;
	private final LabelOptionalField lblNote;
	private final TextArea txtNote;
	private final TextArea txtRawData;
	private final CheckBox chbAnonymizeData;
	private final Button btnSend;
	
	private volatile Report.Builder report;
	private volatile Features features;
	
	public ReportWindow() {
		super(NAME, new VBox(5.0), 450.0, 400.0);
		initModality(Modality.APPLICATION_MODAL);
		
		String textOptional = translation.getSingle("text.optional");
		
		VBox boxReason = new VBox(5.0);
		lblReason = new Label(translation.getSingle("label.reason"));
		chbReason = new ComboBox<>();
		Translation trReason = translation.getTranslation("value.reason");
		chbReason.setCellFactory((view) -> new TranslatableListCell<>(trReason, Reason::name));
		chbReason.setButtonCell(new TranslatableListCell<>(trReason, Reason::name));
		chbReason.valueProperty().addListener((o, ov, nv) -> updateData());
		chbReason.setMaxWidth(Double.MAX_VALUE);
		boxReason.getChildren().addAll(lblReason, chbReason);
		
		VBox boxContactInformation = new VBox(5.0);
		lblEmail = new LabelOptionalField(translation.getSingle("label.email"), textOptional);
		txtEmail = new TextField();
		txtEmail.textProperty().addListener((o, ov, nv) -> updateData());
		boxContactInformation.getChildren().addAll(lblEmail, txtEmail);
		
		VBox boxNote = new VBox(5.0);
		lblNote = new LabelOptionalField(translation.getSingle("label.note"), textOptional);
		txtNote = new TextArea();
		txtNote.textProperty().addListener((o, ov, nv) -> updateData());
		txtNote.setPrefHeight(100.0);
		boxNote.getChildren().addAll(lblNote, txtNote);
		
		VBox boxRawData = new VBox(5.0);
		txtRawData = new TextArea();
		txtRawData.setFont(Font.font("monospaced", txtRawData.getFont().getSize()));
		txtRawData.setEditable(false);
		boxRawData.getChildren().addAll(txtRawData);
		
		Accordion accordionRawData = new Accordion();
		accordionRawData.getStyleClass().add("accordion-labeled");
		TitledPane paneRawData = new TitledPane(translation.getSingle("label.raw_data"), boxRawData);
		paneRawData.setAnimated(false);
		boxRawData.setPadding(new Insets(5.0, 0.0, 0.0, 0.0));
		accordionRawData.expandedPaneProperty().addListener((o, ov, expandedPane) -> {
			boolean isExpanded = expandedPane != null;
			VBox.setVgrow(boxNote, isExpanded ? Priority.NEVER : Priority.ALWAYS);
			VBox.setVgrow(accordionRawData, isExpanded ? Priority.ALWAYS : Priority.NEVER);
		});
		accordionRawData.getPanes().addAll(paneRawData);
		
		VBox boxAnonymizeData = new VBox(5.0);
		chbAnonymizeData = new CheckBox(translation.getSingle("label.anonymize_data"));
		chbAnonymizeData.setOnAction((e) -> updateData());
		boxAnonymizeData.getChildren().addAll(chbAnonymizeData);
		
		HBox boxBottom = new HBox(5.0);
		btnSend = new Button(translation.getSingle("button.send"));
		btnSend.setOnAction((e) -> sendData());
		HBox boxFill = new HBox();
		boxBottom.getChildren().addAll(boxFill, btnSend);
		HBox.setHgrow(boxFill, Priority.ALWAYS);
		
		content.getChildren().addAll(
			boxReason, boxContactInformation, boxNote, accordionRawData, boxAnonymizeData, boxBottom
		);
		content.setPadding(new Insets(10));
		btnSend.setMinWidth(80);
		boxBottom.setAlignment(Pos.CENTER_LEFT);
		VBox.setVgrow(txtNote, Priority.ALWAYS);
		VBox.setVgrow(txtRawData, Priority.ALWAYS);
		VBox.setVgrow(boxNote, Priority.ALWAYS);
		
		FXUtils.onWindowShow(this, () -> {
			Stage parent = (Stage) args.get("parent");
			
			if(parent != null) {
				centerWindow(parent);
			}
			
			loadFeatures();
			
			if(report != null) {
				loadData();
			}
		});
	}
	
	private final void loadFeatures() {
		Feature.OfReason reason = features.get(Feature.OfReason.NAME);
		List<Reason> reasons;
		
		if(reason != null) {
			reasons = reason.allowedReasons();
		} else {
			reasons = List.of(Reason.values());
		}
		
		boolean reasonIsEditable = reasons.size() > 1;
		chbReason.setDisable(!reasonIsEditable);
		chbReason.getItems().setAll(reasons);
		
		boolean emailRequired = false;
		lblEmail.isOptional(!emailRequired);
		
		boolean noteRequired = false;
		Feature.OfNote note = features.get(Feature.OfNote.NAME);
		
		if(note != null) {
			noteRequired = note.isRequired();
		}
		
		lblNote.isOptional(!noteRequired);
	}
	
	private final boolean validateOptionalField(LabelOptionalField label, Supplier<String> textSupplier,
			Translation translation) {
		if(label.isOptional()) {
			return true; // No validation needed
		}
		
		String text = textSupplier.get();
		
		if(text == null || text.isBlank()) {
			Dialog.showError(translation.getSingle("title"), translation.getSingle("text"));
			return false;
		}
		
		return true;
	}
	
	private final void updateReportData() {
		report.reason(chbReason.getValue());
		
		ContactInformation contact = null;
		String email = txtEmail.getText();
		
		if(!email.isBlank()) {
			contact = new ContactInformation(email);
		}
		
		String note = null;
		String noteText = txtNote.getText();
		
		if(!noteText.isBlank()) {
			note = noteText;
		}
		
		report.contact(contact);
		report.note(note);
	}
	
	private final void updatePayload() {
		updateReportData();
		
		boolean anonymize = chbAnonymizeData.isSelected();
		JSONCollection payload = Reporting.payload(report.build(), anonymize);
		
		txtRawData.setText(payload.toString(false));
	}
	
	private final void loadData() {
		chbReason.setValue(report.reason());
		
		ContactInformation contact = report.contact();
		
		if(contact != null) {
			txtEmail.setText(contact.email());
		}
		
		String note = report.note();
		
		if(note != null) {
			txtNote.setText(note);
		}
		
		boolean anonymize = chbAnonymizeData.isSelected();
		JSONCollection payload = Reporting.payload(report.build(), anonymize);
		
		txtRawData.setText(payload.toString(false));
	}
	
	private final void updateData() {
		updatePayload();
	}
	
	private final void clearData() {
		chbReason.setValue(Reason.OTHER);
		txtEmail.setText("");
		txtNote.setText("");
		txtRawData.setText("");
		chbAnonymizeData.setSelected(false);
	}
	
	private final void sendData() {
		updateReportData();
		boolean anonymize = chbAnonymizeData.isSelected();
		
		if(!validateOptionalField(lblNote, txtNote::getText, translation.getTranslation("message.note_required"))) {
			return; // Do not continue
		}
		
		if(GUI.report(report.build(), anonymize)) {
			clearData();
			close();
		}
	}
	
	private final void initArgsBeforeShow(Report.Builder builder, Set<Feature> features) {
		this.report = builder; // May be null
		this.features = new Features(features);
	}
	
	public final void showWithFeatures(Window<?> parent, Report.Builder builder, Set<Feature> features) {
		initArgsBeforeShow(builder, features);
		show(parent);
	}
	
	public final void showWithFeatures(Window<?> parent, Report.Builder builder, Feature... features) {
		showWithFeatures(parent, builder, Set.of(features));
	}
	
	private static final class LabelOptionalField extends Label {
		
		private final String textOriginal;
		private final String textOptional;
		private boolean optional;
		
		public LabelOptionalField(String text, String textOptional) {
			this.textOriginal = text;
			this.textOptional = textOptional;
		}
		
		public void isOptional(boolean isOptional) {
			optional = isOptional;
			String text = textOriginal;
			
			if(isOptional) {
				text += textOptional;
			}
			
			setText(text);
		}
		
		public boolean isOptional() {
			return optional;
		}
	}
	
	private static final class Features {
		
		private final Map<String, Feature> features;
		
		public Features(Set<Feature> features) {
			this.features = Objects.requireNonNull(features).stream().collect(
				Collectors.toUnmodifiableMap(Feature::name, Function.identity())
			);
		}
		
		public <T extends Feature> T get(String name) {
			return Utils.cast(features.get(name));
		}
	}
	
	public static abstract class Feature {
		
		private final String name;
		
		private Feature(String name) {
			this.name = Objects.requireNonNull(name);
		}
		
		protected final String name() {
			return name;
		}
		
		public static final Feature onlyReasons(Reason... reasons) {
			if(reasons.length < 1) {
				throw new IllegalArgumentException("At least one reason must be specified");
			}
			
			return new OfReason(List.of(reasons));
		}
		
		public static final Feature noteRequired() {
			return OfNote.REQUIRED;
		}
		
		private static final class OfReason extends Feature {
			
			private static final String NAME = "reason";
			
			private final List<Reason> allowedReasons;
			
			public OfReason(List<Reason> allowedReasons) {
				super(NAME);
				this.allowedReasons = Objects.requireNonNull(allowedReasons);
			}
			
			public List<Reason> allowedReasons() {
				return allowedReasons;
			}
		}
		
		private static final class OfNote extends Feature {
			
			private static final String NAME = "note";
			private static final OfNote REQUIRED = new OfNote(true);
			
			private final boolean isRequired;
			
			public OfNote(boolean isRequired) {
				super(NAME);
				this.isRequired = isRequired;
			}
			
			public boolean isRequired() {
				return isRequired;
			}
		}
	}
}