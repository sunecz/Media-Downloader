package sune.app.mediadown.authentication;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sune.app.mediadown.configuration.Configuration;
import sune.app.mediadown.configuration.Configuration.ConfigurationProperty;
import sune.app.mediadown.exception.UncheckedException;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.unsafe.Reflection;

/** @since 00.02.09 */
public final class CredentialsMigrator {
	
	private final Credentials credentials;
	private ConfigurationCleaner cleaner;
	
	private CredentialsMigrator(Credentials credentials, ConfigurationCleaner cleaner) {
		this.credentials = credentials;
		this.cleaner = cleaner;
	}
	
	private static final CredentialsManager credentialsManager() throws IOException {
		return CredentialsManager.instance();
	}
	
	public static final boolean isMigrated(String name) throws IOException {
		return credentialsManager().has(name);
	}
	
	public static final Builder ofConfiguration(Configuration.Builder configuration, String... propertyNames) {
		return new Builder(configuration, propertyNames);
	}
	
	private final void maybeClear() {
		if(cleaner == null) {
			return; // Nothing to clear
		}
		
		cleaner.clear();
		cleaner = null;
	}
	
	public final void migrate(String name) throws IOException {
		Objects.requireNonNull(name);
		
		if(credentials == null) {
			return; // Do nothing
		}
		
		if(credentials.isDisposed()) {
			throw new IllegalStateException("Credentials disposed");
		}
		
		try {
			CredentialsManager cm = credentialsManager();
			cm.set(name, credentials);
		} finally {
			credentials.dispose();
			maybeClear();
		}
	}
	
	public final boolean migrateIfAbsent(String name) throws IOException {
		Objects.requireNonNull(name);
		
		if(credentials == null) {
			return false; // Do nothing
		}
		
		CredentialsManager cm = credentialsManager();
		
		if(cm.has(name)) {
			return false; // Already present
		}
		
		if(credentials.isDisposed()) {
			throw new IllegalStateException("Credentials disposed");
		}
		
		try {
			cm.set(name, credentials);
			return true;
		} finally {
			credentials.dispose();
			maybeClear();
		}
	}
	
	private static final class ConfigurationCleaner {
		
		private final WeakReference<Configuration.Builder> configuration;
		private final Set<String> properties;
		
		private ConfigurationCleaner(Configuration.Builder configuration, Set<String> properties) {
			this.configuration = new WeakReference<>(Objects.requireNonNull(configuration));
			this.properties = Objects.requireNonNull(properties);
		}
		
		public final void clear() {
			Configuration.Builder config = configuration.get();
			
			if(config == null) {
				return; // Nothing to clear
			}
			
			for(String name : properties) {
				config.removeProperty(name);
			}
			
			properties.clear();
		}
	}
	
	public static final class Builder {
		
		private final Configuration.Builder configuration;
		private final Map<String, Function<ConfigurationProperty.BuilderBase<?, ?>, Object>> properties;
		
		private Builder(Configuration.Builder configuration, String[] propertyNames) {
			this.configuration = Objects.requireNonNull(configuration);
			this.properties = Stream.of(Objects.requireNonNull(Utils.nonNullContent(propertyNames)))
				.collect(Collectors.toMap((k) -> k, (k) -> (v) -> v.build().value(), (a, b) -> a, LinkedHashMap::new));
		}
		
		private final Credentials newCredentialsInstance(
				Class<? extends Credentials> clazz, Class<?>[] argsClasses, Object[] args
		) {
			Objects.requireNonNull(clazz);
			
			Constructor<? extends Credentials> constructor;
			try {
				constructor = clazz.getDeclaredConstructor(argsClasses);
			} catch(NoSuchMethodException
						| SecurityException ex) {
				throw new UncheckedException(ex);
			}
			
			Credentials instance;
			try {
				Reflection.setAccessible(constructor, true);
				instance = constructor.newInstance(args);
			} catch(InstantiationException
						| InvocationTargetException
						| IllegalAccessException
						| IllegalArgumentException
						| SecurityException ex) {
				throw new UncheckedException(ex);
			}
			
			return instance;
		}
		
		private final Credentials newDefaultCredentials(Class<? extends Credentials> clazz) {
			return newCredentialsInstance(clazz, new Class[0], new Object[0]);
		}
		
		private final Credentials newCredentials(Class<? extends Credentials> clazz, Class<?>[] argsClasses) {
			final int length = properties.size();
			
			if(length != argsClasses.length) {
				throw new IllegalArgumentException("Number of classes does not equal number of properties");
			}
			
			Object[] args = new Object[length];
			
			int i = 0;
			for(Entry<String, Function<ConfigurationProperty.BuilderBase<?, ?>, Object>> property : properties.entrySet()) {
				ConfigurationProperty.BuilderBase<?, ?> configProperty = configuration.getProperty(property.getKey());
				
				if(configProperty == null) {
					throw new IllegalStateException("Configuration property '" + property.getKey() + "' does not exist");
				}
				
				args[i++] = property.getValue().apply(configProperty);
			}
			
			return newCredentialsInstance(clazz, argsClasses, args);
		}
		
		private final ConfigurationCleaner newCleaner() {
			return new ConfigurationCleaner(configuration, properties.keySet());
		}
		
		public Builder map(String propertyName, Function<ConfigurationProperty.BuilderBase<?, ?>, Object> mapper) {
			Objects.requireNonNull(propertyName);
			
			if(!properties.containsKey(propertyName)) { // Do not allow new properties
				return this;
			}
			
			properties.put(propertyName, Objects.requireNonNull(mapper));
			return this;
		}
		
		public CredentialsMigrator asCredentials(Class<? extends Credentials> clazz, Class<?>... argsClasses) {
			return new CredentialsMigrator(
				configuration.isDataLoaded()
					? newCredentials(clazz, argsClasses)
					: newDefaultCredentials(clazz),
				newCleaner()
			);
		}
	}
}