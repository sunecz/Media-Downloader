package sune.app.mediadown.gui.window;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.os.OS;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.Regex;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.08 */
public class AboutWindow extends DraggableWindow<VBox> {
	
	public static final String NAME = "about";
	
	private final Label lblTitle;
	private final TextFlow lblVersion;
	private final TextFlow lblAuthor;
	private final Label lblDescription;
	
	public AboutWindow() {
		super(NAME, new VBox(0.0), 400.0, 250.0);
		initModality(Modality.APPLICATION_MODAL);
		
		lblTitle = new Label(MediaDownloader.TITLE);
		lblVersion = makeText(
			tr("label.version"),
			"version", MediaDownloader.version().compactString(),
			"release_date", formatDate(MediaDownloader.DATE)
		);
		lblAuthor = makeText(
			tr("label.author"),
			"name", MediaDownloader.AUTHOR,
			"email", Data.authorEmail()
  		);
		lblDescription = new Label(tr("label.description"));
		
		VBox.setMargin(lblTitle, new Insets(-10.0, 0, 0, 0));
		VBox.setMargin(lblDescription, new Insets(15.0, 0, 0, 0));
		
		lblTitle.setId("label-title");
		lblVersion.setId("label-version");
		lblAuthor.setId("label-author");
		lblDescription.setId("label-description");
		
		lblTitle.setTextAlignment(TextAlignment.CENTER);
		lblVersion.setTextAlignment(TextAlignment.CENTER);
		lblAuthor.setTextAlignment(TextAlignment.CENTER);
		lblDescription.setTextAlignment(TextAlignment.CENTER);
		lblDescription.setWrapText(true);
		
		content.setAlignment(Pos.CENTER);
		content.getChildren().addAll(lblTitle, lblVersion, lblAuthor, lblDescription);
		
		setMinWidth(400.0);
		setMinHeight(250.0);
		
		FXUtils.onWindowShow(this, () -> {
			Stage parent = (Stage) args.get("parent");
			
			if(parent != null) {
				centerWindow(parent);
			}
		});
	}
	
	private static final String formatDate(String date) {
		DateTimeFormatter formatSource = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter formatTarget = DateTimeFormatter.ofPattern("dd. MM. yyyy");
		return formatTarget.format(formatSource.parse(date));
	}
	
	private static final TextFlow makeText(String format, Object... args) {
		TextFlow flow = new TextFlow();
		flow.getStyleClass().add("text");
		
		Map<String, Object> mapArgs = Utils.stringKeyMap(args);
		Matcher matcher = Regex.of("%\\{([^\\}]+)\\}\\w").matcher(format);
		
		int pos = 0;
		while(matcher.find()) {
			if(pos < matcher.start()) {
				String before = format.substring(pos, matcher.start());
				flow.getChildren().add(new Text(before));
			}
			
			String value = Utils.format(matcher.group(), mapArgs);
			
			switch(matcher.group(1)) {
				case "email": {
					Hyperlink link = new Hyperlink(value);
					link.setPadding(new Insets(0));
					link.setOnAction((e) -> Ignore.callVoid(() -> OS.current().browse(Net.uri("mailto:" + value))));
					flow.getChildren().add(link);
					break;
				}
				case "version": {
					Text text = new Text(value);
					text.setId("label-version-value");
					flow.getChildren().add(text);
					break;
				}
				default: {
					flow.getChildren().add(new Text(value));
					break;
				}
			}
			
			pos = matcher.end();
		}
		
		if(pos < format.length()) {
			String after = format.substring(pos);
			flow.getChildren().add(new Text(after));
		}
		
		return flow;
	}
	
	private static final class Data {
		
		private static final int   T = 1 << 5;
		private static final int[] S = { 0, 2, 6, 3, 1, 3, 2, 7, 4, 1, 5, 0 };
		
		private static final int [] u() { return new int [T]; }
		private static final long[] v() { return new long[T]; }
		private static final byte[] x() { return new byte[(T << 2) + (T << 3)]; }
		
		private static final String d(long[] y) {
			StringBuilder s = new StringBuilder();
			byte[] x = x(); int[] u = u(); long[] v = v();
			long a, b; int i, l, c, p, m = 1 << 3, n = m + (1 << 2);
			long g, d = (1 << 8) - 1, e = 0L, f = 1 << 5;
			e = ((e = ((e = d | e) << 8) | e) << 16) | e;
			for(i = 0, l = x.length; i < l; ++i)
				x[i] = (byte) ((y[i >> 3] >> ((i % m) << 3)) & 0xff);
			for(i = 0, l = x.length; i < l;) {
				for(a = 0L, b = 0L, p = 0; p < n; ++p, ++i) {
					g = (long) (x[i] & 0xff) << (S[p] << 3);
					if(i % 3 == 0) a |= g; else b |= g;
				}
				u[(i - 1) / n] = (int) a;
				v[(i - 1) / n] = b;
			}
			for(i = 0; i < T; ++i)
				if((c = (~(int)((v[i] >> f) ^ (v[i] & e))) ^ (u[i])) != 0)
					s.appendCodePoint(c);
			return s.toString();
		}
		
		public static final String authorEmail() {
			return d(new long[] {
				0x61e360d1fe415d17L, 0x35edc340551d33ceL, 0x9cca3d564fd18508L, 0x6f242c92bc9e45aaL, 
				0xb0c004366714795cL, 0x9767973b0e3b410fL, 0xa7f26d4335c2cfa2L, 0xf3d7f73640ef535dL, 
				0x1e3697a45edf525eL, 0x360faa9e633eced9L, 0xb6a03c91e5c7a6b6L, 0x93b177938a63c339L, 
				0x50d83b68940522baL, 0x964f5e7a2e6ef90eL, 0x89610f7b45ee2c91L, 0x30842027ef423937L, 
				0xb8422d1f11f921bcL, 0xab03b4295d901a48L, 0x1cd8baf659eacd00L, 0x490bb1fd26535af7L, 
				0xdbce96b76345d5a7L, 0x5eb29b243a2b66f1L, 0x20328870669b400dL, 0x9d95356695454a5fL, 
				0xbbeae3afa77560cbL, 0xef0b7e89360e5e02L, 0xb6e9adc0128a02bbL, 0x766c49d8c067f426L, 
				0xc92b5c23a41c3b7dL, 0x101e0ccc7b884dedL, 0x40b75a5de5c58da6L, 0xa287f038ef882ab6L, 
				0xe09ae827f888a58dL, 0x25e12c12f6dcc26dL, 0x3e55f8704ca14cdeL, 0x0ae4df850152c0c4L, 
				0xdec8f2e1d33e09d0L, 0xbb40c7fbe7746ac8L, 0xc39dc7c7d47890a5L, 0xb0a3ce8181075b4cL, 
				0xcfb3829f27cdb394L, 0x7890ab18dfceefc4L, 0xdde4974cb58893baL, 0x4961271436ca7973L, 
				0xee1ee0050ab9bc01L, 0xd0831f1a30a5d9c2L, 0x9914128fc09f7afdL, 0xa93cd0d9f4f99213L
			});
		}
	}
}