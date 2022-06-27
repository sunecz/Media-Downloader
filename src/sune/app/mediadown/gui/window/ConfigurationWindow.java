package sune.app.mediadown.gui.window;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
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
import sune.app.mediadown.configuration.Configuration;
import sune.app.mediadown.configuration.Configuration.ArrayConfigurationProperty;
import sune.app.mediadown.configuration.Configuration.ConfigurationProperty;
import sune.app.mediadown.configuration.Configuration.ConfigurationPropertyType;
import sune.app.mediadown.configuration.Configuration.NullTypeConfigurationProperty;
import sune.app.mediadown.configuration.Configuration.ObjectConfigurationProperty;
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
import sune.app.mediadown.util.Utils;
import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDObject;

public class ConfigurationWindow extends DraggableWindow<BorderPane> {
	
	public static final String NAME = "configuration";
	
	/** @since 00.02.04 */
	private final Map<Configuration, ConfigurationForms> configurations = new LinkedHashMap<>();
	/** @since 00.02.05 */
	private static final Map<String, BiFunction<String, String, FormField>> formFieldsRegistry = new HashMap<>();
	
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
	private static final boolean isObjectOrArray(Entry<String, ConfigurationProperty<?>> entry) {
		ConfigurationPropertyType type = entry.getValue().type();
		return !entry.getValue().isHidden() && (type == ConfigurationPropertyType.ARRAY || type == ConfigurationPropertyType.OBJECT);
	}
	
	/** @since 00.02.04 */
	private static final boolean isNotObjectOrArray(Entry<String, ConfigurationProperty<?>> entry) {
		ConfigurationPropertyType type = entry.getValue().type();
		return !entry.getValue().isHidden() && (type != ConfigurationPropertyType.ARRAY && type != ConfigurationPropertyType.OBJECT);
	}
	
	/** @since 00.02.04 */
	private static final Stream<Entry<String, ConfigurationProperty<?>>> collectionsEntryStream(Map<String, ConfigurationProperty<?>> map) {
		return map.entrySet().stream().filter(ConfigurationWindow::isObjectOrArray);
	}
	
	/** @since 00.02.04 */
	private static final Stream<Entry<String, ConfigurationProperty<?>>> objectsEntryStream(Map<String, ConfigurationProperty<?>> map) {
		return map.entrySet().stream().filter(ConfigurationWindow::isNotObjectOrArray);
	}
	
	/** @since 00.02.04 */
	private static final Map<String, ConfigurationProperty<?>> propertyToCollection(ConfigurationProperty<?> property) {
		switch(property.type()) {
			case ARRAY:  return ((ArrayConfigurationProperty)  property).value();
			case OBJECT: return ((ObjectConfigurationProperty) property).value();
			default:     return null;
		}
	}
	
