package sune.app.mediadown.util;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
	
	// Inspired by sun.datatransfer.DataFlavorUtil
	private static final String charset(DataFlavor flavor) {
		return Optional.ofNullable(flavor.getParameter("charset")).orElseGet(() -> Charset.defaultCharset().name());
	}
	
	private static final byte[] byteBufferToArray(ByteBuffer buf) {
		if(buf.hasArray()) return buf.array();
		buf.rewind();
		byte[] arr = new byte[buf.remaining()];
		buf.get(arr);
		return arr;
	}
	
	private static final char[] charBufferToArray(CharBuffer buf) {
		if(buf.hasArray()) return buf.array();
		buf.rewind();
		char[] arr = new char[buf.remaining()];
		buf.get(arr);
		return arr;
	}
	
	private static final char[] readerToArray(Reader reader) {
		return Utils.ignore(() -> {
			try(CharArrayWriter writer = new CharArrayWriter()) {
				reader.transferTo(writer);
				return writer.toCharArray();
			}
		}, () -> new char[0]);
	}
	
	public static final ClipboardWatcher instance() {
		return INSTANCE == null ? (INSTANCE = new ClipboardWatcher()) : INSTANCE;
	}
	
	// Convert between AWT's DataFlavor and JavaFX's DataFormat
	private final DataFormat dataFormat(DataFlavor flavor, Transferable contents) {
		if(flavor == DataFlavor.stringFlavor) {
			String maybeURL = Utils.ignore(() -> ensureString(contents.getTransferData(flavor), charset(flavor)), "");
			// Also check whether the string is an URL, so a better format is returned
			return Utils.isValidURL(maybeURL) ? DataFormat.URL : DataFormat.PLAIN_TEXT;
		}
		
		if(flavor == DataFlavor.imageFlavor) {
			return DataFormat.IMAGE;
		}
		
		if(flavor == DataFlavor.javaFileListFlavor) {
			return DataFormat.FILES;
		}
		
		// Use MimeType match test rather than the pre-defined HTML flavors
		if(flavor.isMimeTypeEqual("text/html")) {
			return DataFormat.HTML;
		}
		
		// Use plain text as the default format since it should cause less problems
		return DataFormat.PLAIN_TEXT;
	}
	
	// Some data may be a string but they are represented differently (InputStream, ByteBuffer, ...)
	private final String ensureString(Object data, String charset) {
		Class<?> clazz = data.getClass();
		
		// "Value" types
		if(clazz == String.class) return (String) data;
		if(clazz == byte[].class) return Utils.ignore(() -> new String((byte[]) data, charset));
		if(clazz == char[].class) return new String((char[]) data);
		
		// "Class" types
		if(data instanceof InputStream) return Utils.ignore(() -> new String(((InputStream) data).readAllBytes(), charset));
		if(data instanceof ByteBuffer)  return Utils.ignore(() -> new String(byteBufferToArray((ByteBuffer) data), charset));
		if(data instanceof CharBuffer)  return new String(charBufferToArray((CharBuffer) data));
		if(data instanceof Reader)      return new String(readerToArray((Reader) data));
		
		return null;
	}
	
	// Cast value in a format to a proper value, if needed
	@SuppressWarnings("unchecked")
	private final Object contentsValue(DataFormat format, Object data, DataFlavor flavor) {
		if(format == DataFormat.PLAIN_TEXT
				|| format == DataFormat.HTML
				|| format == DataFormat.RTF) {
			return ensureString(data, charset(flavor));
		} else if(format == DataFormat.URL) {
			return Utils.uri(ensureString(data, charset(flavor)));
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
			Object value = contentsValue(format, data, flavor);
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