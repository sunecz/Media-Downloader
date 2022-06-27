package sune.app.mediadown.event;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/** @since 00.02.04 */
public final class EventSupport {
	
	// Forbid anyone to create an instance of this class
	private EventSupport() {
	}
	
	public static final <T> CompatibilityEventRegistry<T> compatibilityEventRegistry(Class<T> clazz) {
		return new CompatibilityEventRegistry<>(clazz);
	}
	
	public static class ArgsWrapper {
		
		private final List<Object> args;
		
		private ArgsWrapper(Object... args) {
			this.args = List.of(args);
		}
		
		public <T> T get(int index) {
			if(index < 0 || index >= args.size())
				throw new IndexOutOfBoundsException();
			@SuppressWarnings("unchecked")
			T castedArg = (T) args.get(index);
			return castedArg;
		}
		
		public List<Object> all() {
			return List.copyOf(args);
		}
		
		public int length() {
			return args.size();
		}
		
		public boolean isEmpty() {
			return args.isEmpty();
		}
	}
	
	public static class CompatibilityEventRegistry<T> extends EventRegistry<IEventType> {
		
		private final Class<T> clazz;
		private final Class<?>[] interfaces;
		private T proxy;
		
		private final Map<String, EventType<IEventType, ArgsWrapper>> eventTypes = new HashMap<>();
		
		private CompatibilityEventRegistry(Class<T> clazz) {
			if(!clazz.isInterface())
				throw new IllegalArgumentException("Not an interface: " + clazz.getName());
			this.clazz = clazz;
			this.interfaces = interfacesOf(clazz);
		}
		
		private static final Class<?>[] interfacesOf(Class<?> clazz) {
			List<Class<?>> interfaces = new ArrayList<>();
			
			Queue<Class<?>> interfacesToProcess = new ArrayDeque<>();
			interfacesToProcess.add(clazz);
			
			for(Class<?> current; (current = interfacesToProcess.poll()) != null;) {
				interfaces.add(current);
				interfacesToProcess.addAll(List.of(current.getInterfaces()));
			}
			
			return interfaces.toArray(Class[]::new);
		}
		
		private static final String methodNotFoundExceptionMessage(String methodName, Class<?>... classes) {
			StringBuilder builder = new StringBuilder();
			builder.append("No method found:");
			builder.append(" name=").append(methodName);
			if(classes.length == 0) {
				builder.append(" args=none");
			} else {
				builder.append(" args=[");
				boolean first = true;
				for(Class<?> clazz : classes) {
					if(first) first = false;
					else builder.append(", ");
					builder.append(clazz.getName());
				}
				builder.append("]");
			}
			return builder.toString();
		}
		
		private final Method findMethodNoArgs(String methodName) {
			// The order of interfaces is in the depth-ascending order, meaning that
			// the last interface to implement/override a method is found first.
			for(Class<?> clazz : interfaces) {
				for(Method method : clazz.getMethods()) {
					if(method.getName().equals(methodName))
						return method;
				}
			}
			// No method found, just throw an exception
			throw new IllegalStateException(methodNotFoundExceptionMessage(methodName));
		}
		
		private final Method findMethodWithArgs(String methodName, Class<?>... classes) {
			// The order of interfaces is in the depth-ascending order, meaning that
			// the last interface to implement/override a method is found first.
			for(Class<?> clazz : interfaces) {
				try {
					return clazz.getMethod(methodName, classes);
				} catch(NoSuchMethodException | SecurityException ex) {
					// Ignore and continue
				}
			}
			// No method found, just throw an exception
			throw new IllegalStateException(methodNotFoundExceptionMessage(methodName, classes));
		}
		
		private final String methodId(Method method) {
			return methodId(true, method.getName(), method.getParameterTypes());
		}
		
		private final String methodId(boolean givenArgs, String methodName, Class<?>... classes) {
			StringBuilder builder = new StringBuilder();
			builder.append(methodName);
			builder.append(':');
			builder.append(givenArgs ? 1 : 0);
			if(classes.length > 0) {
				builder.append(':');
				boolean first = true;
				for(Class<?> clazz : classes) {
					if(first) first = false;
					else builder.append(',');
					builder.append(clazz.getName());
				}
			}
			return builder.toString();
		}
		
		private final EventType<IEventType, ArgsWrapper> typeOf(boolean givenArgs, String methodId, Method method) {
			EventType<IEventType, ArgsWrapper> eventType = new EventType<>();
			eventTypes.put(methodId, eventType);
			if(!givenArgs) {
				// Recompute method id and values to the maps, so that the event type can also be obtained
				// using a method instance in the proxy class.
				methodId = methodId(method);
				eventTypes.put(methodId, eventType);
			}
			return eventType;
		}
		
		private final EventType<IEventType, ArgsWrapper> typeOf(Method method) {
			String methodId = methodId(method);
			EventType<IEventType, ArgsWrapper> eventType;
			return (eventType = eventTypes.get(methodId)) != null
						? eventType
						: typeOf(true, methodId, method);
		}
		
		public EventType<IEventType, ArgsWrapper> typeOf(String methodName) {
			String methodId = methodId(false, methodName);
			EventType<IEventType, ArgsWrapper> eventType;
			return (eventType = eventTypes.get(methodId)) != null
						? eventType
						: typeOf(false, methodId, findMethodNoArgs(methodName));
		}
		
		public EventType<IEventType, ArgsWrapper> typeOfWithArgs(String methodName, Class<?>... classes) {
			String methodId = methodId(true, methodName, classes);
			EventType<IEventType, ArgsWrapper> eventType;
			return (eventType = eventTypes.get(methodId)) != null
						? eventType
						: typeOf(true, methodId, findMethodWithArgs(methodName, classes));
		}
		
		public T proxy() {
			if(proxy == null) {
				@SuppressWarnings("unchecked")
				T localProxy = (T) Proxy.newProxyInstance(clazz.getClassLoader(), interfacesOf(clazz), new CompatibilityProxy());
				proxy = localProxy;
			}
			return proxy;
		}
		
		private final class CompatibilityProxy implements InvocationHandler {
			
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				call0(typeOf(method), new ArgsWrapper(args));
				return null;
			}
		}
	}
}