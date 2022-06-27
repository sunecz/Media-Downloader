package sune.app.mediadown.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sune.api.process.Processes;
import sune.api.process.ReadOnlyProcess;

// Windows SysInternals PsSuspend Tool
public final class PsSuspend {
	
	private static Path file_pssuspend;
	private static final Path ensurePsSuspend() {
		if((file_pssuspend == null)) {
			file_pssuspend = NIO.localPath("resources/binary", "pssuspend.exe"); // Windows-only
			if(!NIO.isRegularFile(file_pssuspend))
				throw new IllegalStateException("pssuspend.exe was not found at: " + file_pssuspend.toAbsolutePath().toString());
			try {
				if(!EULA.isAccepted())
					EULA.accept();
			} catch(IOException ex) {
				throw new IllegalStateException("PsSuspend: EULA cannot be automatically accepted.", ex);
			}
		}
		return file_pssuspend;
	}
	
	private static final class EULA {
		
		private static final String REG_KEY = "HKCU\\Software\\Sysinternals\\PsSuspend";
		
		public static final boolean isAccepted() throws IOException {
			boolean isAccepted = false;
			Process process = Runtime.getRuntime().exec(String.format("reg query %s", REG_KEY));
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			Pattern pattern = Pattern.compile("^\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)$");
			String line;
			while((line = reader.readLine()) != null) {
				Matcher matcher = pattern.matcher(line);
				if((matcher.matches())) {
					String name  = matcher.group(1);
					String value = matcher.group(3);
					isAccepted = name.equals("EulaAccepted") && value.equals("0x1");
				}
			}
			return isAccepted;
		}
		
		public static final void accept() throws IOException {
			Runtime.getRuntime().exec(String.format("reg ADD %s /v EulaAccepted /t REG_DWORD /d 1 /f", REG_KEY));
		}
	}
	
	public static final boolean suspend(long pid) {
		ensurePsSuspend();
		try(ReadOnlyProcess process = Processes.createSynchronous(file_pssuspend)) {
			process.execute(String.format("%d", pid));
			return process.waitFor() == 0;
		} catch(Exception ex) {
			// Ignore
		}
		return false;
	}
	
	public static final boolean resume(long pid) {
		ensurePsSuspend();
		try(ReadOnlyProcess process = Processes.createSynchronous(file_pssuspend)) {
			process.execute(String.format("-r %d", pid));
			return process.waitFor() == 0;
		} catch(Exception ex) {
			// Ignore
		}
		return false;
	}
	
	// Forbid anyone to create an instance of this class
	private PsSuspend() {
	}
}