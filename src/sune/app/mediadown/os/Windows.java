package sune.app.mediadown.os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.regex.Matcher;

import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Regex;

/** @since 00.02.07 */
final class Windows implements OS {
	
	static Windows INSTANCE = new Windows();
	
	// Forbid anyone to create an instance of this class
	private Windows() {
	}
	
	@Override
	public void highlight(Path path) throws IOException {
		Runtime.getRuntime().exec(new String[] {
			"explorer.exe",
			"/select,", // Mind the comma at the end!
			path.toAbsolutePath().toString()
		});
	}
	
	@Override
	public void browse(URI uri) throws IOException {
		// See: com.sun.javafx.application.HostServicesDelegate$StandaloneHostService::showDocument
		// method for official implementation as of OpenJDK 24.
		Runtime.getRuntime().exec(new String[] {
			"rundll32", "url.dll,FileProtocolHandler", uri.toString()
		});
	}
	
	@Override
	public String executableFileName(String name) {
		return name + ".exe";
	}
	
	@Override
	public Name name() {
		return Name.WINDOWS;
	}
	
	@Override
	public boolean suspendProcess(long pid) {
		return PsSuspend.INSTANCE.suspend(pid);
	}
	
	@Override
	public boolean resumeProcess(long pid) {
		return PsSuspend.INSTANCE.resume(pid);
	}
	
	/** @since 00.02.09 */
	private static enum PsSuspend {
		INSTANCE;
		
		private volatile boolean initialized;
		private Path file;
		
		private final void ensureFile() throws IOException {
			Path path = NIO.localPath("resources/binary", "pssuspend.exe").toAbsolutePath();
			
			if(!NIO.isRegularFile(path)) {
				throw new IOException("Not a regular file");
			}
		}
		
		private final void ensureEULA() throws IOException {
			if(!EULA.isAccepted()) EULA.accept();
		}
		
		private final PsSuspend ready() {
			if(initialized) {
				return this;
			}
			
			synchronized(this) {
				if(!initialized) {
					try {
						ensureFile();
						ensureEULA();
					} catch(IOException ex) {
						throw new UncheckedIOException(ex);
					}
					
					initialized = true;
				}
			}
			
			return this;
		}
		
		private final boolean run(String... args) {
			try {
				String[] finalArgs = new String[1 + args.length];
				finalArgs[0] = file.toString();
				System.arraycopy(args, 0, finalArgs, 1, args.length);
				
				return (
					(new ProcessBuilder(finalArgs))
						.directory(file.getParent().toFile())
						.inheritIO().start()
				).waitFor() == 0;
			} catch(InterruptedException | IOException ex) {
				return false;
			}
		}
		
		public boolean suspend(long pid) {
			return ready().run(Long.toString(pid));
		}
		
		public boolean resume(long pid) {
			return ready().run("-r", Long.toString(pid));
		}
		
		private static final class EULA {
			
			private static final String REG_KEY = "HKCU\\Software\\Sysinternals\\PsSuspend";
			
			public static final boolean isAccepted() throws IOException {
				boolean isAccepted = false;
				Process process = Runtime.getRuntime().exec(new String[] { "reg", "query", REG_KEY });
				
				try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					Regex pattern = Regex.of("^\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)$");
					
					String line;
					for(Matcher matcher; (line = reader.readLine()) != null;) {
						if(!(matcher = pattern.matcher(line)).matches()) {
							continue;
						}
						
						String name  = matcher.group(1);
						String value = matcher.group(3);
						isAccepted = name.equals("EulaAccepted") && value.equals("0x1");
					}
				}
				
				try { process.waitFor(); } catch(InterruptedException ex) {} // Ignore the exit code
				
				return isAccepted;
			}
			
			public static final void accept() throws IOException {
				Process process = Runtime.getRuntime().exec(new String[] {
					"reg", "ADD", REG_KEY, "/v", "EulaAccepted", "/t", "REG_DWORD", "/d", "1", "/f"
				});
				
				try { process.waitFor(); } catch(InterruptedException ex) {} // Ignore the exit code
			}
		}
	}
}
