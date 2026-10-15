package sune.app.mediadown.gui.configuration;

import static sune.app.mediadown.gui.window.ConfigurationWindow.localValueTranslator;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import sune.app.mediadown.configuration.ApplicationConfigurationAccessor;
import sune.app.mediadown.entity.ProxyProvider;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.app.mediadown.gui.form.FormFieldType;
import sune.app.mediadown.gui.form.field.TranslatableSelectField;
import sune.app.mediadown.gui.form.field.TranslatableSelectField.ValueTransformer;
import sune.app.mediadown.gui.window.ConfigurationWindow.ConfigurationFormFieldProperty;
import sune.app.mediadown.gui.window.ConfigurationWindow.FormFieldSupplier;
import sune.app.mediadown.net.Transport.ProxyManager;
import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDNode;

/** @since 00.02.09 */
public class ProxyProviderFormFieldSupplier implements FormFieldSupplier {
	
	@Override
	public FormField<ConfigurationFormFieldProperty> get(
		ConfigurationFormFieldProperty fieldProperty,
		String name,
		String title
	) {
		return new Field(fieldProperty, FormFieldType.OTHER, name, title);
	}
	
	static final class Field extends FormField<ConfigurationFormFieldProperty> {
		
		private final TranslatableSelectField<ConfigurationFormFieldProperty, ProxyProvider.Factory> cmbProvider;
		private ProxyProviderGUI guiProvider;
		private ProxyProvider.Factory providerFactory;
		private SSDCollection value;
		
		public Field(
			ConfigurationFormFieldProperty property,
			FormFieldType type,
			String name,
			String title
		) {
			super(property, type, name, title);
			this.cmbProvider = new TranslatableSelectField<>(
				property,
				name,
				title,
				ProxyManager.factories(),
				ValueTransformer.of(
					ProxyManager::factory,
					ProxyProvider.Factory::name,
					localValueTranslator(
						ApplicationConfigurationAccessor.PROPERTY_PROXY_PROVIDER_FACTORY,
						ProxyProvider.Factory::name
					)
				)
			);
			cmbProvider.control().valueProperty().addListener((o, ov, nv) -> providerChanged(nv));
		}
		
		private final void providerChanged(ProxyProvider.Factory factory) {
			if(value == null) return;
			this.providerFactory = factory;
			String name = factory.name();
			ProxyProviderGUI.Factory guiProviderFactory = ProxyProviderGUIRegistry.factory(name);
			wrapper.getChildren().clear();
			if(guiProviderFactory != null) {
				SSDCollection settings = value.getDirectCollection("settings", null);
				guiProvider = guiProviderFactory.create(providerFactory.loadSettings(settings));
				wrapper.getChildren().add(guiProvider.createPane());
			} else {
				guiProvider = null;
			}
		}
		
		private final StackPane wrapper = new StackPane();
		
		@Override
		public Node render(Form form) {
			VBox pane = new VBox(5.0);
			pane.getChildren().add(cmbProvider.render(form));
			if(guiProvider != null) {
				wrapper.getChildren().add(guiProvider.createPane());
			}
			pane.getChildren().add(wrapper);
			return pane;
		}
		
		@Override
		public void value(SSDNode node) {
			if(!(node instanceof SSDCollection)) {
				return;
			}
			SSDCollection coll = (SSDCollection) node;
			String name = coll.getDirectString("name");
			cmbProvider.value(coll.get("name"));
			this.value = coll.copy();
			this.providerFactory = ProxyManager.factory(name);
			providerChanged(providerFactory);
		}
		
		@Override
		public Object value() {
			value.set("name", cmbProvider.value());
			if(guiProvider != null) {
				value.set("settings", guiProvider.save());
			} else {
				value.remove("settings");
			}
			return value;
		}
	}
}
