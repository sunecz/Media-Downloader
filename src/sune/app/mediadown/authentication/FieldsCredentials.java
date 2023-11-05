package sune.app.mediadown.authentication;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import sune.app.mediadown.util.JSON.JSONCollection;
import sune.app.mediadown.util.JSON.JSONObject;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public class FieldsCredentials implements Credentials, AutoCloseable {
	
	private Map<String, byte[]> fields;
	private boolean disposed;
	
	protected FieldsCredentials(Object... fields) {
		this(Utils.<String, byte[]>toMap(fields));
	}
	
	protected FieldsCredentials(Map<String, byte[]> fields) {
		this.fields = checkFields(fields);
	}
	
	protected static final Map<String, byte[]> checkFields(Map<String, byte[]> fields) {
		Objects.requireNonNull(fields);
		
		for(Entry<String, byte[]> field : fields.entrySet()) {
			Objects.requireNonNull(field.getKey());
		}
		
		return fields;
	}
	
	protected final void defineFields(Object... fields) {
		defineFields(Utils.<String, byte[]>toMap(fields));
	}
	
	protected final void defineFields(Map<String, byte[]> fields) {
		checkFields(fields);
		int size = this.fields.size() + fields.size();
		Map<String, byte[]> newFields = new LinkedHashMap<>(size);
		newFields.putAll(this.fields);
		newFields.putAll(fields);
		this.fields = newFields;
	}
	
	// Method to be reused by subclasses
	protected void serialize(JSONCollection json) {
		for(Entry<String, byte[]> field : fields.entrySet()) {
			json.set(field.getKey(), CredentialsUtils.string(field.getValue()));
		}
	}
	
	// Method to be reused by subclasses
	protected void deserialize(JSONCollection json) {
		for(JSONObject object : json.objectsIterable()) {
			String name = object.name();
			
			if(!fields.containsKey(name)) {
				continue; // Do not allow absent names
			}
			
			fields.put(name, CredentialsUtils.bytes(object.stringValue()));
		}
	}
	
	protected byte[] get(String name) {
		return fields.get(Objects.requireNonNull(name));
	}
	
	@Override
	public byte[] serialize() {
		JSONCollection data = JSONCollection.empty();
		
		if(isInitialized()) {
			serialize(data);
		}
		
		byte[] bytes = CredentialsUtils.bytes(data.toString(true));
		data.clear();
		return bytes;
	}
	
	@Override
	public void deserialize(byte[] data) {
		JSONCollection json = CredentialsUtils.json(data);
		deserialize(json);
		json.clear();
		disposed = false;
	}
	
	@Override
	public void dispose() {
		if(!isInitialized()) {
			return;
		}
		
		for(Entry<String, byte[]> field : fields.entrySet()) {
			CredentialsUtils.dispose(field.getValue());
			field.setValue(null);
		}
		
		disposed = true;
	}
	
	@Override
	public void close() {
		dispose();
	}
	
	@Override
	public boolean isInitialized() {
		return fields.values().stream().allMatch(Objects::nonNull);
	}
	
	@Override
	public boolean isDisposed() {
		return disposed;
	}
}