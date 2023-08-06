package sune.app.mediadown.util;

import java.util.Map;

/** @since 00.02.08 */
public class Metadata extends SimpleDataStorable {
	
	private static Metadata EMPTY;
	
	private Metadata() {
	}
	
	private Metadata(Map<String, Object> values) {
		super(values);
	}
	
	public static final Metadata create() {
		return new Metadata();
	}
	
	public static final Metadata of(Object... objects) {
		return new Metadata(Utils.stringKeyMap(objects));
	}
	
	public static final Metadata empty() {
		return EMPTY == null ? (EMPTY = new Immutable()) : EMPTY;
	}
	
	public static final Metadata mutable(Metadata metadata) {
		return metadata instanceof Immutable ? metadata.copy() : metadata;
	}
	
	public void setAll(Metadata metadata) {
		ensureOwnData().putAll(metadata.data);
	}
	
	public void removeAll(Metadata metadata) {
		if(isEmptyData()) {
			return;
		}
		
		data.keySet().removeAll(metadata.data.keySet());
	}
	
	public void clear() {
		if(isEmptyData()) {
			return;
		}
		
		data.clear();
	}
	
	public Metadata copy() {
		return new Metadata(data);
	}
	
	public Metadata seal() {
		return new Immutable(this);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(!super.equals(obj))
			return false;
		if(getClass() != obj.getClass())
			return false;
		return true;
	}
	
	private static final class Immutable extends Metadata {
		
		private Immutable() {
		}
		
		private Immutable(Metadata metadata) {
			super(metadata.data);
		}
		
		@Override public void set(String name, Object value) { /* Do nothing */ }
		@Override public void remove(String name) { /* Do nothing */ }
		
		@Override
		public int hashCode() {
			return super.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(!super.equals(obj))
				return false;
			if(getClass() != obj.getClass())
				return false;
			return true;
		}
	}
}