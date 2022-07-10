package sune.app.mediadown.util;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.input.DataFormat;

/** @since 00.02.07 */
public final class ClipboardWatcher {
	
	private static ClipboardWatcher INSTANCE;
	
	private Listener listener;
	private final ReadOnlyObjectWrapper<ClipboardContents> contents;
	
	private ClipboardWatcher() {
		contents = new ReadOnlyObjectWrapper<>();
	}
	
	private static final Clipboard systemClipboard() {
		return Toolkit.getDefaultToolkit().getSystemClipboard();
	}
	
	public static final ClipboardWatcher instance() {
		return INSTANCE == null ? (INSTANCE = new ClipboardWatcher()) : INSTANCE;
	}
	
	// Convert between AWT's DataFlavor and JavaFX's DataFormat
	private final DataFormat dataFormat(DataFlavor flavor, Transferable contents) {
		if(flavor == DataFlavor.stringFlavor) {
			String maybeURL = Utils.ignore(() -> (String) contents.getTransferData(flavor), "");
			// Also check whether the string is an URL, so a better format is returned
			return Utils.isValidURL(maybeURL) ? DataFormat.URL : DataFormat.PLAIN_TEXT;
		}
		
		if(flavor == DataFlavor.imageFlavor) {
			return DataFormat.IMAGE;
		}
		
		if(flavor == DataFlavor.allHtmlFlavor
				|| flavor == DataFlavor.selectionHtmlFlavor
				|| flavor == DataFlavor.fragmentHtmlFlavor) {
			return DataFormat.HTML;
		}
		
		if(flavor == DataFlavor.javaFileListFlavor) {
			return DataFormat.FILES;
		}
		
		// Use plain text as the default format since it should cause less problems
		return DataFormat.PLAIN_TEXT;
	}
	
	// Cast value in a format to a proper value, if needed
	@SuppressWarnings("unchecked")
	private final Object contentsValue(DataFormat format, Object data) {
		if(format == DataFormat.PLAIN_TEXT
				|| format == DataFormat.RTF) {
			return (String) data;
		} else if(format == DataFormat.URL) {
			return Utils.uri((String) data);
		} else if(format == DataFormat.IMAGE) {
			// Currently, do not convert to JavaFX to not include another module (javafx.swing)
			return (Image) data;
		} else if(format == DataFormat.FILES) {
			return ((List<File>) data).stream()
						.map(File::toPath)
						.collect(Collectors.toList());
		} else return null;
	}
	
	private final void contentsChanged(Clipboard clipboard, Transferable contents) {
		DataFlavor flavor = Stream.of(contents.getTransferDataFlavors())
				.filter(contents::isDataFlavorSupported)
				.findFirst().orElse(null);
		
		if(flavor == null) return; // No content available
		
		try {
			Object data = contents.getTransferData(flavor);
			DataFormat format = dataFormat(flavor, contents);
			Object value = contentsValue(format, data);
			this.contents.set(new ClipboardContents(format, value));
		} catch(UnsupportedFlavorException | IOException ex) {
			// Ignore
		}
	}
	
	public void start() {
		if(listener == null)
			listener = new Listener(systemClipboard());
		listener.start();
	}
	
	public void stop() {
		if(listener == null)
			return;
		listener.stop();
	}
	
	public boolean isActive() {
		return listener != null && listener.running.get();
	}
	
	public ReadOnlyObjectProperty<ClipboardContents> contentsProperty() {
		return contents.getReadOnlyProperty();
	}
	
	/**
	 * Listens for changes in a clipboard.
	 * <br><br>
	 * Sources:
	 * <ul>
	 * 	<li>https://www.coderanch.com/t/377833/java/listen-clipboard</li>
	 * 	<li>https://stackoverflow.com/a/14226456</li>
	 * </ul>
	 * 
	 * @author marc weber
	 * @author Sune
	 */
	private final class Listener implements ClipboardOwner {
		
		private static final long WAIT_MILLIS = 250L;
		
		private final Clipboard clipboard;
		private final AtomicBoolean running = new AtomicBoolean();
		
		public Listener(Clipboard clipboard) {
			this.clipboard = Objects.requireNonNull(clipboard);
		}
		
		private final Transferable contents() {
			return clipboard.getContents(this);
		}
		
		private final void takeOwnership(Transferable contents) {
			clipboard.setContents(contents, this);
		}
		
		@Override
		public void lostOwnership(Clipboard c, Transferable t) {
			if(!running.get()) return;
			Utils.ignore(() -> Thread.sleep(WAIT_MILLIS));
			Transferable contents = contents();
			contentsChanged(c, contents);
			takeOwnership(contents);
		}
		
		public void start() {
			if(running.get())
				return; // Already running
			running.set(true);
			takeOwnership(contents());
		}
		
		public void stop() {
			running.set(false);
		}
	}
	
	public static final class ClipboardContents {
		
		private final DataFormat format;
		private final Object value;
		
		private ClipboardContents(DataFormat format, Object value) {
			this.format = Objects.requireNonNull(format);
			this.value = value;
		}
		
		public DataFormat format() {
			return format;
		}
		
		public <T> T value() {
			return Utils.cast(value);
		}
	}
}