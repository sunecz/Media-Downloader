package sune.app.mediadown.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** @since 00.02.02 */
public final class SelfProcess {
	
	private SelfProcess() {
	}
	
	private static final void extractCommands(Collection<String> collection, String command) {
		StringBuilder sb = new StringBuilder();
		boolean indq = false;
		boolean insq = false;
		
		for(int i = 0, l = command.length(), c, n; i < l; i += n) {
			c = command.codePointAt(i);
			n = Character.charCount(c);
			
			// Quotes
			if(c == '\"' && !insq) indq = !indq; else
			if(c == '\'' && !indq) insq = !insq;
			// Parsing
			else if(c == ' ' && !(indq || insq)) {
				if(sb.length() > 0) {
					collection.add(sb.toString());
					sb.setLength(0);
				}
			} else {
				sb.appendCodePoint(c);
			}
		}
		
		if(sb.length() > 0) {
			// Add the last left-over string
			collection.add(sb.toString());
		}
	}
	
	private static final Process launchProcess(Path path, String command, Path dir) throws Exception {
		List<String> commands = new ArrayList<>();
		if(path == null) {
			extractCommands(commands, command);
			path = Path.of(commands.get(0)).toAbsolutePath();
		} else {
			commands.add(path.toAbsolutePath().toString());
			extractCommands(commands, command);
		}
		return launchProcess(path, commands, dir);
	}
	
	private static final Process launchProcess(Path path, List<String> commands, Path dir) throws Exception {
		ProcessBuilder builder = new ProcessBuilder(commands)
				.directory((dir != null ? dir : path.getParent()).toFile())
				.redirectErrorStream(true);
		return builder.start();
	}
	
	public static final boolean inJAR() {
		return SelfProcess.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm().endsWith(".jar");
	}
	
	public static final Path jarPath() {
		Path path = Utils.ignore(() -> Path.of(SelfProcess.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
		// Special case for running from a build directory
		if(path != null && !path.getFileName().toString().endsWith(".jar")) {
			path = path.getParent().resolve("jar/media-downloader.jar");
		}
		return path.toAbsolutePath();
	}
	
	public static final Path currentDirectory() {
		return PathSystem.getPath(SelfProcess.class, "");
	}
	
	public static final Path exePath() {
		return Path.of(ProcessHandle.current().info().command().orElseThrow()).toAbsolutePath();
	}
	
	public static final long pid() {
		return ProcessHandle.current().pid();
	}
	
	/** @since 00.02.04 */
	public static final String commandNoExePath(Path jarPath, List<String> arguments) {
		List<String> args = new ArrayList<>();
		if(!inJAR()) {
			args.add("-Dfile.encoding=UTF-8");
			args.add("-p");
			args.add("\"" + System.getProperty("jdk.module.path") + "\"");
			args.add("--add-modules");
			args.add("ALL-SYSTEM");
			args.add("-m");
			args.add("sune.app.mediadown/sune.app.mediadown.App");
		} else {
			args.add("-jar");
			args.add("\"" + (jarPath != null ? jarPath : jarPath()).toString() + "\"");
		}
		args.addAll(arguments);
		return args.stream().reduce("", (a, b) -> a + " " + b).stripLeading();
	}
	
	public static final String commandNoExePath(List<String> arguments) {
		return commandNoExePath(null, arguments);
	}
	
	public static final String command(List<String> arguments) {
		return String.format("\"%s\" %s", exePath(), commandNoExePath(arguments));
	}
	
	public static final Process launch(String command) throws Exception {
		return launchProcess(null, command, currentDirectory());
	}
	
	public static final Process launch(List<String> arguments) throws Exception {
		return launch(exePath(), arguments);
	}
	
	public static final Process launch(Path exePath, List<String> arguments) throws Exception {
		return launchProcess(exePath, commandNoExePath(arguments), currentDirectory());
	}
	
	/** @since 00.02.04 */
	public static final Process launchJAR(Path jarPath, List<String> arguments) throws Exception {
		return launchJAR(jarPath, exePath(), arguments);
	}
	
	/** @since 00.02.04 */
	public static final Process launchJAR(Path jarPath, Path exePath, List<String> arguments) throws Exception {
		return launchProcess(exePath, commandNoExePath(jarPath, arguments), currentDirectory());
	}
}