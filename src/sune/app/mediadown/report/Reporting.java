package sune.app.mediadown.report;

import java.net.URI;
import java.util.List;
import java.util.Set;

import sune.app.mediadown.net.Net;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.net.Web.Response;
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
			payload.set("environment", OSInformation.obtain(anonymize));
			
			return payload;
		}
		
		public static final Response.OfStream send(Report report, boolean anonymize) throws Exception {
			return Web.requestStream(request(report, anonymize));
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
				
				parent.set(name, System.getProperty(name).replace("\\", "\\\\"));
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