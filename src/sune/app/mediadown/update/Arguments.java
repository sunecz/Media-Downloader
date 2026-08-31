package sune.app.mediadown.update;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

import sune.app.mediadown.Shared;
import sune.app.mediadown.os.OS;
import sune.app.mediadown.util.Regex;

/** @since 00.02.09 */
public final class Arguments {
	
	private final Optional<Integer> windowsVersionMajor;
	
	private Arguments(Optional<Integer> windowsVersionMajor) {
		this.windowsVersionMajor = Objects.requireNonNull(windowsVersionMajor);
	}
	
	private static final int windowsBuildNumber() {
		int buildNumber;
		try {
			String value = WindowsRegistry.queryValue(
				"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
				"CurrentBuildNumber"
			);
			
			buildNumber = Integer.parseInt(value);
		} catch(IOException ex) {
			throw new IllegalStateException("Cannot obtain Windows build number");
		} catch(NumberFormatException ex) {
			throw new IllegalStateException("Windows build number is not a number");
		}
		
		return buildNumber;
	}
	
	private static final int windowsVersionMajor(int buildNumber) {
		if(buildNumber >= 22000) return 11;
		if(buildNumber >= 10240) return 10;
		if(buildNumber >= 9200)  return 8;
		if(buildNumber >= 7600)  return 7;
		throw new IllegalStateException("Unsupported Windows build number: " + buildNumber);
	}
	
	private static final Optional<Integer> windowsVersionMajor(OS current) {
		if(!OS.Name.WINDOWS.equals(current.name())) {
			return Optional.empty(); // Only Windows
		}
		
		int versionMajor = windowsVersionMajor(windowsBuildNumber());
		return Optional.of(versionMajor);
	}
	
	public static final Arguments ofCurrent() {
		OS current = OS.current();
		return new Arguments(windowsVersionMajor(current));
	}
	
	public final Map<String, Object> asMap() {
		Map<String, Object> args = new LinkedHashMap<>();
		windowsVersionMajor.ifPresent((v) -> args.put("windows_version_major", v));
		return args;
	}
	
	private static final class WindowsRegistry {
		
		private static final Regex REGEX_KEY_VALUE = Regex.of("^\\s*\\S+\\s+\\S+\\s+(\\S+)\\s*$");
		
		private WindowsRegistry() {
			throw new AssertionError("No instances");
		}
		
		private static final Process newProcess(String... args) throws IOException {
			return new ProcessBuilder(args).redirectErrorStream(true).start();
		}
		
		public static final String queryValue(String key, String name) throws IOException {
			Process process = newProcess("reg", "query", key, "/v", name);
			String output = new String(process.getInputStream().readAllBytes(), Shared.CHARSET);
			
			try {
				if(process.waitFor() != 0) {
					throw new IOException("Failed to query value (" + key + ", " + name + ")");
				}
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IOException(ex);
			}
			
			Matcher matcher = REGEX_KEY_VALUE.matcher(output);
			
			if(!matcher.matches()) {
				throw new IOException("Invalid query value (" + key + ", " + name + "): " + output);
			}
			
			return matcher.group(1);
		}
	}
}
