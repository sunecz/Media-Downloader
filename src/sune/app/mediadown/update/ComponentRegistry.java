package sune.app.mediadown.update;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import sune.app.mediadown.net.Net;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.os.OS;
import sune.app.mediadown.util.JSON;
import sune.app.mediadown.util.JSON.JSONCollection;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public class ComponentRegistry {
	
	protected final String endpointUri;
	
	public ComponentRegistry(String endpointUri) {
		this.endpointUri = Objects.requireNonNull(endpointUri);
	}
	
	protected JSONCollection request(Web.Request request) throws Exception {
		try(Web.Response.OfStream response = Web.requestStream(request)) {
			if(response.statusCode() != 200) {
				throw new IllegalStateException("Error response from Component registry");
			}
			
			return JSON.read(response.stream());
		}
	}
	
	protected List<Artifact> artifacts(
		OS.Name osName,
		OS.Arch osArch,
		Arguments arguments,
		Channel channel
	) throws Exception {
		Map<String, Object> args = new LinkedHashMap<>();
		args.putAll(arguments.asMap());
		args.putAll(Map.of(
			"os", osName.name().toLowerCase(),
			"arch", osArch.name().toLowerCase(),
			"channel", channel.registryName()
		));
		args.put("args", Net.queryString(args));
		
		URI uri = Net.uri(Utils.format(endpointUri, args));
		JSONCollection result = request(Web.Request.of(uri).GET());
		ArtifactParser parser = new ArtifactParser();
		
		return (
			result.collectionsStream()
				.map(parser::parse)
				.collect(Collectors.toList())
		);
	}
	
	public List<Artifact> artifacts(Environment environment, Channel channel) throws Exception {
		return artifacts(environment.osName(), environment.osArch(), environment.arguments(), channel);
	}
	
	public String endpointUri() {
		return endpointUri;
	}
}
