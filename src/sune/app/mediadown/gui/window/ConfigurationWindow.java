package sune.app.mediadown.gui.window;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import sune.app.mediadown.Arguments;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.MediaDownloader.JarUpdater;
import sune.app.mediadown.configuration.ApplicationConfiguration;
import sune.app.mediadown.configuration.ApplicationConfigurationAccessor;
import sune.app.mediadown.configuration.ApplicationConfigurationAccessor.UsePreReleaseVersions;
import sune.app.mediadown.configuration.Configuration;
import sune.app.mediadown.configuration.Configuration.ConfigurationProperty;
import sune.app.mediadown.configuration.Configuration.ConfigurationPropertyType;
import sune.app.mediadown.configuration.Configuration.NullTypeConfigurationProperty;
import sune.app.mediadown.configuration.Configuration.TypeConfigurationProperty;
import sune.app.mediadown.configuration.ConfigurationReloadable;
import sune.app.mediadown.gui.Dialog;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.gui.control.FixedWidthTreeView;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormBuilder;
import sune.app.mediadown.gui.form.FormField;
import sune.app.mediadown.gui.form.field.CheckBoxField;
import sune.app.mediadown.gui.form.field.IntegerField;
import sune.app.mediadown.gui.form.field.PasswordField;
import sune.app.mediadown.gui.form.field.SelectField;
import sune.app.mediadown.gui.form.field.SelectLanguageField;
import sune.app.mediadown.gui.form.field.SelectMediaTitleFormatField;
import sune.app.mediadown.gui.form.field.SelectThemeField;
import sune.app.mediadown.gui.form.field.TextField;
import sune.app.mediadown.gui.form.field.TextFieldMediaTitleFormat;
import sune.app.mediadown.gui.form.field.TranslatableSelectField;
import sune.app.mediadown.gui.form.field.TranslatableSelectField.ValueTransformer;
import sune.app.mediadown.language.Language;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.media.MediaTitleFormats.NamedMediaTitleFormat;
import sune.app.mediadown.plugin.PluginFile;
import sune.app.mediadown.plugin.Plugins;
import sune.app.mediadown.resource.Resources.StringReceiver;
import sune.app.mediadown.theme.Theme;
import sune.app.mediadown.update.Version;
import sune.app.mediadown.update.VersionType;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Password;
import sune.app.mediadown.util.Regex;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;
import sune.util.ssdf2.SSDObject;
import sune.util.ssdf2.SSDType;

public class ConfigurationWindow extends DraggableWindow<BorderPane> {
	
	public static final String NAME = "configuration";
	
	/** @since 00.02.07 */
	private static final List<FormFieldSupplierFactory> formFieldSupplierFactories = new LinkedList<>();
	/** @since 00.02.09 */
	private static final List<String> groups;
	
