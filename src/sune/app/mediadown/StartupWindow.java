package sune.app.mediadown;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

// Since this window is shown well before anything else is initialized,
// we must use only basic methods and components.
public final class StartupWindow extends Stage {
	
	static {
		InternalFontLoader.loadFonts();
	}
	
	private final Scene scene;
	private final StackPane pane;
	private final StackPane paneBack;
	private final StackPane paneTitle;
	private final BorderPane content;
	private final VBox boxBottom;
	private final Label lblStatus;
	private final ProgressBar prgbar;
	
	private volatile int current;
	private volatile double total = Double.NaN;
	
	public StartupWindow(String title, int total) {
		initStyle(StageStyle.UNDECORATED);
		pane      = new StackPane();
		scene     = new Scene(pane, 600.0, 400.0);
		paneBack  = new StackPane();
		paneTitle = new StackPane();
		content   = new BorderPane();
		boxBottom = new VBox();
		lblStatus = new Label("Initializing window...");
		prgbar    = new ProgressBar();
		String[] words = title.split("\\s+");
		for(int i = 0, l = words.length; i < l; ++i) {
			Label lblTitle = new Label(words[i].toUpperCase());
			lblTitle.getStyleClass().add("lbl-title");
			lblTitle.setId("lbl-title-" + i);
			paneTitle.getChildren().add(lblTitle);
		}
		pane.getChildren().addAll(paneBack, paneTitle, content);
		boxBottom.getChildren().addAll(lblStatus, prgbar);
		content.setBottom(boxBottom);
		paneBack .setId("background");
		paneTitle.setId("pane-title");
		lblStatus.setId("lbl-status");
		boxBottom.setId("box-bottom");
		scene.setCursor(Cursor.WAIT);
		scene.getStylesheets().add(stylesheet("window-startup.css"));
		setScene(scene);
		getIcons().setAll(MediaDownloader.ICON);
		setTitle(title);
		setTotal(total);
	}
	
	private static final String stylesheet(String name) {
		return StartupWindow.class.getResource("/resources/theme/" + name).toExternalForm();
	}
	
	private static final void fxThread(Runnable r) {
		if(!Platform.isFxApplicationThread()) Platform.runLater(r); else r.run();
	}
	
	private static final <T> T fxValue(Callable<T> action, T defaultValue) {
		class Holder { Object value; }
		Holder holder = new Holder();
		Object result = null;
		
		fxThread(() -> {
			T value = null;
			try {
				value = action.call();
			} catch(Exception ex) {
				// Ignore
			} finally {
				synchronized(holder) {
					holder.value = value;
					holder.notify();
				}
			}
		});
		
		synchronized(holder) {
			try {
				holder.wait();
				result = holder.value;
			} catch(InterruptedException ex) {
				// Ignore
			}
		}
		
		if(result == null) {
			return defaultValue;
		}
		
		@SuppressWarnings("unchecked")
		T casted = (T) result;
		return casted;
	}
	
	public final void update(String text) {
		++current;
		
		fxThread(() -> {
			setProgress(current / total);
			setText(text);
		});
	}
	
	public final void setText(String text) {
		fxThread(() -> lblStatus.setText(text));
	}
	
	public final void setTotal(int value) {
		total = value;
		fxThread(() -> prgbar.setProgress(current / total));
	}
	
	public final void setProgress(double value) {
		fxThread(() -> prgbar.setProgress(value));
	}
	
	public final double getProgress() {
		return fxValue(prgbar::getProgress, 0.0);
	}
	
	private static final class InternalFontLoader {
		
