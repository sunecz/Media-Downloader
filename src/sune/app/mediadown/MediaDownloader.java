package sune.app.mediadown;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import sune.app.mediadown.configuration.ApplicationConfiguration;
import sune.app.mediadown.configuration.ApplicationConfigurationAccessor;
import sune.app.mediadown.event.EventSupport;
import sune.app.mediadown.event.EventSupport.CompatibilityEventRegistry;
import sune.app.mediadown.gui.Dialog;
import sune.app.mediadown.gui.DialogWindow;
import sune.app.mediadown.gui.Menu;
import sune.app.mediadown.gui.Window;
import sune.app.mediadown.gui.window.ClipboardWatcherWindow;
import sune.app.mediadown.gui.window.ConfigurationWindow;
import sune.app.mediadown.gui.window.DownloadConfigurationWindow;
import sune.app.mediadown.gui.window.InformationWindow;
import sune.app.mediadown.gui.window.MainWindow;
import sune.app.mediadown.gui.window.MediaGetterWindow;
import sune.app.mediadown.gui.window.MediaInfoWindow;
import sune.app.mediadown.gui.window.MessageWindow;
import sune.app.mediadown.gui.window.PreviewWindow;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.initialization.InitializationState;
import sune.app.mediadown.language.Language;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.library.NativeLibraries;
import sune.app.mediadown.library.NativeLibraries.NativeLibraryLoadListener;
import sune.app.mediadown.library.NativeLibrary;
import sune.app.mediadown.logging.Log;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaTitleFormat;
import sune.app.mediadown.plugin.PluginFile;
import sune.app.mediadown.plugin.PluginLoadListener;
import sune.app.mediadown.plugin.PluginUpdater;
import sune.app.mediadown.plugin.Plugins;
import sune.app.mediadown.registry.NamedRegistry;
import sune.app.mediadown.registry.ResourceNamedRegistry;
import sune.app.mediadown.registry.ResourceNamedRegistry.ResourceRegistryEntry;
import sune.app.mediadown.registry.SimpleNamedRegistry;
import sune.app.mediadown.resource.ExternalResources;
import sune.app.mediadown.resource.Extractable;
import sune.app.mediadown.resource.InputStreamResolver;
import sune.app.mediadown.resource.JRE;
import sune.app.mediadown.resource.JRE.JREEvent;
import sune.app.mediadown.resource.ResourceRegistry;
import sune.app.mediadown.resource.Resources;
import sune.app.mediadown.resource.Resources.StringReceiver;
import sune.app.mediadown.theme.Theme;
import sune.app.mediadown.update.CheckListener;
import sune.app.mediadown.update.FileCheckListener;
import sune.app.mediadown.update.FileChecker;
import sune.app.mediadown.update.FileDownloadListener;
import sune.app.mediadown.update.FileDownloader;
import sune.app.mediadown.update.RemoteConfiguration;
import sune.app.mediadown.update.RemoteConfiguration.Property;
import sune.app.mediadown.update.Requirements;
import sune.app.mediadown.update.Updater;
import sune.app.mediadown.update.Version;
import sune.app.mediadown.util.CSSParser;
import sune.app.mediadown.util.CSSParser.CSS;
import sune.app.mediadown.util.CSSParser.CSSProperty;
import sune.app.mediadown.util.CSSParser.CSSRule;
import sune.app.mediadown.util.CheckedFunction;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.IllegalAccessWarnings;
import sune.app.mediadown.util.MathUtils;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.PathSystem;
import sune.app.mediadown.util.Reflection2;
import sune.app.mediadown.util.Reflection3;
import sune.app.mediadown.util.SelfProcess;
import sune.app.mediadown.util.Threads;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Web;
import sune.app.mediadown.util.Web.GetRequest;
import sune.app.mediadown.util.Web.StreamResponse;
import sune.util.load.Libraries;
import sune.util.load.Libraries.Library;
import sune.util.load.Libraries.LibraryLoadListener;
import sune.util.load.ModuleUtils;
import sune.util.ssdf2.SSDAnnotation;
import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDF;
import sune.util.ssdf2.SSDNode;
import sune.util.ssdf2.SSDObject;

public final class MediaDownloader {
	
	private static final boolean DEBUG = false;
	private static final boolean GENERATE_LISTS = false;
	
	public static final String  TITLE   = "Media Downloader";
	public static final Version VERSION = Version.fromString("00.02.07-dev.5");
	public static final String  DATE    = "2022-07-10";
	public static final String  AUTHOR  = "Sune";
	public static final Image   ICON    = icon("app.png");
	
	private static final String URL_BASE     = "https://app.sune.tech/mediadown/";
	private static final String URL_BASE_VER = URL_BASE + "ver/";
	private static final String URL_BASE_LIB = URL_BASE + "lib/";
	private static final String URL_BASE_DAT = URL_BASE + "dat/";
	
	private static ApplicationConfigurationWrapper configuration;
	private static boolean applicationUpdated;
	private static boolean forceCheckPlugins;
	/** @since 00.02.02 */
	private static Arguments arguments;
	/** @since 00.02.02 */
	private static String jreVersion;
	
	private static final AtomicBoolean isDisposed = new AtomicBoolean();
	private static final String BASE_RESOURCE = "/resources/";
	
	private static final int TIMEOUT = 8000;
	
	private static final InputStream stream(String base, String path) {
		return MediaDownloader.class.getResourceAsStream(base + path);
	}
	
	private static final Image icon(String path) {
		return new Image(MediaDownloader.class.getResourceAsStream("/resources/icon/" + path));
	}
	
	/** @since 00.02.00 */
	private static final class InitializationStates {
		
		public static final InitializationState FIRST_STATE = new InternalInitialization();
		private static volatile StartupWindow window;
		private static int classesCount = -1;
		
		public static final void init(int total) {
			FXUtils.init(() -> (window = new StartupWindow(TITLE, total)).show());
		}
		
		public static final void close() {
			if(window != null)
				FXUtils.thread(window::close);
		}
		
		public static final void update(String text) {
			if(window != null && text != null)
				window.update(text);
		}
		
		public static final void setText(String text) {
			if(window != null)
				window.setText(text);
		}
		
		public static final void setTotal(int total) {
			if(window != null)
				window.setTotal(total);
		}
		
		public static final void setProgress(double progress) {
			if(window != null)
				window.setProgress(progress);
		}
		
		public static final double getProgress() {
			return window != null ? window.getProgress() : Double.NaN;
		}
		
		public static final void updateTotal(boolean countPlugins) {
			setTotal(count(countPlugins));
		}
		
		private static final int count(boolean countPlugins) {
			if(classesCount < 0) {
				classesCount = 0;
				for(Class<?> clazz : InitializationStates.class.getClasses()) {
					try {
						if(clazz.getMethod("getTitle").getDeclaringClass() != InitializationState.class)
							++classesCount;
					} catch(NoSuchMethodException | SecurityException ex) {
						// Ignore
					}
				}
			}
			classesCount += NativeLibraries.all().size();
			classesCount += Libraries.all().size();
			classesCount += countPlugins ? Plugins.all().size() : 0;
			return classesCount;
		}
		
