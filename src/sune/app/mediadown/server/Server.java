package sune.app.mediadown.server;

import javafx.scene.image.Image;
import sune.app.mediadown.MediaGetter;

public interface Server extends MediaGetter {
	
	String title();
	String url();
	String version();
	String author();
	Image  icon();
}