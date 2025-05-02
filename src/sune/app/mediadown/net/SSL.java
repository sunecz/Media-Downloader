package sune.app.mediadown.net;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import sune.app.mediadown.concurrent.VarLoader;

/** @since 00.02.09 */
public final class SSL {
	
	// Forbid anyone to create an instance of this class
	private SSL() {
	}
	
	private static final URI caIssuers(byte[] aia) throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(aia);
		return Net.uri(AIA.caIssuers(buf));
	}
	
	private static X509Certificate fetchCertificate(URI uri)
			throws IOException, CertificateException {
		Objects.requireNonNull(uri);
		
		// We could cache the certificate here ourselves to not download it multiple times,
		// however:
		// - there should be only a single instance of the AIA-fetching SSLContext,
		// - an SSLContext in its implementation (OpenJDK) uses an SSLSessionContext,
		// - and an SSLSessionContext implementation (OpenJDK) uses its own caching mechanism,
		// thus we can omit our cache altogether, since the built-in cache should be enough.
		
		try(InputStream is = uri.toURL().openStream()) {
			return (X509Certificate) Constants.CERT_FACTORY.generateCertificate(is);
		}
	}
	
	private static X509Certificate fetchAIACertificate(X509Certificate cert)
			throws IOException, CertificateException {
		byte[] aia = cert.getExtensionValue(Constants.OID_AIA);
		
		if(aia == null) {
			return null;
		}
		
		URI uri = caIssuers(aia);
		
		if(uri == null) {
			return null;
		}
		
		return fetchCertificate(uri);
	}
	
	private static final X509TrustManager defaultX509TrustManager()
			throws NoSuchAlgorithmException, KeyStoreException {
		String defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(defaultAlgorithm);
		tmf.init((KeyStore) null);
		
		for(TrustManager tm : tmf.getTrustManagers()) {
			if(tm instanceof X509TrustManager) {
				return (X509TrustManager) tm;
			}
		}
		
		throw new IllegalStateException("No default X509TrustManager found");
	}
	
	private static final boolean isCACertificate(X509Certificate cert) {
		return cert.getBasicConstraints() != -1;
	}
	
	private static final <T> T[] concat(T[] array, T item) {
		@SuppressWarnings("unchecked")
		T[] newArray = (T[]) Array.newInstance(
			array.getClass().getComponentType(),
			array.length + 1
		);
		
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[array.length] = item;
		
		return newArray;
	}
	
	public static final class Contexts {
		
		private static final VarLoader<SSLContext> aiaFetching;
		
		static {
			aiaFetching = VarLoader.ofChecked(Contexts::newAIAFetching);
		}
		
		private Contexts() {
		}
		
		private static final SSLContext newAIAFetching()
				throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
			X509TrustManager tm = new AIAFetchingX509TrustManager(defaultX509TrustManager());
			String protocol = Security.getProperty("sslContextProtocol");
			
			if(protocol == null) {
				protocol = "TLS";
			}
			
			SSLContext sslContext = SSLContext.getInstance(protocol);
			sslContext.init(null, new TrustManager[] { tm }, new SecureRandom());
			return sslContext;
		}
		
		public static final SSLContext aiaFetching() {
			return aiaFetching.value();
		}
	}
	
	private static final class Constants {
		
		private static final String OID_AIA = "1.3.6.1.5.5.7.1.1";
		private static final String OID_CA_ISSUERS = "1.3.6.1.5.5.7.48.2";
		private static final CertificateFactory CERT_FACTORY;
		
		static {
			CertificateFactory _cf;
			
			try {
				_cf = CertificateFactory.getInstance("X.509");
			} catch(CertificateException ex) {
				throw new IllegalStateException(ex);
			}
			
			CERT_FACTORY = _cf;
		}
	}
	
	// Reference: https://datatracker.ietf.org/doc/html/rfc5280#section-4.2.2.1
	private static final class AIA {
		
		private AIA() {
		}
		
		private static final ByteBuffer view(ByteBuffer buf, int length) {
			ByteBuffer view = buf.slice(); // Do not waste memory by copying
			view.limit(length);
			return view;
		}
		
		private static final int readLength(ByteBuffer buf) {
			int b = buf.get() & 0xff;
			
			// length < 128
			if((b & 0x80) == 0) {
				return b;
			}
			
			// length >= 128, lower 7 bits encode the number of bytes
			int numBytes = b & 0x7f;
			
			int len = 0;
			for(int i = 0; i < numBytes; ++i) {
				len = (len << 8) | (buf.get() & 0xff);
			}
			
			return len;
		}
		
		private static final byte[] readBytes(ByteBuffer buf, int length) throws IOException {
			if(length > buf.remaining()) {
				throw new IOException("Not enough bytes: " + length);
			}
			
			byte[] arr = new byte[length];
			buf.get(arr);
			return arr;
		}
		
		private static final String readOID(byte[] bytes) {
			if(bytes.length == 0) {
				return "";
			}
			
			StringBuilder oid = new StringBuilder();
			int first = bytes[0] & 0xff;
			oid.append(first / 40).append(".").append(first % 40);
			
			long value = 0L;
			for(int i = 1, l = bytes.length; i < l; ++i) {
				int b = bytes[i] & 0xff;
				value = (value << 7L) | (b & 0x7F);
				
				if((b & 0x80) == 0) {
					oid.append('.').append(value);
					value = 0L;
				}
			}
			
			return oid.toString();
		}
		
		private static final String readString(ByteBuffer buf) throws IOException {
			int length = readLength(buf);
			return new String(readBytes(buf, length));
		}
		
		public static final String caIssuers(ByteBuffer buf) throws IOException {
			if((buf.get() & 0xff) != 0x04) {
				throw new IOException("Expected OCTET STRING");
			}
			
			int innerLen = readLength(buf);
			ByteBuffer inner = view(buf, innerLen);
			
			if((inner.get() & 0xff) != 0x30) {
				throw new IOException("Expected SEQUENCE");
			}
			
			int seqLen = readLength(inner);
			
			for(int i = 0; i < seqLen; ++i) {
				if((inner.get() & 0xff) != 0x30) {
					break; // End of SEQUENCE
				}
				
				int adLen = readLength(inner);
				int limit = inner.position() + adLen;
				
				if((inner.get() & 0xff) != 0x06) {
					break; // Not IA5String
				}
				
				int oidLen = readLength(inner);
				byte[] oidBytes = readBytes(inner, oidLen);
				String oid = readOID(oidBytes);
				
				if((inner.get() & 0xff) != 0x86) {
					continue; // Not IA5String
				}
				
				if(Constants.OID_CA_ISSUERS.equals(oid)) {
					return readString(inner); // We've got it
				}
				
				inner.position(limit); // Skip to the correct position
			}
			
			return null; // Not found
		}
	}
	
	private static final class AIAFetchingX509TrustManager implements X509TrustManager {
		
		private final X509TrustManager delegate;
		
		public AIAFetchingX509TrustManager(X509TrustManager delegate) {
			this.delegate = Objects.requireNonNull(delegate);
		}
		
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return delegate.getAcceptedIssuers(); // Delegate
		}
		
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			delegate.checkClientTrusted(chain, authType); // Delegate
		}
		
		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			do {
				try {
					delegate.checkServerTrusted(chain, authType); // Delegate
					return; // All is OK
				} catch(CertificateException ex) {
					if(!(ex.getCause() instanceof CertPathBuilderException)) {
						throw ex; // Rethrow
					}
					
					X509Certificate lastCert = chain[chain.length - 1];
					
					if(isCACertificate(lastCert)) {
						// The last certificate is already a CA but the chain is still failing.
						// That means that its root CA is not in the trust store, therefore just
						// stop the AIA fetching process and rethrow the original exception.
						throw ex;
					}
					
					X509Certificate cert;
					try {
						cert = fetchAIACertificate(lastCert);
					} catch(CertificateException | IOException ignored) {
						throw ex; // Rethrow the original exception
					}
					
					if(cert == null) {
						// The chain is failing and we cannot obtain another certificate
						// in the chain, thus we cannot continue, rethrow the original exception.
						throw ex;
					}
					
					chain = concat(chain, cert);
				}
			} while(true);
		}
	}
}
