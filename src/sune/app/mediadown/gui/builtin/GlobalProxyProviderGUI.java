package sune.app.mediadown.gui.builtin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.authentication.Credentials;
import sune.app.mediadown.authentication.CredentialsManager;
import sune.app.mediadown.builtin.GlobalProxyProviderSettings;
import sune.app.mediadown.builtin.GlobalProxyProviderSettings.ProxyEndpoint;
import sune.app.mediadown.builtin.GlobalProxyProviderSettings.ProxyGroup;
import sune.app.mediadown.entity.ProxyProvider;
import sune.app.mediadown.gui.GUI.CredentialsRegistry;
import sune.app.mediadown.gui.GUI.CredentialsRegistry.CredentialsEntry;
import sune.app.mediadown.gui.configuration.ProxyProviderGUI;
import sune.app.mediadown.gui.util.FXUtils;
import sune.app.mediadown.gui.window.CredentialsEditDialogWindow;
import sune.app.mediadown.gui.window.CredentialsWindow;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.resource.ResourceRegistry;
import sune.util.ssdf2.SSDCollection;

/** @since 00.02.09 */
final class GlobalProxyProviderGUI implements ProxyProviderGUI {
	
	private static final String DEFAULT_CREDENTIALS_NAME = "proxy/default";
	
	private final Map<Web.Hint.Geolocation, ProxyGroup> groups;
	private Runnable saver;
	
	public GlobalProxyProviderGUI(GlobalProxyProviderSettings settings) {
		this.groups = settings != null ? new HashMap<>(settings.groups()) : new HashMap<>();
	}
	
	private final void openCredentialsWindow(String credentialsName) {
		CredentialsEntry entry = CredentialsRegistry.entry(credentialsName);
		
		if(entry == null) {
			return; // Entry doesn't exist
		}
		
		CredentialsEditDialogWindow window = MediaDownloader.window(CredentialsEditDialogWindow.NAME);
		Credentials credentials = window.showAndGet(MediaDownloader.window(CredentialsWindow.NAME), entry);
		
		if(credentials == null) {
			return; // Cancelled
		}
		
		try {
			CredentialsManager cm = CredentialsManager.instance();
			cm.set(entry.name(), credentials);
		} catch(IOException ex) {
			MediaDownloader.error(ex);
		}
	}
	
	private final ProxyEndpoint endpointOf(
		Web.Hint.Geolocation geolocation,
		Function<ProxyGroup, ProxyEndpoint> mapper
	) {
		return Optional.ofNullable(groups.get(geolocation)).map(mapper).orElse(null);
	}
	
	private final Supplier<ProxyEndpoint> addProxyEndpointRow(
		GridPane pane,
		int row,
		String title,
		Supplier<ProxyEndpoint> endpointSupplier
	) {
		Label lblValue = new Label(title);
		TextField txtValue = new TextField();
		Button btnCreds = new Button();
		Image imgCreds = ResourceRegistry.icon("credentials.png");
		btnCreds.setGraphic(FXUtils.toGraphics(imgCreds, 16.0));
		
		pane.getChildren().addAll(lblValue, txtValue, btnCreds);
		GridPane.setConstraints(lblValue, 0, row);
		GridPane.setConstraints(txtValue, 1, row);
		GridPane.setConstraints(btnCreds, 2, row);
		GridPane.setHgrow(txtValue, Priority.ALWAYS);
		
		ProxyEndpoint endpoint = endpointSupplier.get();
		String credsName = DEFAULT_CREDENTIALS_NAME;
		
		if(endpoint != null) {
			txtValue.setText(endpoint.uri().toString());
			credsName = endpoint.credentialsName();
		}
		
		final String _credsName = credsName;
		btnCreds.setOnAction((e) -> openCredentialsWindow(_credsName));
		
		return () -> {
			return new ProxyEndpoint(Net.uri(txtValue.getText()), _credsName);
		};
	}
	
	private final Pane createProxyGroupPane(Web.Hint.Geolocation geolocation) {
		GridPane pane = new GridPane();
		pane.setHgap(5.0);
		pane.setVgap(5.0);
		
		Supplier<ProxyEndpoint> forHttp = addProxyEndpointRow(
			pane, 0, "HTTP",
			() -> endpointOf(geolocation, ProxyGroup::forHttp)
		);
		Supplier<ProxyEndpoint> forHttps = addProxyEndpointRow(
			pane, 1, "HTTPS",
			() -> endpointOf(geolocation, ProxyGroup::forHttps)
		);
		
		saver = () -> {
			groups.put(geolocation, new ProxyGroup(forHttp.get(), forHttps.get()));
		};
		
		return pane;
	}
	
	@Override
	public Pane createPane() {
		VBox pane = new VBox(5.0);
		pane.getChildren().add(createProxyGroupPane(Web.Hint.Geolocation.ANY));
		return pane;
	}
	
	@Override
	public SSDCollection save() {
		saver.run(); // Update the values first
		return GlobalProxyProviderSettings.Save.save(groups);
	}
	
	static final class Factory implements ProxyProviderGUI.Factory {
		
		@Override
		public ProxyProviderGUI create(ProxyProvider.Settings settings) {
			if(!(settings instanceof GlobalProxyProviderSettings)) return null;
			return new GlobalProxyProviderGUI((GlobalProxyProviderSettings) settings);
		}
		
		@Override public String name() { return "global"; }
	}
}
