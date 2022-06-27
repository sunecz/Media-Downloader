package sune.app.mediadown.gui.window;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.media.MediaTitleFormat;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.Previews;
import sune.app.mediadown.util.Previews.MediaTitleFormatPreviewMask;

/** @since 00.02.05 */
public final class PreviewWindow extends DraggableWindow<StackPane> {
	
	public static final String NAME = "preview";
	
	private static final List<MediaTitleFormatPreviewMask> previews = List.of(
		MediaTitleFormatPreviewMask.of(0b00001),
		MediaTitleFormatPreviewMask.of(0b00011),
		MediaTitleFormatPreviewMask.of(0b00101),
		MediaTitleFormatPreviewMask.of(0b00111),
		MediaTitleFormatPreviewMask.of(0b01001),
		MediaTitleFormatPreviewMask.of(0b01011),
		MediaTitleFormatPreviewMask.of(0b01101),
		MediaTitleFormatPreviewMask.of(0b01111),
		MediaTitleFormatPreviewMask.of(0b10111),
		MediaTitleFormatPreviewMask.of(0b11111)
	);
	
	private final ScrollPane scrollPane;
	private final VBox panePreview;
	
	public PreviewWindow() {
		super(NAME, new StackPane(), 400.0, 300.0);
		initModality(Modality.APPLICATION_MODAL);
		
		scrollPane = new ScrollPane();
		panePreview = new VBox(10.0);
		
		panePreview.setPadding(new Insets(10.0));
		scrollPane.setContent(panePreview);
		getContent().getChildren().add(scrollPane);
		
		FXUtils.onWindowShow(this, () -> {
			// Center the window
			Stage parent = (Stage) args.get("parent");
			if(parent != null) centerWindow(parent);
		});
	}
	
	public final void showPreview(MediaTitleFormat format) {
		if(panePreview.getChildren().isEmpty()) {
			panePreview.getChildren().setAll(
				IntStream.range(0, previews.size()).mapToObj((i) -> new PreviewItem()).collect(Collectors.toList())
			);
		}
		
		Iterator<MediaTitleFormatPreviewMask> it = previews.iterator();
		for(Node node : panePreview.getChildren()) {
			((PreviewItem) node).preview(format, it.next());
		}
		
		show();
	}
	
	private static final class PreviewItem extends VBox {
		
		private static Translation mediaNamingTranslation;
		private static Font fontDescription;
		private static Font fontPreviewText;
		
		private final Label lblDescription;
		private final Label lblPreviewText;
		
		public PreviewItem() {
			lblDescription = new Label();
			lblPreviewText = new Label();
			
			if(fontDescription == null) {
				fontDescription = Font.font(lblDescription.getFont().getSize() * 0.85);
			}
			
			if(fontPreviewText == null) {
				fontPreviewText = Font.font(lblPreviewText.getFont().getSize() * 1.15);
			}
			
			lblDescription.setFont(fontDescription);
			lblPreviewText.setFont(fontPreviewText);
			getChildren().addAll(lblDescription, lblPreviewText);
		}
		
		private final String buildDescription(MediaTitleFormatPreviewMask mask) {
			StringBuilder builder = new StringBuilder();
			
			if(mask.isProgramName()) builder.append(", program_name");
			if(mask.isSeason())      builder.append(", season");
			if(mask.isEpisode())     builder.append(", episode");
			if(mask.isEpisodeName()) builder.append(", episode_name");
			if(mask.isSplit())       builder.append(", split");
			
			return builder.toString().replaceAll("^,\\s+", "");
		}
		
		private final String buildPreviewText(MediaTitleFormat format, MediaTitleFormatPreviewMask mask) {
			if(mediaNamingTranslation == null) {
				mediaNamingTranslation = MediaDownloader.translation().getTranslation("media.naming");
			}
			
			return Previews.preview(format, mask, mediaNamingTranslation);
		}
		
		public void preview(MediaTitleFormat format, MediaTitleFormatPreviewMask mask) {
			lblDescription.setText(buildDescription(mask));
			lblPreviewText.setText(buildPreviewText(format, mask));
		}
	}
}