	static {
		// Ensure that main groups are in a specific order
		groups = List.of(
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
	}
	
	/** @since 00.02.07 */
	private final Map<String, FormBuilder> formBuilders = new LinkedHashMap<>();
	/** @since 00.02.07 */
	private final List<Form> forms = new ArrayList<>();
	/** @since 00.02.07 */
	private final Map<String, String> groupTitles = new HashMap<>();
	/** @since 00.02.08 */
	private final List<ConfigurationEntry> configurationEntries = new ArrayList<>();
	/** @since 00.02.09 */
	private final Map<String, String> groupParents = new HashMap<>();
	
	private final ConfigurationTabPane tabPane;
	private final Button btnSave;
	private final Button btnClose;
	
	public ConfigurationWindow() {
		super(NAME, new BorderPane(), 600.0, 480.0);
		initModality(Modality.APPLICATION_MODAL);
		tabPane = new ConfigurationTabPane();
		content.setCenter(tabPane);
		btnSave = new Button(translation.getSingle("buttons.save"));
		btnClose = new Button(translation.getSingle("buttons.close"));
		btnSave.setOnAction((e) -> actionSave());
		btnClose.setOnAction((e) -> actionClose());
		btnSave.setMinWidth(80.0);
		btnClose.setMinWidth(80.0);
		HBox boxButtonsPanel = new HBox();
		HBox boxButtonsBackground = new HBox();
		HBox boxFiller = new HBox();
		HBox boxButtonsWrapper = new HBox(5.0);
		boxButtonsPanel.getStyleClass().add("buttons-panel");
		boxButtonsBackground.getStyleClass().add("buttons-panel-background");
		boxButtonsWrapper.getStyleClass().add("buttons-panel-wrapper");
		boxButtonsWrapper.getChildren().addAll(btnSave, btnClose);
		boxButtonsPanel.getChildren().addAll(boxButtonsBackground, boxFiller, boxButtonsWrapper);
		boxButtonsPanel.setAlignment(Pos.CENTER_RIGHT);
		HBox.setHgrow(boxFiller, Priority.ALWAYS);
		content.setBottom(boxButtonsPanel);
		setMinWidth(350.0);
		// Load configurations later, so that all plugins can also append their configurations,
		// if they have any.
		FXUtils.onWindowShowOnce(this, this::loadConfigurations);
		// Center the window
		centerWindowAfterLayoutUpdate();
	}
	
	/** @since 00.02.07 */
	private static final FormFieldSupplier findFormFieldSupplier(String name,
			ConfigurationFormFieldProperty fieldProperty) {
		return formFieldSupplierFactories.stream()
					.map((factory) -> factory.create(name, fieldProperty))
					.filter(Objects::nonNull)
					.findFirst().orElse(null);
	}
	
	/** @since 00.02.07 */
	private static final <T> Function<T, String> localValueTranslator(String propertyName,
			Function<T, String> stringConverter) {
		Objects.requireNonNull(propertyName);
		return valueTranslator("windows." + NAME + ".values." + propertyName, stringConverter);
	}
	
	/** @since 00.02.07 */
	public static final <T extends Enum<?>> FormFieldSupplier enumFormFieldSupplier(Class<T> enumClass) {
		return enumFormFieldSupplier(enumClass, enumClass::getEnumConstants);
	}
	
	/** @since 00.02.07 */
	public static final <T extends Enum<?>> FormFieldSupplier enumFormFieldSupplier(Class<T> enumClass,
			Supplier<T[]> valuesSupplier) {
		return ((property, name, title) -> new SelectField<>(property, name, title, List.of(valuesSupplier.get())));
	}
	
	/** @since 00.02.07 */
	public static final <T extends Enum<?>> FormFieldSupplier enumFormFieldSupplier(Class<T> enumClass,
			Supplier<T[]> valuesSupplier, ValueTransformer<T> transformer) {
		return ((property, name, title) -> new TranslatableSelectField<>(property, name, title,
				List.of(valuesSupplier.get()), transformer));
	}
	
	/** @since 00.02.07 */
	public static final FormFieldSupplierFactory isOfTypeClass(Class<?> targetTypeClass, FormFieldSupplier supplier) {
		return ((name, fieldProperty) -> {
			ConfigurationProperty<?> property = fieldProperty.property();
			ConfigurationPropertyType type = property.type();
			
			if(type == ConfigurationPropertyType.ARRAY || type == ConfigurationPropertyType.OBJECT)
				return null; // No fields for containers
			
			if(type == ConfigurationPropertyType.STRING
					// Must use instanceof since no isTyped()-like method exists
					&& (property instanceof TypeConfigurationProperty || property instanceof NullTypeConfigurationProperty)) {
				Class<?> typeClass = (
					property instanceof TypeConfigurationProperty     ? ((TypeConfigurationProperty<?>)     property).typeClass() :
					property instanceof NullTypeConfigurationProperty ? ((NullTypeConfigurationProperty<?>) property).typeClass() :
					null
				);
				
				if(typeClass == targetTypeClass) {
					return supplier;
				}
			}
			
			return null;
		});
	}
	
	/** @since 00.02.07 */
	public static final <T extends Enum<?>> FormFieldSupplierFactory isOfEnumTypeClass(Class<T> targetEnumClass) {
		return isOfTypeClass(targetEnumClass, enumFormFieldSupplier(targetEnumClass));
	}
	
	/** @since 00.02.07 */
	public static final <T extends Enum<?>> FormFieldSupplierFactory isOfEnumClass(Class<T> targetEnumClass,
			Supplier<T[]> valuesSupplier) {
		return isOfTypeClass(targetEnumClass, enumFormFieldSupplier(targetEnumClass, valuesSupplier));
	}
	
	/** @since 00.02.07 */
	public static final <T extends Enum<?>> FormFieldSupplierFactory isOfEnumClass(Class<T> targetEnumClass,
			Supplier<T[]> valuesSupplier, ValueTransformer<T> transformer) {
		return isOfTypeClass(targetEnumClass, enumFormFieldSupplier(targetEnumClass, valuesSupplier, transformer));
	}
	
	/** @since 00.02.08 */
	public static final FormFieldSupplierFactory isOfName(String fieldName, FormFieldSupplier supplier) {
		return ((name, fieldProperty) -> name.equals(fieldName) ? supplier : null);
	}
	
	/** @since 00.02.07 */
	public static final void registerFormField(FormFieldSupplierFactory factory) {
		formFieldSupplierFactories.add(Objects.requireNonNull(factory));
	}
	
	/** @since 00.02.07 */
	public static final void unregisterFormField(FormFieldSupplierFactory factory) {
		formFieldSupplierFactories.remove(Objects.requireNonNull(factory));
	}
	
	/** @since 00.02.07 */
	public static final <T> Function<T, String> valueTranslator(String translationPath,
			Function<T, String> stringConverter) {
		Objects.requireNonNull(translationPath);
		Objects.requireNonNull(stringConverter);
		
		Translation tr = MediaDownloader.translation().getTranslation(translationPath);
		return ((v) -> tr.getSingle(stringConverter.apply(v)));
	}
	
	/** @since 00.02.07 */
	private final String groupTitle(String group) {
		Objects.requireNonNull(group);
		return translation.hasSingle("group." + group)
					? translation.getSingle("group." + group)
					: groupTitles.getOrDefault(group, group);
	}
	
	/** @since 00.02.07 */
	private final void addGroupTitlesFromTranslation(String prefix, Translation translation) {
		if(!translation.hasCollection("group")) return;
		String namePrefix = translation.getData().getParent().getFullName();
		namePrefix = namePrefix.replaceFirst("^" + Regex.quote(prefix + '.'), "");
		for(SSDObject item : translation.getTranslation("group").getData().objectsIterable()) {
			if(item.getType() != SSDType.STRING) continue;
			groupTitles.putIfAbsent(namePrefix + '.' + item.getName(), item.stringValue());
		}
	}
	
	/** @since 00.02.07 */
	private final ConfigurationTab addFormAndCreateTab(String parent, String group, FormBuilder builder) {
		builder.name(group);
		builder.pane(new VBox(5.0));
		Form form = builder.build();
		forms.add(form);
		ScrollPane scrollable = new ScrollPane();
		scrollable.setFitToWidth(true);
		StackPane content = new StackPane();
		ConfigurationTab tab = new ConfigurationTab(parent, group, groupTitle(group), scrollable);
		content.getChildren().add(form);
		content.getStyleClass().add("scroll-tab-content");
		scrollable.setContent(content);
		return tab;
	}
	
	/** @since 00.02.04 */
	private final void loadConfigurations() {
		// Prepare the form builders to ensure the correct order of groups
		groups.forEach((s) -> formBuilders.put(s, new FormBuilder()));
		
		// Add the application configuratin
		addConfiguration(MediaDownloader.configuration(), translation);
		// Add configurations of loaded plugins
		Plugins.allLoaded().forEach(this::addPluginConfiguration);
		
		// Finally, build forms of all added configurations
		buildConfigurationForms();
		
		tabPane.addTabs(
			formBuilders.entrySet().stream()
				.map((entry) -> addFormAndCreateTab(
					groupParents.get(Regex.of("\\.general$").replaceAll(entry.getKey(), "")),
					entry.getKey(), entry.getValue()
				))
				.toArray(ConfigurationTab[]::new)
		);
	}
	
	/** @since 00.02.04 */
	private final void addPluginConfiguration(PluginFile plugin) {
		Configuration configuration = plugin.getConfiguration();
		if(configuration == null || configuration.isEmpty()) return; // Plugin has no configuration
		
		Translation translation = plugin.getInstance().translation();
		if(translation != null && translation.hasCollection("configuration"))
			translation = translation.getTranslation("configuration");
		if(translation == null) translation = this.translation; // Translation cannot be null
		
		addGroupTitlesFromTranslation("plugin", translation);
		addConfiguration(configuration, translation);
		groupParents.putIfAbsent(configuration.name(), "plugins");
	}
	
	/** @since 00.02.07 */
	private final FormBuilder formBuilder(ConfigurationProperty<?> property) {
		String group = Optional.ofNullable(property.group()).orElse(ApplicationConfigurationAccessor.GROUP_GENERAL);
		return formBuilders.computeIfAbsent(group, (k) -> new FormBuilder());
	}
	
	/** @since 00.02.08 */
	private final void buildConfigurationForms() {
		configurationEntries.stream()
			.flatMap((c) -> c.properties().stream())
			.sorted()
			.forEachOrdered((p) -> formBuilder(p.property()).addField(propertyField(p)));
	}
	
	/** @since 00.02.08 */
	private final FormField<ConfigurationFormFieldProperty> propertyField(ConfigurationPropertyEntry entry) {
		SSDObject object = (SSDObject) entry.property().toNode();
		FormField<ConfigurationFormFieldProperty> field = getFormField(
			entry.configuration(), entry.property(), entry.name(), entry.title()
		);
		field.value(object.getFormattedValue(), object.getType());
		return field;
	}
	
	/** @since 00.02.04 */
	private final void addConfiguration(Configuration configuration, Translation translation) {
		configurationEntries.add(new ConfigurationEntry(configuration, translation));
	}
	
	private final FormField<ConfigurationFormFieldProperty> getFormField(Configuration configuration,
			ConfigurationProperty<?> property, String name, String title) {
		ConfigurationFormFieldProperty fieldProperty = new ConfigurationFormFieldProperty(configuration, property);
		
		// Allow injection of custom form fields
		FormFieldSupplier supplier;
		if((supplier = findFormFieldSupplier(name, fieldProperty)) != null) {
			FormField<ConfigurationFormFieldProperty> field = supplier.get(fieldProperty, name, title);
			if(field != null) return field; // Custom form fields may be null, so the check is needed
		}
		
		ConfigurationPropertyType type = property.type();
		switch(type) {
			case BOOLEAN: return new CheckBoxField<>(fieldProperty, name, title);
			case INTEGER: return new IntegerField<>(fieldProperty, name, title);
			case DECIMAL: return new TextField<>(fieldProperty, name, title); // Currently no decimal field type
			case STRING:
			case NULL:
				return new TextField<>(fieldProperty, name, title);
			default:
				throw new IllegalStateException("Unsupported field type for type: " + type); // Should not happen
		}
	}
	
	private final void actionSave() {
		Path dir = NIO.localPath("resources/config");
		if(!NIO.exists(dir)
				&& !Ignore.callAndCheck(() -> NIO.createDir(dir), MediaDownloader::error)) {
			return; // Return, if error
		}
		
		ApplicationConfiguration appConfiguration = MediaDownloader.configuration();
		String dialogTitle = translation.getSingle("dialog.save_title");
		StringBuilder errorContentBuilder = null;
		Set<Configuration> configurations = new LinkedHashSet<>();
		
		// Remember the old value for detecting changes
		UsePreReleaseVersions preReleaseVersionsOldValue = appConfiguration.usePreReleaseVersions();
		
		// Save all fields data in memory
		for(FormField<?> f : Utils.iterable(forms.stream().flatMap((f) -> f.fields().stream()).iterator())) {
			FormField<ConfigurationFormFieldProperty> field = Utils.cast(f);
			ConfigurationFormFieldProperty fieldProperty = field.property();
			Configuration configuration = fieldProperty.configuration();
			
			String regexConfigName = '^' + Regex.quote(configuration.name() + '.');
			String name = field.name().replaceFirst(regexConfigName, "");
			configuration.writer().set(name, field.value());
			
			configurations.add(configuration);
		}
		
		// Save all configurations to their respective files
		for(Configuration configuration : configurations) {
			try {
				NIO.save(dir.resolve(configuration.name() + ".ssdf"), configuration.data().toString());
			} catch(IOException ex) {
				(errorContentBuilder == null
						? errorContentBuilder = new StringBuilder()
						: errorContentBuilder)
					.append(Utils.throwableToString(ex))
					.append("\n");
			}
		}
		
		// Reload all configurations to reflect changes immediately
		configurations.stream()
			.filter((c) -> c instanceof ConfigurationReloadable)
			.map(Utils::<ConfigurationReloadable>cast)
			.forEach(ConfigurationReloadable::reload);
		
		if(errorContentBuilder == null) {
			Dialog.showInfo(dialogTitle, translation.getSingle("dialog.save_success"));
			
			// Before closing, check for changes with using pre-release versions
			UsePreReleaseVersions preReleaseVersionsNewValue = appConfiguration.usePreReleaseVersions();
			if(MediaDownloader.version().type() != VersionType.RELEASE
					&&  preReleaseVersionsNewValue == UsePreReleaseVersions.NEVER
					&& (preReleaseVersionsOldValue == UsePreReleaseVersions.TILL_NEXT_RELEASE
							|| preReleaseVersionsOldValue == UsePreReleaseVersions.ALWAYS)) {
				String trPath = "dialogs.pre_release_versions.configuration_changed";
				Translation tr = MediaDownloader.translation().getTranslation(trPath);
				
				if(Dialog.showPrompt(tr.getSingle("title"), tr.getSingle("text"))) {
					Version versionCurrentRelease = MediaDownloader.version().release();
					Version versionPreviousRelease = Version.UNKNOWN;
					
					// This will only work for 00.02.** versions but is should suffice
					if(versionCurrentRelease.patch() > 0) {
						versionPreviousRelease = Version.of(
							versionCurrentRelease.type(),
							versionCurrentRelease.major(),
							versionCurrentRelease.minor(),
							versionCurrentRelease.patch() - 1,
							versionCurrentRelease.value()
						);
					}
					
					if(versionPreviousRelease != Version.UNKNOWN) {
						Version targetVersion = versionPreviousRelease;
						Arguments args = MediaDownloader.arguments();
						StringReceiver receiver = ((s) -> { /* Do nothing */ });
						Ignore.callVoid(() -> JarUpdater.doUpdateProcess(targetVersion, false, args, receiver),
						                MediaDownloader::error);
					}
				}
			}
			
			close();
		} else {
			Dialog.showContentError(dialogTitle, translation.getSingle("dialog.save_fail"),
			                        errorContentBuilder.toString());
		}
	}
	
	private final void actionClose() {
		close();
	}
	
	/** @since 00.02.08 */
	private static final class ConfigurationPropertyEntry implements Comparable<ConfigurationPropertyEntry> {
		
		private final Configuration configuration;
		private final ConfigurationProperty<?> property;
		private final String name;
		private final String title;
		
		public ConfigurationPropertyEntry(Configuration configuration, ConfigurationProperty<?> property, String name,
		        String title) {
			this.configuration = configuration;
			this.property = property;
			this.name = name;
			this.title = title;
		}
		
		@Override
		public int compareTo(ConfigurationPropertyEntry o) {
			return Integer.compare(property.order(), o.property.order());
		}
		
		public Configuration configuration() { return configuration; }
		public ConfigurationProperty<?> property() { return property; }
		public String name() { return name; }
		public String title() { return title; }
	}
	
	/** @since 00.02.08 */
	private static final class ConfigurationEntry {
		
		private final Configuration configuration;
		private final Translation translation;
		
		public ConfigurationEntry(Configuration configuration, Translation translation) {
			this.configuration = Objects.requireNonNull(configuration);
			this.translation = Objects.requireNonNull(translation);
		}
		
		public List<ConfigurationPropertyEntry> properties() {
			Map<String, ConfigurationProperty<?>> properties = configuration.properties();
			List<ConfigurationPropertyEntry> entries = new ArrayList<>(properties.size());
			
			String configName = configuration.name();
			boolean isAppConfig = configName.equals(MediaDownloader.configuration().name());
			String formName = isAppConfig ? null : configName;
			String regexConfigName = '^' + Regex.quote(configName + '.');
			
			for(Entry<String, ConfigurationProperty<?>> entry : properties.entrySet()) {
				ConfigurationProperty<?> property = entry.getValue();
				if(property.isHidden()) continue;
				
				// Currently objects and arrays are not supported
				ConfigurationPropertyType type = property.type();
				if(type == ConfigurationPropertyType.ARRAY
						|| type == ConfigurationPropertyType.OBJECT) {
					continue;
				}
				
				String name = (formName != null ? formName + '.' : "") + entry.getKey();
				String title = translation.getSingle("fields." + name.replaceFirst(regexConfigName, ""));
				entries.add(new ConfigurationPropertyEntry(configuration, property, name, title));
			}
			
			return entries;
		}
	}
	
	/** @since 00.02.09 */
	private static final class ConfigurationTabPane extends BorderPane {
		
		private final Map<String, WeakReference<TreeItem<ConfigurationTab>>> parents = new HashMap<>();
		
		private final FixedWidthTreeView<ConfigurationTab> header;
		private final StackPane content;
		
		public ConfigurationTabPane() {
			header = new FixedWidthTreeView<>();
			content = new StackPane();
			
			header.setShowRoot(false);
			header.setRoot(new TreeItem<>());
			header.setEditable(false);
			
			header.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
			header.getSelectionModel().selectedItemProperty().addListener((o, ov, selectedItem) -> {
				if(selectedItem == null) {
					return;
				}
				
				content.getChildren().setAll(selectedItem.getValue().content());
			});
			
			addParent(null, root());
			
			ObservableList<TreeItem<ConfigurationTab>> children = root().getChildren();
			FXUtils.once(
				children::addListener, children::removeListener,
				(Change<? extends TreeItem<ConfigurationTab>> change) -> {
					header.getSelectionModel().clearSelection();
					header.getSelectionModel().select(root().getChildren().get(0));
				}
			);
			
			setLeft(header);
			setCenter(content);
			
			header.getStyleClass().add("configuration-tab-pane-header");
			content.getStyleClass().add("configuration-tab-pane-content");
			getStyleClass().add("configuration-tab-pane");
		}
		
		private final TreeItem<ConfigurationTab> root() {
			return header.getRoot();
		}
		
		private final void addParent(String parentName, TreeItem<ConfigurationTab> item) {
			parents.put(parentName, new WeakReference<>(item));
		}
		
		private final TreeItem<ConfigurationTab> parentOf(String parentName) {
			WeakReference<TreeItem<ConfigurationTab>> ref;
			return (ref = parents.get(parentName)) != null ? ref.get() : null;
		}
		
		public void addTabs(ConfigurationTab... tabs) {
			for(ConfigurationTab tab : tabs) {
				TreeItem<ConfigurationTab> item = new TreeItem<>(tab);
				addParent(tab.name(), item);
				parentOf(tab.parent()).getChildren().add(item);
			}
		}
	}
	
	/** @since 00.02.09 */
	private static final class ConfigurationTab {
		
		private final String parent;
		private final String name;
		private final String title;
		private final Node content;
		
		public ConfigurationTab(String parent, String name, String title, Node content) {
			this.parent = parent; // null = root
			this.name = Objects.requireNonNull(name);
			this.title = Objects.requireNonNull(title);
			this.content = Objects.requireNonNull(content);
		}
		
		public String parent() {
			return parent;
		}
		
		public String name() {
			return name;
		}
		
		public String title() {
			return title;
		}
		
		public Node content() {
			return content;
		}
		
		@Override
		public String toString() {
			return title();
		}
	}
	
	/** @since 00.02.07 */
	public static final class ConfigurationFormFieldProperty {
		
		private final Configuration configuration;
		private final ConfigurationProperty<?> property;
		
		public ConfigurationFormFieldProperty(Configuration configuration, ConfigurationProperty<?> property) {
			this.configuration = Objects.requireNonNull(configuration);
			this.property = Objects.requireNonNull(property);
		}
		
		public Configuration configuration() {
			return configuration;
		}
		
		public ConfigurationProperty<?> property() {
			return property;
		}
	}
	
	/** @since 00.02.07 */
	@FunctionalInterface
	public static interface FormFieldSupplier {
		
		FormField<ConfigurationFormFieldProperty> get(ConfigurationFormFieldProperty fieldProperty,
				String name, String title);
	}
	
	/** @since 00.02.07 */
	@FunctionalInterface
	public static interface FormFieldSupplierFactory {
		
		FormFieldSupplier create(String name, ConfigurationFormFieldProperty fieldProperty);
	}
}