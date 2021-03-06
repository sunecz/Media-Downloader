package sune.app.mediadown.gui.window;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.configuration.ApplicationConfigurationAccessor;
import sune.app.mediadown.configuration.Configuration;
import sune.app.mediadown.configuration.Configuration.ConfigurationProperty;
import sune.app.mediadown.configuration.Configuration.ConfigurationPropertyType;
import sune.app.mediadown.configuration.Configuration.NullTypeConfigurationProperty;
import sune.app.mediadown.configuration.Configuration.TypeConfigurationProperty;
import sune.app.mediadown.gui.Dialog;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormBuilder;
import sune.app.mediadown.gui.form.FormField;
import sune.app.mediadown.gui.form.field.CheckBoxField;
import sune.app.mediadown.gui.form.field.IntegerField;
import sune.app.mediadown.gui.form.field.PasswordField;
import sune.app.mediadown.gui.form.field.SelectLanguageField;
import sune.app.mediadown.gui.form.field.SelectMediaTitleFormatField;
import sune.app.mediadown.gui.form.field.SelectThemeField;
import sune.app.mediadown.gui.form.field.TextField;
import sune.app.mediadown.language.Language;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.media.MediaTitleFormats.NamedMediaTitleFormat;
import sune.app.mediadown.plugin.PluginFile;
import sune.app.mediadown.plugin.Plugins;
import sune.app.mediadown.theme.Theme;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.HorizontalLeftTabPaneSkin;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.Password;
import sune.app.mediadown.util.TriFunction;
import sune.app.mediadown.util.Utils;
import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDObject;
import sune.util.ssdf2.SSDType;

public class ConfigurationWindow extends DraggableWindow<BorderPane> {
	
	public static final String NAME = "configuration";
	
	/** @since 00.02.07 */
	private static final List<Form> forms = new ArrayList<>();
	/** @since 00.02.05 */
	private static final Map<String, TriFunction<ConfigurationFormFieldProperty,
	                                             String, String,
	                                             FormField<ConfigurationFormFieldProperty>>>
		formFieldsRegistry = new HashMap<>();
	/** @since 00.02.07 */
	private static final Map<String, FormBuilder> formBuilders = new LinkedHashMap<>();
	/** @since 00.02.07 */
	private static final Map<String, String> groupTitles = new HashMap<>();
	
	static {
		// Ensure that main groups are in a specific order
		Stream.of(
			ApplicationConfigurationAccessor.GROUP_GENERAL,
			ApplicationConfigurationAccessor.GROUP_DOWNLOAD,
			ApplicationConfigurationAccessor.GROUP_CONVERSION,
			ApplicationConfigurationAccessor.GROUP_NAMING,
			ApplicationConfigurationAccessor.GROUP_PLUGINS
		).forEach((s) -> formBuilders.put(s, new FormBuilder()));
	}
	
	private final TabPane tabPane;
	private final Button btnSave;
	private final Button btnClose;
	
	public ConfigurationWindow() {
		super(NAME, new BorderPane(), 600.0, 480.0);
		initModality(Modality.APPLICATION_MODAL);
		tabPane = new TabPane();
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabPane.setSide(Side.LEFT);
		Utils.ignore(() -> tabPane.setSkin(new HorizontalLeftTabPaneSkin(tabPane)));
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
		// Graphical fix for "extending" the tab pane header
		FXUtils.once(scene::addPostLayoutPulseListener,
		             scene::removePostLayoutPulseListener,
		             this::updateBackgroundExtender);
		// Load configurations later, so that all plugins can also append their configurations,
		// if they have any.
		FXUtils.onWindowShowOnce(this, this::loadConfigurations);
		// Center the window
		centerWindowAfterLayoutUpdate();
	}
	
