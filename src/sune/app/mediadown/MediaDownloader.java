package sune.app.mediadown;

import static sune.app.mediadown.gui.window.ConfigurationWindow.isOfEnumClass;
import static sune.app.mediadown.gui.window.ConfigurationWindow.isOfName;
import static sune.app.mediadown.gui.window.ConfigurationWindow.isOfTypeClass;
import static sune.app.mediadown.gui.window.ConfigurationWindow.localValueTranslator;
import static sune.app.mediadown.gui.window.ConfigurationWindow.predefineGroups;
import static sune.app.mediadown.gui.window.ConfigurationWindow.registerFormField;
import static sune.app.mediadown.gui.window.ConfigurationWindow.typeFormFieldSupplier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.scene.image.Image;
import sune.app.mediadown.MediaDownloader.Versions.VersionEntryAccessor;
import sune.app.mediadown.concurrent.Threads;
import sune.app.mediadown.configuration.ApplicationConfiguration;
import sune.app.mediadown.configuration.ApplicationConfigurationAccessor;
import sune.app.mediadown.configuration.ApplicationConfigurationAccessor.UsePreReleaseVersions;
import sune.app.mediadown.configuration.Configuration;
import sune.app.mediadown.conversion.ConversionProvider;
import sune.app.mediadown.conversion.Conversions;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.FileDownloader;
import sune.app.mediadown.event.CheckEvent;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventBindableAction;
import sune.app.mediadown.event.EventBinder;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.FileCheckEvent;
import sune.app.mediadown.event.LibraryEvent;
import sune.app.mediadown.event.NativeLibraryLoaderEvent;
import sune.app.mediadown.event.PluginLoaderEvent;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.exception.TranslatableException;
import sune.app.mediadown.gui.Dialog;
import sune.app.mediadown.gui.Window;
import sune.app.mediadown.gui.form.field.PasswordField;
import sune.app.mediadown.gui.form.field.SelectLanguageField;
import sune.app.mediadown.gui.form.field.SelectMediaTitleFormatField;
import sune.app.mediadown.gui.form.field.SelectThemeField;
import sune.app.mediadown.gui.form.field.TextFieldMediaTitleFormat;
import sune.app.mediadown.gui.form.field.TranslatableSelectField.ValueTransformer;
import sune.app.mediadown.gui.window.AboutWindow;
import sune.app.mediadown.gui.window.ClipboardWatcherWindow;
import sune.app.mediadown.gui.window.ConfigurationWindow;
import sune.app.mediadown.gui.window.CredentialsEditDialogWindow;
import sune.app.mediadown.gui.window.CredentialsWindow;
import sune.app.mediadown.gui.window.DownloadConfigurationWindow;
import sune.app.mediadown.gui.window.MainWindow;
import sune.app.mediadown.gui.window.MediaGetterWindow;
import sune.app.mediadown.gui.window.MediaInfoWindow;
import sune.app.mediadown.gui.window.MessageWindow;
import sune.app.mediadown.gui.window.PluginManagerWindow;
import sune.app.mediadown.gui.window.PreviewWindow;
import sune.app.mediadown.gui.window.ReportWindow;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.language.Language;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.library.Libraries;
import sune.app.mediadown.library.Library;
import sune.app.mediadown.library.NativeLibraries;
import sune.app.mediadown.library.NativeLibrary;
import sune.app.mediadown.logging.Log;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaTitleFormat;
import sune.app.mediadown.media.MediaTitleFormats.NamedMediaTitleFormat;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.net.Web.Response;
import sune.app.mediadown.plugin.PluginConfiguration;
import sune.app.mediadown.plugin.PluginFile;
import sune.app.mediadown.plugin.PluginUpdater;
import sune.app.mediadown.plugin.Plugins;
import sune.app.mediadown.registry.ResourceNamedRegistry;
import sune.app.mediadown.registry.ResourceNamedRegistry.ResourceRegistryEntry;
import sune.app.mediadown.resource.ExternalResources;
import sune.app.mediadown.resource.Extractable;
import sune.app.mediadown.resource.InputStreamResolver;
import sune.app.mediadown.resource.InternalURLProtocols;
import sune.app.mediadown.resource.JRE;
import sune.app.mediadown.resource.JRE.JREEvent;
import sune.app.mediadown.resource.ResourceRegistry;
import sune.app.mediadown.resource.Resources;
import sune.app.mediadown.resource.Resources.InternalResource;
import sune.app.mediadown.resource.Resources.StringReceiver;
import sune.app.mediadown.theme.Theme;
import sune.app.mediadown.update.FileChecker;
import sune.app.mediadown.update.RemoteConfiguration;
import sune.app.mediadown.update.Requirements;
import sune.app.mediadown.update.Updater;
import sune.app.mediadown.update.Version;
import sune.app.mediadown.update.VersionType;
import sune.app.mediadown.util.CheckedFunction;
import sune.app.mediadown.util.CheckedRunnable;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.IllegalAccessWarnings;
import sune.app.mediadown.util.MathUtils;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Password;
import sune.app.mediadown.util.PathSystem;
import sune.app.mediadown.util.Ref;
import sune.app.mediadown.util.Reflection2;
import sune.app.mediadown.util.Reflection3;
import sune.app.mediadown.util.Regex;
import sune.app.mediadown.util.SelfProcess;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;
import sune.util.load.ModuleUtils;
import sune.util.ssdf2.SSDAnnotation;
import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDF;
import sune.util.ssdf2.SSDNode;
import sune.util.ssdf2.SSDObject;

public final class MediaDownloader {
	
	public static final String  TITLE   = "Media Downloader";
	public static final Version VERSION = Version.of("00.02.09-dev.26");
	public static final String  DATE    = "2025-05-08";
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
	/** @since 00.02.08 */
	private static Libraries libraries;
	/** @since 00.02.09 */
	private static Set<String> pluginConfigurationsToUpdate = new LinkedHashSet<>();
	/** @since 00.02.09 */
	private static Log log;
	
	private static final AtomicBoolean isDisposed = new AtomicBoolean();
	private static final String BASE_RESOURCE = "/resources/";
	
	private static final int TIMEOUT = 8000;
	
	private static final InputStream stream(String base, String path) {
		return MediaDownloader.class.getResourceAsStream(base + path);
	}
	
	private static final Image icon(String path) {
		return new Image(MediaDownloader.class.getResourceAsStream("/resources/icon/" + path));
	}
	
	/** @since 00.02.08 */
	private static interface InitializationState {
		
		public static final double PROGRESS_INDETERMINATE = -1.0;
		
		InitializationState run(Arguments args);
		default String getTitle() { return null; }
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
			if(window != null) {
				FXUtils.thread(() -> {
					window.close();
					window = null;
				});
			}
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
			classesCount += libraries != null ? libraries.all().size() : 0;
			classesCount += countPlugins ? Plugins.all().size() : 0;
			return classesCount;
		}
		