		private static final int    MD_HPS_LEN = 6036;
		private static final long[] MD_HPS_DAT = new long[] {
			0x774f464600010000L, 0x0000179400120000L, 0x0000424800010000L, 0x0000000000000000L, 0x0000000000000000L,
			0x000000004646544dL, 0x000017780000001cL, 0x0000001c8d767015L, 0x4744454600000ef0L, 0x0000002400000026L,
			0x0029003347504f53L, 0x00000f7000000805L, 0x00002c7c726658f0L, 0x4753554200000f14L, 0x0000005a0000006cL,
			0x408060134f532f32L, 0x000002080000004eL, 0x0000006043a4452eL, 0x636d61700000028cL, 0x000000670000016eL,
			0x14f60f8663767420L, 0x0000054c0000004eL, 0x0000004e1cb6169eL, 0x6670676d000002f4L, 0x000001b100000265L,
			0x53b42fa767617370L, 0x00000ee800000008L, 0x0000000800000010L, 0x676c7966000005b8L, 0x000006db00000914L,
			0xaaea2d8b68656164L, 0x0000019400000034L, 0x000000361337ef69L, 0x68686561000001c8L, 0x0000001e00000024L,
			0x0dae04be686d7478L, 0x0000025800000034L, 0x000000343445046cL, 0x6c6f63610000059cL, 0x0000001c0000001cL,
			0x09a20c6e6d617870L, 0x000001e800000020L, 0x00000020012701feL, 0x6e616d6500000c94L, 0x0000022300000456L,
			0x0d789577706f7374L, 0x00000eb80000002dL, 0x0000003cfc8c01a9L, 0x70726570000004a8L, 0x000000a3000000ecL,
			0x75a817b0789c6360L, 0x64606000e2fd55ceL, 0xcdf1fc365f19e439L, 0x1840e0747f0b1388L, 0x7e24a0f090c1e9ffL,
			0x633665d665402e07L, 0x035814002a2b0a9dL, 0x789c6360646060efL, 0xfce7c2c0c096c300L, 0x046cca0c8c0ca880L,
			0x1700458d027d0000L, 0x00010000000d002fL, 0x0002000000000002L, 0x0001000200160000L, 0x010001cb00000000L,
			0x789c63606659cdb4L, 0x87819581897516abL, 0x310303c33708cd6cL, 0xccb083e12c030313L, 0x37071b13130b1001L,
			0xe518199080021000L, 0x29478670b6c67f8dL, 0x0c0cec9d8c7b606aL, 0x98d899b682943030L, 0x010053330b510000L,
			0x02ec004400000000L, 0x02aa0000049d0042L, 0x04e1007904260079L, 0x025c007b03db0079L, 0x05e9006a04f10079L,
			0x04f10062049d0079L, 0x066c0042789ccd8dL, 0x3112803008049718L, 0x8d893ec1cfd85bd9L, 0xf8053fe8fb22828dL,
			0x9596ee0c1c771302L, 0xd0e035215ceceac4L, 0x7c6451ed5485c0caL, 0x56abfad9b51e9a76L, 0xb6d1588fb4389e26L,
			0x7a32c5e6014453b9L, 0x1f48d0167822bca3L, 0x9762eb17529f8b7dL, 0x0ce387cd5f700275L, 0x49089900789c5d51L,
			0xbb4e5b4110dd0d0fL, 0x0381c4d82039da14L, 0xb39990c67ba10509L, 0xc4d58d62643b85e5L, 0x08693772918b7101L,
			0x1f4081440ddaaf19L, 0xa0a1a4489b062117L, 0x487c423e2112336bL, 0x88a2343b3bb373ceL, 0x99334bca91aa77e9L,
			0x6bcf53e72490c2ddL, 0x06cd36fd4e48b5b3L, 0x00f7a4ebeb8d8cb4L, 0x83075a6c66f4ca75L, 0xfbfe0b069bd1943bL,
			0x6a00e53d6f290f19L, 0x4d3b815ab4a7fed1L, 0xfc0a86fbfc1ff33bL, 0x18b434d3f4d43a09L, 0xe92104e69b714b83L,
			0xef19cdbacb0ffa82L, 0xd5e1623030a498a6L, 0xe22e3fa652feb734L, 0xe76acbb0b591d1bcL, 0x833311f9c9344053L,
			0x6b6d049afed421d5L, 0xf371144b90cba6b1L, 0x369898b2fe2413c1L, 0x85c9745553b5ccf8L, 0xdac15db2b3e86083L,
			0x2acd8107d8c35679L, 0x0c1e0e0f2614d2b7L, 0x24ca2c0d11f662abL, 0xc40811931c0a39e5L, 0xdcc9fea440f94812L,
			0xc6bc494a3be386b5L, 0x06c691d7c0a0364fL, 0xb3ff3c9b4d6d6f1dL, 0xc2f8591cc177bf19L, 0x4b3af8c886da1811L,
			0x623b622980094442L, 0x4655f9861acfbd2cL, 0x06e452fbcf409480L, 0xe5f18f7f9d08b4eeL, 0xd8443c97b5750e31L,
			0x56087a7edbdcf2cbL, 0x8abb56b9ce8b4277L, 0x6faa6aa8d229cdfbL, 0x5ecebec7039e1e0bL, 0xc34163c19bcffbfeL,
			0x4a81fa3c2cae3468L, 0x0e04437a377affa2L, 0xb5ea88abbc173eb2L, 0x27d3bfb15a000000L, 0x789cdbc1f8bf7503L,
			0x632f83f7068e8088L, 0x8d8c8c7d911bddd8L, 0xb423143708447a6fL, 0x100902321a226537L, 0xb069c744306c6056L,
			0x70ddc0acedb28153L, 0xc17513f30c266d30L, 0x8703c8e1b48172d8L, 0x811c0e3d28870dc8L, 0x6157877258811c36L,
			0x05288705c8619584L, 0x72b840a69d807018L, 0x377043ede0018a72L, 0x0732696f64762b03L, 0x7279155c773170d7L,
			0xff67808bf0814478L, 0x19951022fc402d7cL, 0x6170ae0090cbef02L, 0xe3466e10d1060002L, 0x2742b40000000427L,
			0x058b012300f30100L, 0x01040108010f011bL, 0x0129015601480156L, 0x015a0166016b0106L, 0x009f012c00e000cfL,
			0x0171014f013b0142L, 0x013400d6014c0132L, 0x00c70119013700b0L, 0x0080007800b50044L, 0x051100000000002cL,
			0x002c002c00b4010aL, 0x0150017401a00230L, 0x027e02de0354048aL, 0x789c7d556b881bd7L, 0x15be77de9ad54a33L,
			0x7a8fa4d16a349664L, 0x652c6d2ceda3a2eeL, 0xdadd2cee7691ddd0L, 0x26c11863c2767f6cL, 0xed84b63165598cebL,
			0x5ff9116f62932e34L, 0x21d44e4ad3509a84L, 0xdc3bda4058dca2feL, 0xd8fc0986c0828b9bL, 0xa4c50dad5168b736L,
			0xc4dbaca39df4dc91L, 0x1f5b2815e83ece9dL, 0x39f39def7ce75cc4L, 0xa12984b839f171c4L, 0x2319d52846c3fb5cL,
			0x5928feb34e25f1e3L, 0x7d2ecfc112519e99L, 0x45667665a9d4dbe7L, 0x62666fe8965eb474L, 0x6b8acb7bbbf0cbdeL,
			0xbcf8f89d37a7842bL, 0x085ca249848413e2L, 0x124aa121f4127213L, 0x083964b0e14a1c72L, 0x562246421a746844L,
			0xec62921f26e82a8dL, 0x0f74a9851d32965eL, 0x9db8f1f95e147754L, 0x62d44224d7a17c74L, 0x8b889db6c08b5187L,
			0x185a7bc8c8451d17L, 0xb6f973f973b614d2L, 0x234d176c30a1155eL, 0x10734335f6c3eff2L, 0xc6fd4d8d1c48631aL,
			0x477a84484d442503L, 0x1666f3e1bde32363L, 0xe3691cb5ea89249bL, 0x0a65de0a61d92a94L, 0x70a3c495256e5210L,
			0xc6974ee2672a9672L, 0xece5af7b7f9a542dL, 0xef7a30267a1b6601L, 0xbfd84dd61d67e4d1L, 0x48258a1fe54ee046L,
			0x25e16d6c7fdb3b33L, 0x8323c7df3b58e446L, 0x17b0f2d65bde17c8L, 0xe76311f8382bbe86L, 0x34e0e3207243c007L,
			0xd595ae2b002134abL, 0xdc67420f7789aed1L, 0x24766840f349a149L, 0x1df00a003ccb1681L, 0x26117482197c7d02L,
			0x37ea391c89c7b810L, 0xb6ad02ec26b8d191L, 0x1a6717247971e1a3L, 0xa75ef878b935b37cL, 0xedbc77ae8567bf3bL,
			0x75643411dbfbbdfdL, 0x99317d5a749289afL, 0xbdeafdedd3eb5ef7L, 0x17e358cb1c5f7965L, 0xf99d63598e439861L,
			0xe5af41eea26806b9L, 0x11867430d0859cc3L, 0x420a7457e4600443L, 0xfae400808ef9a007L, 0x435d22d5dd41a43aL,
			0xed03837a808da23fL, 0x0e041c1ac70e6268L, 0xadba89ad9d636191L, 0x472d4fc3f32daf85L, 0xdf6bf5fed19a161fL,
			0xf9d6ba770bfeebd3L, 0xebc01b46a761f82bL, 0x609191855c09b0f8L, 0x383051fc0f8be1aeL, 0xff0fdcff84ac5b85L,
			0xd338d3c2dedf7d6fL, 0x772e23df0f8be937L, 0xe06700ed41aeca62L, 0x12f9ee5d5fc17bbeL, 0x5cd18f400c00eac1L,
			0xfb2ee3f7d0e2740bL, 0x9f696d9bbee32fafL, 0x4f7f027e4f20249eL, 0x05bf457409b9bb98L, 0xcab506940dcc8106L,
			0x26a56192bf4a7376L, 0xbd4e51b04b106c35L, 0x9a829c6a20f8725fL, 0xf07fb87cabc1042fL, 0x901c28deec5039b6L,
			0x4594ceeac4d8cd3bL, 0x7d7b0aec4647a481L, 0xf8169515050ed1bbL, 0xb29232cc5c5fde78L, 0x45560286f940ea79L,
			0x903ae5ad66936a29L, 0x90cc609345a2457cL, 0xa567705fefc0d3e8L, 0x3eace2326f174a65L, 0x5e52b116b70a27ecL,
			0x247ecaacaa3fff2dL, 0x3e5a318564deebb5L, 0x38812357aaa6f28aL, 0x80f9af506b1a4f73L, 0x2fe1b96c75fb39aeL,
			0x53c97b1f6dcf7967L, 0xa79fece120967eb0L, 0xbdbf6a720b3ff43eL, 0xf3fee2fd6bbecfbbL, 0xb008fc64a023b869L,
			0xc64fb8e1d34e0680L, 0x9faccfbc01d4181aL, 0x55819770b04b4d98L, 0x55a8512a469ba07aL, 0x9c86a59a6a36fbd9L,
			0x4824737c526fb0acL, 0x00ee1a5fd66dc84cL, 0x70a81ab28f1c3d5aL, 0xbcd3f26e0c95062aL, 0x4f1c79acbcc51255L,
			0xcd4eafac7d709c6fL, 0x8014aae6fc1faf5cL, 0x9ee9bddeafc9d9afL, 0x3e152e886f80cecbL, 0xd007dd305385014aL,
			0x1799d26d26efdd3eL, 0xbe98d625318de618L, 0x2ea8c90accb91880L, 0x0a8b0c9fcda0aa88L, 0xe18b8e44feab240bL,
			0x356894b11cf7a02eL, 0x43dcece1a5b5c585L, 0xf79f3f74e8f9f717L, 0x16d7960ee393a1e2L, 0xd4e8e85431746fe6L,
			0xd63670e152a371c9L, 0xfbf3c686f7c9c591L, 0x918bd8de78b13d6bL, 0x59b3eecf2eb8b3b6L, 0x3debdeed298bd053L,
			0xb2a88abe8fdc0c63L, 0x576ff45bca6ea9bbL, 0x629819010ad59020L, 0x929a1f491a0ad5acL, 0x93b4466d08420611L,
			0x0ec36c33861342b3L, 0x49649d06e32c2893L, 0xf5c86093ecd6890cL, 0x0d27b2a3e124e2b1L, 0x102743127cfe71d9L,
			0xd677769e8730c4bdL, 0x78eac68f376f479cL, 0x56d3f9cec166d4bbL, 0x56ac068b27879ff4L, 0x6eb6f0b1a787c69dL,
			0x54ac325156d2a6a9L, 0xf89de8da66e3e9b9L, 0x2786c6cfbffac661L, 0xb37cfad663db3d5eL, 0x89560f7f63f29013L,
			0x8696043a82fb44b2L, 0x414735fc0172cb2cL, 0x53f952a3c1587048L, 0x0c94343c4c625749L, 0xb44e530320a63a7dL,
			0xb85f5c9d5f6df658L, 0x1185484d237b3a54L, 0x4a6f914067757ffeL, 0xf633cc2c9212d456L, 0x11ec018506325b21L,
			0xa276563bebb7d7d8L, 0x212d151552d44264L, 0x578786a026c3f0deL, 0xcce64ffda3505821L, 0x6138d23b34071754L,
			0x168edede7aa15faeL, 0xd11a0deb0a1c09d4L, 0xc86eb1925eedfce8L, 0x8be7e07080485a5bL, 0x960251a7adb07175L,
			0xffa97fffd2b787b5L, 0xb616d699e5c32fd3L, 0xbe45d7da113d0a77L, 0x9ea9b5d3a601af64L, 0xd8487359a59d652bL,
			0x92d3da662e0bef74L, 0xe8e645ff9d9ad6aeL, 0xd6f6c0f508df7870L, 0x3d12a5e982995d94L, 0xf08d1dd726f8dfb1L,
			0xcbb09c3749ba890eL, 0x24026a289cd95594L, 0x6445d32351239d35L, 0x737baab5fffdc307L, 0xe2771f2ff59fff7fL,
			0x4ffbed2905c543f2L, 0xac29f91d29c74592L, 0x51d055323a96c663L, 0xe3165398244b2ac7L, 0xaee2f200666349e5L,
			0x2671ae6c71921ab7L, 0x52afdd2c66a5f346L, 0x3ea14b581bb20441L, 0x7d687ca6ea5d2996L, 0xa53317bcf57251b5L,
			0x1e9968884b55fbd4L, 0xd5cfbccdd7f9df55L, 0xcabde54b388251efL, 0xd9e270c829cf7b1fL, 0x7aef78bf5e766ceeL,
			0xf3deef2d6b0e7f13L, 0xff043f0b6de13f53L, 0x72699c00789cb592L, 0xbf6adb5014c6bf2bL, 0x3bc6260e4d3c7408L,
			0xa5dc968eb12c2b1eL, 0x8ca18b638c071b0cL, 0x01efc2526c11c912L, 0xcac5c20f90adf43dL, 0x52ba74eb50287468L,
			0xb70e9dfb08ed3bf4L, 0x937c206448a14375L, 0xb9f7feced13ddf39L, 0xf70f80676a0185fdL, 0x37c05658a186cfc2L,
			0x162af82e5cc10bf5L, 0x54b88a9a1a0b1fe0L, 0x4885c235fa6f85ebL, 0x78a2de0b37c83f85L, 0x0f716abd146e926fL,
			0x848f7164bd133e41L, 0xd5fa22dc42cbfacdL, 0xecaadaa065ca4a0aL, 0x568cfe206c31f357L, 0xe10a86f8215c4553L,
			0xf5850f70aa66c235L, 0xfa73e13a9eab37c2L, 0x0df227e143f4d42fL, 0xe1267ad66be163d6L, 0xfc56f80475eba370L,
			0x0bafac6fb883c605L, 0x12a4d821438815d6L, 0xac59c385832e478dL, 0x0902e488381ab636L, 0xe6f0b0c435c70c3eL,
			0xff8ff867cb1e952aL, 0x3169532a14aa313dL, 0x1eed1dcee899c266L, 0xb40ddce98b24dd65L, 0xe16a6db4eb745d3dL,
			0x09f22830a63df796L, 0xd75ee6eb51b00da2L, 0x248d838de1d238f5L, 0x36bb333db5e78c8dL, 0x99b4cd2253dcd0f0L,
			0xdbeb94b398258f99L, 0xb7a8a09833ee2728L, 0x7763733f9aaf463fL, 0x10d87bfab4cfd98bL, 0x3dbbe85122d99871L,
			0x92ad02edda8e1ee8L, 0x7d1a42bf7dde761dL, 0xb7f748190b26cbe8L, 0x08cb12340fb0485bL, 0x342c82ec264c36baL,
			0x6b3b8ef348f88867L, 0x15b1f27df08c96c7L, 0xfa8b73335c8e9117L, 0x192acc3c6fa5a7c6L, 0xfff7f5ebf20653eeL,
			0xb9c39697cde6ca7bL, 0x9558346cde70717bL, 0x1d0619930e3a9d3cL, 0xcf6dbf148ca9672fL, 0x93b8f31f04277c1fL,
			0x1a973cc0e2e5449cL, 0xafd883623793b9beL, 0x0ce3340aafc280e6L, 0x907251e11f2691ffL, 0xb7407dbff481842eL,
			0x03ff00ead7bbfa00L, 0x789c6360620083dfL, 0xb31976306003bc40L, 0xccc8c0c4a0c2a0ceL, 0xa0c1a0c3a0cf60c0L,
			0x60c860c460ca6005L, 0x00881d0431000000L, 0x00010001ffff000fL, 0x789c6360646060e0L, 0x0162310639062606L,
			0x460666208f918105L, 0x28c204c48c100c00L, 0x0958005d789c6360L, 0x646060e062b061f0L, 0x6260cc492cc963e0L,
			0x60506260758c7255L, 0x6050720e0a0192beL, 0xfe3e4032c8df1748L, 0x86047903490686ffL, 0xff1940fa18d38a12L,
			0x93813a18203c0616L, 0x063630cdc1c00c24L, 0x8580988f81012ecbL, 0x000047ef0c2b0000L, 0x789cd55a4d685b47L,
			0x105ebb491b546184L, 0x7960444855023988L, 0x2694e0430fa928a5L, 0x2d84346940e4547cL, 0x694973689b824b2fL,
			0xc1c7e2834fa60442L, 0xd0c184e063112687L, 0x604a0804118c3126L, 0x18131ec104238230L, 0x01618428a2f4a0e9L,
			0x37b3fbfe9ffe7fecL, 0x8ed093b4bbefedbcL, 0x996fe79bd92735a1L, 0x944aa84fd51535f1L, 0xf377bffda24ea9b3L,
			0xeae4e7df7ef5a13aL, 0xfb45fe068ed7ae5fL, 0xc5317ffd1a8e37f2L, 0x5fe3a81491e2f326L, 0x7efae1573e43e95fL,
			0xea849a94cf536af2L, 0xbc855feabc3a9f54L, 0x57716d45356ad036L, 0xd974972aea5809edL, 0xd14b65510def5daaL,
			0xe2f8e6a8358a95ecL, 0x512ba02c958ab4ddL, 0x42eb9108d5a9219fL, 0x0fc98eefa5b7e3d6L, 0x09b6b0540eaf4b78L,
			0x2b2ad2bee0e9003dL, 0xab6881f5a0539e8aL, 0xf89e85d619acbb04L, 0x7d3f729d12fce1a0L, 0xda58ad617a53622bL,
			0xdb4597253a1daaf4L, 0x88b5d27325236df3L, 0xeeb7a3c77b776299L, 0xd7719205f31a9d44L, 0x7d970bf86eacde03L,
			0xb754258e97707ca5L, 0x7254a2757c7b1aecL, 0x17bd2caae37755ffL, 0xa65d33c01a3e27d1L, 0x0ed8ae88992ab4cdL,
			0x33d30bac321d0b2eL, 0xa0d78b5919b1640aL, 0xa3cb8c22977d12c3L, 0xe721e1601baf42dcL, 0xfd82011117109dbcL,
			0x964674d4d0753ac0L, 0xab089d96d84eb081L, 0xf2b38c78ab410bf0L, 0xa88dbe948c6fc828L, 0x67c43eeeea70048aL,
			0xc5ade8cb6dcf186dL, 0xcce41893167c606dL, 0xd1a28f5b5852f80eL, 0xbe8367d9521b4015L, 0x22397de33b7f6a14L,
			0x4ab1e5315fb17d2fL, 0x56410d1a31d35403L, 0xfdd5f8f3069284b9L, 0x764510ad73ba13aeL, 0x9d12b1e34f8c408fL,
			0x18a1bfc6334f5732L, 0xa33f684f8e05ac2bL, 0x8ea18e9d2ced1de0L, 0x8cf1c4d18af3023fL, 0xa34473c1c1c5ac39L,
			0xad05226295fed131L, 0xc099917ba02b62a5L, 0x6007bea3b511e811L, 0x236c03c49d2cecb1L, 0x1f88df1237957272L,
			0x00f3cbfdf4add151L, 0xc925f8cef6f9ce93L, 0x500c1bc97a73aecdL, 0xb55d157eb1e36671L, 0xe23a7a6bbc2ae5dbL,
			0xc8d9c5c7a093815fL, 0xbad7e08ab38690a6L, 0xce777bf81c4873e1L, 0x68d9c40c94707bb3L, 0x94098c2fd25cd3afL,
			0xd31c5ef33437549dL, 0xde00418bf0e022adL, 0x28462cfca8a670e7L, 0x92c5f9d8d5309ca0L, 0x7fca87f16963258bL,
			0x9654925ed233643dL, 0x0bb0f7cc20ac23f1L, 0xa78ada3cbeb7a635L, 0x71502fa3395bf8dbL, 0xfc6e38e330a68e57L,
			0x0def039dfdf5af91L, 0x7bc72c69c48215d8L, 0xa9ec6910d611e3dbL, 0x5a4072ae8d7ef589L, 0xbda2cd380fe8191eL,
			0xa12bc07acbfeaad4L, 0xa9c3114b3844fb8aL, 0xd7dfaa54c607f404L, 0xc7cbcc41b4a1edd6L, 0xfc37a083b0371584L,
			0xa5a6846b26f1fb26L, 0x5643a5ddbdb5169dL, 0x7bb8cca154878ca8L, 0x354a5061c03e121bL, 0x5ef85a07ce413513L,
			0xfb7d079d39a70bc4L, 0x0b7aed61dc6ddbf0L, 0xeb8bfc78d7e1a7deL, 0x6d054cfb575c96d6L, 0xe18504cfcabea34dL,
			0xf84ee33da3f580ffL, 0x6ac2c3ce4c69b322L, 0xd368951c039a4d21L, 0xc6260c3f56d474cfL, 0x3aad52b9d9400535L,
			0xdbe4bbb410af8a6aL, 0xc6b963dffac9baf7L, 0x5073f21b11c7d316L, 0xe5702f05c4cf6dcaL, 0x40a7e9987cb037cdL,
			0x98f1c4f3add753c7L, 0x6b94c5d33559177dL, 0x5fc57f3dce9c18ebL, 0x2a92a5991aa1ece5L, 0x0a010cbb16a5e7f4L,
			0x0a18aff09e2d7e6fL, 0x4a4ca987f3f7ae24L, 0xe16ac1d5cbbc468fL, 0xc4823cf70ad7a826L, 0xe70d87f4489fd23cL,
			0x0823176b7f01ba94L, 0xf47739ced20313abL, 0x06cdaf3c0cdc0cb4L, 0x6765377b8c027bbfL, 0xc67ae3f87c0e3e7cL,
			0x2bfe0aefa49c0b9dL, 0xe372b0d2384fd32eL, 0xaf0dcee7e1e7b23aL, 0x6dd6dd611febae82L, 0x95a6b3ba6a2822e6L,
			0x7d6871f327aec523L, 0xd7684844aa883ec0L, 0xb7f1bfe6a03e5806L, 0x57a9c32619ae70c1L, 0x061fd165ba83cf5bL,
			0x8ae35316bdb7e847L, 0x4687ecb294e836cfL, 0x481703e79760db12L, 0x46709e65499cb0d0L, 0x9203c276ccbed032L,
			0xbcc0efbbb4dc8786L, 0x91fd68c33dc138beL, 0xe7cb559c9a613b14L, 0xc76d679fa63fce13L, 0x491b0e2e4925b785L,
			0x9665d941782dbb65L, 0xf3c2c1255305def3L, 0x23dee45fcbea8c62L, 0x74317a52c0c3eff4L, 0x8cd134505e9c129dL,
			0x0acc534006f8542dL, 0x8a4e9a550bbc2346L, 0x8f8d2f1ef9771a25L, 0x4ad66049d6c9129dL, 0xa6a1e30aed98ecafL,
			0x67ade8097865dfb5L, 0xba457ff2bea1f0ddL, 0x823fbf94b187986bL, 0xd3446a8fa5f508b7L, 0xd612bbd9882a495fL,
			0x6f6f3a391c0bdfc1L, 0x379cf1afd01add45L, 0x4b011c5ca5fb8c4bL, 0xd400da8a25207c13L, 0xcce6fa8eb6705765L,
			0xbc3f130bcd08c6a7L, 0x11d36fd012505df6L, 0x76fffac83d9d4ac5L, 0xa6fb06adb22be032L, 0x57a79d709797c54eL,
			0xfb3dcf1f11ac7ecdL, 0x0bc5f8d961bd0dafL, 0xf6a3c59811eb9196L, 0xa7d15103caecd0afL, 0xd8b5c43d2193bc63L,
			0xdb7cf65513b54176L, 0xae735e05d4c6ed13L, 0xd888e2cae42ec19eL, 0xf068cbb49c46e448L, 0x2196334f5ef057efL,
			0x212ce4ba60f3a9b8L, 0xe7166edc4e45b4c8L, 0xc6a39df281318cc0L, 0x18d445343c36c2cfL, 0x47104973a8e65ba1L,
			0xb6ed5e77046fe9b1L, 0x3c4f8c110f47c05bL, 0x57b10e55a9051ce8L, 0xdd3cde09f998f744L, 0xfaaeee7adb254db5L,
			0x42847075557286dbL, 0x6aa0270dedf9c8d9L, 0x4509b4053855189aL, 0x99760fa8de028397L, 0xc466cae488f5f858L,
			0xd341a7f8e8636c27L, 0x7bc06df31f5d910bL, 0x6fae80096dd1e90eL, 0xaace35936ddabdb3L, 0x30b2c1ad9856c357L,
			0xd1271ee0555d077aL, 0x7981c64c9278c7e3L, 0x11dd43457511d5d6L, 0xb47a3776be16cf50L, 0x42d29be75b8d4ed2L,
			0x63c9ee6d336fcf95L, 0x8127f43010739dd6L, 0xaa124ea5e7911e67L, 0xb7ccdb1fd7f8caf2L, 0x9e263d40a6ba87daL,
			0x6e173a396b27c825L, 0x9f74a153cd5c33f0L, 0xbc8efe908c9fd7d1L, 0x7a28176f1804a6f4L, 0x9cb00ce7cac8bc50L,
			0x45384ff733c0fa02L, 0xc771f769729bf955L, 0x26d2b6d362f0977aL, 0xc68e3ba55cf79d11L, 0x3de691873fa555a0L,
			0xfd0a709d0603c6c9L, 0x286ad7c9603cf3b2L, 0x376fd50777d3ff67L, 0x32d379c8b8a40b0eL, 0x3e2291a82195abd6L,
			0xf138fc5f8ed65442L, 0x63d03cade4b862c7L, 0x45a6a395513e81ebL, 0x6afeeda09da496ddL, 0xed74d6c8b4a9206bL,
			0xa8a9346a7cbdab35L, 0xe6ff0db5165f3c6bL, 0x8ce35f30dd48eb75L, 0x175f5d2012eb5db6L, 0x4238bb404fd6b962L,
			0xe89c39d95198474dL, 0x3dcbe7b590cefffaL, 0x9c50efe0c5ccf481L, 0xf96f30bfdf43db49L, 0x1c4f81d3df57c9ffL,
			0x00d4994c6c000000L, 0x0000000100000000L, 0xdfd6cb3100000000L, 0xcb8f840200000000L, 0xe21020e100000000L,
		};
		
		private static final InputStream stream(long[] array, int length) {
			byte[] buf = new byte[length];
			
			for(int i = 0, p = 0, l = array.length; i < l; ++i) {
				long v = array[i];
				for(int k = 0, n = Long.BYTES, t = Math.min(n, length - p); k < t; ++k, ++p) {
					buf[p] = (byte) ((v >> (n - k - 1) * 8) & 0xff);
				}
			}
			
			return new ByteArrayInputStream(buf);
		}
		
		private static final void loadFont(InputStream stream) {
			try(InputStream is = stream) {
				if(Font.loadFont(is, 0.0) == null) {
					throw new IllegalStateException("Invalid or unsupported font format");
				}
			} catch(IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		
		private static final InputStream mdhps() { return stream(MD_HPS_DAT, MD_HPS_LEN); }
		
		public static final void loadFonts() {
			loadFont(mdhps());
		}
	}
}