		public static final class InternalInitialization implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				initExceptionHandlers();
				disableIllegalAccessWarnings();
				initAutoDispose();
				return new ShowStartupWindow();
			}
		}
		
		public static final class ShowStartupWindow implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				int count = count(false);
				if(!args.has("no-startup-gui")) {
					init(count);
				}
				return new CheckJRE();
			}
		}
		
		// Update the JRE, if needed, as soon as possible, since some libraries and/or plugins
		// may rely on it.
		/** @since 00.02.02 */
		public static final class CheckJRE implements InitializationState {
			
			private static final Path oldJREPath()  { return PathSystem.getPath("jre"); }
			private static final Path newJREPath()  { return PathSystem.getPath("jre-new"); }
			private static final Path versionPath() { return PathSystem.getPath("resources/jre_version"); }
			
			@Override
			public InitializationState run(Arguments args) {
				try {
					// Obtain the required JRE version from the remote configuration
					jreVersion = remoteConfiguration().value("jre");
					if(args.has("jre-update") && args.has("pid")) {
						long pid = Long.valueOf(args.getValue("pid"));
						if(pid <= 0L) throw new IllegalStateException("Invalid PID");
						setText("Waiting for process to finish...");
						// Get the parent process
						ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
						// Check whether the old process still exists
						if(handle != null) {
							// Wait for it to finish
							handle.onExit().get();
						}
						// Move the directories around so that the new JRE is in the correct location
						Path oldJREPath = oldJREPath();
						setText("Deleting old JRE...");
						NIO.deleteDir(oldJREPath);
						setText("Copying new JRE...");
						NIO.copyDir(newJREPath(), oldJREPath);
						// Update the contents of the JRE version file
						NIO.save(versionPath(), jreVersion);
						// Launch the previous process again
						setText("Launching application using the new JRE...");
						String runCommand = args.getValue("run-command");
						runCommand = new String(Base64.getDecoder().decode(runCommand), Shared.CHARSET);
						runCommand += " --jre-update-finish";
						SelfProcess.launch(runCommand);
						// Exit normally
						System.exit(0);
					} else if(args.has("jre-update-finish")) {
						// Finish the whole JRE update process by deleting the temporary JRE directory
						NIO.deleteDir(newJREPath());
					} else {
						Path path = versionPath();
						// Check the current JRE version and update, if necessary
						if(!NIO.exists(path) || !NIO.read(path).equals(jreVersion)) {
							JRE jre = JRE.newInstance();
							jre.addEventListener(JREEvent.CHECK, (context) -> {
								setText(String.format("Checking %s...", context.name()));
							});
							jre.addEventListener(JREEvent.DOWNLOAD_BEGIN, (context) -> {
								setText(String.format("Downloading %s...", context.path().getFileName().toString()));
							});
							jre.addEventListener(JREEvent.DOWNLOAD_UPDATE, (context) -> {
								setText(String.format("Downloading %s... %s%%", context.path().getFileName().toString(),
								                      MathUtils.round(context.tracker().getProgress() * 100.0, 2)));
							});
							jre.addEventListener(JREEvent.DOWNLOAD_END, (context) -> {
								setText(String.format("Downloading %s... done", context.path().getFileName().toString()));
							});
							jre.addEventListener(JREEvent.ERROR, (context) -> {
								error(context.exception());
							});
							Path oldJREPath = oldJREPath();
							Path newJREPath = newJREPath();
							Set<Path> visitedFiles = new HashSet<>();
							// Check the files and if any is changed, continue the process
							if(jre.check(oldJREPath, newJREPath, Requirements.CURRENT, jreVersion, visitedFiles)) {
								// Copy all files that were not updated and exist on the web (were visited)
								NIO.mergeDirectories(oldJREPath, newJREPath, (p, np) -> visitedFiles.contains(p) && !NIO.exists(np));
								// Get the current run command, so that the application can be run again
								String runCommand = SelfProcess.command(List.of(args.args()));
								runCommand = Base64.getEncoder().encodeToString(runCommand.getBytes(Shared.CHARSET));
								// Get Java executable in the new directory
								Path exePath = SelfProcess.exePath();
								// Check whether the current process was run in the old JRE directory
								Path parent = exePath;
								while((parent = parent.getParent()) != null && !parent.equals(oldJREPath));
								// If run in the old JRE directory, change the new executable path to the new JRE directory
								if(parent != null && parent.equals(oldJREPath)) {
									exePath = newJREPath.resolve(oldJREPath.relativize(exePath));
								}
								// Start a new process to finish updating the JRE
								SelfProcess.launch(exePath, List.of(
									"--jre-update",
									"--pid", String.valueOf(SelfProcess.pid()),
									"--run-command", runCommand
								));
								// Exit normally
								System.exit(0);
							} else {
								// No files changed, just update the contents of the JRE version file
								NIO.save(versionPath(), jreVersion);
								// Also delete the empty new JRE directory (from checking)
								NIO.deleteDir(newJREPath);
							}
						}
					}
				} catch(Exception ex) {
					error(ex);
				}
				return new RegistrationOfLibrariesAndResources();
			}
		}
		
		public static final class RegistrationOfLibrariesAndResources implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				registerNativeLibraries();
				registerLibraries();
				registerResources();
				updateTotal(false);
				return new MaybeListGeneration();
			}
		}
		
		public static final class MaybeListGeneration implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				if(GENERATE_LISTS) {
					generateList();
					generateResourcesList("original");
					generateResourcesList("compressed");
					generateJREList();
					close();
					return null; // Do not continue
				}
				return new CheckLibraries();
			}
		}
		
		public static final class CheckLibraries implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				Update.checkLibraries(new CheckListener() {
					
					@Override
					public void begin() {
					}
					
					@Override
					public void compare(String name) {
						setText(String.format("Checking library %s...", name));
					}
					
					@Override
					public void end() {
					}
					
					@Override
					public FileCheckListener fileCheckListener() {
						return new FileCheckListener() {
							
							private final Path rootDir = Paths.get(PathSystem.getCurrentDirectory());
							
							@Override
							public void begin(Path dir) {
							}
							
							@Override
							public void update(Path file, String hash) {
								String path = file.subpath(rootDir.getNameCount(), file.getNameCount()).toString();
								setText(String.format("Checking %s...%s", path, hash != null ? " done" : ""));
							}
							
							@Override
							public void end(Path dir) {
							}
							
							@Override
							public void error(Exception ex) {
								setText(String.format("Checking... Error"));
							}
						};
					}
					
					@Override
					public FileDownloadListener fileDownloadListener() {
						return new FileDownloadListener() {
							
							private final Path rootDir = Paths.get(PathSystem.getCurrentDirectory());
							private double pvalue;
							
							@Override
							public void begin(String url, Path file) {
								String path = file.subpath(rootDir.getNameCount(), file.getNameCount()).toString();
								setText(String.format("Downloading library %s...", path));
								pvalue = getProgress();
								setProgress(0.0);
							}
							
							@Override
							public void update(String url, Path file, long current, long total) {
								double percent0 = (current / (double) total);
								double percent1 = (percent0 * 100.0);
								String path = file.subpath(rootDir.getNameCount(), file.getNameCount()).toString();
								setText(String.format("Downloading library %s... %s%%", path, MathUtils.round(percent1, 2)));
								setProgress(percent0);
							}
							
							@Override
							public void end(String url, Path file) {
								String path = file.subpath(rootDir.getNameCount(), file.getNameCount()).toString();
								setText(String.format("Downloading library %s... Done", path));
								setProgress(pvalue);
							}
							
							@Override
							public void error(Exception ex) {
								setText(String.format("Downloading library... Error"));
								setProgress(pvalue);
							}
						};
					}
				});
				return new LoadNativeLibraries();
			}
			
			@Override public String getTitle() { return "Checking libraries..."; }
		}
		
		public static final class LoadNativeLibraries implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				NativeLibraries.load(new NativeLibraryLoadListener() {
					
					@Override
					public void onLoading(NativeLibrary library) {
						setText(String.format("Loading native library %s (%s)...",
						                      library.getName(), library.getPath().getFileName().toString()));
					}
					
					@Override
					public void onLoaded(NativeLibrary library, boolean success, Throwable exception) {
						update(String.format("Loading native library %s (%s)... %s",
						                     library.getName(), library.getPath().getFileName().toString(),
						                     success ? "done" : "error"));
					}
					
					@Override
					public void onNotLoaded(NativeLibrary[] libraries) {
						String text = String.format("Cannot load native libraries (%d)", libraries.length);
						StringBuilder content = new StringBuilder();
						for(NativeLibrary library : libraries) {
							content.append(String.format("%s (%s)\n", library.getName(), library.getPath()));
						}
						Dialog.showContentError("Critical error", text, content.toString());
						System.exit(-1);
					}
				});
				return new LoadLibraries();
			}
			
			@Override public String getTitle() { return "Loading native libraries..."; }
		}
		
		public static final class LoadLibraries implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				Libraries.load(new LibraryLoadListener() {
					
					@Override
					public void onLoading(Library library) {
						setText(String.format("Loading library %s...", library.getName()));
					}
					
					@Override
					public void onLoaded(Library library, boolean success) {
						update(String.format("Loading library %s... %s", library.getName(), success ? "done" : "error"));
					}
					
					@Override
					public void onNotLoaded(Library[] libraries) {
						String text = String.format("Cannot load libraries (%d)", libraries.length);
						StringBuilder content = new StringBuilder();
						for(Library library : libraries) {
							content.append(String.format("%s (%s)\n", library.getName(), library.getPath()));
						}
						Dialog.showContentError("Critical error", text, content.toString());
						System.exit(-1);
					}
				});
				return new MaybeDisposeOfExternalResources();
			}
			
			@Override public String getTitle() { return "Loading libraries..."; }
		}
		
		public static final class MaybeDisposeOfExternalResources implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				if(DEBUG) {
					disposeExternalResources();
				}
				return new InitializeConfiguration();
			}
		}
		
		public static final class InitializeConfiguration implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				initConfiguration();
				return new InitializeInternalResources();
			}
			
			@Override public String getTitle() { return "Initializing configuration..."; }
		}
		
		public static final class InitializeInternalResources implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				InternalResources.initializeDefaults();
				InternalResources.ensure();
				return new LoadExternalResources();
			}
			
			@Override public String getTitle() { return "Initializing internal resources..."; }
		}
		
		public static final class LoadExternalResources implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				loadExternalResources();
				return new CheckExternalResources();
			}
			
			@Override public String getTitle() { return "Initializing external resources..."; }
		}
		
		public static final class CheckExternalResources implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				checkExternalResources();
				addAutomaticLanguage();
				return new InitializeMiscellaneousResources();
			}
			
			@Override public String getTitle() { return "Checking external resources..."; }
		}
		
		public static final class InitializeMiscellaneousResources implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				loadMiscellaneousResources(InitializationStates::setText);
				return new FinalizeConfiguration();
			}
			
			@Override public String getTitle() { return "Initializating miscellaneous resources..."; }
		}
		
		public static final class FinalizeConfiguration implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				finalizeConfiguration();
				return new CheckVersion();
			}
		}
		
		public static final class CheckVersion implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				if(Update.isAutoUpdateCheckEnabled()) {
					Update.update(args);
				}
				return new RegisterWindows();
			}
			
			@Override public String getTitle() { return "Checking new versions..."; }
		}
		
		public static final class RegisterWindows implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				if(FXUtils.isInitialized())
					GUI.registerWindows();
				return new RegisterDialogs();
			}
			
			@Override public String getTitle() { return "Registering windows..."; }
		}
		
		public static final class RegisterDialogs implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				if(FXUtils.isInitialized())
					GUI.registerDialogs();
				return new RegisterMenus();
			}
			
			@Override public String getTitle() { return "Registering dialogs..."; }
		}
		
		public static final class RegisterMenus implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				if(FXUtils.isInitialized())
					GUI.registerMenus();
				return new InitializeDefaultPlugins();
			}
			
			@Override public String getTitle() { return "Registering menus..."; }
		}
		
		public static final class InitializeDefaultPlugins implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				initDefaultPlugins();
				return new InitializePlugins();
			}
			
			@Override public String getTitle() { return "Initializing default plugins..."; }
		}
		
		public static final class InitializePlugins implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				setProgress(PROGRESS_INDETERMINATE);
				registerPlugins();
				updateTotal(true);
				loadPlugins(new PluginLoadListener() {
					
					@Override
					public void onLoading(PluginFile plugin) {
						setText("Loading plugin " + plugin.getPlugin().instance().name() + "...");
					}
					
					@Override
					public void onLoaded(PluginFile plugin, boolean success) {
						update("Loading plugin " + plugin.getPlugin().instance().name() + "... " + (success ? "done" : "error"));
					}
					
					@Override
					public void onNotLoaded(PluginFile[] pluginFiles) {
						String text = "Cannot load plugins (" + pluginFiles.length + ")";
						StringBuilder content = new StringBuilder();
						for(PluginFile plugin : pluginFiles) {
							content.append(plugin.getPlugin().instance().name());
							content.append(" (");
							content.append(plugin.getPath());
							content.append(")\n");
						}
						if(FXUtils.isInitialized()) Dialog.showContentError("Error", text, content.toString());
						else System.err.println(text + "\n" + content.toString()); // FX not available, print to stderr
					}
				});
				return new MaybeRunStandalonePlugin();
			}
			
			@Override public String getTitle() { return "Initializing plugins..."; }
		}
		
		/** @since 00.02.02 */
		public static final class MaybeRunStandalonePlugin implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				String pluginName, className;
				if((pluginName = args.getValue("plugin")) != null
						&& (className = args.getValue("class")) != null) {
					// Check whether the plugin was successfully loaded
					if(Plugins.allLoaded().stream()
							.filter((p) -> p.getPlugin().instance().name().equals(pluginName))
							.findFirst().isPresent()) {
						// Check whether the class actually exists
						Class<?> clazz = Reflection2.getClass(className);
						if(clazz != null) {
							// Call the main method in that class
							try {
								Reflection3.invoke(null, clazz, "run", new Object[] { args.args() });
							} catch(Exception ex) {
								// Just print exception and exit
								ex.printStackTrace();
								System.exit(255);
							}
						}
					}
					return null; // Do not continue
				}
				return new InitializationDone();
			}
		}
		
		public static final class InitializationDone implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				FXUtils.thread(() -> {
					window("main").show();
					close();
				});
				return null;
			}
			
			@Override public String getTitle() { return "Initialization done"; }
		}
	}
	
	public static final void initialize(String[] args) {
		arguments = Arguments.parse(args);
		Log.initialize(Level.ALL);
		for(InitializationState state = InitializationStates.FIRST_STATE;
				state != null;
				state = state.run(arguments)) {
			InitializationStates.update(state.getTitle());
		}
	}
	
	/** @since 00.02.02 */
	public static final Arguments arguments() {
		return arguments;
	}
	
	private static final class Update {
		
		private static final StringReceiver receiver = InitializationStates::setText;
		private static final String NAME_JAR = "media-downloader.jar";
		private static final String NAME_JAR_NEW = "media-downloader-new.jar";
		
		private static Version newestVersion;
		
		/** @since 00.02.07 */
		private static final String versionFileURI() {
			return URL_BASE_VER + "version" + (configuration.usePreReleaseVersions() ? "_pre" : "");
		}
		
		/** @since 00.02.07 */
		private static final String remoteJARFileName() {
			return (configuration.usePreReleaseVersions() ? "pre-release/" : "") + "application.jar";
		}
		
		public static final boolean isAutoUpdateCheckEnabled() {
			return configuration.isAutoUpdateCheck();
		}
		
		/** @since 00.02.04 */
		public static final void update(Arguments args) {
			if(args.has("is-jar-update") || (checkVersion() && showUpdateDialog())) {
				try {
					Path reqJAR = PathSystem.getPath(NAME_JAR);
					Path newJAR = PathSystem.getPath(NAME_JAR_NEW);
					if(args.has("jar-update") && args.has("pid")) {
						long pid = Long.valueOf(args.getValue("pid"));
						if(pid <= 0L) throw new IllegalStateException("Invalid PID");
						receiver.receive("Waiting for the previous process to finish...");
						// Get the parent process
						ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
						// Check whether the old process still exists
						if(handle != null) {
							// Wait for it to finish
							handle.onExit().get();
						}
						// Copy the new (current) JAR file to the required one
						receiver.receive("Replacing the old JAR file...");
						NIO.copyFile(newJAR, reqJAR);
						// Launch the previous process again
						receiver.receive("Launching the new version...");
						String runCommand = args.getValue("run-command");
						runCommand = new String(Base64.getDecoder().decode(runCommand), Shared.CHARSET);
						runCommand += " --jar-update-finish";
						runCommand += " --is-jar-update";
						SelfProcess.launch(runCommand);
						// Exit normally
						System.exit(0);
					} else if(args.has("jar-update-finish")) {
						// Finish the whole JAR update process by deleting the new JAR version
						receiver.receive("Deleting the temporary JAR file...");
						NIO.deleteFile(newJAR);
					} else {
						Version newestVersion = newestVersion();
						String  newestVersionURL = URL_BASE_VER + newestVersion.stringRelease() + "/" + remoteJARFileName();
						// Create the download listener using the new event registry support
						CompatibilityEventRegistry<FileDownloadListener> eventRegistry
							= EventSupport.compatibilityEventRegistry(FileDownloadListener.class);
						eventRegistry.add(eventRegistry.typeOf("begin"), (argsWrapper) -> {
							receiver.receive("Downloading the new version...");
						});
						eventRegistry.add(eventRegistry.typeOf("update"), (argsWrapper) -> {
							long current = argsWrapper.get(2);
							long total = argsWrapper.get(3);
							receiver.receive(String.format(Locale.US, "Downloading the new version... %.2f%%", current * 100.0 / total));
						});
						eventRegistry.add(eventRegistry.typeOf("end"), (argsWrapper) -> {
							receiver.receive("Downloading the new version... done");
						});
						eventRegistry.add(eventRegistry.typeOf("error"), (argsWrapper) -> {
							Exception ex = argsWrapper.get(0);
							MediaDownloader.error(ex);
						});
						// Download the new version's JAR file
						FileDownloader.download(newestVersionURL, newJAR, eventRegistry.proxy());
						// Get the current run command, so that the application can be run again
						String runCommand = SelfProcess.command(List.of(args.args()));
						runCommand = Base64.getEncoder().encodeToString(runCommand.getBytes(Shared.CHARSET));
						Path exePath = SelfProcess.exePath();
						// Start a new process to finish updating the application
						SelfProcess.launchJAR(newJAR, exePath, List.of(
							"--jar-update",
							"--pid", String.valueOf(SelfProcess.pid()),
							"--run-command", runCommand,
							"--is-jar-update"
						));
						// Exit normally
						System.exit(0);
					}
				} catch(Exception ex) {
					error(ex);
				}
			}
		}
		
		public static final void checkLibraries(CheckListener listener) {
			try {
				String baseURL = Utils.urlConcat(URL_BASE_LIB, remoteConfiguration().value("lib"));
				Updater.checkLibraries(baseURL, NIO.localPath(), TIMEOUT, listener);
			} catch(IOException ex) {
				// Config cannot be accessed, just skip it
			}
		}
		
		public static final boolean checkVersion() {
			Version newestVersion = newestVersion();
			return  newestVersion != null && Updater.compare(VERSION, newestVersion);
		}
		
		public static final Version newestVersion() {
			return newestVersion(false);
		}
		
		public static final Version newestVersion(boolean forceGet) {
			if(newestVersion == null
					// The version can be obtained again once set, if needed
					|| forceGet) {
				newestVersion = Version.fromURL(versionFileURI(), TIMEOUT);
			}
			return newestVersion;
		}
		
		public static final boolean showUpdateDialog() {
			// If pre-release versions should be used, automatically accept the update.
			// This is due to the fact that all pre-release versions share configuration
			// and sometimes there can be incompatibilities, so just always use
			// the latest pre-release version.
			return configuration.usePreReleaseVersions()
						|| FXUtils.fxTaskValue(() -> new UpdateDialog().wasAccepted());
		}
		
		private static final class UpdateDialog extends Alert {
			
			static final String TITLE;
			static final String TEXT;
			static final ButtonType[] BUTTONS;
			static {
				TITLE   = "Update available";
				TEXT    = "A new version is available! Do you want to update the application?";
				BUTTONS = new ButtonType[] {
					ButtonType.YES,
					ButtonType.NO
				};
			}
			
			public UpdateDialog() {
				super(AlertType.CONFIRMATION);
				Stage stage = (Stage) getDialogPane().getScene().getWindow();
				stage.getIcons().setAll(ICON);
				setHeaderText(null);
				setTitle(TITLE);
				setContentText(TEXT);
				getButtonTypes().setAll(BUTTONS);
			}
			
			public boolean wasAccepted() {
				return showAndWait().get() == ButtonType.YES;
			}
		}
	}
	
	private static RemoteConfiguration remoteConfiguration;
	
	public static final RemoteConfiguration remoteConfiguration() {
		if(remoteConfiguration == null) {
			try {
				String configURL = Utils.urlConcat(URL_BASE_VER, VERSION.stringRelease(), "config");
				remoteConfiguration = RemoteConfiguration.from(Utils.urlStream(configURL, TIMEOUT));
			} catch(IOException ex) {
				error(ex);
			}
		}
		return remoteConfiguration;
	}
	
	public static final FileChecker localFileChecker(boolean checkRequirements) {
		Path currentDir = NIO.localPath();
		Path dir = NIO.localPath("lib/");
		FileChecker checker = new FileChecker.PrefixedFileChecker(dir, null, currentDir);
		// Generate list of all native libraries to check
		for(NativeLibrary library : NativeLibraries.all()) {
			Requirements requirements = Requirements.create(library.getOSName(), library.getOSArch());
			checker.addEntry(library.getPath(), requirements, library.getVersion());
		}
		// Generate list of all libraries to check
		for(Library library : Libraries.all()) {
			checker.addEntry(library.getPath(), Requirements.ANY, "");
		}
		// Generate the list of entries
		return checker.generate((path) -> true, checkRequirements, true) ? checker : null;
	}
	
	protected static final void generateList() {
		FileChecker checker = localFileChecker(false);
		if((checker != null)) {
			try {
				// Save the list of entries to a file
				NIO.save(NIO.localPath("list.sha1"), checker.toString());
			} catch(IOException ex) {
				error(ex);
			}
		}
	}
	
	/** @since 00.02.07 */
	protected static final void generateResourcesList(String dirName) {
		FileChecker checker = Resources.etcFileChecker(dirName, true);
		if((checker != null)) {
			try {
				// Save the list of entries to a file
				NIO.save(NIO.localPath("list_resources_" + Utils.fileName(dirName) + ".sha1"), checker.toString());
			} catch(IOException ex) {
				error(ex);
			}
		}
	}
	
	protected static final void generateJREList() {
		try {
			JRE.newInstance().generateHashLists(jreVersion);
		} catch(IOException ex) {
			error(ex);
		}
	}
	
	private static final void initExceptionHandlers() {
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			// If the error is not null, show it
			if((throwable != null)) error(throwable);
		});
		FXUtils.setExceptionHandler((throwable) -> {
			// If the error is not null, show it
			if((throwable != null)) error(throwable);
		});
	}
	
	private static final void disableIllegalAccessWarnings() {
		IllegalAccessWarnings.tryDisable();
	}
	
	private static final void initAutoDispose() {
		Runtime.getRuntime().addShutdownHook(new Thread(MediaDownloader::dispose));
	}
	
	/** @since 00.02.07 */
	private static final void addLibrary(Path path, String name) {
		addLibrary(path, name, Requirements.ANY);
	}
	
	/** @since 00.02.07 */
	private static final void addLibrary(Path path, String name, Requirements requirements) {
		if(requirements != Requirements.ANY
				&& !requirements.equals(Requirements.CURRENT))
			return;
		Libraries.add(path, name);
	}
	
	private static final void registerNativeLibraries() {
		// No libraries
	}
	
	private static final void registerLibraries() {
		Path path = Paths.get(PathSystem.getFullPath("lib/"));
		addLibrary(path.resolve("infomas-asl.jar"),      "infomas.asl");
		addLibrary(path.resolve("ssdf2.jar"),            "ssdf2");
		addLibrary(path.resolve("sune-memory.jar"),      "sune.memory");
		addLibrary(path.resolve("sune-process-api.jar"), "sune.api.process");
		addLibrary(path.resolve("jsoup.jar"),            "org.jsoup");
		// Define modules for builtin libraries so that plugins can use them
		ModuleUtils.defineDummyModule("sune.app.mediadown");
		ModuleUtils.defineDummyModule("sune.util.load");
	}
	
	private static final void registerResources() {
		Optional.ofNullable(remoteConfiguration().properties("res"))
		        .ifPresent((p) -> p.entrySet()
		                           .forEach((r) -> ResourcesManager.addResource(r.getKey(), r.getValue())));
	}
	
	private static final void initConfiguration() {
		boolean wasLoadedFromInternal = false;
		Path configDir  = NIO.localPath(BASE_RESOURCE).resolve("config");
		Path configPath = configDir.resolve("application.ssdf");
		
		if((NIO.exists(configDir)
				|| Utils.ignoreWithCheck(() -> NIO.createDir(configDir), MediaDownloader::error))
			&& !NIO.exists(configPath)) {
			try {
				// Copy the configuration data to the destination file
				NIO.copy(stream(BASE_RESOURCE, "configuration.ssdf"), configPath);
				wasLoadedFromInternal = true;
			} catch(IOException ex) {
				throw new RuntimeException("Unable to extract the default configuration", ex);
			}
		}
		
		SSDCollection data = SSDF.read(configPath.toFile());
		// Add a version field to the configuration
		if(wasLoadedFromInternal) {
			data.set("version", VERSION.string());
			Utils.ignore(() -> NIO.save(configPath, data.toString()), MediaDownloader::error);
		}
		// Check whether the external configuration has all the properties
		else {
			if((ResourcesUpdater.Merger.ssdf(data, SSDF.read(stream(BASE_RESOURCE, "configuration.ssdf"))))) {
				Utils.ignore(() -> NIO.save(configPath, data.toString()), MediaDownloader::error);
			}
		}
		
		// Load the configuration
		configuration = new ApplicationConfigurationWrapper(configPath);
		configuration.loadData(data);
		// Check whether the application was updated (probably)
		Version versionCurrent = VERSION;
		Version versionLast    = configuration.version();
		if((versionLast == Version.UNKNOWN
				|| versionCurrent.compareTo(versionLast) > 0)) {
			// Show the prompt later on so that the message shown can be translated
			applicationUpdated = true;
		}
	}
	
	private static final class ResourcesManager {
		
		private static final String OS_WIN64 = OSUtils.OS_NAME_WINDOWS + OSUtils.OS_ARCH_64;
		private static final String OS_UNX64 = OSUtils.OS_NAME_UNIX    + OSUtils.OS_ARCH_64;
		private static final String OS_MAC64 = OSUtils.OS_NAME_MACOS   + OSUtils.OS_ARCH_64;
		
		private static final String WIN_EXT = ".exe";
		
		private static final boolean hasFlag(Set<String> flags, String flag) {
			return flags.isEmpty() || flags.contains(flag);
		}
		
		public static final void addResource(String name, Property property) {
			Set<String> flags = property.flags();
			String version = property.value();
			if(hasFlag(flags, OS_WIN64)) Resources.add(name + WIN_EXT, name, version, OS_WIN64);
			if(hasFlag(flags, OS_UNX64)) Resources.add(name,           name, version, OS_UNX64);
			if(hasFlag(flags, OS_MAC64)) Resources.add(name,           name, version, OS_MAC64);
		}
	}
	
	private static final class ResourcesUpdater {
		
		protected static final class Merger {
			
			public static final boolean ssdf(SSDCollection dst, SSDCollection src) {
				boolean changed = false;
				Deque<Pair<SSDCollection, SSDCollection>> stack = new LinkedList<>();
				stack.push(new Pair<>(dst, src));
				while(!stack.isEmpty()) {
					Pair<SSDCollection, SSDCollection> pair = stack.pop();
					for(SSDNode node : pair.b) {
						String name = node.getName();
						if(!pair.a.hasDirect(name)) {
							changed = true; // Data changed
							if((node.isCollection())) pair.a.set(name, (SSDCollection) node);
							else                      pair.a.set(name, (SSDObject) node);
						} else if(node.isCollection()) {
							stack.push(new Pair<>((SSDCollection) node, pair.a.getDirectCollection(name)));
						}
					}
				}
				return changed;
			}
			
			public static final void css(CSS dst, CSS src) {
				Map<String, CSSRule> selectors = new HashMap<>();
				dst.getRules().stream().forEach((r) -> selectors.put(r.getSelector(), r));
				for(CSSRule rule : src.getRules()) {
					CSSRule dstRule;
					if((dstRule = selectors.get(rule.getSelector())) != null) {
						Map<String, CSSProperty> properties = new HashMap<>();
						dstRule.getProperties().stream().forEach((p) -> properties.put(p.getName(), p));
						for(CSSProperty property : rule.getProperties()) {
							if(!properties.containsKey(property.getName()))
								dstRule.getProperties().add(property);
						}
					} else {
						dst.getRules().add(new CSSRule(rule.getSelector(), new ArrayList<>(rule.getProperties())));
					}
				}
			}
		}
		
		public static final void configuration() {
			Path configDir  = NIO.localPath(BASE_RESOURCE).resolve("config");
			Path configPath = configDir.resolve("application.ssdf");
			if(!NIO.exists(configDir)
					&& !Utils.ignoreWithCheck(() -> NIO.createDir(configDir), MediaDownloader::error)) {
				return;
			}
			
			// Move the application configuration file if it exists on the old path
			Path configPathOld = NIO.localPath(BASE_RESOURCE).resolve("configuration.ssdf");
			if(NIO.exists(configPathOld)) {
				try {
					if(!NIO.exists(configPath)) NIO.move  (configPathOld, configPath);
					else                        NIO.delete(configPathOld);
				} catch(IOException ex) {
					error(ex);
				}
			}
			
			// Update content of the application configuration
			try {
				// Get the internal configuration
				SSDCollection internal = SSDF.read(stream(BASE_RESOURCE, "configuration.ssdf"));
				internal.set("version", VERSION.string());
				// Get the current configuration
				SSDCollection current = configuration.data();
				// Fix the theme, if needed
				if(current.getString("theme", "default").equalsIgnoreCase("default"))
					current.set("theme", Theme.getDefault().getName());
				// Remove the annotations at every object
				for(SSDObject object : current.objectsIterable()) {
					for(SSDAnnotation annotation : object.getAnnotations()) {
						object.removeAnnotation(annotation);
					}
				}
				// Add missing fields from the internal to the current configuration
				Merger.ssdf(current, internal);
				// Save the updated configuration
				NIO.save(configPath, current.toString());
			} catch(IOException ex) {
				error(ex);
			}
		}
		
		private static final void language(Language current) {
			InputStream stream = stream(BASE_RESOURCE, "language/" + current.getName() + ".ssdf");
			if((stream == null)) return; // Language does not exist internally
			try {
				Path pathLanguage = NIO.localPath(BASE_RESOURCE).resolve(current.getPath());
				Language internal = Language.from(current.getPath(), stream);
				// Add missing fields from the internal to the current language
				Merger.ssdf(current.getTranslation().getData(), internal.getTranslation().getData());
				// Save the updated language
				NIO.save(pathLanguage, current.getTranslation().getData().toString());
			} catch(IOException ex) {
				error(ex);
			}
		}
		
		public static final void languages() {
			ResourceRegistry.languages.values().forEach(ResourcesUpdater::language);
		}
		
		private static final void style(Theme theme, String style) {
			InputStream stream = stream(BASE_RESOURCE, "theme/" + theme.getName() + "/" + style);
			if((stream == null)) return; // Style does not exist internally
			try {
				Path pathStyle = theme.getPath().resolve(style);
				CSS internal = CSSParser.parse(Utils.streamToString(stream));
				CSS current = CSSParser.parse(NIO.read(pathStyle));
				// Add missing CSS rules from the internal to the current style
				Merger.css(current, internal);
				// Save the updated style
				NIO.save(pathStyle, current.toString());
			} catch(IOException ex) {
				error(ex);
			}
		}
		
		private static final void theme(Theme theme) {
			Arrays.asList(theme.getStyles()).stream().forEach((style) -> style(theme, style));
		}
		
		public static final void themes() {
			ResourceRegistry.themes.values().forEach(ResourcesUpdater::theme);
		}
		
		private static final Set<String> getLegacyDefaultPlugins() {
			Set<String> list = new HashSet<>();
			try {
				String urlLegacy = URL_BASE_DAT + "plugin/list";
				try(StreamResponse response = Web.requestStream(new GetRequest(Utils.url(urlLegacy), Shared.USER_AGENT));
					BufferedReader reader = new BufferedReader(new InputStreamReader(response.stream))) {
					reader.lines().forEach(list::add);
				}
			} catch(Exception ex) {
				error(ex);
			}
			return list;
		}
		
		private static final Set<String> getDefaultPlugins() {
			Set<String> list = new HashSet<>();
			try {
				PluginListObtainer.obtain().stream().map((p) -> p.b).forEach(list::add);
			} catch(Exception ex) {
				error(ex);
			}
			return list;
		}
		
		private static final void removePlugins(Set<String> plugins) {
			for(String plugin : plugins) {
				String fileName = plugin.replaceAll("[^A-Za-z0-9]+", "-") + ".jar";
				Path path = NIO.localPath(BASE_RESOURCE, "plugin", fileName);
				try {
					NIO.deleteFile(path);
				} catch(IOException ex) {
					error(ex);
				}
			}
		}
		
		public static final void plugins() {
			Set<String> listLegacy = getLegacyDefaultPlugins();
			Set<String> listCurrent = getDefaultPlugins();
			listLegacy.removeAll(listCurrent);
			removePlugins(listLegacy);
			forceCheckPlugins = true;
		}
		
		public static final void binary() {
			Path dir = NIO.localPath(BASE_RESOURCE);
			try {
				String fileName;
				for(Path file : Utils.iterable(Files.list(dir).iterator())) {
					if(!NIO.isRegularFile(file)
							// Keep only the log and JRE version file
							|| (fileName = file.getFileName().toString())
							            .equals("log.txt")
							||  fileName.equals("jre_version"))
						continue;
					try {
						NIO.deleteFile(file);
					} catch(Exception ex) {
						error(ex);
					}
				}
			} catch(Exception ex) {
				error(ex);
			}
		}
		
		/** @since 00.02.05 */
		public static final void messages(String previousVersion) {
			Version current = Version.fromString(previousVersion);
			// 00.02.04 -> 00.02.05: Messages format update (V0 -> V1)
			if(current.equals(Version.fromString("00.02.04"))) {
				// Do not bother with conversion and just remove the messages.ssdf file
				Utils.ignore(() -> NIO.deleteFile(NIO.localPath(BASE_RESOURCE).resolve("messages.ssdf")),
				             MediaDownloader::error);
			}
		}
		
		public static final void clean() {
			Path dir = NIO.localPath(BASE_RESOURCE);
			// Delete the old plugins directory
			try { NIO.deleteDir(dir.resolve("plugins")); } catch(Exception ex) { error(ex); }
			// Delete the old default theme
			try {
				NIO.deleteDir(dir.resolve("theme/default"));
			} catch(Exception ex) {
				SSDCollection data = configuration.data();
				if(!data.has("removeAtInit"))
					data.set("removeAtInit", SSDCollection.emptyArray());
				SSDCollection removeAtInit = data.getCollection("removeAtInit");
				Path path = dir.resolve("theme/default").toAbsolutePath();
				removeAtInit.add(path.toString().replace('\\', '/'));
				saveConfiguration();
			}
			try {
				for(Path file : Utils.iterable(Files.list(dir).iterator())) {
					// Delete empty directories
					if((NIO.isDirectory(file) && NIO.isEmptyDirectory(file))) {
						try {
							NIO.deleteFile(file);
						} catch(Exception ex) {
							error(ex);
						}
					}
				}
			} catch(Exception ex) {
				error(ex);
			}
		}
	}
	
	private static final void updateResourcesDirectory(String previousVersion) {
		ResourcesUpdater.configuration();
		ResourcesUpdater.languages();
		ResourcesUpdater.themes();
		ResourcesUpdater.plugins();
		ResourcesUpdater.binary();
		ResourcesUpdater.messages(previousVersion);
		ResourcesUpdater.clean();
	}
	
	private static final void saveConfiguration() {
		try {
			Path configDir  = NIO.localPath(BASE_RESOURCE).resolve("config");
			Path pathConfig = configDir.resolve("application.ssdf");
			NIO.save(pathConfig, configuration.data().toString());
		} catch(IOException ex) {
			error(ex);
		}
	}
	
	private static final void finalizeConfiguration() {
		configuration.build();
		// Remove specified files, if any
		SSDCollection data = configuration.data();
		if(data.has("removeAtInit")) {
			for(SSDObject path : data.getCollection("removeAtInit").objectsIterable()) {
				Utils.ignore(() -> NIO.delete(NIO.path(path.stringValue())), MediaDownloader::error);
			}
			data.remove("removeAtInit");
			saveConfiguration();
		}
		if(applicationUpdated) {
			String previousVersion = data.getString("version", VERSION.string());
			// Automatically (i.e. without a prompt) update the resources directory
			updateResourcesDirectory(previousVersion);
			// Update the version in the configuration file (even if the resources directory is not updated)
			data.set("version", VERSION.string());
			saveConfiguration();
		}
	}
	
	public static final void error(Throwable throwable) {
		Log.error(throwable, "An error occurred");
		if((FXUtils.isInitialized())) FXUtils.showExceptionWindow(null, throwable);
		else throwable.printStackTrace(); // FX not available, print to stderr
	}
	
	private static final class InternalResources {
		
		public static final void addLanguage(String path, boolean isExtractable) {
			path = "language/" + path; // prefix the path
			InputStream stream   = stream(BASE_RESOURCE, path);
			Language    language = Language.from(path, stream);
			ResourceRegistry.languages.registerValue(language.getName(), language, isExtractable);
		}
		
		public static final void addTheme(Theme theme, boolean isExtractable) {
			ResourceRegistry.themes.registerValue(theme.getName(), theme, isExtractable);
		}
		
		public static final void addIcon(String path, boolean isExtractable) {
			InputStream              stream = stream(BASE_RESOURCE, "icon/" + path);
			javafx.scene.image.Image icon   = new javafx.scene.image.Image(stream);
			ResourceRegistry.icons.registerValue(path, icon, isExtractable);
		}
		
		@SuppressWarnings("unused")
		public static final void addImage(String path, boolean isExtractable) {
			InputStream              stream = stream(BASE_RESOURCE, "image/" + path);
			javafx.scene.image.Image image  = new javafx.scene.image.Image(stream);
			ResourceRegistry.images.registerValue(path, image, isExtractable);
		}
		
		public static final void initializeDefaults() {
			// Languages
			addLanguage("english.ssdf", true);
			addLanguage("czech.ssdf", true);
			// Themes
			addTheme(Theme.getLight(), true);
			addTheme(Theme.getDark(), true);
			// Icons
			addIcon("automatic.png", false);
		}
		
		private static final <T> List<String> extract(ResourceNamedRegistry<T> registry, String prefix, String suffix,
				String pathDest) {
			List<String> extracted = new ArrayList<>();
			if(registry.isEmpty()) return extracted; // Do not create the resource subfolder when no resources are present
			
			InputStreamResolver inputStreamResolver = ((path) -> stream(BASE_RESOURCE, path));
			Path folder = Paths.get(pathDest);
			for(Entry<String, ResourceRegistryEntry<T>> entry : registry) {
				String name = entry.getKey();
				ResourceRegistryEntry<T> resource = entry.getValue();
				
				// Do not extract the resource, if requested
				if(!resource.isExtractable())
					continue;
				
				T value = resource.value();
				try {
					if(value instanceof Extractable) {
						((Extractable) value).extract(folder, inputStreamResolver);
					} else {
						String path = prefix + name + suffix;
						Path   file = folder.resolve(name + suffix);
						if(!NIO.exists(file)) {
							// Ensure that the parent directory exists
							NIO.createDir(file.getParent());
							// Copy the internal resource's bytes to the destination file
							NIO.copy(stream(BASE_RESOURCE, path), file);
						}
					}
					
					extracted.add(name);
				} catch(Exception ex) {
					throw new RuntimeException("Unable to extract internal resource: " + name, ex);
				}
			}
			
			return extracted;
		}
		
		/** @since 00.02.07 */
		private static final <T> void clear(ResourceNamedRegistry<T> registry, List<String> extracted) {
			extracted.stream().forEach(registry::unregister);
		}
		
		public static final void ensure() {
			String baseDest = PathSystem.getFullPath(BASE_RESOURCE);
			// Ensure the folder is existent
			Utils.ignore(() -> NIO.createDir(Paths.get(baseDest)), MediaDownloader::error);
			// Extract the internal resources to the destination folder
			clear(ResourceRegistry.languages, extract(ResourceRegistry.languages, "language/", ".ssdf", baseDest + "language/"));
			clear(ResourceRegistry.themes,    extract(ResourceRegistry.themes,    "theme/",    "",      baseDest + "theme/"));
			clear(ResourceRegistry.icons,     extract(ResourceRegistry.icons,     "icon/",     "",      baseDest + "icon/"));
			clear(ResourceRegistry.images,    extract(ResourceRegistry.images,    "image/",    "",      baseDest + "image/"));
		}
	}
	
	private static final class ExternalResourcesLoader {
		
		// Must use Callable<?> since generic types do not allow Callable<Map<String, ?>>.
		// The types are checked in the add() method.
		private static final Map<ResourceNamedRegistry<?>, Callable<?>> mapper = new HashMap<>();
		
		public static final <T> void add(ResourceNamedRegistry<T> registry, CheckedFunction<Path, Map<String, T>> function,
				Path path) {
			if((function == null || path == null))
				throw new NullPointerException();
			mapper.put(Objects.requireNonNull(registry), () -> function.apply(path));
		}
		
		private static final <T> void loadToRegistry(ResourceNamedRegistry<T> registry, Map<String, T> data) {
			data.forEach((name, value) -> registry.registerValue(name, value));
		}
		
		@SuppressWarnings("unchecked")
		private static final <T> void load(ResourceNamedRegistry<T> registry, Callable<?> callable) throws Exception {
			loadToRegistry(registry, ((Callable<Map<String, T>>) callable).call());
		}
		
		public static final void load() {
			for(Entry<ResourceNamedRegistry<?>, Callable<?>> entry : mapper.entrySet()) {
				try {
					load(entry.getKey(), entry.getValue());
				} catch(Exception ex) {
					error(ex);
				}
			}
		}
	}
	
	private static final class ExternalResourceChecker {
		
		// This should be done differently, but since the current resource system
		// is written as terribly as it is, we will do it this way.
		private static final List<String> internalLanguages = Utils.toList("english", "czech");
		
		public static final boolean isInternalLanguage(Language language) {
			return language != null && internalLanguages.contains(language.getName());
		}
		
		private static final Language getDefaultLanguage() {
			String path = "language/english.ssdf";
			return Language.from(path, stream(BASE_RESOURCE, path));
		}
		
		private static final class SSDFNamesChecker {
			
			public static final boolean check(SSDCollection original, SSDCollection other) {
				Deque<Pair<SSDCollection, SSDCollection>> stack = new LinkedList<>();
				stack.push(new Pair<>(original, other));
				while(!stack.isEmpty()) {
					Pair<SSDCollection, SSDCollection> pair = stack.pop();
					for(SSDNode node : pair.a) {
						if(!pair.b.hasDirect(node.getName()))
							return false;
						if(node.isCollection())
							stack.push(new Pair<>((SSDCollection) node, pair.b.getDirectCollection(node.getName())));
					}
				}
				return true;
			}
		}
		
		public static final boolean checkLanguage(Language language) {
			if((language == null))
				throw new NullPointerException();
			SSDCollection dataDefault = getDefaultLanguage().getTranslation().getData();
	        SSDCollection dataCurrent = language            .getTranslation().getData();
			return SSDFNamesChecker.check(dataDefault, dataCurrent);
		}
	}
	
	private static final void loadExternalResources() {
		Path basePath      = NIO.localPath(BASE_RESOURCE);
		Path pathLanguages = NIO.path(basePath, "language/");
		Path pathThemes    = NIO.path(basePath, "theme/"   );
		Path pathIcons     = NIO.path(basePath, "icon/"    );
		Path pathImages    = NIO.path(basePath, "image/"   );
		ExternalResourcesLoader.add(ResourceRegistry.languages, ExternalResources::findLanguages, pathLanguages);
		ExternalResourcesLoader.add(ResourceRegistry.themes,    ExternalResources::findThemes,    pathThemes);
		ExternalResourcesLoader.add(ResourceRegistry.icons,     ExternalResources::findIcons,     pathIcons);
		ExternalResourcesLoader.add(ResourceRegistry.images,    ExternalResources::findImages,    pathImages);
		ExternalResourcesLoader.load();
	}
	
	private static final void checkExternalResources() {
		for(Language language : new ArrayList<>(ResourceRegistry.languages.values())) {
			// Check whether the language is invalid
			if(!ExternalResourceChecker.checkLanguage(language)) {
				// Invalid language, unload it
				ResourceRegistry.languages.unregister(language.getName());
				// Check whether we can replace it from the internal languages
				if((ExternalResourceChecker.isInternalLanguage(language))) {
					// Replace the language with the internal one
					InternalResources.addLanguage(language.getName() + ".ssdf", true);
				}
			}
		}
	}
	
	private static final void addAutomaticLanguage() {
		// Add automatic language to the language resources
		Language autoLanguage = Languages.autoLanguage();
		ResourceRegistry.languages.registerValue(autoLanguage.getName(), autoLanguage);
	}
	
	private static final void loadMiscellaneousResources(StringReceiver stringReceiver) {
		try {
			Resources.ensureResources(stringReceiver, configuration.isCheckResourcesIntegrity());
		} catch(Exception ex) {
			error(ex);
		}
	}
	
	private static final void disposeExternalResources() {
		Path pathBase = NIO.localPath(BASE_RESOURCE);
		try {
			NIO.deleteDir(pathBase.resolve("language"));
			NIO.deleteDir(pathBase.resolve("theme"));
			NIO.deleteDir(pathBase.resolve("icon"));
			NIO.deleteDir(pathBase.resolve("image"));
		} catch(IOException ex) {
			error(ex);
		}
	}
	
	private static final class GUI {
		
		private static final NamedRegistry<Menu>               menus   = new SimpleNamedRegistry<>();
		private static final NamedRegistry<Window<?>>          windows = new SimpleNamedRegistry<>();
		private static final NamedRegistry<DialogWindow<?, ?>> dialogs = new SimpleNamedRegistry<>();
		
		private static final void registerMenus() {
		}
		
		private static final void registerWindows() {
			windows.register(MainWindow.NAME, FXUtils.fxTaskValue(MainWindow::new));
			windows.register(InformationWindow.NAME, FXUtils.fxTaskValue(InformationWindow::new));
			windows.register(MediaGetterWindow.NAME, FXUtils.fxTaskValue(MediaGetterWindow::new));
			windows.register(DownloadConfigurationWindow.NAME, FXUtils.fxTaskValue(DownloadConfigurationWindow::new));
			windows.register(ConfigurationWindow.NAME, FXUtils.fxTaskValue(ConfigurationWindow::new));
			windows.register(TableWindow.NAME, FXUtils.fxTaskValue(TableWindow::new));
			windows.register(MessageWindow.NAME, FXUtils.fxTaskValue(MessageWindow::new));
			windows.register(MediaInfoWindow.NAME, FXUtils.fxTaskValue(MediaInfoWindow::new));
			windows.register(PreviewWindow.NAME, FXUtils.fxTaskValue(PreviewWindow::new));
			windows.register(ClipboardWatcherWindow.NAME, FXUtils.fxTaskValue(ClipboardWatcherWindow::new));
		}
		
		private static final void registerDialogs() {
		}
	}
	
	private static final class PluginFileDownloadListener implements FileDownloadListener {
		
		private final StringReceiver receiver = InitializationStates::setText;
		
		@Override
		public void begin(String url, Path file) {
			String text = String.format("Downloading plugin %s...",
			                            file.getFileName().toString());
			receiver.receive(text);
		}
		
		@Override
		public void end(String url, Path file) {
			String text = String.format("Downloading plugin %s... done",
			                            file.getFileName().toString());
			receiver.receive(text);
		}
		
		@Override
		public void update(String url, Path file, long current, long total) {
			double percent = current / (double) total;
			String text = String.format("Downloading plugin %s... %.2f%%",
			                            file.getFileName().toString(),
			                            percent);
			receiver.receive(text);
		}
		
		@Override
		public void error(Exception ex) {
			MediaDownloader.error(ex);
		}
	}
	
	private static final class PluginListObtainer {
		
		private static final String URL_DIR = URL_BASE_DAT + "plugin/";
		
		private static final List<Pair<String, String>> parseList() throws Exception {
			List<Pair<String, String>> plugins = new ArrayList<>();
			
			Requirements requirements = Requirements.CURRENT;
			String version = VERSION.stringRelease();
			RemoteConfiguration config = remoteConfiguration();
			String listURL = Utils.urlConcat(URL_BASE_VER, version, config.value("plugin_list"));
			String prefix  = config.value("plugin_prefix");
			
			try(StreamResponse response = Web.requestStream(new GetRequest(Utils.url(listURL), Shared.USER_AGENT));
				BufferedReader reader = new BufferedReader(new InputStreamReader(response.stream))) {
				for(String line; (line = reader.readLine()) != null;) {
					// Parsing of metadata
					if(line.startsWith("[")) {
						int index = line.indexOf("]");
						if(index <= 0) continue; // Invalid plugin line, skip
						String[] array = line.substring(1, index).split(",");
						Map<String, String> metadata = new HashMap<>();
						for(String item : array) {
							String[] pair = item.split(":", 2);
							String name = null, value = pair[0];
							if(pair.length > 1) {
								name = value;
								value = pair[1];
							}
							metadata.put(name, value);
						}
						// Check the metadata
						boolean isOk = true;
						String os = metadata.get("os");
						if(os != null) {
							String[] osArray = os.split("\\|"); // Support for multiple OS
							boolean any = false;
							for(String value : osArray) {
								if(requirements.equals(Requirements.parseNoDelimiter(value))) {
									any = true; break;
								}
							}
							if(!any) isOk = false;
						}
						// Do not add this plugin, if not OK
						if(!isOk) continue;
						// Remove metadata string from the line
						line = line.substring(index + 1);
					}
					String pluginURL = URL_DIR + line;
					// Strip the prefix, if non-null
					if((prefix != null
							&& line.startsWith(prefix)))
						line = line.substring(prefix.length());
					plugins.add(new Pair<>(pluginURL, line));
				}
			}
			
			return plugins;
		}
		
		public static final List<Pair<String, String>> obtain() throws Exception {
			return parseList();
		}
	}
	
	private static final void initDefaultPlugins() {
		try {
			FileDownloadListener listener = new PluginFileDownloadListener();
			for(Pair<String, String> plugin : PluginListObtainer.obtain()) {
				String fileName = plugin.b.replaceAll("[^A-Za-z0-9]+", "-").replaceFirst("^plugin-", "") + ".jar";
				Path path = NIO.localPath(BASE_RESOURCE, "plugin", fileName);
				if(!NIO.exists(path)) {
					String versionURL = PluginUpdater.newestVersionURL(plugin.a);
					// Check whether there is a file available for the current application version
					if((versionURL != null)) {
						String jarURL = Utils.urlConcat(versionURL, "plugin.jar");
						FileDownloader.download(jarURL, path, listener);
					}
				}
			}
		} catch(Exception ex) {
			error(ex);
		}
	}
	
	private static final void registerPlugins() {
		Path folder = NIO.localPath(BASE_RESOURCE, "plugin");
		// Ignore, if no such folder exists
		if(!NIO.exists(folder))
			return;
		// Find and register all the plugins
		try {
			StringReceiver receiver = InitializationStates::setText;
			FileDownloadListener listener = new PluginFileDownloadListener();
			Files.walk(folder)
				// Filter out only the files
				.filter((p) -> Files.isRegularFile(p))
				// Sort files in the same directory lexicographically but put shorter names first
				.sorted((a, b) -> {
					// Check whether the files are in the same directory
					if(a.getParent().equals(b.getParent())) {
						String[] aNames = Utils.fileNameNoType(a.getFileName().toString()).split("-");
						String[] bNames = Utils.fileNameNoType(b.getFileName().toString()).split("-");
						int cmp, i = 0, l = Math.min(aNames.length, bNames.length);
						do {
							String aName = aNames[i];
							String bName = bNames[i];
							cmp = aName.compareTo(bName);
							++i;
						} while(cmp == 0 && i < l);
						// The names are the same up to the smallest length
						if(cmp == 0) {
							// Select the path with smaller length
							return aNames.length < bNames.length ? -1 : 1;
						}
					}
					// If they are not in the same directory, use default comparison
					return a.compareTo(b);
				})
				.map((file) -> {
					try {
						// Parse the file and extract all plugin information
						return PluginFile.from(file);
					} catch(Exception ex) {
						error(ex);
					}
					// Cannot load, will be filtered out
					return null;
				})
				.filter((plugin) -> plugin != null)
				.map((plugin) -> {
					try {
						// Update the plugin, if needed
						if((configuration.isPluginsAutoUpdateCheck() || forceCheckPlugins)) {
							receiver.receive(String.format("Checking plugin %s...", plugin.getPlugin().instance().name()));
							String pluginURL = PluginUpdater.check(plugin);
							// Check whether there is a newer version of the plugin
							if((pluginURL != null)) {
								Path file = Paths.get(plugin.getPath());
								FileDownloader.download(pluginURL, file, listener);
								// Must reload the plugin, otherwise it will have incorrect information
								PluginFile.resetPluginFileLoader();
								plugin = PluginFile.from(file);
							}
							receiver.receive(String.format("Checking plugin %s... done", plugin.getPlugin().instance().name()));
						}
					} catch(Exception ex) {
						error(ex);
					}
					return plugin;
				})
				// Add the plugin to the list, so it will be loaded
				.forEach(Plugins::add);
		} catch(IOException ex) {
			error(ex);
		}
	}
	
	private static final void loadPlugins(PluginLoadListener listener) {
		try {
			Plugins.loadAll(listener);
		} catch(Exception ex) {
			error(ex);
		}
	}
	
	/** @since 00.02.02 */
	public static final class Languages {
		
		private static Language autoLanguage;
		
		private static final String localCode() {
			return Locale.getDefault().getISO3Language();
		}
		
		private static final Language localLanguage(Supplier<Language> defaultLanguage) {
			String code = localCode();
			return ResourceRegistry.languages.values().stream()
					               .filter((l) -> l.getCode().equalsIgnoreCase(code))
					               .findFirst().orElseGet(defaultLanguage);
		}
		
		public static final Language autoLanguage() {
			if(autoLanguage == null) {
				Language local = localLanguage();
				String currentLanguageName = configuration.data().getDirectString("language");
				boolean isCurrentLanguageAuto = currentLanguageName.equalsIgnoreCase("auto");
				Language currentLanguage = isCurrentLanguageAuto ? local : ResourceRegistry.language(currentLanguageName);
				String title = currentLanguage.getTranslation().getSingle("generic.language.auto");
				autoLanguage = new Language("", "auto", "0000", title, "auto", local.getTranslation());
			}
			return autoLanguage;
		}
		
		public static final Language localLanguage() {
			return localLanguage(() -> ResourceRegistry.language("english")); // English by default
		}
	}
	
	public static final ApplicationConfiguration configuration() {
		return configuration.configuration();
	}
	
	public static final Language language() {
		return configuration.language();
	}
	
	public static final Translation translation() {
		return configuration.language().getTranslation();
	}
	
	public static final Theme theme() {
		return configuration.theme();
	}
	
	// Will be called automatically when closed properly
	protected static final void dispose() {
		if((isDisposed.get()))
			return;
		try {
			Plugins.dispose();
		} catch(Exception ex) {
			error(ex);
		}
		Disposables.dispose();
		Threads    .destroy();
		Web        .clear();
		isDisposed.set(true);
	}
	
	// https://stackoverflow.com/questions/4159802/how-can-i-restart-a-java-application
	public static final void restart() {
		try {
			Path pathJava = Paths.get(System.getProperty("java.home"), "bin", "java");
			Path pathJAR  = Paths.get(MediaDownloader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			if((!pathJAR.getFileName().toString().endsWith(".jar")))
				return;
			List<String> commands = Arrays.asList(pathJava.toAbsolutePath().toString(), "-jar",
			                                      pathJAR .toAbsolutePath().toString());
			new ProcessBuilder(commands).start();
			close();
		} catch(Exception ex) {
			throw new RuntimeException("Unable to restart the application");
		}
	}
	
	public static final void close() {
		dispose();
		FXUtils.exit();
	}
	
	public static final Version version() {
		return VERSION;
	}
	
	public static final Menu menu(String name) {
		return GUI.menus.get(name);
	}
	
	@SuppressWarnings("unchecked")
	public static final <W extends Window<?>> W window(String name) {
		return (W) GUI.windows.get(name);
	}
	
	@SuppressWarnings("unchecked")
	public static final <D extends DialogWindow<?, ?>> D dialog(String name) {
		return (D) GUI.dialogs.get(name);
	}
	
	// Forbid anyone to create an instance of this class
	private MediaDownloader() {
	}
	
	/** @since 00.02.04 */
	private static final class ApplicationConfigurationWrapper implements ApplicationConfigurationAccessor {
		
		private ApplicationConfiguration configuration;
		private ApplicationConfiguration.Builder builder;
		private SSDCollection data;
		
		public ApplicationConfigurationWrapper(Path path) {
			builder = ApplicationConfiguration.builder(path);
		}
		
		private final ApplicationConfigurationAccessor accessor() {
			return configuration != null ? configuration : builder;
		}
		
		public final void loadData(SSDCollection data) {
			builder.loadData(data != null ? this.data = data : this.data);
		}
		
		public final void build() {
			configuration = (ApplicationConfiguration) builder.build();
		}
		
		@Override public Version version() { return accessor().version(); }
		@Override public Language language() { return accessor().language(); }
		@Override public Theme theme() { return accessor().theme(); }
		@Override public boolean isAutoUpdateCheck() { return accessor().isAutoUpdateCheck(); }
		@Override public int acceleratedDownload() { return accessor().acceleratedDownload(); }
		@Override public int parallelDownloads() { return accessor().parallelDownloads(); }
		@Override public int parallelConversions() { return accessor().parallelConversions(); }
		@Override public boolean computeStreamSize() { return accessor().computeStreamSize(); }
		@Override public int requestTimeout() { return accessor().requestTimeout(); }
		@Override public boolean isCheckResourcesIntegrity() { return accessor().isCheckResourcesIntegrity(); }
		@Override public boolean isPluginsAutoUpdateCheck() { return accessor().isPluginsAutoUpdateCheck(); }
		/** @since 00.02.05 */
		@Override public Path lastDirectory() { return accessor().lastDirectory(); }
		/** @since 00.02.05 */
		@Override public MediaFormat lastOpenFormat() { return accessor().lastOpenFormat(); }
		/** @since 00.02.05 */
		@Override public MediaFormat lastSaveFormat() { return accessor().lastSaveFormat(); }
		/** @since 00.02.05 */
		@Override public MediaTitleFormat mediaTitleFormat() { return accessor().mediaTitleFormat(); }
		/** @since 00.02.05 */
		@Override public String customMediaTitleFormat() { return accessor().customMediaTitleFormat(); }
		/** @since 00.02.07 */
		@Override public boolean usePreReleaseVersions() { return accessor().usePreReleaseVersions(); }
		@Override public SSDCollection data() { return accessor().data(); }
		
		public ApplicationConfiguration configuration() { return configuration; }
	}
}