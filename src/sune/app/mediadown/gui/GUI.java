package sune.app.mediadown.gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.authentication.Credentials;
import sune.app.mediadown.authentication.CredentialsManager;
import sune.app.mediadown.authentication.EmailCredentials;
import sune.app.mediadown.authentication.UsernameCredentials;
import sune.app.mediadown.gui.window.ReportWindow;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.report.Report;
import sune.app.mediadown.report.Reporting;
import sune.app.mediadown.report.Reporting.ReportStatus;

/** @since 00.02.09 */
public final class GUI {
	
	// Forbid anyone to create an instance of this class
	private GUI() {
	}
	
	public static final void showReportWindow(Stage parent, Report.Builder builder,
			ReportWindow.Feature... features) {
		((ReportWindow) MediaDownloader.window(ReportWindow.NAME)).showWithFeatures(parent, builder, features);
	}
	
	public static final boolean report(Report report, boolean anonymize) {
		try {
			ReportStatus status = Reporting.send(report, anonymize);
			boolean success = status.isSuccess();
			Translation trr = MediaDownloader.translation().getTranslation("dialogs.report");
			
			if(success) {
				Translation tr = trr.getTranslation("success");
				Dialog.showInfo(tr.getSingle("title"), tr.getSingle("text"));
			} else {
				Translation tr = trr.getTranslation("error");
				String content = status.message();
				Dialog.showContentError(tr.getSingle("title"), tr.getSingle("text"), content);
			}
			
			return success;
		} catch(Exception ex) {
			MediaDownloader.error(ex);
		}
		
		return false;
	}
	
	/**
	 * <p>
	 * Differs from {@linkplain CredentialsManager} in registering only
	 * those credentials that are visible in the GUI.
	 * </p>
	 * 
	 * <p>
	 * This class provides unified place to register existing credentials
	 * that are configurable and should be displayed in the program.
	 * </p>
	 * 
	 * @since 00.02.09
	 */
	public static final class CredentialsRegistry {
		
		private static final Map<Class<? extends Credentials>, CredentialsType<?>> types = new HashMap<>();
		private static final Map<String, CredentialsEntry> entries = new HashMap<>();
		
		static {
			registerType(new CredentialsType.OfUsernameCredentials(typeTranslation("username_credentials")));
			registerType(new CredentialsType.OfEmailCredentials(typeTranslation("email_credentials")));
		}
		
		// Forbid anyone to create an instance of this class
		private CredentialsRegistry() {
		}
		
		private static final Translation typeTranslation(String name) {
			return MediaDownloader.translation().getTranslation("credentials.type." + name);
		}
		
		public static final <T extends Credentials> void registerType(CredentialsType<?> type) {
			Objects.requireNonNull(type);
			types.put(type.clazz(), type);
		}
		
		public static final void unregisterType(CredentialsType<?> type) {
			types.remove(Objects.requireNonNull(type).clazz());
		}
		
		public static final CredentialsType<?> typeOf(Credentials credentials) {
			Objects.requireNonNull(credentials);
			return types.get(credentials.getClass());
		}
		
		public static final void registerEntry(CredentialsEntry entry) {
			Objects.requireNonNull(entry);
			entries.put(entry.name(), entry);
		}
		
		public static final void unregisterEntry(String name) {
			entries.remove(Objects.requireNonNull(name));
		}
		
		public static final Collection<CredentialsEntry> entries() {
			return entries.values();
		}
		
		public static abstract class CredentialsType<T extends Credentials> {
			
			protected final Class<T> clazz;

			protected CredentialsType(Class<T> clazz) {
				this.clazz = clazz;
			}
			
			public abstract Pane create();
			public abstract void load(Pane pane, T credentials);
			public abstract T save(Pane pane);
			
			public Class<T> clazz() {
				return clazz;
			}
			
			public static class OfUsernameCredentials extends CredentialsType<UsernameCredentials> {
				
				private final Translation translation;
				
				protected OfUsernameCredentials(Translation translation) {
					super(UsernameCredentials.class);
					this.translation = Objects.requireNonNull(translation);
				}
				