		private static final class InternalInitialization implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				initExceptionHandlers();
				disableIllegalAccessWarnings();
				initAutoDispose();
				initInternalProtocol();
				return new ShowStartupWindow();
			}
		}
		
		private static final class ShowStartupWindow implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				int count = count(false);
				if(!args.has("no-startup-gui")) {
					init(count);
				}
				return new InitializeConfiguration();
			}
		}
		
		private static final class InitializeConfiguration implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				initConfiguration();
				Versions.load();
				Ignore.callVoid(MediaDownloader::initRemoteConfiguration);
				return new CheckJRE();
			}
			
			@Override public String getTitle() { return "Initializing configuration..."; }
		}
		
		// Update the JRE, if needed, as soon as possible, since some libraries and/or plugins
		// may rely on it.
		/** @since 00.02.02 */
		private static final class CheckJRE implements InitializationState {
			
			private static final Path oldJREPath()  { return PathSystem.getPath("jre"); }
			private static final Path newJREPath()  { return PathSystem.getPath("jre-new"); }
			
			private final void doRun(Arguments args, RemoteConfiguration configuration) throws Exception {
				// Obtain the required JRE version from the remote configuration
				jreVersion = configuration.value("jre");
				
				if(args.has("jre-update") && args.has("pid")) {
					long pid = Long.valueOf(args.getValue("pid"));
					
					if(pid <= 0L) {
						throw new IllegalStateException("Invalid PID");
					}
					
					// Get the parent process
					ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
					
					// Check whether the old process still exists
					if(handle != null) {
						// Wait for it to finish
						setText("Waiting for process to finish...");
						handle.onExit().get();
					}
					
					// Move the directories around so that the new JRE is in the correct location
					Path oldJREPath = oldJREPath();
					Path newJREPath = newJREPath();
					setText("Deleting old JRE...");
					NIO.deleteDir(oldJREPath);
					setText("Copying new JRE...");
					NIO.copyDir(newJREPath, oldJREPath);
					
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
					// Update the JRE version so that we know which version is present
					Versions.Common.jre().set(Version.of(jreVersion));
				} else if(AppArguments.isUpdateEnabled()) {
					VersionEntryAccessor version = Versions.Common.jre();
					Version verLocal = version.get();
					Version verRemote = Version.of(jreVersion);
					
					// Check the current JRE version and update, if necessary
					if(verLocal.compareTo(verRemote) != 0 || verLocal == Version.UNKNOWN) {
						JRE jre = JRE.newInstance();
						jre.addEventListener(JREEvent.CHECK, (context) -> {
							setText(String.format("Checking %s...", context.name()));
						});
						jre.addEventListener(JREEvent.DOWNLOAD_BEGIN, (context) -> {
							setText(String.format("Downloading %s...", context.path().getFileName().toString()));
						});
						jre.addEventListener(JREEvent.DOWNLOAD_UPDATE, (context) -> {
							setText(String.format("Downloading %s... %s%%", context.path().getFileName().toString(),
							                      MathUtils.round(context.tracker().progress() * 100.0, 2)));
						});
						jre.addEventListener(JREEvent.DOWNLOAD_END, (context) -> {
							setText(String.format("Downloading %s... done", context.path().getFileName().toString()));
						});
						jre.addEventListener(JREEvent.ERROR, (context) -> {
							error(context.exception());
						});
						
						boolean checkIntegrity = true; // Always check integrity
						Path oldJREPath = oldJREPath();
						Path newJREPath = newJREPath();
						Set<Path> visitedFiles = new HashSet<>();
						
						// Check the files and if any is changed, continue the process
						if(jre.check(oldJREPath, newJREPath, Requirements.CURRENT, jreVersion, visitedFiles,
						             (p) -> checkIntegrity, null)) {
							// Copy all files that were not updated and exist on the web (were visited)
							NIO.mergeDirectories(oldJREPath, newJREPath, (p, np) -> visitedFiles.contains(p) && !NIO.exists(np));
							
							// Get the current run command, so that the application can be run again
							String runCommand = SelfProcess.command(args.argsList());
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
							
							// Make sure the new executable is actually executable
							NIO.makeExecutable(exePath);
							
							// Start a new process to finish updating the JRE
							SelfProcess.launch(exePath, List.of(
								"--jre-update",
								"--pid", String.valueOf(SelfProcess.pid()),
								"--run-command", runCommand
							));
							
							// Exit normally
							System.exit(0);
						} else {
							// Also delete the empty new JRE directory (from checking)
							NIO.deleteDir(newJREPath);
							// Update the JRE version so that we know which version is present
							version.set(Version.of(jreVersion));
						}
					}
				}
			}
			
			@Override
			public InitializationState run(Arguments args) {
				try {
					RemoteConfiguration remoteConfiguration = remoteConfiguration();
					
					if(remoteConfiguration != null) {
						// Actually run only if the remote configuration has been obtained
						doRun(args, remoteConfiguration);
					}
				} catch(Exception ex) {
					error(ex);
				}
				
				return new RegistrationOfLibrariesAndResources();
			}
		}
		
		private static final class RegistrationOfLibrariesAndResources implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				libraries = Libraries.create();
				registerNativeLibraries();
				registerLibraries();
				registerResources();
				updateTotal(false);
				return new CheckLibraries();
			}
		}
		
		private static final class CheckLibraries implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				if(AppArguments.isUpdateEnabled()) {
					final Path rootDir = Path.of(PathSystem.getCurrentDirectory());
					final Ref.Mutable<Double> progressValue = new Ref.Mutable<>(0.0);
					
					try(FileDownloader downloader = new FileDownloader(new TrackerManager())) {
						downloader.addEventListener(DownloadEvent.BEGIN, (context) -> {
							Path file = context.output();
							String path = file.subpath(rootDir.getNameCount(), file.getNameCount()).toString();
							
							progressValue.set(getProgress());
							setText(String.format("Downloading library %s...", path));
							setProgress(0.0);
						});
						
						downloader.addEventListener(DownloadEvent.UPDATE, (context) -> {
							Path file = context.output();
							DownloadTracker tracker = (DownloadTracker) context.trackerManager().tracker();
							double current = tracker.current();
							double total = tracker.total();
							double percent0 = (current / (double) total);
							double percent1 = (percent0 * 100.0);
							
							String path = file.subpath(rootDir.getNameCount(), file.getNameCount()).toString();
							setText(String.format("Downloading library %s... %s%%", path, MathUtils.round(percent1, 2)));
							setProgress(percent0);
						});
						
						downloader.addEventListener(DownloadEvent.END, (d) -> {
							Path file = d.output();
							String path = file.subpath(rootDir.getNameCount(), file.getNameCount()).toString();
							
							setText(String.format("Downloading library %s... Done", path));
							setProgress(progressValue.get());
						});
						
						downloader.addEventListener(DownloadEvent.ERROR, (pair) -> {
							setText(String.format("Downloading library... Error"));
							setProgress(progressValue.get());
						});
						
						EventBindableAction<EventType, Void> action = Update.checkLibraries(downloader);
						
						action.addEventListener(CheckEvent.COMPARE, (name) -> {
							setText(String.format("Checking library %s...", name));
						});
						
						action.addEventListener(FileCheckEvent.UPDATE, (pair) -> {
							String path = pair.a.subpath(rootDir.getNameCount(), pair.a.getNameCount()).toString();
							setText(String.format("Checking %s...%s", path, pair.b != null ? " done" : ""));
						});
						
						action.addEventListener(FileCheckEvent.ERROR, (ex) -> {
							setText(String.format("Checking... Error"));
						});
						
						action.execute();
					} catch(Exception ex) {
						error(ex);
					}
				}
				
				return new LoadNativeLibraries();
			}
			
			@Override public String getTitle() { return "Checking libraries..."; }
		}
		
		private static final class LoadNativeLibraries implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				NativeLibraries.addEventListener(NativeLibraryLoaderEvent.LOADING, (library) -> {
					setText(String.format(
						"Loading native library %s (%s)...",
						library.getName(),
						library.getPath().getFileName().toString()
					));
				});
				
				NativeLibraries.addEventListener(NativeLibraryLoaderEvent.LOADED, (pair) -> {
					update(String.format(
						"Loading native library %s (%s)... %s",
						pair.a.getName(),
						pair.a.getPath().getFileName().toString(),
						pair.b == null ? "done" : "error"
					));
				});
				
				NativeLibraries.addEventListener(NativeLibraryLoaderEvent.NOT_LOADED, (libraries) -> {
					String text = String.format("Cannot load native libraries (%d)", libraries.size());
					StringBuilder content = new StringBuilder();
					
					for(NativeLibrary library : libraries) {
						content.append(String.format("%s (%s)\n", library.getName(), library.getPath()));
					}
					
					Dialog.showContentError("Critical error", text, content.toString());
					System.exit(-1);
				});
				
				NativeLibraries.load();
				
				return new LoadLibraries();
			}
			
			@Override public String getTitle() { return "Loading native libraries..."; }
		}
		
		private static final class LoadLibraries implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				List<Library> notLoaded = new LinkedList<>();
				
				libraries.addEventListener(LibraryEvent.LOADING, (library) -> {
					setText(String.format("Loading library %s...", library.name()));
				});
				
				libraries.addEventListener(LibraryEvent.LOADED, (library) -> {
					update(String.format("Loading library %s... %s", library.name(), "done"));
				});
				
				libraries.addEventListener(LibraryEvent.NOT_LOADED, (pair) -> {
					notLoaded.add(pair.a);
				});
				
				boolean success = libraries.load(ClassLoader.getSystemClassLoader());
				
				if(!success) {
					String text = String.format("Cannot load libraries (%d)", notLoaded.size());
					StringBuilder content = new StringBuilder();
					
					for(Library library : notLoaded) {
						content.append(String.format("%s (%s)\n", library.name(), library.path()));
					}
					
					Dialog.showContentError("Critical error", text, content.toString());
					System.exit(-1);
				}
				
				return new MaybeDisposeOfExternalResources();
			}
			
			@Override public String getTitle() { return "Loading libraries..."; }
		}
		
		private static final class MaybeDisposeOfExternalResources implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				if(AppArguments.isDebugEnabled()) {
					disposeExternalResources();
				}
				return new InitializeInternalResources();
			}
		}
		
		private static final class InitializeInternalResources implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				InternalResources.initializeDefaults();
				InternalResources.ensure();
				return new LoadExternalResources();
			}
			
			@Override public String getTitle() { return "Initializing internal resources..."; }
		}
		
		private static final class LoadExternalResources implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				loadExternalResources();
				return new CheckExternalResources();
			}
			
			@Override public String getTitle() { return "Initializing external resources..."; }
		}
		
		private static final class CheckExternalResources implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				checkExternalResources();
				addAutomaticLanguage();
				return new InitializeMiscellaneousResources();
			}
			
			@Override public String getTitle() { return "Checking external resources..."; }
		}
		
		private static final class InitializeMiscellaneousResources implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				loadMiscellaneousResources(InitializationStates::setText);
				return new InitializeDefaults();
			}
			
			@Override public String getTitle() { return "Initializating miscellaneous resources..."; }
		}
		
		/** @since 00.02.09 */
		private static final class InitializeDefaults implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				Ignore.callVoid(MediaDownloader::initDefaults, MediaDownloader::error);
				return new FinalizeConfiguration();
			}
			
			@Override public String getTitle() { return "Initializing defaults..."; }
		}
		
		private static final class FinalizeConfiguration implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				finalizeConfiguration();
				return new CheckVersion();
			}
		}
		
		private static final class CheckVersion implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				if(AppArguments.isUpdateEnabled()) {
					if(args.has("is-jar-update") || Update.canAutoUpdate()) {
						Update.update(args);
					}
				}
				
				return new RegisterWindows();
			}
			
			@Override public String getTitle() { return "Checking new versions..."; }
		}
		
		private static final class RegisterWindows implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				if(FXUtils.isInitialized())
					GUI.registerWindows();
				return new InitializeDefaultPlugins();
			}
			
			@Override public String getTitle() { return "Registering windows..."; }
		}
		
		private static final class InitializeDefaultPlugins implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				initDefaultPlugins();
				return new InitializePlugins();
			}
			
			@Override public String getTitle() { return "Initializing default plugins..."; }
		}
		
		private static final class InitializePlugins implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				setProgress(PROGRESS_INDETERMINATE);
				registerPlugins();
				updateTotal(true);
				
				Plugins.addEventListener(PluginLoaderEvent.LOADING, (plugin) -> {
					setText("Loading plugin " + plugin.getPlugin().instance().name() + "...");
				});
				
				Plugins.addEventListener(PluginLoaderEvent.LOADED, (pair) -> {
					update("Loading plugin " + pair.a.getPlugin().instance().name() + "... " + (pair.b ? "done" : "error"));
				});
				
				Plugins.addEventListener(PluginLoaderEvent.NOT_LOADED, (plugins) -> {
					String text = "Cannot load plugins (" + plugins.size() + ")";
					StringBuilder content = new StringBuilder();
					
					for(PluginFile plugin : plugins) {
						content.append(plugin.getPlugin().instance().name());
						content.append(" (");
						content.append(plugin.getPath());
						content.append(")\n");
					}
					
					errorWithContent(text, content.toString());
				});
				
				Plugins.addEventListener(PluginLoaderEvent.ERROR_LOAD, (pair) -> {
					String message = String.format("Cannot load plugin: %s", pair.a.getPlugin().instance().name());
					error(new IllegalStateException(message, pair.b));
				});
				
				Plugins.addEventListener(PluginLoaderEvent.ERROR_DISPOSE, (pair) -> {
					String message = String.format("Cannot dispose plugin: %s", pair.a.getPlugin().instance().name());
					error(new IllegalStateException(message, pair.b));
				});
				
				Ignore.callVoid(Plugins::loadAll, MediaDownloader::error);
				
				// Run the plugin update triggers here so that the plugins themselves have an opportunity
				// to register their own plugin update triggers.
				UpdateTriggers.OfPlugin.run();
				
				return new Finalization();
			}
			
			@Override public String getTitle() { return "Initializing plugins..."; }
		}
		
		/** @since 00.02.07 */
		private static final class Finalization implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				Set<Configuration> configurations = null;
				
				if(applicationUpdated) {
					UpdateTriggers.OfApplication.run(
						UpdateTriggers.OfApplication.Stage.AFTER_CONFIGURATION_FINALIZATION
					);
					
					configurations = new LinkedHashSet<>();
					configurations.add(configuration());
				}
				
				if(!pluginConfigurationsToUpdate.isEmpty()) {
					if(configurations == null) {
						configurations = new LinkedHashSet<>();
					}
					
					Plugins.allLoaded().stream()
						.filter((p) -> pluginConfigurationsToUpdate.contains(p.getPlugin().instance().name()))
						.map(PluginFile::getConfiguration)
						.filter(Objects::nonNull)
						.filter(Predicate.not(PluginConfiguration::isEmpty))
						.forEachOrdered(configurations::add);
					
					pluginConfigurationsToUpdate = null; // Clean up
				}
				
				// To prevent some issues, re-save all updated configurations to force
				// all properties to be revalidated.
				saveConfigurations(configurations);
				
				return new MaybeExitEarly();
			}
		}
		
		/** @since 00.02.08 */
		private static final class MaybeExitEarly implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				if(AppArguments.isOnlyInitializationEnabled()) {
					System.exit(0);
				}
				
				return new MaybeRunStandalonePlugin();
			}
		}
		
		/** @since 00.02.02 */
		private static final class MaybeRunStandalonePlugin implements InitializationState {
			
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
		
		private static final class InitializationDone implements InitializationState {
			
			@Override
			public InitializationState run(Arguments args) {
				FXUtils.thread(() -> {
					window(MainWindow.NAME).show();
					close();
					FXUtils.refreshExceptionWindow();
				});
				
				return null;
			}
			
			@Override public String getTitle() { return "Initialization done"; }
		}
	}
	
	public static final void initialize(String[] args) {
		arguments = Arguments.parse(args);
		log = Log.initialize("Media-Downloader", "application.log", Level.ALL);
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
	
	/** @since 00.02.09 */
	public static final Log log() {
		return log;
	}
	
	/** @since 00.02.08 */
	public static final class AppArguments {
		
		private AppArguments() {
		}
		
		public static final boolean isDebugEnabled() {
			return arguments.booleanValue("debug");
		}
		
		public static final boolean isUpdateEnabled() {
			return !arguments.booleanValue("no-update");
		}
		
		public static final boolean isOnlyInitializationEnabled() {
			return arguments.booleanValue("only-init");
		}
	}
	
	private static final class Update {
		
		private static final StringReceiver receiver = InitializationStates::setText;
		private static Version newestVersion;
		
		/** @since 00.02.07 */
		private static final boolean usePreReleaseVersions() {
			UsePreReleaseVersions value = configuration.usePreReleaseVersions();
			return (value != UsePreReleaseVersions.NEVER && value != UsePreReleaseVersions.UNKNOWN)
						|| VERSION.type() != VersionType.RELEASE;
		}
		
		/** @since 00.02.07 */
		private static final String versionFileURI() {
			return URL_BASE_VER + "version" + (usePreReleaseVersions() ? "_pre" : "");
		}
		
		/** @since 00.02.07 */
		private static final boolean showUpdateDialog() {
			Translation tr = translation().getTranslation("dialogs.update_available");
			return Dialog.showPrompt(tr.getSingle("title"), tr.getSingle("text"));
		}
		
		/** @since 00.02.07 */
		public static final boolean canAutoUpdate() {
			return configuration.isAutoUpdateCheck() || VERSION.type() != VersionType.RELEASE;
		}
		
		/** @since 00.02.04 */
		public static final void update(Arguments args) {
			if(args.has("is-jar-update") || (checkVersion() && updateDialog())) {
				try {
					boolean needsVersion = (!args.has("jar-update") || !args.has("pid")) && !args.has("jar-update-finish");
					Version version = Version.UNKNOWN;
					
					if(needsVersion) {
						// Allow manual version selection using arguments
						String custom = args.getValue("jar-version");
						version = custom != null ? Version.of(custom) : Version.UNKNOWN;
						
						if(version == Version.UNKNOWN) {
							version = newestVersion();
						}
					}
					
					JarUpdater.doUpdateProcess(version, usePreReleaseVersions(), args, receiver);
				} catch(Exception ex) {
					error(ex);
				}
			}
		}
		
		public static final EventBindableAction<EventType, Void> checkLibraries(FileDownloader downloader)
				throws Exception {
			RemoteConfiguration remoteConfiguration = remoteConfiguration();
			
			if(remoteConfiguration == null) {
				return new EventBindableAction.OfNone<>(null); // Do not continue
			}
			
			boolean checkIntegrity = isCheckIntegrityEnabled();
			boolean checkLibraries = true;
			String versionLib = remoteConfiguration.value("lib");
			VersionEntryAccessor version = Versions.Common.lib();
			
			// If there is no integrity checking, we have to manually check the versions
			if(!checkIntegrity) {
				Version verLocal = version.get();
				Version verRemote = Version.of(versionLib);
				checkLibraries = verLocal.compareTo(verRemote) != 0 || verLocal == Version.UNKNOWN;
			}
			
			if(checkLibraries) {
				EventBinder binder = new EventBinder();
				
				FileChecker checker = localFileChecker(true, (path) -> true);
				String baseURL = Net.uriConcat(URL_BASE_LIB, versionLib);
				Updater updater = Updater.ofLibraries(baseURL, NIO.localPath(), TIMEOUT, checker, downloader, null);
				
				binder.register(CheckEvent.class, updater);
				binder.register(FileCheckEvent.class, checker);
				
				return new EventBindableAction.OfBinder<>(binder) {
					
					@Override
					public Void execute() throws Exception {
						if(updater.check()) {
							version.set(Version.of(versionLib));
						}
						
						return null;
					}
				};
			}
			
			return new EventBindableAction.OfNone<>(null);
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
				Request request = Request.of(Net.uri(versionFileURI())).GET();
				newestVersion = Ignore.defaultValue(() -> Version.of(Web.request(request).body()), Version.UNKNOWN);
			}
			return newestVersion;
		}
		
		public static final boolean updateDialog() {
			// If pre-release versions should be used, automatically accept the update.
			// This is due to the fact that all pre-release versions share configuration
			// and sometimes there can be incompatibilities, so just always use
			// the latest pre-release version.
			return usePreReleaseVersions()
						|| FXUtils.fxTaskValue(Update::showUpdateDialog);
		}
	}
	
	/** @since 00.02.07 */
	public static final class JarUpdater {
		
		private static final String NAME_JAR = "media-downloader.jar";
		private static final String NAME_JAR_NEW = "media-downloader-new.jar";
		
		// Forbid anyone to create an instance of this class
		private JarUpdater() {
		}
		
		private static final String remoteJarFileURI(Version version, boolean preRelease) {
			return URL_BASE_VER + version.stringRelease() + "/" + (preRelease ? "pre-release/" : "") + "application.jar";
		}
		
		public static final void doUpdateProcess(Version version, boolean preRelease, Arguments args,
				StringReceiver receiver) throws Exception {
			Path reqJar = PathSystem.getPath(NAME_JAR);
			Path newJar = PathSystem.getPath(NAME_JAR_NEW);
			
			if(args.has("jar-update") && args.has("pid")) {
				long pid = Long.valueOf(args.getValue("pid"));
				
				if(pid <= 0L) {
					throw new IllegalStateException("Invalid PID");
				}
				
				// Get the parent process
				ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
				
				// Check whether the old process still exists
				if(handle != null) {
					// Wait for it to finish
					receiver.receive("Waiting for the previous process to finish...");
					handle.onExit().get();
				}
				
				// Copy the new (current) JAR file to the required one
				receiver.receive("Replacing the old JAR file...");
				NIO.copyFile(newJar, reqJar);
				
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
				NIO.deleteFile(newJar);
			} else {
				String jarUrl = remoteJarFileURI(version, preRelease);
				
				// Check whether the remote JAR file exists or not
				do {
					try(Response response = Web.peek(Request.of(Net.uri(jarUrl)).HEAD())) {
						// If the remote file exists, we can continue in the process
						if(response.statusCode() == 200) break;
						
						// If pre-release version is not set we cannot do anything else.
						if(!preRelease) {
							throw new IllegalStateException("Remote file does not exist: " + jarUrl);
						}
						
						// Otherwise, try the full version instead of the pre-release one.
						preRelease = false;
						jarUrl = remoteJarFileURI(version, preRelease);
					}
				} while(true);
				
				try(FileDownloader downloader = new FileDownloader(new TrackerManager())) {
					downloader.addEventListener(DownloadEvent.BEGIN, (context) -> {
						receiver.receive("Downloading the new version...");
					});
					
					downloader.addEventListener(DownloadEvent.UPDATE, (context) -> {
						DownloadTracker tracker = (DownloadTracker) context.trackerManager().tracker();
						long current = tracker.current();
						long total = tracker.total();
						receiver.receive(String.format(Locale.US, "Downloading the new version... %.2f%%", current * 100.0 / total));
					});
					
					downloader.addEventListener(DownloadEvent.END, (context) -> {
						receiver.receive("Downloading the new version... done");
					});
					
					downloader.addEventListener(DownloadEvent.ERROR, (context) -> {
						error(context.exception());
					});
					
					// Download the new version's JAR file
					Request request = Request.of(Net.uri(jarUrl)).GET();
					downloader.start(request, newJar, DownloadConfiguration.ofDefault());
				}
				
				// Get the current run command, so that the application can be run again
				String runCommand = SelfProcess.command(args.argsList());
				runCommand = Base64.getEncoder().encodeToString(runCommand.getBytes(Shared.CHARSET));
				Path exePath = SelfProcess.exePath();
				
				// Start a new process to finish updating the application
				SelfProcess.launchJAR(newJar, exePath, List.of(
					"--jar-update",
					"--pid", String.valueOf(SelfProcess.pid()),
					"--run-command", runCommand,
					"--is-jar-update"
				));
				
				// Exit normally
				System.exit(0);
			}
		}
	}
	
	private static RemoteConfiguration remoteConfiguration;
	
	private static final void initRemoteConfiguration() throws IOException {
		if(!AppArguments.isUpdateEnabled()) {
			return;
		}
		
		try {
			String configURL = Net.uriConcat(URL_BASE_VER, VERSION.stringRelease(), "config");
			remoteConfiguration = RemoteConfiguration.from(
				Net.stream(Net.uri(configURL), Duration.ofMillis(TIMEOUT))
			);
		} catch(Exception ex) {
			errorDebug(ex);
		}
	}
	
	public static final RemoteConfiguration remoteConfiguration() {
		return remoteConfiguration;
	}
	
	/** @since 00.02.07 */
	public static final FileChecker localFileChecker(boolean checkRequirements, Predicate<Path> predicateComputeHash)
			throws Exception {
		Path currentDir = NIO.localPath();
		Path dir = NIO.localPath("lib/");
		FileChecker checker = new FileChecker.PrefixedFileChecker(dir, currentDir);
		
		// Generate list of all native libraries to check
		for(NativeLibrary library : NativeLibraries.all()) {
			Requirements requirements = Requirements.create(library.getOSName(), library.getOSArch());
			checker.addEntry(library.getPath(), requirements, library.getVersion());
		}
		
		// Generate list of all libraries to check
		for(Library library : libraries.all()) {
			checker.addEntry(library.path(), Requirements.ANY, "");
		}
		
		// Generate the list of entries
		checker.generate((path) -> true, checkRequirements, predicateComputeHash);
		return checker;
	}
	
	private static final void initExceptionHandlers() {
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> error(throwable));
		FXUtils.setExceptionHandler((throwable) -> error(throwable));
	}
	
	private static final void disableIllegalAccessWarnings() {
		IllegalAccessWarnings.tryDisable();
	}
	
	private static final void initAutoDispose() {
		Runtime.getRuntime().addShutdownHook(Threads.newThreadUnmanaged(MediaDownloader::dispose));
	}
	
	/** @since 00.02.09 */
	private static final void initInternalProtocol() {
		InternalURLProtocols.register(InternalProtocol.PROTOCOL_NAME, new InternalProtocol());
	}
	
	/** @since 00.02.09 */
	private static final class InternalProtocol extends URLStreamHandler {
		
		private static final String PROTOCOL_NAME = "internal";
		
		private InternalProtocol() {
		}
		
		@Override
		protected URLConnection openConnection(URL url) throws IOException {
			return new Connection(url);
		}
		
		private static final class Connection extends URLConnection {
			
			protected Connection(URL url) {
				super(checkUrl(url));
			}
			
			private static final URL checkUrl(URL url) {
				if(!url.getProtocol().equalsIgnoreCase(PROTOCOL_NAME)) {
					throw new IllegalArgumentException("Invalid protocol");
				}
				
				return url;
			}
			
			@Override
			public void connect() throws IOException {
				// Nothing to do
			}
			
			@Override
			public InputStream getInputStream() throws IOException {
				String path = url.getPath(); // Leave the path as is
				return InternalProtocol.class.getResourceAsStream(path);
			}
		}
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
		libraries.add(path, name);
	}
	
	private static final void registerNativeLibraries() {
		// No libraries
	}
	
	private static final void registerLibraries() {
		Path path = Path.of(PathSystem.getFullPath("lib/"));
		addLibrary(path.resolve("infomas-asl.jar"),      "infomas.asl");
		addLibrary(path.resolve("sune-memory.jar"),      "sune.memory");
		addLibrary(path.resolve("sune-process-api.jar"), "sune.api.process");
		addLibrary(path.resolve("jsoup.jar"),            "org.jsoup");
		// Define modules for builtin libraries so that plugins can use them
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		ModuleUtils.defineDummyModule("sune.app.mediadown", classLoader);
		ModuleUtils.defineDummyModule("sune.util.load", classLoader);
		ModuleUtils.defineDummyModule("ssdf2", classLoader);
	}
	
	private static final void registerResources() {
		RemoteConfiguration remoteConfiguration = remoteConfiguration();
		
		if(remoteConfiguration == null) {
			return; // Do not continue
		}
		
		Optional.ofNullable(remoteConfiguration.properties("res"))
		        .ifPresent((p) -> p.entrySet()
		                           .forEach((r) -> ResourcesManager.addResource(r.getKey(), r.getValue())));
	}
	
	private static final void initConfiguration() {
		Path configDir = NIO.localPath(BASE_RESOURCE).resolve("config");
		Ignore.callVoid(() -> NIO.createDir(configDir), MediaDownloader::error);
		
		Path configPath = configDir.resolve("application.ssdf");
		SSDCollection data = NIO.exists(configPath) ? SSDF.read(configPath.toFile()) : SSDCollection.empty();
		
		// Load the configuration
		configuration = new ApplicationConfigurationWrapper(configPath);
		configuration.loadData(data);
		
		// Check whether the application was probably updated
		applicationUpdated = !VERSION.equals(configuration.version());
		
		// Set configuration-dependant values early
		Web.defaultConnectTimeout(Duration.ofMillis(configuration.requestConnectTimeout()));
		Web.defaultReadTimeout(Duration.ofMillis(configuration.requestReadTimeout()));
		
		if(applicationUpdated) {
			UpdateTriggers.OfApplication.init(configuration.version(), VERSION);
			addApplicationUpdateTriggers();
			UpdateTriggers.OfApplication.run(UpdateTriggers.OfApplication.Stage.EARLY);
		}
	}
	
	private static final class ResourcesManager {
		
		private static final String OS_WIN64 = OSUtils.OS_NAME_WINDOWS + OSUtils.OS_ARCH_64;
		private static final String OS_UNX64 = OSUtils.OS_NAME_UNIX    + OSUtils.OS_ARCH_64;
		private static final String OS_MAC64 = OSUtils.OS_NAME_MACOS   + OSUtils.OS_ARCH_64;
		
		private static final boolean hasFlag(Set<String> flags, String flag) {
			return flags.isEmpty() || flags.contains(flag);
		}
		
		public static final void addResource(String name, RemoteConfiguration.Property property) {
			Set<String> flags = property.flags();
			String version = property.value();
			if(hasFlag(flags, OS_WIN64)) Resources.add(name, name, version, OS_WIN64);
			if(hasFlag(flags, OS_UNX64)) Resources.add(name, name, version, OS_UNX64);
			if(hasFlag(flags, OS_MAC64)) Resources.add(name, name, version, OS_MAC64);
		}
	}
	
	private static final class ResourcesUpdater {
		
		private static final Set<String> keepFiles = Set.of(
			"versions.ssdf", "messages.ssdf", "cm.store", "crd.store"
		);
		
		public static final void configuration(Version previousVersion) {
			Path configDir  = NIO.localPath(BASE_RESOURCE).resolve("config");
			Path configPath = configDir.resolve("application.ssdf");
			
			if(!NIO.exists(configDir)
					&& !Ignore.callAndCheck(() -> NIO.createDir(configDir), MediaDownloader::error)) {
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
				SSDCollection current = configuration.data();
				
				// Fix the theme, if needed
				if(current.getString(ApplicationConfiguration.PROPERTY_THEME, "default")
						  .equalsIgnoreCase("default"))
					current.set(ApplicationConfiguration.PROPERTY_THEME, Theme.ofDefault().name());
				
				// Remove the annotations at every object
				for(SSDObject object : current.objectsIterable()) {
					for(SSDAnnotation annotation : object.getAnnotations()) {
						object.removeAnnotation(annotation);
					}
				}
				
				if(previousVersion.compareTo(Version.of("00.02.07-dev.10")) <= 0) {
					// Uncheck resources integrity checking
					current.set(ApplicationConfiguration.PROPERTY_CHECK_RESOURCES_INTEGRITY, false);
				}
				
				if(configuration.usePreReleaseVersions() == UsePreReleaseVersions.UNKNOWN) {
					String preReleaseVersions = current.getString(ApplicationConfiguration.PROPERTY_USE_PRE_RELEASE_VERSIONS);
					// Convert the old boolean value to a new one
					UsePreReleaseVersions newValue
						= preReleaseVersions.equalsIgnoreCase("true")
								? UsePreReleaseVersions.TILL_NEXT_RELEASE
								: UsePreReleaseVersions.NEVER;
					
					configuration.configuration().writer()
						.set(ApplicationConfiguration.PROPERTY_USE_PRE_RELEASE_VERSIONS, newValue.name());
					configuration.reload();
				}
				
				// Save the updated configuration
				NIO.save(configPath, current.toString());
			} catch(IOException ex) {
				error(ex);
			}
		}
		
		private static final void language(Language current, boolean force) {
			InputStream stream = stream(BASE_RESOURCE, "language/" + current.name() + ".ssdf");
			if(stream == null) return; // Language does not exist internally
			
			try {
				Path pathLanguage = NIO.localPath(BASE_RESOURCE).resolve(current.path());
				Language internal = Language.from(current.path(), stream);
				
				// Check whether the internal version is higher than the current one
				if(force || internal.version().compareTo(current.version()) > 0) {
					// If so, replace the whole content, do not merge the files
					NIO.save(pathLanguage, internal.translation().getData().toString());
				} else {
					// Otherwise add missing fields from the internal to the current language
					Merger.ssdf(current.translation().getData(), internal.translation().getData());
					// Save the updated language file
					NIO.save(pathLanguage, current.translation().getData().toString());
				}
			} catch(IOException ex) {
				error(ex);
			}
		}
		
		public static final void languages(boolean force) {
			ResourceRegistry.languages.values().forEach((l) -> ResourcesUpdater.language(l, force));
		}
		
		/** @since 00.02.07 */
		private static final boolean hasInternalVersion(Theme current) {
			try {
				InputStreamResolver resolver = ((path) -> stream(BASE_RESOURCE, path));
				Theme internal = Theme.Reader.readInternal("theme/" + current.name(), resolver);
				return internal.version() != Version.UNKNOWN;
			} catch(IOException ex) {
				// Ignore
			}
			
			return false;
		}
		
		private static final void theme(Theme current, boolean force) {
			// Only internal themes can be automatically fixed since reference files
			// are available internally and can be re-extracted.
			if(!hasInternalVersion(current)) return;
			
			try {
				InputStreamResolver resolver = ((path) -> stream(BASE_RESOURCE, path));
				Theme internal = Theme.Reader.readInternal("theme/" + current.name(), resolver);
				Path pathTheme = current.externalPath();
				
				// Check whether the internal version is higher than the current one
				if(force || internal.version().compareTo(current.version()) > 0) {
					// If so, first delete all the theme files
					NIO.deleteDir(pathTheme);
					// Then just extract the theme files again
					internal.extract(pathTheme.getParent(), resolver);
				}
			} catch(Exception ex) {
				error(ex);
			}
		}
		
		public static final void themes(boolean force) {
			ResourceRegistry.themes.values().forEach((t) -> ResourcesUpdater.theme(t, force));
		}
		
		private static final Set<String> getLegacyDefaultPlugins() {
			Set<String> list = new HashSet<>();
			
			try {
				String urlLegacy = URL_BASE_DAT + "plugin/list";
				try(Response.OfStream response = Web.requestStream(Request.of(Net.uri(urlLegacy)).GET());
					BufferedReader reader = new BufferedReader(new InputStreamReader(response.stream()))) {
					reader.lines().forEach(list::add);
				}
			} catch(Exception ex) {
				error(ex);
			}
			
			return list;
		}
		
		private static final Set<String> getDefaultPlugins() {
			return Optional.ofNullable(
				Ignore.call(() -> PluginListObtainer.obtain().stream()
				                                    .map((p) -> p.b)
				                                    .collect(Collectors.toSet()))
			).orElseGet(Set::of);
		}
		
		private static final void removePlugins(Set<String> plugins) {
			for(String plugin : plugins) {
				String fileName = plugin.replaceAll("[^A-Za-z0-9]+", "-") + ".jar";
				Path path = NIO.localPath(BASE_RESOURCE, "plugin", fileName);
				Ignore.callVoid(() -> NIO.deleteFile(path), MediaDownloader::error);
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
			// Do nothing for now
		}
		
		/** @since 00.02.05 */
		public static final void messages(Version previousVersion) {
			// 00.02.04 -> 00.02.05: Messages format update (V0 -> V1)
			if(previousVersion.compareTo(Version.of("00.02.04")) <= 0) {
				// Do not bother with conversion and just remove the messages.ssdf file
				Ignore.callVoid(() -> NIO.deleteFile(NIO.localPath(BASE_RESOURCE).resolve("messages.ssdf")),
				                MediaDownloader::error);
			}
		}
		
		public static final void clean(Version previousVersion) {
			Path dir = NIO.localPath(BASE_RESOURCE);
			
			// Delete the old plugins directory
			Ignore.callVoid(() -> NIO.deleteDir(dir.resolve("plugins")), MediaDownloader::error);
			
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
			
			// Delete libraries that are not used anymore (are now built-in)
			if(previousVersion.compareTo(Version.of("00.02.07-dev.10")) <= 0) {
				// Delete the libraries ONLY if run from the JAR file (not from a development environment)
				if(SelfProcess.inJAR()) {
					Ignore.callVoid(() -> NIO.deleteFile(NIO.localPath("lib/ssdf2.jar")),
					                MediaDownloader::error);
				}
			}
			
			// Delete non-standard files
			try {
				for(Path file : Utils.iterable(Files.list(dir).iterator())) {
					if(!NIO.isRegularFile(file)
							|| keepFiles.contains(file.getFileName().toString()))
						continue;
					
					Ignore.callVoid(() -> NIO.deleteFile(file), MediaDownloader::error);
				}
			} catch(Exception ex) {
				error(ex);
			}
			
			// Delete empty directories
			try {
				for(Path file : Utils.iterable(Files.list(dir).iterator())) {
					if(!NIO.isDirectory(file) || !NIO.isEmptyDirectory(file))
						continue;
					
					Ignore.callVoid(() -> NIO.deleteFile(file), MediaDownloader::error);
				}
			} catch(Exception ex) {
				error(ex);
			}
		}
		
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
							
							if(node.isCollection()) pair.a.set(name, (SSDCollection) node);
							else                    pair.a.set(name, (SSDObject) node);
						} else if(node.isCollection()) {
							stack.push(new Pair<>((SSDCollection) node, pair.a.getDirectCollection(name)));
						}
					}
				}
				
				return changed;
			}
		}
	}
	
	/** @since 00.02.07 */
	public static final void updateResources() {
		updateResourcesDirectory(VERSION, true);
		
		// To prevent some issues delete the versions.ssdf file so that
		// all resources will have to be checked on the next start up.
		Ignore.callVoid(() -> NIO.deleteFile(NIO.localPath(BASE_RESOURCE).resolve("versions.ssdf")),
		                MediaDownloader::error);
		
		// To prevent some issues, re-save all registered configurations
		// to force all properties to be revalidated.
		saveConfigurations(allConfigurations());
	}
	
	/** @since 00.02.09 */
	private static final Set<Configuration> allConfigurations() {
		return Stream.concat(
			Stream.of(configuration()),
			Plugins.allLoaded().stream()
				.map(PluginFile::getConfiguration)
				.filter(Objects::nonNull)
				.filter(Predicate.not(PluginConfiguration::isEmpty))
		).collect(Collectors.toUnmodifiableSet());
	}
	
	/** @since 00.02.09 */
	private static final void saveConfigurations(Set<Configuration> configurations) {
		if(configurations == null || configurations.isEmpty()) {
			return; // Nothing to do
		}
		
		Path configDir = NIO.localPath(BASE_RESOURCE).resolve("config");
		configurations.stream().forEach(
			(c) -> Ignore.callVoid(
				() -> c.writer().save(configDir.resolve(c.name() + ".ssdf")),
				MediaDownloader::error
			)
		);
	}
	
	private static final void updateResourcesDirectory(Version previousVersion, boolean force) {
		ResourcesUpdater.configuration(previousVersion);
		ResourcesUpdater.languages(force);
		ResourcesUpdater.themes(force);
		ResourcesUpdater.plugins();
		ResourcesUpdater.binary();
		ResourcesUpdater.messages(previousVersion);
		ResourcesUpdater.clean(previousVersion);
	}
	
	private static final void saveConfiguration() {
		Ignore.callVoid(() -> NIO.save(configuration.path(), configuration.data().toString()), MediaDownloader::error);
	}
	
	private static final void finalizeConfiguration() {
		configuration.build();
		
		SSDCollection data = configuration.data();
		String propertyName;
		
		// Remove specified files, if any
		propertyName = ApplicationConfiguration.PROPERTY_REMOVE_AT_INIT;
		if(data.hasCollection(propertyName)) {
			for(SSDObject path : data.getCollection(propertyName).objectsIterable()) {
				Ignore.callVoid(() -> NIO.delete(NIO.path(path.stringValue())), MediaDownloader::error);
			}
			
			data.remove(propertyName);
			saveConfiguration();
		}
		
		if(applicationUpdated) {
			propertyName = ApplicationConfiguration.PROPERTY_VERSION;
			Version previousVersion = Version.of(data.getString(propertyName, VERSION.string()));
			
			// Automatically (i.e. without a prompt) update the resources directory
			updateResourcesDirectory(previousVersion, false);
			
			// Check configuration of using pre-release versions
			if(configuration.usePreReleaseVersions() == UsePreReleaseVersions.TILL_NEXT_RELEASE
					&& previousVersion.type() != VersionType.RELEASE
					&& VERSION        .type() == VersionType.RELEASE) {
				// Only ask if the current version is actually newer than the previous one
				if(VERSION.compareTo(previousVersion) > 0) {
					Translation tr = translation().getTranslation("dialogs.pre_release_versions.application_updated");
					if(!Dialog.showPrompt(tr.getSingle("title"), tr.getSingle("text"))) {
						configuration.configuration().writer().set(
							ApplicationConfigurationAccessor.PROPERTY_USE_PRE_RELEASE_VERSIONS,
							UsePreReleaseVersions.NEVER
						);
						configuration.reload();
					}
				}
			}
			
			// Update the version in the configuration file (even if the resources directory is not updated)
			data.set(propertyName, VERSION.string());
			saveConfiguration();
		}
	}
	
	/** @since 00.02.09 */
	private static final void addApplicationUpdateTriggers() {
		// Update computeStreamSize to the new default value
		UpdateTriggers.OfApplication.add(
			UpdateTriggers.OfApplication.Stage.EARLY,
			Version.ZERO,
			Version.of("00.02.09-dev.18"),
			() -> {
				Configuration.BooleanConfigurationProperty.Builder property
					= (Configuration.BooleanConfigurationProperty.Builder) configuration.builder.getProperty("computeStreamSize");
				
				@SuppressWarnings("unused") // Silence incorrect warning
				final boolean oldDefaultValue = true;
				final boolean newDefaultValue = property.defaultValue();
				boolean currentValue = configuration.computeStreamSize();
				
				if(currentValue == oldDefaultValue) {
					property.withValue(newDefaultValue);
				}
			}
		);
	}
	
	/** @since 00.02.09 */
	public static abstract class UpdateTriggers {
		
		protected final List<Trigger> triggers = new ArrayList<>();
		protected Version oldVersion;
		protected Version newVersion;
		
		protected UpdateTriggers() {
		}
		
		protected static final boolean intersect(Version aStart, Version aEnd, Version bStart, Version bEnd) {
			return bStart.compareTo(aEnd) <= 0 && bEnd.compareTo(aStart) >= 0;
		}
		
		protected void initVersions(Version oldVersion, Version newVersion) {
			this.oldVersion = Objects.requireNonNull(oldVersion);
			this.newVersion = Objects.requireNonNull(newVersion);
		}
		
		protected void addTrigger(Trigger trigger) {
			triggers.add(Objects.requireNonNull(trigger));
		}
		
		protected abstract boolean canRun(Trigger trigger);
		
		protected void runTriggers() {
			if(oldVersion == null || newVersion == null) {
				throw new IllegalArgumentException();
			}
			
			for(Trigger trigger : triggers) {
				if(!canRun(trigger)) {
					continue; // Do not run
				}
				
				try {
					trigger.run();
				} catch(Exception ex) {
					// Do not interrupt other triggers
					error(ex);
				}
			}
		}
		
		public static final class OfApplication extends UpdateTriggers {
			
			private static final OfApplication instance = new OfApplication();
			
			private Stage stage;
			
			private OfApplication() {
			}
			
			protected static final void init(Version oldVersion, Version newVersion) {
				instance.initVersions(oldVersion, newVersion);
			}
			
			protected static final void add(Stage stage, Version minVersion, Version maxVersion, CheckedRunnable action) {
				instance.addTrigger(new ApplicationTrigger(action, minVersion, maxVersion, stage));
			}
			
			protected static final void run(Stage stage) {
				instance.runTriggers(stage);
			}
			
			public static final void add(Version minVersion, Version maxVersion, CheckedRunnable action) {
				add(Stage.AFTER_CONFIGURATION_FINALIZATION, minVersion, maxVersion, action);
			}
			
			@Override
			protected final boolean canRun(Trigger trigger) {
				return ((ApplicationTrigger) trigger).stage() == stage
							&& intersect(oldVersion, newVersion, trigger.minVersion(), trigger.maxVersion());
			}
			
			protected final void runTriggers(Stage stage) {
				if(stage == null) {
					throw new IllegalArgumentException();
				}
				
				this.stage = stage;
				super.runTriggers();
			}
			
			protected static enum Stage {
				
				EARLY, AFTER_CONFIGURATION_FINALIZATION;
			}
			
			protected static final class ApplicationTrigger extends Trigger {
				
				private final Stage stage;
				
				private ApplicationTrigger(CheckedRunnable action, Version minVersion, Version maxVersion, Stage stage) {
					super(action, minVersion, maxVersion);
					this.stage = Objects.requireNonNull(stage);
				}
				
				public Stage stage() { return stage; }
			}
		}
		
		public static final class OfPlugin extends UpdateTriggers {
			
			private static final OfPlugin instance = new OfPlugin();
			private static final Map<String, Pair<Version, Version>> updates = new HashMap<>();
			
			private String pluginName;
			
			private OfPlugin() {
			}
			
			protected static final void run() {
				for(Entry<String, Pair<Version, Version>> update : updates.entrySet()) {
					String pluginName = update.getKey();
					Pair<Version, Version> value = update.getValue();
					Version oldVersion = value.a;
					Version newVersion = value.b;
					instance.initVersions(oldVersion, newVersion);
					instance.pluginName = pluginName;
					instance.runTriggers();
				}
			}
			
			protected static final void addUpdate(String pluginName, Version oldVersion, Version newVersion) {
				updates.put(pluginName, new Pair<>(oldVersion, newVersion));
			}
			
			public static final void add(String pluginName, Version minVersion, Version maxVersion, CheckedRunnable action) {
				instance.addTrigger(new PluginTrigger(action, minVersion, maxVersion, pluginName));
			}
			
			@Override
			protected final boolean canRun(Trigger trigger) {
				return ((PluginTrigger) trigger).pluginName().equals(pluginName)
							&& intersect(oldVersion, newVersion, trigger.minVersion(), trigger.maxVersion());
			}
			
			protected static final class PluginTrigger extends Trigger {
				
				private final String pluginName;
				
				private PluginTrigger(CheckedRunnable action, Version minVersion, Version maxVersion, String pluginName) {
					super(action, minVersion, maxVersion);
					this.pluginName = Objects.requireNonNull(pluginName);
				}
				
				public String pluginName() { return pluginName; }
			}
		}
		
		protected static class Trigger {
			
			protected final CheckedRunnable action;
			protected final Version minVersion;
			protected final Version maxVersion;
			
			protected Trigger(CheckedRunnable action, Version minVersion, Version maxVersion) {
				this.action = Objects.requireNonNull(action);
				this.minVersion = Objects.requireNonNull(minVersion);
				this.maxVersion = Objects.requireNonNull(maxVersion);
			}
			
			public void run() throws Exception { action.run(); }
			public Version minVersion() { return minVersion; }
			public Version maxVersion() { return maxVersion; }
		}
	}
	
	/** @since 00.02.09 */
	private static final Throwable maybeUnwrapThrowableForView(Throwable throwable) {
		if(throwable instanceof ExecutionException) {
			throwable = ((ExecutionException) throwable).getCause();
		}
		
		return throwable;
	}
	
	public static final void error(Throwable throwable) {
		if(throwable == null) {
			return; // Do nothing
		}
		
		throwable = maybeUnwrapThrowableForView(throwable);
		log.error(throwable, "An error occurred");
		
		if(FXUtils.isInitialized()) {
			// Display TranslatableException differently
			if(throwable instanceof TranslatableException) {
				TranslatableException exception = (TranslatableException) throwable;
				String title = "Error";
				String text = translation().getSingle(exception.translationPath());
				
				Throwable cause;
				if((cause = exception.getCause()) != null) {
					String content = Utils.throwableToString(cause);
					Dialog.showContentError(title, text, content);
				} else {
					Dialog.showError(title, text);
				}
			} else {
				FXUtils.showExceptionWindow(throwable);
			}
		} else {
			// FX not available, print to stderr
			throwable.printStackTrace();
		}
	}
	
	public static final void errorWithContent(String message, String content) {
		if(message == null) {
			return; // Do nothing
		}
		
		String text = message + "\n" + content;
		log.error(text);
		
		if(FXUtils.isInitialized()) {
			FXUtils.showExceptionWindow(message, content);
		} else {
			// FX not available, print to stderr
			System.err.println(text);
		}
	}
	
	/** @since 00.02.09 */
	public static final void errorDebug(Throwable throwable) {
		if(AppArguments.isDebugEnabled()) {
			error(throwable);
		}
	}
	
	private static final class InternalResources {
		
		public static final void addLanguage(String name, boolean isExtractable) {
			String path = "language/" + name;
			Language language = Language.from(path, stream(BASE_RESOURCE, path));
			ResourceRegistry.languages.registerValue(language.name(), language, isExtractable);
		}
		
		public static final void addTheme(Theme theme, boolean isExtractable) {
			ResourceRegistry.themes.registerValue(theme.name(), theme, isExtractable);
		}
		
		public static final void addIcon(String path, boolean isExtractable) {
			javafx.scene.image.Image icon = new javafx.scene.image.Image(stream(BASE_RESOURCE, "icon/" + path));
			ResourceRegistry.icons.registerValue(path, icon, isExtractable);
		}
		
		@SuppressWarnings("unused")
		public static final void addImage(String path, boolean isExtractable) {
			javafx.scene.image.Image image = new javafx.scene.image.Image(stream(BASE_RESOURCE, "image/" + path));
			ResourceRegistry.images.registerValue(path, image, isExtractable);
		}
		
		public static final void initializeDefaults() {
			// Languages
			addLanguage("english.ssdf", true);
			addLanguage("czech.ssdf", true);
			// Themes
			addTheme(Theme.ofLight(), true);
			addTheme(Theme.ofDark(), true);
			// Icons
			addIcon("automatic.png", false);
			addIcon("show.png", false);
			addIcon("hide.png", false);
		}
		
		private static final <T> List<String> extract(ResourceNamedRegistry<T> registry, String prefix, String suffix,
				String pathDest) {
			List<String> extracted = new ArrayList<>();
			if(registry.isEmpty()) return extracted; // Do not create the resource subfolder when no resources are present
			
			InputStreamResolver inputStreamResolver = ((path) -> stream(BASE_RESOURCE, path));
			Path folder = Path.of(pathDest);
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
			Ignore.callVoid(() -> NIO.createDir(Path.of(baseDest)), MediaDownloader::error);
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
		private static final List<String> internalLanguages = List.of("english", "czech");
		
		public static final boolean isInternalLanguage(Language language) {
			return language != null && internalLanguages.contains(language.name());
		}
		
		private static final Language getDefaultLanguage() {
			String path = "language/english.ssdf";
			return Language.from(path, stream(BASE_RESOURCE, path));
		}
		
		public static final boolean checkLanguage(Language language) {
			if((language == null))
				throw new NullPointerException();
			SSDCollection dataDefault = getDefaultLanguage().translation().getData();
	        SSDCollection dataCurrent = language            .translation().getData();
			return SSDFNamesChecker.check(dataDefault, dataCurrent);
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
				ResourceRegistry.languages.unregister(language.name());
				// Check whether we can replace it from the internal languages
				if((ExternalResourceChecker.isInternalLanguage(language))) {
					// Replace the language with the internal one
					InternalResources.addLanguage(language.name() + ".ssdf", true);
				}
			}
		}
	}
	
	private static final void addAutomaticLanguage() {
		// Add automatic language to the language resources
		Language autoLanguage = Languages.autoLanguage();
		ResourceRegistry.languages.registerValue(autoLanguage.name(), autoLanguage);
	}
	
	/** @since 00.02.09 */
	private static final boolean isCheckIntegrityEnabled() {
		// Always check integrity when the application is updated to ensure valid files,
		// but still respect the no-update flag.
		return AppArguments.isUpdateEnabled()
					&& (applicationUpdated || configuration.isCheckResourcesIntegrity());
	}
	
	private static final void loadMiscellaneousResources(StringReceiver stringReceiver) {
		boolean checkIntegrity = isCheckIntegrityEnabled();
		Set<Path> pathsToCheck = new HashSet<>();
		Path pathResources = NIO.localPath("resources/binary");
		
		// If there is no integrity checking, we have to manually check the versions
		if(!checkIntegrity) {
			for(InternalResource resource : Resources.localResources()) {
				Version verLocal = Versions.get("res_" + resource.name());
				Version verRemote = Version.of(resource.version());
				
				if(verLocal.compareTo(verRemote) != 0 || verLocal == Version.UNKNOWN) {
					Path path = pathResources.resolve(OSUtils.getExecutableName(resource.name()));
					pathsToCheck.add(path);
				}
			}
		}
		
		try {
			Resources.ensureResources(stringReceiver, (path) -> checkIntegrity || pathsToCheck.contains(path), null);
			
			for(InternalResource resource : Resources.localResources()) {
				VersionEntryAccessor version = VersionEntryAccessor.of("res_" + resource.name());
				Version verLocal = version.get();
				Version verRemote = Version.of(resource.version());
				
				if(verLocal.compareTo(verRemote) != 0 || verLocal == Version.UNKNOWN) {
					version.set(verRemote);
				}
			}
		} catch(Exception ex) {
			errorDebug(ex);
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
	
	/** @since 00.02.09 */
	private static final void initDefaults() throws ClassNotFoundException {
		// <---- Conversion providers
		Conversions.Providers.register("sune.app.mediadown.ffmpeg.FFmpeg$Provider");
		// Conversion providers ---->
		
		// <---- Configuration window
		// Ensure that main groups are in a specific order
		predefineGroups(
			ApplicationConfigurationAccessor.GROUP_GENERAL,
			ApplicationConfigurationAccessor.GROUP_DOWNLOAD,
			ApplicationConfigurationAccessor.GROUP_CONVERSION,
			ApplicationConfigurationAccessor.GROUP_NAMING,
			ApplicationConfigurationAccessor.GROUP_PLUGINS
		);
		// Built-in form fields
		registerFormField(isOfTypeClass(Password.class, PasswordField::new));
		registerFormField(isOfTypeClass(Language.class, SelectLanguageField::new));
		registerFormField(isOfTypeClass(Theme.class, SelectThemeField::new));
		registerFormField(isOfEnumClass(UsePreReleaseVersions.class, UsePreReleaseVersions::validValues,
			ValueTransformer.of(UsePreReleaseVersions::of, Enum::name, localValueTranslator(
				ApplicationConfigurationAccessor.PROPERTY_USE_PRE_RELEASE_VERSIONS, Enum::name
			))
		));
		registerFormField(isOfTypeClass(NamedMediaTitleFormat.class, SelectMediaTitleFormatField::new));
		registerFormField(isOfName(
			ApplicationConfigurationAccessor.PROPERTY_NAMING_CUSTOM_MEDIA_TITLE_FORMAT,
			TextFieldMediaTitleFormat::new
		));
		registerFormField(isOfTypeClass(ConversionProvider.class, typeFormFieldSupplier(
			Conversions.Providers.registry()::allValues,
			ValueTransformer.of(
				Conversions.Providers::ofName, ConversionProvider::name,
				localValueTranslator(
					ApplicationConfigurationAccessor.PROPERTY_CONVERSION_PROVIDER,
					ConversionProvider::name
				)
			)
		)));
		// Configuration ---->
	}
	
	private static final class GUI {
		
		private static final GUIRegistry<Window<?>> windows = new GUIRegistry<>(true);
		
		/** @since 00.02.09 */
		private static final <T> Initializator<T> initializator(Callable<T> callable) {
			return () -> FXUtils.fxTaskValue(callable);
		}
		
		private static final void registerWindows() {
			windows.register(MainWindow.NAME, initializator(MainWindow::new));
			windows.register(MediaGetterWindow.NAME, initializator(MediaGetterWindow::new));
			windows.register(DownloadConfigurationWindow.NAME, initializator(DownloadConfigurationWindow::new));
			windows.register(ConfigurationWindow.NAME, initializator(ConfigurationWindow::new));
			windows.register(TableWindow.NAME, initializator(TableWindow::new));
			windows.register(MessageWindow.NAME, initializator(MessageWindow::new));
			windows.register(MediaInfoWindow.NAME, initializator(MediaInfoWindow::new));
			windows.register(PreviewWindow.NAME, initializator(PreviewWindow::new));
			windows.register(ClipboardWatcherWindow.NAME, initializator(ClipboardWatcherWindow::new));
			windows.register(AboutWindow.NAME, initializator(AboutWindow::new));
			windows.register(ReportWindow.NAME, initializator(ReportWindow::new));
			windows.register(CredentialsWindow.NAME, initializator(CredentialsWindow::new));
			windows.register(CredentialsEditDialogWindow.NAME, initializator(CredentialsEditDialogWindow::new));
			windows.register(PluginManagerWindow.NAME, initializator(PluginManagerWindow::new));
		}
		
		/** @since 00.02.09 */
		private static interface Initializator<T> {
			
			T initialize() throws Exception;
		}
		
		/** @since 00.02.09 */
		private static final class GUIRegistry<T> {
			
			private final Map<String, RegistryEntry<T>> values = new HashMap<>();
			private final boolean isResetting;
			
			public GUIRegistry(boolean isResetting) {
				this.isResetting = isResetting;
			}
			
			private final RegistryEntry<T> newEntry(Initializator<T> initializator) {
				return isResetting
							? new RegistryEntry.OfResetting<>(initializator)
							: new RegistryEntry.OfCached<>(initializator);
			}
			
			public void register(String name, Initializator<T> initializator) {
				values.computeIfAbsent(name, (k) -> newEntry(initializator));
			}
			
			@SuppressWarnings("unused")
			public void unregister(String name) {
				values.remove(name);
			}
			
			public T get(String name) throws Exception {
				RegistryEntry<T> entry;
				return (entry = values.get(name)) != null ? entry.value() : null;
			}
			
			private static interface RegistryEntry<T> {
				
				static final Object NULL = new Object();
				
				public T value() throws Exception;
				
				static class OfResetting<T> implements RegistryEntry<T> {
					
					private final Initializator<T> initializator;
					
					public OfResetting(Initializator<T> initializator) {
						this.initializator = Objects.requireNonNull(initializator);
					}
					
					@Override
					public T value() throws Exception {
						return initializator.initialize();
					}
				}
				
				static class OfCached<T> implements RegistryEntry<T> {
					
					private final Initializator<T> initializator;
					private Object value = NULL;
					
					public OfCached(Initializator<T> initializator) {
						this.initializator = Objects.requireNonNull(initializator);
					}
					
					@Override
					public T value() throws Exception {
						if(value == NULL) {
							value = initializator.initialize();
						}
						
						@SuppressWarnings("unchecked")
						T casted = (T) value;
						return casted;
					}
				}
			}
		}
	}
	
	private static final class PluginListObtainer {
		
		private static final String URL_DIR = URL_BASE_DAT + "plugin/";
		
		private static final List<Pair<String, String>> parseList() throws Exception {
			RemoteConfiguration remoteConfiguration = remoteConfiguration();
			
			if(remoteConfiguration == null) {
				return List.of(); // Do not continue
			}
			
			List<Pair<String, String>> plugins = new ArrayList<>();
			Requirements requirements = Requirements.CURRENT;
			String version = VERSION.stringRelease();
			String listURL = Net.uriConcat(URL_BASE_VER, version, remoteConfiguration.value("plugin_list"));
			String prefix  = remoteConfiguration.value("plugin_prefix");
			
			try(Response.OfStream response = Web.requestStream(Request.of(Net.uri(listURL)).GET());
				BufferedReader reader = new BufferedReader(new InputStreamReader(response.stream()))) {
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
	
	/** @since 00.02.08 */
	private static final void bindPluginDownloadEvents(FileDownloader downloader) {
		final StringReceiver receiver = InitializationStates::setText;
		
		downloader.addEventListener(DownloadEvent.BEGIN, (context) -> {
			receiver.receive(String.format(
				"Downloading plugin %s...",
				context.output().getFileName().toString()
			));
		});
		
		downloader.addEventListener(DownloadEvent.END, (context) -> {
			receiver.receive(String.format(
				"Downloading plugin %s... done",
				context.output().getFileName().toString()
			));
		});
		
		downloader.addEventListener(DownloadEvent.UPDATE, (context) -> {
			DownloadTracker tracker = (DownloadTracker) context.trackerManager().tracker();
			long current = tracker.current();
			long total = tracker.total();
			double percent = current / (double) total;
			
			receiver.receive(String.format(
				"Downloading plugin %s... %.2f%%",
				context.output().getFileName().toString(),
				percent
			));
		});
		
		downloader.addEventListener(DownloadEvent.ERROR, (context) -> {
			error(context.exception());
		});
	}
	
	private static final void initDefaultPlugins() {
		if(!AppArguments.isUpdateEnabled()) return;
		
		try(FileDownloader downloader = new FileDownloader(new TrackerManager())) {
			DownloadConfiguration downloadConfiguration = DownloadConfiguration.ofDefault();
			bindPluginDownloadEvents(downloader);
			
			Regex regexOnlyAlphanum = Regex.of("[^A-Za-z0-9]+");
			Regex regexPluginPrefix = Regex.of("^plugin-");
			
			for(Pair<String, String> plugin : PluginListObtainer.obtain()) {
				String fileName = regexPluginPrefix.replaceFirst(regexOnlyAlphanum.replaceAll(plugin.b, "-"), "") + ".jar";
				Path path = NIO.localPath(BASE_RESOURCE, "plugin", fileName);
				
				if(!NIO.exists(path)) {
					Pair<String, Version> pluginInfo = PluginUpdater.newestVersionInfo(plugin.a);
					String versionUrl = pluginInfo.a;
					
					// Check whether there is a file available for the current application version
					if(versionUrl != null) {
						String jarUrl = Net.uriConcat(versionUrl, "plugin.jar");
						Request request = Request.of(Net.uri(jarUrl)).GET();
						
						NIO.createDir(path.getParent()); // Ensure parent directory
						downloader.start(request, path, downloadConfiguration);
						
						String pluginName = regexPluginPrefix.replaceFirst(regexOnlyAlphanum.replaceAll(plugin.b, "."), "");
						Version oldVersion = Version.ZERO;
						Version newVersion = pluginInfo.b;
						UpdateTriggers.OfPlugin.addUpdate(pluginName, oldVersion, newVersion);
						pluginConfigurationsToUpdate.add(pluginName);
					}
				}
			}
		} catch(Exception ex) {
			error(ex);
		}
	}
	
	private static final void registerPlugins() {
		Path dir = NIO.localPath(BASE_RESOURCE, "plugin");
		
		// Ignore, if no such folder exists
		if(!NIO.exists(dir)) return;
		
		// Find and register all the plugins
		try(FileDownloader downloader = new FileDownloader(new TrackerManager())) {
			final StringReceiver receiver = InitializationStates::setText;
			DownloadConfiguration downloadConfiguration = DownloadConfiguration.ofDefault();
			bindPluginDownloadEvents(downloader);
			
			Files.walk(dir)
				// Filter out only the files
				.filter((p) -> {
					return NIO.isRegularFile(p)
								&& Utils.OfPath.fileType(p).equalsIgnoreCase("jar");
				})
				// Sort files in the same directory lexicographically but put shorter names first
				.sorted((a, b) -> {
					// Check whether the files are in the same directory
					if(a.getParent().equals(b.getParent())) {
						String[] aNames = Utils.OfPath.fileName(a).split("-");
						String[] bNames = Utils.OfPath.fileName(b).split("-");
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
				.filter(Objects::nonNull)
				.map((plugin) -> {
					try {
						if(AppArguments.isUpdateEnabled()) {
							// Update the plugin, if needed
							if(configuration.isPluginsAutoUpdateCheck() || forceCheckPlugins) {
								receiver.receive(String.format(
									"Checking plugin %s...",
									plugin.getPlugin().instance().name()
								));
								
								String pluginURL = PluginUpdater.check(plugin);
								
								// Check whether there is a newer version of the plugin
								if(pluginURL != null) {
									Path file = Path.of(plugin.getPath());
									Version oldVersion = Version.of(plugin.getPlugin().instance().version());
									Request request = Request.of(Net.uri(pluginURL)).GET();
									
									NIO.createDir(file.getParent()); // Ensure parent directory
									NIO.deleteFile(file); // Ensure the file does not exist
									downloader.start(request, file, downloadConfiguration);
									
									// Must reload the plugin, otherwise it will have incorrect information
									PluginFile.resetPluginFileLoader();
									plugin = PluginFile.from(file);
									
									String pluginName = plugin.getPlugin().instance().name();
									Version newVersion = Version.of(plugin.getPlugin().instance().version());
									UpdateTriggers.OfPlugin.addUpdate(pluginName, oldVersion, newVersion);
									pluginConfigurationsToUpdate.add(pluginName);
								}
								
								receiver.receive(String.format(
									"Checking plugin %s... done",
									plugin.getPlugin().instance().name()
								));
							}
						}
					} catch(Exception ex) {
						error(ex);
					}
					
					return plugin;
				})
				// Add the plugin to the list, so it will be loaded
				.forEach(Plugins::add);
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
					               .filter((l) -> l.code().equalsIgnoreCase(code))
					               .findFirst().orElseGet(defaultLanguage);
		}
		
		public static final Language autoLanguage() {
			if(autoLanguage == null) {
				Language local = localLanguage();
				String currentLanguageName = configuration.data().getDirectString("language");
				boolean isCurrentLanguageAuto = currentLanguageName.equalsIgnoreCase("auto");
				Language currentLanguage = isCurrentLanguageAuto ? local : ResourceRegistry.language(currentLanguageName);
				String title = currentLanguage.translation().getSingle("generic.language.auto");
				autoLanguage = new Language("", "auto", Version.ZERO, title, "auto", local.translation());
			}
			return autoLanguage;
		}
		
		public static final Language localLanguage() {
			return localLanguage(() -> ResourceRegistry.language(defaultLanguageName()));
		}
		
		/** @since 00.02.07 */
		public static final String defaultLanguageName() {
			return "english";
		}
		
		/** @since 00.02.07 */
		public static final String defaultLanguageCode() {
			return "eng";
		}
		
		/** @since 00.02.07 */
		public static final Language currentLanguage() {
			Language language = language();
			return language.code().equalsIgnoreCase("auto") ? localLanguage() : language;
		}
		
		/** @since 00.02.09 */
		private static final Language defaultLanguage() {
			return DefaultLanguage.INSTANCE;
		}
		
		/**
		 * Used only when either the local language or any other language is still not
		 * available. This may happen during an early error when, for example, the program
		 * tries to show the Error window.
		 * @since 00.02.09
		 */
		private static final class DefaultLanguage {
			
			private static final Language INSTANCE = obtainInstance();
			private DefaultLanguage() {}
			
			private static final Language obtainInstance() {
				try(InputStream stream = stream("/resources/language", "/english.ssdf")) {
					return Language.from("", stream);
				} catch(IOException ex) {
					throw new IllegalStateException("Failed to obtain the default language", ex);
				}
			}
		}
	}
	
	/** @since 00.02.09 */
	public static final class Themes {
		
		private Themes() {
		}
		
		public static final Theme defaultTheme() {
			return Theme.ofDefault();
		}
	}
	
	public static final ApplicationConfiguration configuration() {
		return configuration.configuration();
	}
	
	public static final Language language() {
		Language language;
		if((language = configuration.language()) == null) {
			return Languages.defaultLanguage();
		}
		
		return language;
	}
	
	public static final Translation translation() {
		return language().translation();
	}
	
	public static final Theme theme() {
		Theme theme;
		if((theme = configuration.theme()) == null) {
			return Themes.defaultTheme();
		}
		
		return theme;
	}
	
	// Will be called automatically when closed properly
	protected static final void dispose() {
		if(!isDisposed.compareAndSet(false, true)) {
			return;
		}
		
		Ignore.callVoid(Plugins::dispose, MediaDownloader::error);
		Ignore.callVoid(Disposables::dispose, MediaDownloader::error);
		Ignore.callVoid(Threads::destroy, MediaDownloader::error);
		Ignore.callVoid(Web::clear, MediaDownloader::error);
	}
	
	// https://stackoverflow.com/questions/4159802/how-can-i-restart-a-java-application
	public static final void restart() {
		try {
			Path pathJava = Path.of(System.getProperty("java.home"), "bin", "java");
			Path pathJAR  = Path.of(MediaDownloader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
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
		// Ensure that all FX stuff is run in the FX thread
		FXUtils.thread(FXUtils::exit);
	}
	
	public static final Version version() {
		return VERSION;
	}
	
	public static final <W extends Window<?>> W window(String name) {
		@SuppressWarnings("unchecked")
		W casted = Ignore.call(() -> (W) GUI.windows.get(name), MediaDownloader::error);
		return casted;
	}
	
	// Forbid anyone to create an instance of this class
	private MediaDownloader() {
	}
	
	/** @since 00.02.07 */
	public static final class Versions {
		
		private static final Map<String, Version> versions = new TreeMap<>(comparator());
		private static SSDCollection data;
		
		// Forbid anyone to create an instance of this class
		private Versions() {
		}
		
		private static final Comparator<String> comparator() {
			return Comparator.nullsLast(String::compareTo);
		}
		
		private static final String normalizeName(String name) {
			return name.strip().toLowerCase();
		}
		
		private static final Path filePath() {
			return PathSystem.getPath("resources/versions.ssdf");
		}
		
		private static final void unchecked(CheckedRunnable op) {
			Ignore.callVoid(op);
		}
		
		public static final void load() {
			Path path = filePath();
			
			if(NIO.exists(path)) {
				data = SSDF.read(path.toFile());
				
				for(SSDObject item : data.objectsIterable()) {
					String name = normalizeName(item.getName());
					Version version = Version.of(item.stringValue());
					versions.put(name, version);
				}
			} else {
				data = SSDCollection.empty();
				save();
			}
		}
		
		public static final void save() {
			unchecked(() -> NIO.save(filePath(), data.toString()));
		}
		
		public static final boolean has(String name) {
			return versions.containsKey(normalizeName(name));
		}
		
		public static final Version get(String name) {
			return versions.getOrDefault(normalizeName(name), Version.UNKNOWN);
		}
		
		public static final void set(String name, Version version) {
			Objects.requireNonNull(version);
			
			String normalizedName = normalizeName(name);
			Version previous = versions.put(normalizedName, version);
			
			if(previous == null || !previous.equals(version)) {
				data.setDirect(normalizedName, version.string());
				save();
			}
		}
		
		public static final void remove(String name) {
			String normalizedName = normalizeName(name);
			versions.remove(normalizedName);
			
			if(data.hasDirect(name)) {
				data.removeDirect(normalizedName);
			}
			
			save();
		}
		
		public static final Map<String, Version> all() {
			return Collections.unmodifiableMap(versions);
		}
		
		public static final class Common {
			
			// Forbid anyone to create an instance of this class
			private Common() {
			}
			
			public static final VersionEntryAccessor jre() {
				return VersionEntryAccessor.of("jre");
			}
			
			public static final VersionEntryAccessor lib() {
				return VersionEntryAccessor.of("lib");
			}
			
			public static final VersionEntryAccessor ffmpeg() {
				return VersionEntryAccessor.of("ffmpeg");
			}
			
			public static final VersionEntryAccessor ffprobe() {
				return VersionEntryAccessor.of("ffprobe");
			}
			
			public static final VersionEntryAccessor pssuspend() {
				return VersionEntryAccessor.of("pssuspend");
			}
		}
		
		public static final class VersionEntryAccessor {
			
			private final String name;
			
			private VersionEntryAccessor(String name) {
				this.name = checkString(name);
			}
			
			private static final String checkString(String string) {
				if(string == null || string.isBlank())
					throw new IllegalArgumentException();
				
				return string;
			}
			
			public static final VersionEntryAccessor of(String name) {
				return new VersionEntryAccessor(name);
			}
			
			public Version get() {
				return Versions.get(name);
			}
			
			public void set(Version version) {
				Versions.set(name, version);
			}
			
			public String name() {
				return name;
			}
		}
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
		/** @since 00.02.08 */
		@Override public int requestConnectTimeout() { return accessor().requestConnectTimeout(); }
		/** @since 00.02.08 */
		@Override public int requestReadTimeout() { return accessor().requestReadTimeout(); }
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
		@Override public UsePreReleaseVersions usePreReleaseVersions() { return accessor().usePreReleaseVersions(); }
		/** @since 00.02.07 */
		@Override public boolean autoEnableClipboardWatcher() { return accessor().autoEnableClipboardWatcher(); }
		/** @since 00.02.09 */
		@Override public ConversionProvider conversionProvider() { return accessor().conversionProvider(); }
		/** @since 00.02.09 */
		@Override public boolean checkMessagesOnStartup() { return accessor().checkMessagesOnStartup(); }
		/** @since 00.02.09 */
		@Override public String reportEmail() { return accessor().reportEmail(); }
		@Override public SSDCollection data() { return accessor().data(); }
		/** @since 00.02.07 */
		@Override public boolean reload() { return accessor().reload(); }
		/** @since 00.02.07 */
		@Override public Path path() { return accessor().path(); }
		
		public ApplicationConfiguration configuration() { return configuration; }
	}
}