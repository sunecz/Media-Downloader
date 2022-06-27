package sune.app.mediadown.pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import sune.app.mediadown.util.Reflection2;
import sune.app.mediadown.util.Reflection2.InstanceCreationException;

public final class PipelineTaskRegistry {
	
	private static final Map<String, Class<?>> tasksMap = new HashMap<>();
	
	// Forbid anyone to create an instance of this class
	private PipelineTaskRegistry() {
	}
	
	public static final <T extends PipelineTask<?>> void register(String name, Class<T> clazz) {
		tasksMap.put(Objects.requireNonNull(name), Objects.requireNonNull(clazz));
	}
	
	@SuppressWarnings("unchecked")
	public static final <R extends PipelineResult<?>, T extends PipelineTask<R>> Class<T> clazz(String name) {
		return (Class<T>) tasksMap.get(name);
	}
	
	public static final <R extends PipelineResult<?>, T extends PipelineTask<R>> T instance(String name,
			PipelineTaskInputData data) throws InstanceCreationException {
		Class<T> clazz = clazz(name);
		if(clazz == null) return null;
		return Reflection2.newInstance(clazz, data);
	}
	
	public static final class PipelineTaskInputData {
		
		private final Map<String, Object> values;
		
		public PipelineTaskInputData(Map<String, Object> values) {
			this.values = new HashMap<>(values);
		}
		
		public boolean has(String name) {
			return values.containsKey(name);
		}
		
		public <T> T get(String name) {
			@SuppressWarnings("unchecked")
			T value = (T) values.get(name);
			return value;
		}
	}
}