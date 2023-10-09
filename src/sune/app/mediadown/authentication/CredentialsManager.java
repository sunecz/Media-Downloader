package sune.app.mediadown.authentication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Set;

import javax.crypto.SecretKey;

import sune.app.mediadown.Disposable;
import sune.app.mediadown.Disposables;
import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.util.NIO;

/** @since 00.02.09 */
public final class CredentialsManager implements CredentialsStore, Disposable {
	
	private static final VarLoader<CredentialsManager> instance = VarLoader.ofChecked(CredentialsManager::newInstance);
	private static final char[] PW = new char[0];
	
	private final Path path;
	private KeyStore keyStore;
	private KeySecuredCredentialsStore store;
	
	private CredentialsManager() throws KeyStoreException {
		this.path = NIO.localPath("resources", "cm.store");
		this.keyStore = keyStore();
		Disposables.add(this);
	}
	
	private static final KeyStore keyStore() throws KeyStoreException {
		return KeyStore.getInstance("pkcs12");
	}
	
	private static final CredentialsManager newInstance() throws IOException {
		try {
			CredentialsManager manager = new CredentialsManager();
			manager.loadKeyStore();
			manager.loadStore();
			return manager;
		} catch(KeyStoreException ex) {
			throw new IOException(ex);
		}
	}
	
	public static final CredentialsManager instance() throws IOException {
		try {
			return instance.valueChecked();
		} catch(Exception ex) {
			throw new IOException(ex);
		}
	}
	
	private final void loadKeyStore(InputStream stream) throws IOException {
		try {
			keyStore.load(stream, null);
		} catch(NoSuchAlgorithmException
					| CertificateException ex) {
			throw new IOException(ex);
		}
	}
	
	private final void loadKeyStore() throws IOException {
		if(!NIO.exists(path)) {
			loadKeyStore(null);
			initKeyStore();
			return; // Do not continue
		}
		
		try(InputStream stream = Files.newInputStream(
			path, StandardOpenOption.READ
		)) {
			loadKeyStore(stream);
		}
	}
	
	private final void saveKeyStore() throws IOException {
		try(OutputStream stream = Files.newOutputStream(
			path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
		)) {
			keyStore.store(stream, null);
		} catch(NoSuchAlgorithmException
					| CertificateException
					| KeyStoreException ex) {
			throw new IOException(ex);
		}
	}
	
	private final void initKeyStore() throws IOException {
		try {
			storeKey(generateKey());
			saveKeyStore();
		} catch(KeyStoreException ex) {
			throw new IOException(ex);
		}
	}
	
	private final SecretKey generateKey() {
		return Crypto.AES256.generateKey();
	}
	
	private final void storeKey(SecretKey key) throws KeyStoreException {
		keyStore.setEntry("crdk", new KeyStore.SecretKeyEntry(key), new KeyStore.PasswordProtection(PW));
	}
	
	private final SecretKey loadKey() throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException {
		return ((KeyStore.SecretKeyEntry) keyStore.getEntry("crdk", new KeyStore.PasswordProtection(PW))).getSecretKey();
	}
	
	private final SecretKey key() throws UnrecoverableEntryException, KeyStoreException {
		try {
			return loadKey();
		} catch(NoSuchAlgorithmException ex) {
			throw new KeyStoreException(ex); // Should not happen
		}
	}
	
	private final void loadStore() throws IOException {
		try {
			store = KeySecuredCredentialsStore.of(NIO.localPath("resources", "crd.store"), key());
		} catch(UnrecoverableEntryException
					| KeyStoreException ex) {
			throw new IOException(ex);
		}
	}
	
	@Override
	public void dispose() throws Exception {
		try {
			store.close();
		} finally {
			store = null;
			keyStore = null;
		}
	}
	
	@Override
	public void close() throws Exception {
		dispose();
	}
	
	@Override
	public Credentials get(String path) throws IOException {
		return store.get(path);
	}
	
	@Override
	public boolean has(String path) throws IOException {
		return store.has(path);
	}
	
	@Override
	public void set(String path, Credentials credentials) throws IOException {
		store.set(path, credentials);
	}
	
	@Override
	public void remove(String path) throws IOException {
		store.remove(path);
	}
	
	@Override
	public Set<String> paths() throws IOException {
		return store.paths();
	}
}