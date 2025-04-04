package sune.app.mediadown.os;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import sune.app.mediadown.util.FXUtils;

/** @since 00.02.07 */
class Linux implements OS {
	
	private static Linux INSTANCE;
	
	/** @since 00.02.08 */
	private final PathHighlighter pathHighlighter;
	
	// Forbid anyone to create an instance of this class
	private Linux() {
		pathHighlighter = new PathHighlighter();
	}
	
	public static final Linux instance() {
		return INSTANCE == null ? (INSTANCE = new Linux()) : INSTANCE;
	}
	
	@Override
	public void highlight(Path path) throws IOException {
		pathHighlighter.highlight(path);
	}
	
	@Override
	public void browse(URI uri) throws IOException {
		// Delegate to the existing method
		FXUtils.openURI(uri);
	}
	
	/** @since 00.02.08 */
	private static final class PathHighlighter {
		
		private volatile HighlightMethod workingMethod;
		
		private final HighlightMethod selectWorkingMethod(Path path) throws Exception {
			for(HighlightMethod method : HighlightMethod.values()) {
				try {
					method.highlight(path);
					return method;
				} catch(Exception ex) {
					continue;
				}
			}
			
			throw new IllegalStateException("Working highlight method not found");
		}
		
		public void highlight(Path path) throws IOException {
			try {
				if(workingMethod == null) {
					synchronized(this) {
						if(workingMethod == null) {
							workingMethod = selectWorkingMethod(path);
						}
					}
				} else {
					workingMethod.highlight(path);
				}
			} catch(Exception ex) {
				throw new IOException(ex);
			}
		}
		
		private static enum HighlightMethod {
			
			// Note: Do not change the order of the following enum items. The idea is such that
			//       we want to use the built-in method first, if it is supported, if not, then
			//       some common ones, and if all of them fails then the more low-level ones,
			//       such as gdbus or dbus, and even if they fail, just show the parent directory,
			//       which at least should be supported natively in AWT.
			
			AWT {
				
				@Override
				public void highlight(Path path) throws Exception {
					Desktop.getDesktop().browseFileDirectory(path.toAbsolutePath().toFile());
				}
			},
			NAUTILUS {
				
				@Override
				public void highlight(Path path) throws Exception {
					Runtime.getRuntime().exec(new String[] {
						"nautilus", "-s", path.toAbsolutePath().toString()
					});
				}
			},
			GDBUS {
				
				@Override
				public void highlight(Path path) throws Exception {
					// Same as the DBUS method but using the gdbus command which supports
					// commas in the path.
					Runtime.getRuntime().exec(new String[] {
						"gdbus", "call", "--session",
						"--dest", "org.freedesktop.FileManager1",
						"--object-path", "/org/freedesktop/FileManager1",
						"--method", "org.freedesktop.FileManager1.ShowItems",
						"['file://" + path.toAbsolutePath().toString() + "']",
						""
					});
				}
			},
			DBUS {
				
				@Override
				public void highlight(Path path) throws Exception {
					// Source: https://askubuntu.com/a/1424380
					Runtime.getRuntime().exec(new String[] {
						"dbus-send", "--print-reply",
						"--dest=org.freedesktop.FileManager1",
						"/org/freedesktop/FileManager1",
						"org.freedesktop.FileManager1.ShowItems",
						"array:string:\"file://" + path.toAbsolutePath().toString() + "\"",
						"string:\"\""
					});
				}
			},
			XDG_OPEN {
				
				@Override
				public void highlight(Path path) throws Exception {
					Runtime.getRuntime().exec(new String[] {
						"xdg-open", path.getParent().toAbsolutePath().toString()
					});
				}
			},
			OPEN_DIR {
				
				@Override
				public void highlight(Path path) throws Exception {
					Desktop.getDesktop().open(path.getParent().toAbsolutePath().toFile());
				}
			};
			
			public abstract void highlight(Path path) throws Exception;
		}
	}
}