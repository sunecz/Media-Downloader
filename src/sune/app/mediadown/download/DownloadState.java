package sune.app.mediadown.download;

import sune.app.mediadown.pipeline.Metrics;
import sune.app.mediadown.util.JSON.JSONCollection;
import sune.app.mediadown.util.JSON.JSONObject;
import sune.app.mediadown.util.JSONSerializable;

/** @since 00.02.09 */
public interface DownloadState extends JSONSerializable {
	
	Metrics metrics();
	
	@Override
	default JSONCollection serialize() {
		JSONCollection parent = JSONCollection.empty();
		Metrics metrics = metrics();
		
		for(String name : metrics.names()) {
			parent.set(name, JSONObject.of(metrics.get(name)));
		}
		
		return parent;
	}
}