	/** @since 00.02.04 */
	private static final FieldType getFieldType(ConfigurationProperty<?> property) {
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
			if(typeClass == null) return FieldType.TEXT;
			return typeClass == Language.class              ? FieldType.SELECT_LANGUAGE :
				   typeClass == Theme.class                 ? FieldType.SELECT_THEME :
				   typeClass == NamedMediaTitleFormat.class ? FieldType.SELECT_MEDIA_TITLE_FORMAT :
				   typeClass == Password.class              ? FieldType.TEXT_PASSWORD :
				   FieldType.TEXT;
		}
		switch(type) {
			case BOOLEAN: return FieldType.CHECKBOX;
			case INTEGER: return FieldType.INTEGER;
			case DECIMAL: return FieldType.TEXT; // Currently no decimal field type
			case STRING:
			case NULL:
				return FieldType.TEXT;
			default:
				throw new IllegalStateException("Unsupported field type for type: " + type); // Should not happen
		}
	}
	
	/** @since 00.02.05 */
	public static final void registerFormField(String name, BiFunction<String, String, FormField> supplier) {
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
	
	/** @since 00.02.04 */
	private final void loadConfigurations() {
		// Add the application configuratin
		addConfiguration(MediaDownloader.configuration(), translation);
		// Add configurations of loaded plugins
		Plugins.allLoaded().forEach(this::addPluginConfiguration);
		// Add all the forms and their tabs
		String defaultTabName = "general";
		configurations.entrySet().stream().forEach((entry) -> {
			ConfigurationForms data = entry.getValue();
			Translation translation = data.translation();
			String regexConfigName = '^' + Pattern.quote(entry.getKey().name());
			data.forms().forEach((form) -> {
				String tabName = Optional.ofNullable(form.getName())
						.orElse(defaultTabName)
						.replaceFirst(regexConfigName, "");
				if(tabName.isEmpty()) tabName = defaultTabName;
				StackPane content = new StackPane();
				Tab tab = new Tab(translation.getSingle("titles." + tabName), content);
				content.getChildren().add(form);
				tabPane.getTabs().add(tab);
			});
		});
	}
	
	/** @since 00.02.04 */
	private final void addPluginConfiguration(PluginFile plugin) {
		Configuration configuration = plugin.getConfiguration();
		if(configuration == null || configuration.isEmpty()) return; // Plugin has no configuration
		Translation translation = plugin.getInstance().translation();
		if(translation != null && translation.hasCollection("configuration"))
			translation = translation.getTranslation("configuration");
		if(translation == null) translation = this.translation; // Translation cannot be null
		addConfiguration(configuration, translation);
	}
	
	/** @since 00.02.04 */
	private final void addConfiguration(Configuration configuration, Translation translation) {
		Map<String, ConfigurationProperty<?>> rootProperties = configuration.rootProperties();
		List<Form> forms = new ArrayList<>();
		String configName = configuration.name();
		boolean isAppConfig = configName.equals(MediaDownloader.configuration().name());
		String rootName = isAppConfig ? null : configName;
		// Create all the forms with their fields
		Stream.concat(Stream.of(buildForm(configName, rootName, objectsEntryStream(rootProperties), translation)),
		              collectionsEntryStream(rootProperties)
		                    .map((entry) -> buildForm(configName,
		                                              (rootName != null ? rootName + '.' : "") + entry.getKey(),
		                                              objectsEntryStream(propertyToCollection(entry.getValue())),
		                                              translation)))
		      .filter(Objects::nonNull)
		      .forEach(forms::add);
		if(!forms.isEmpty()) {
			configurations.put(configuration, new ConfigurationForms(forms, translation));
		}
	}
	
	private final Form buildForm(String configName, String formName, Stream<Entry<String, ConfigurationProperty<?>>> data,
			Translation translation) {
		FormBuilder builder = new FormBuilder();
		boolean isEmpty = true;
		String regexConfigName = '^' + Pattern.quote(configName + '.');
		for(Entry<String, ConfigurationProperty<?>> entry : Utils.iterable(data.iterator())) {
			String name = (formName != null ? formName + '.' : "") + entry.getKey();
			ConfigurationProperty<?> property = entry.getValue();
			if(property.isHidden()) continue;
			String title = translation.getSingle("fields." + name.replaceFirst(regexConfigName, ""));
			FormField field = getFormField(property, name, title);
			SSDObject object = (SSDObject) property.toNode();
			field.setValue(object.getFormattedValue(), object.getType());
			builder.addField(field);
			isEmpty = false;
		}
		if(isEmpty) return null; // Indicate empty form, will be filtered out
		builder.setName(formName);
		builder.setPane(new VBox(5.0));
		return builder.build();
	}
	
	private final FormField getFormField(ConfigurationProperty<?> property, String name, String title) {
		// Allow injection of custom form fields
		BiFunction<String, String, FormField> supplier;
		if((supplier = formFieldsRegistry.get(name)) != null) {
			FormField field = supplier.apply(name, title);
			if(field != null) return field; // Custom form fields may be null
		}
		
		switch(getFieldType(property)) {
			case CHECKBOX:                  return new CheckBoxField(name, title);
			case INTEGER:                   return new IntegerField(name, title);
			case SELECT_LANGUAGE:           return new SelectLanguageField(name, title);
			case SELECT_THEME:              return new SelectThemeField(name, title);
			case SELECT_MEDIA_TITLE_FORMAT: return new SelectMediaTitleFormatField(name, title);
			case TEXT_PASSWORD:             return new PasswordField(name, title);
			default:                        return new TextField(name, title);
		}
	}
	
	private final void saveForm(String configName, SSDCollection data, Form form) {
		String regexConfigName = '^' + Pattern.quote(configName + '.');
		for(FormField field : form.getFields()) {
			String name = field.getName().replaceFirst(regexConfigName, "");
			data.set(name, SSDObject.of(field.getValue()));
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
		for(Entry<Configuration, ConfigurationForms> entry : configurations.entrySet()) {
			Configuration configuration = entry.getKey();
			String name = configuration.name();
			if(name == null || name.isEmpty()) continue;
			
			SSDCollection data = configuration.data();
			for(Form form : entry.getValue().forms()) {
				saveForm(name, data, form);
			}
			
			try {
				NIO.save(dir.resolve(name + ".ssdf"), data.toString());
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
	
	/** @since 00.02.04 */
	private static enum FieldType {
		
		// General types
		CHECKBOX, INTEGER, TEXT, TEXT_PASSWORD,
		// Specific types (should be removed in the future)
		SELECT_LANGUAGE, SELECT_THEME,
		/** @since 00.02.05 */
		SELECT_MEDIA_TITLE_FORMAT;
	}
	
	private static final class ConfigurationForms {
		
		private final List<Form> forms;
		private final Translation translation;
		
		public ConfigurationForms(List<Form> forms, Translation translation) {
			this.forms = Objects.requireNonNull(forms);
			this.translation = Objects.requireNonNull(translation);
		}
		
		public List<Form> forms() {
			return forms;
		}
		
		public Translation translation() {
			return translation;
		}
	}
}