				@Override
				public Pane create() {
					GridPane grid = new GridPane();
					grid.setHgap(5.0);
					grid.setVgap(5.0);
					Translation tr = translation.getTranslation("label");
					Label lblUsername = new Label(tr.getSingle("username"));
					TextField txtUsername = new TextField();
					txtUsername.getStyleClass().add("field-username");
					Label lblPassword = new Label(tr.getSingle("password"));
					PasswordField txtPassword = new PasswordField();
					txtPassword.getStyleClass().add("field-password");
					grid.getChildren().addAll(
						lblUsername, txtUsername,
						lblPassword, txtPassword
					);
					GridPane.setConstraints(lblUsername, 0, 0);
					GridPane.setConstraints(txtUsername, 1, 0);
					GridPane.setConstraints(lblPassword, 0, 1);
					GridPane.setConstraints(txtPassword, 1, 1);
					GridPane.setHgrow(txtUsername, Priority.ALWAYS);
					GridPane.setHgrow(txtPassword, Priority.ALWAYS);
					return grid;
				}
				
				@Override
				public void load(Pane pane, UsernameCredentials credentials) {
					GridPane grid = (GridPane) pane;
					TextField txtUsername = (TextField) grid.lookup(".field-username");
					PasswordField txtPassword = (PasswordField) grid.lookup(".field-password");
					txtUsername.setText(credentials.username());
					txtPassword.setText(credentials.password());
				}
				
				@Override
				public UsernameCredentials save(Pane pane) {
					GridPane grid = (GridPane) pane;
					TextField txtUsername = (TextField) grid.lookup(".field-username");
					PasswordField txtPassword = (PasswordField) grid.lookup(".field-password");
					String username = txtUsername.getText();
					String password = txtPassword.getText();
					return new UsernameCredentials(username, password);
				}
			}
			
			public static class OfEmailCredentials extends CredentialsType<EmailCredentials> {
				
				private final Translation translation;
				
				protected OfEmailCredentials(Translation translation) {
					super(EmailCredentials.class);
					this.translation = Objects.requireNonNull(translation);
				}
				
				@Override
				public Pane create() {
					GridPane grid = new GridPane();
					grid.setHgap(5.0);
					grid.setVgap(5.0);
					Translation tr = translation.getTranslation("label");
					Label lblEmail = new Label(tr.getSingle("email"));
					TextField txtEmail = new TextField();
					txtEmail.getStyleClass().add("field-email");
					Label lblPassword = new Label(tr.getSingle("password"));
					PasswordField txtPassword = new PasswordField();
					txtPassword.getStyleClass().add("field-password");
					grid.getChildren().addAll(
						lblEmail, txtEmail,
						lblPassword, txtPassword
					);
					GridPane.setConstraints(lblEmail, 0, 0);
					GridPane.setConstraints(txtEmail, 1, 0);
					GridPane.setConstraints(lblPassword, 0, 1);
					GridPane.setConstraints(txtPassword, 1, 1);
					GridPane.setHgrow(txtEmail, Priority.ALWAYS);
					GridPane.setHgrow(txtPassword, Priority.ALWAYS);
					return grid;
				}
				
				@Override
				public void load(Pane pane, EmailCredentials credentials) {
					GridPane grid = (GridPane) pane;
					TextField txtEmail = (TextField) grid.lookup(".field-email");
					PasswordField txtPassword = (PasswordField) grid.lookup(".field-password");
					txtEmail.setText(credentials.email());
					txtPassword.setText(credentials.password());
				}
				
				@Override
				public EmailCredentials save(Pane pane) {
					GridPane grid = (GridPane) pane;
					TextField txtEmail = (TextField) grid.lookup(".field-email");
					PasswordField txtPassword = (PasswordField) grid.lookup(".field-password");
					String email = txtEmail.getText();
					String password = txtPassword.getText();
					return new EmailCredentials(email, password);
				}
			}
		}
		
		public static final class CredentialsEntry {
			
			private final String name;
			private final String title;
			private final Supplier<Image> iconSupplier;
			private Image icon;
			
			private CredentialsEntry(String name, String title, Supplier<Image> iconSupplier) {
				this.name = Objects.requireNonNull(name);
				this.title = Objects.requireNonNull(title);
				this.iconSupplier = iconSupplier;
			}
			
			public static final CredentialsEntry of(String name) {
				return new CredentialsEntry(name, name, null);
			}
			
			public static final CredentialsEntry of(String name, String title) {
				return new CredentialsEntry(name, title, null);
			}
			
			public static final CredentialsEntry of(String name, String title, Supplier<Image> iconSupplier) {
				return new CredentialsEntry(name, title, iconSupplier);
			}
			
			public String name() {
				return name;
			}
			
			public String title() {
				return title;
			}
			
			public Image icon() {
				return icon == null ? (iconSupplier == null ? null : (icon = iconSupplier.get())) : icon;
			}
		}
	}
}