package sune.app.mediadown.logging;

import java.io.IOException;
import java.lang.StackWalker.StackFrame;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Utils;

/** @since 00.02.00 */
public final class Log {
	
	/** @since 00.02.08 */
	private static final Path LOG_DIRECTORY = NIO.localPath("resources/log");
	/** @since 00.02.09 */
	private static final Log NOOP = new Log();
	
	private Logger logger;
	
	// Used only for the NOOP log
	/** @since 00.02.09 */
	private Log() {
		this.logger = null;
	}
	
	/** @since 00.02.09 */
	private Log(Logger logger) {
		this.logger = Objects.requireNonNull(logger);
	}
	
	/** @since 00.02.09 */
	public static final Log initialize(String name, Level minLevel) {
		return initialize(name, name, minLevel);
	}
	
	/** @since 00.02.09 */
	public static final Log initialize(String name, String fileName, Level level) {
		return initialize(name, LOG_DIRECTORY.resolve(fileName), level);
	}
	
	/** @since 00.02.09 */
	public static final Log initialize(String name, Path path, Level level) {
		if(name == null || path == null || level == null) {
			throw new NullPointerException();
		}
		
		if(level.intValue() == Level.OFF.intValue()) {
			return NOOP;
		}
		
		Logger logger = Logger.getLogger(name);
		logger.setLevel(level);
		logger.setUseParentHandlers(false);
		
		Handler handler;
		Formatter formatter = new LogRecordFormatter();
		Exception exception = null;
		try {
			NIO.createDir(path.getParent());
			handler = new FileHandler(path.toAbsolutePath().toString());
			handler.setFormatter(formatter);
		} catch(SecurityException | IOException ex) {
			handler = new StreamHandler(System.out, formatter) {
				
				@Override
				public synchronized void publish(LogRecord record) {
					super.publish(record);
					flush(); // Auto-flush
				}
			};
			exception = ex;
		}
		
		logger.addHandler(handler);
		Log log = new Log(logger);
		
		if(exception != null) {
			log.error(exception, "Failed to initialize logging to a file. Defaulting to standard output.");
		}
		
		log.info("Logger initialized.");
		return log;
	}
	
	private final void log(Level level, Throwable throwable, String message, Object... args) {
		if(logger == null) {
			return; // Noop
		}
		
		String sourceClass = null;
		String sourceMethod = null;
		
		StackFrame stackFrame = SourceFinder.stackFrame();
		if(stackFrame != null) {
			sourceClass = stackFrame.getClassName();
			sourceMethod = stackFrame.getMethodName();
		}
		
		if(args.length > 0) {
			message = String.format(message, args);
		}
		
		logger.logp(level, sourceClass, sourceMethod, message, throwable);
	}
	
	public final void debug(String message, Object... args) {
		debug(null, message, args);
	}
	
	public final void debug(Throwable throwable, String message, Object... args) {
		log(Level.FINE, throwable, message, args);
	}
	
	public final void info(String message, Object... args) {
		info(null, message, args);
	}
	
	public final void info(Throwable throwable, String message, Object... args) {
		log(Level.INFO, throwable, message, args);
	}
	
	public final void warning(String message, Object... args) {
		warning(null, message, args);
	}
	
	public final void warning(Throwable throwable, String message, Object... args) {
		log(Level.WARNING, throwable, message, args);
	}
	
	public final void error(String message, Object... args) {
		error(null, message, args);
	}
	
	public final void error(Throwable throwable, String message, Object... args) {
		log(Level.SEVERE, throwable, message, args);
	}
	
	/** @since 00.02.08 */
	public final void toPath(Path path, String message, Object... args) throws IOException {
		NIO.save(path, args.length == 0 ? message : String.format(message, args));
	}
	
	/** @since 00.02.08 */
	public final Path toUniquePath(String prefix, String message, Object... args)
			throws IOException {
		return toUniquePath(LOG_DIRECTORY, prefix, message, args);
	}
	
	/** @since 00.02.08 */
	public final Path toUniquePath(Path directory, String prefix, String message, Object... args)
			throws IOException {
		Path path = NIO.uniqueFile(directory, prefix, ".log");
		toPath(path, message, args);
		return path;
	}
	
	private static final class LogRecordFormatter extends Formatter {
		
		private static final ZoneId ZONE_ID = ZoneId.from(ZoneOffset.UTC);
		private static final String FORMAT  = "%1$s (%3$s) %2$s\n%4$s: %5$s%6$s\n";
		
		private static final int LEVEL_DEBUG   = Level.FINE.intValue();
		private static final int LEVEL_INFO    = Level.INFO.intValue();
		private static final int LEVEL_WARNING = Level.WARNING.intValue();
		private static final int LEVEL_ERROR   = Level.SEVERE.intValue();
		
		private static final Map<Integer, String> LEVELS_NAMES = Utils.toMap(
			LEVEL_DEBUG,   "DEBUG",
			LEVEL_INFO,    "INFO",
			LEVEL_WARNING, "WARNING",
			LEVEL_ERROR,   "ERROR"
		);
		
		// Forbid anyone to create an instance of this class
		private LogRecordFormatter() {
		}
		
		private static final String getLevelName(Level level) {
			return LEVELS_NAMES.getOrDefault(level.intValue(), "UNKNOWN");
		}
		
		@Override
		public String format(LogRecord record) {
			ZonedDateTime zdt = ZonedDateTime.ofInstant(record.getInstant(), ZONE_ID);
			String dateTime = DateTimeFormatter.ISO_DATE_TIME.format(zdt);
			
			String source;
			if(record.getSourceClassName() != null) {
				source = record.getSourceClassName();
				
				if(record.getSourceMethodName() != null) {
					source += "::" + record.getSourceMethodName();
				}
			} else {
				source = record.getLoggerName();
			}
			
			String message = formatMessage(record);
			String loggerName = record.getLoggerName();
			String level = getLevelName(record.getLevel());
			Throwable thrown = record.getThrown();
			String throwable = "";
			
			if(thrown != null) {
				throwable = '\n' + Utils.throwableToString(thrown);
			}
			
			return String.format(FORMAT, dateTime, source, loggerName, level, message, throwable);
		}
	}
	
	private static final class SourceFinder {
		
		private static final StackWalker WALKER
			= StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		
		// Forbid anyone to create an instance of this class
		private SourceFinder() {
		}
		
		private static final boolean filter(StackFrame frame) {
			Class<?> c = frame.getDeclaringClass();
			return c != Log.class && c != SourceFinder.class;
		}
		
		/** @since 00.02.08 */
		public static final StackFrame stackFrame() {
			return WALKER.walk((s) -> s.filter(SourceFinder::filter).findFirst()).orElse(null);
		}
	}
}