	/** @since 00.02.04 */
	private static final ConfigurationFormFieldType getFieldType(ConfigurationProperty<?> property) {
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
			if(typeClass == null) return ConfigurationFormFieldType.TEXT;
			return typeClass == Language.class              ? ConfigurationFormFieldType.SELECT_LANGUAGE :
				   typeClass == Theme.class                 ? ConfigurationFormFieldType.SELECT_THEME :
				   typeClass == NamedMediaTitleFormat.class ? ConfigurationFormFieldType.SELECT_MEDIA_TITLE_FORMAT :
				   typeClass == Password.class              ? ConfigurationFormFieldType.TEXT_PASSWORD :
				   ConfigurationFormFieldType.TEXT;
		}
		switch(type) {
			case BOOLEAN: return ConfigurationFormFieldType.CHECKBOX;
			case INTEGER: return ConfigurationFormFieldType.INTEGER;
			case DECIMAL: return ConfigurationFormFieldType.TEXT; // Currently no decimal field type
			case STRING:
			case NULL:
				return ConfigurationFormFieldType.TEXT;
			default:
				throw new IllegalStateException("Unsupported field type for type: " + type); // Should not happen
		}
	}
	
	/** @since 00.02.05 */
	public static final void registerFormField(String name,
			TriFunction<ConfigurationFormFieldProperty,
			            String, String,
			            FormField<ConfigurationFormFieldProperty>> supplier) {
		formFieldsRegistry.put(Objects.requireNonNull(name), Objects.requireNonNull(supplier));
	}
	
	/** @since 00.02.05 */
	public static final void unregisterFormField(String name) {
		formFieldsRegistry.remove(Objects.requireNonNull(name));
	}
	
	/** @since 00.02.04 */
	private final void updateBackgroundExtender() {
		((Region) content.lookup(".buttons-panel-background"))
			.setMinWidth(((Region) tabPane.lookup(".headers-region")).getWidth() + 10.0);
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
		namePrefix = namePrefix.replaceFirst("^" + Pattern.quote(prefix + '.'), "");
		for(SSDObject item : translation.getTranslation("group").getData().objectsIterable()) {
			if(item.getType() != SSDType.STRING) continue;
			groupTitles.putIfAbsent(namePrefix + '.' + item.getName(), item.stringValue());
		}
	}
	
	/** @since 00.02.07 */
	private final Tab addFormAndCreateTab(String group, FormBuilder builder) {
		builder.name(group);
		builder.pane(new VBox(5.0));
		Form form = builder.build();
		forms.add(form);
		ScrollPane scrollable = new ScrollPane();
		scrollable.setFitToWidth(true);
		StackPane content = new StackPane();
		Tab tab = new Tab(groupTitle(group), scrollable);
		content.getChildren().add(form);
		content.getStyleClass().add("scroll-tab-content");
		scrollable.setContent(content);
		return tab;
	}
	
	/** @since 00.02.04 */
	private final void loadConfigurations() {
		// Add the application configuratin
		addConfiguration(MediaDownloader.configuration(), translation);
		// Add configurations of loaded plugins
		Plugins.allLoaded().forEach(this::addPluginConfiguration);
		
		tabPane.getTabs().addAll(
			formBuilders.entrySet().stream()
				.map((entry) -> addFormAndCreateTab(entry.getKey(), entry.getValue()))
				.toArray(Tab[]::new)
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
	}
	
	/** @since 00.02.07 */
	private final FormBuilder formBuilder(ConfigurationProperty<?> property) {
		String group = Optional.ofNullable(property.group()).orElse(ApplicationConfigurationAccessor.GROUP_GENERAL);
		return formBuilders.computeIfAbsent(group, (k) -> new FormBuilder());
	}
	
	/** @since 00.02.04 */
	private final void addConfiguration(Configuration configuration, Translation translation) {
		String configName = configuration.name();
		boolean isAppConfig = configName.equals(MediaDownloader.configuration().name());
		String formName = isAppConfig ? null : configName;
		String regexConfigName = '^' + Pattern.quote(configName + '.');
		
		for(Entry<String, ConfigurationProperty<?>> entry : configuration.properties().entrySet()) {
			ConfigurationProperty<?> property = entry.getValue();
			if(property.isHidden()) continue;
			
			// Currently objects and arrays are not supported
			ConfigurationPropertyType type = property.type();
			if(type == ConfigurationPropertyType.ARRAY
					|| type == ConfigurationPropertyType.OBJECT)
				continue;
			
			String name = (formName != null ? formName + '.' : "") + entry.getKey();
			String title = translation.getSingle("fields." + name.replaceFirst(regexConfigName, ""));
			SSDObject object = (SSDObject) property.toNode();
			
			FormField<ConfigurationFormFieldProperty> field = getFormField(configuration, property, name, title);
			field.value(object.getFormattedValue(), object.getType());
			formBuilder(property).addField(field);
		}
	}
	
	private final FormField<ConfigurationFormFieldProperty> getFormField(Configuration configuration,
			ConfigurationProperty<?> property, String name, String title) {
		ConfigurationFormFieldProperty fieldProperty = new ConfigurationFormFieldProperty(configuration, property);
		
		// Allow injection of custom form fields
		TriFunction<ConfigurationFormFieldProperty, String, String, FormField<ConfigurationFormFieldProperty>> supplier;
		if((supplier = formFieldsRegistry.get(name)) != null) {
			FormField<ConfigurationFormFieldProperty> field = supplier.apply(fieldProperty, name, title);
			if(field != null) return field; // Custom form fields may be null
		}
		
		switch(getFieldType(property)) {
			case CHECKBOX:                  return new CheckBoxField<>(fieldProperty, name, title);
			case INTEGER:                   return new IntegerField<>(fieldProperty, name, title);
			case SELECT_LANGUAGE:           return new SelectLanguageField<>(fieldProperty, name, title);
			case SELECT_THEME:              return new SelectThemeField<>(fieldProperty, name, title);
			case SELECT_MEDIA_TITLE_FORMAT: return new SelectMediaTitleFormatField<>(fieldProperty, name, title);
			case TEXT_PASSWORD:             return new PasswordField<>(fieldProperty, name, title);
			default:                        return new TextField<>(fieldProperty, name, title);
		}
	}
	
	private final void actionSave() {
		Path dir = NIO.localPath("resources/config");
		if(!NIO.exists(dir)
				&& !Utils.ignoreWithCheck(() -> NIO.createDir(dir), MediaDownloader::error)) {
			return; // Return, if error
		}
		
		String dialogTitle = translation.getSingle("dialog.save_title");
		StringBuilder errorContentBuilder = null;
		Set<Configuration> configurations = new LinkedHashSet<>();
		
		// Save all fields data in memory
		for(FormField<?> f : Utils.iterable(forms.stream().flatMap((f) -> f.fields().stream()).iterator())) {
			FormField<ConfigurationFormFieldProperty> field = Utils.cast(f);
			ConfigurationFormFieldProperty fieldProperty = field.property();
			Configuration configuration = fieldProperty.configuration();
			
			SSDCollection data = configuration.data();
			String regexConfigName = '^' + Pattern.quote(configuration.name() + '.');
			String name = field.name().replaceFirst(regexConfigName, "");
			data.set(name, SSDObject.of(field.value()));
			
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
		
		if(errorContentBuilder == null) {
			Dialog.showInfo(dialogTitle, translation.getSingle("dialog.save_success"));
			close();
		} else {
			Dialog.showContentError(dialogTitle, translation.getSingle("dialog.save_fail"),
			                        errorContentBuilder.toString());
		}
	}
	
	private final void actionClose() {
		close();
	}
	
	/** @since 00.02.07 */
	private static enum ConfigurationFormFieldType {
		
		// General types
		CHECKBOX, INTEGER, TEXT, TEXT_PASSWORD,
		// Specific types (should be removed in the future)
		SELECT_LANGUAGE, SELECT_THEME,
		/** @since 00.02.05 */
		SELECT_MEDIA_TITLE_FORMAT;
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
}