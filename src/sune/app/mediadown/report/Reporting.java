package sune.app.mediadown.report;

import java.net.URI;
import java.util.List;
import java.util.Set;

import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.configuration.ApplicationConfiguration;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.net.Web.Response;
import sune.app.mediadown.plugin.Plugin;
import sune.app.mediadown.plugin.PluginFile;
import sune.app.mediadown.plugin.Plugins;
import sune.app.mediadown.util.JSON;
import sune.app.mediadown.util.JSON.JSONCollection;
import sune.app.mediadown.util.JSON.JSONObject;

/** @since 00.02.09 */
public class Reporting {
	
	// Forbid anyone to create an instance of this class
	private Reporting() {
	}
	
	public static final JSONCollection payload(Report report, boolean anonymize) {
		return API.payload(report, anonymize);
	}
	
	public static final ReportStatus send(Report report, boolean anonymize) throws Exception {
		try(Response.OfStream response = API.send(report, anonymize)) {
			int code = response.statusCode();
			JSONCollection data = JSON.read(response.stream());
			
			if(code >= 200 && code < 300 && data.getBoolean("success")) {
				return ReportStatus.success();
			}
			
			return ReportStatus.error(data.getString("message"));
		}
	}
	
	public static final ContactInformation defaultContactInformation() {
		String email = MediaDownloader.configuration().reportEmail();
		
		if(email == null || email.isBlank()) {
			return null;
		}
		
		return new ContactInformation(email);
	}
	
	private static final class API {
		
		private static final URI APP_URI = Net.uri("https://app.sune.tech/mediadown/api/v1/");
		
		// Forbid anyone to create an instance of this class
		private API() {
		}
		
		private static final Request request(Report report, boolean anonymize) {
			URI uri = APP_URI.resolve("report");
			String body = payload(report, anonymize).toString(true);
			return Request.of(uri).POST(body, "application/json; charset=utf-8");
		}
		
		public static final JSONCollection payload(Report report, boolean anonymize) {
			JSONCollection payload = JSONCollection.empty();
			
			payload.set("name", report.name());
			payload.set("reason", report.reason().name());
			
			ContactInformation contact = report.contact();
			if(contact != null) {
				JSONCollection dataContact = JSONCollection.empty();
				dataContact.set("email", contact.email());
				payload.set("contact", dataContact);
			}
			
			payload.set("context", report.context().serialize(anonymize));
			payload.set("data", report.serializeData(anonymize));
			payload.set("note", JSONObject.of(report.note()));
			
			JSONCollection environment = JSONCollection.empty();
			environment.set("os", OSInformation.obtain(anonymize));
			environment.set("application", ApplicationInformation.obtain(anonymize));
			payload.set("environment", environment);
			
			return payload;
		}
		
		public static final Response.OfStream send(Report report, boolean anonymize) throws Exception {
			return Web.requestStream(request(report, anonymize));
		}
	}
	
	private static final class ApplicationInformation {
		
		private ApplicationInformation() {
		}
		
		private static final JSONCollection obtainConfiguration(boolean anonymize) {
			JSONCollection configuration = JSONCollection.empty();
			ApplicationConfiguration appConfig = MediaDownloader.configuration();
			
			configuration.set("accelerated_download", appConfig.acceleratedDownload());
			configuration.set("compute_stream_size", appConfig.computeStreamSize());
			configuration.set("is_auto_update_check", appConfig.isAutoUpdateCheck());
			configuration.set("is_check_resources_integrity", appConfig.isCheckResourcesIntegrity());
			configuration.set("is_plugins_auto_update_check", appConfig.isPluginsAutoUpdateCheck());
			configuration.set("parallel_conversions", appConfig.parallelConversions());
			configuration.set("parallel_downloads", appConfig.parallelDownloads());
			configuration.set("request_connect_timeout", appConfig.requestConnectTimeout());
			configuration.set("request_read_timeout", appConfig.requestReadTimeout());
			configuration.set("use_pre_release_versions", appConfig.usePreReleaseVersions().name());
			
			return configuration;
		}
		
		private static final JSONCollection obtainPlugins(boolean anonymize) {
			JSONCollection plugins = JSONCollection.emptyArray();
			
			for(PluginFile pluginFile : Plugins.allLoaded()) {
				JSONCollection plugin = JSONCollection.empty();
				Plugin pluginInstance = pluginFile.getPlugin().instance();
				
				plugin.set("name", pluginInstance.name());
				plugin.set("version", pluginInstance.version());
				plugin.set("author", pluginInstance.author());
				plugin.set("module_name", pluginInstance.moduleName());
				plugin.set("url", pluginInstance.url());
				
				plugins.add(plugin);
			}
			
			return plugins;
		}
		
		public static final JSONCollection obtain(boolean anonymize) {
			JSONCollection parent = JSONCollection.empty();
			
			parent.set("version", MediaDownloader.version().string());
			parent.set("language", MediaDownloader.language().name());
			parent.set("theme", MediaDownloader.theme().name());
			parent.set("configuration", obtainConfiguration(anonymize));
			parent.set("plugins", obtainPlugins(anonymize));
			
			return parent;
		}
	}
	
	private static final class OSInformation {
		
		private static final List<String> PROPERTY_NAMES = List.of(
			"java.home",
			"java.io.tmpdir",
			"java.vendor",
			"java.vendor.version",
			"java.version",
			"java.version.date",
			"java.vm.name",
			"java.vm.vendor",
			"os.arch",
			"os.name",
			"os.version",
			"user.country",
			"user.dir",
			"user.language"
		);
		
		private static final Set<String> ANONYMIZED_PROPERTY_NAMES = Set.of(
			"java.home",
			"java.io.tmpdir",
			"user.country",
			"user.dir",
			"user.language"
		);
		
		private OSInformation() {
		}
		
		public static final JSONCollection obtain(boolean anonymize) {
			JSONCollection parent = JSONCollection.empty();
			
			for(String name : PROPERTY_NAMES) {
				if(anonymize && ANONYMIZED_PROPERTY_NAMES.contains(name)) {
					continue;
				}
				
				String value = System.getProperty(name);
				
				if(value != null) {
					parent.set(name, value.replace("\\", "\\\\"));
				} else {
					parent.setNull(name);
				}
			}
			
			return parent;
		}
	}
	
	public static class ReportStatus {
		
		private static final ReportStatus SUCCESS = new ReportStatus(true, null);
		
		private final boolean success;
		private final String message;
		
		private ReportStatus(boolean success, String message) {
			this.success = success;
			this.message = message;
		}
		
		public static final ReportStatus success() {
			return SUCCESS;
		}
		
		public static final ReportStatus error(String message) {
			return new ReportStatus(false, message);
		}
		
		public boolean isSuccess() {
			return success;
		}
		
		public String message() {
			return message;
		}
	}
}