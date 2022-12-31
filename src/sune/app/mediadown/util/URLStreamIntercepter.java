package sune.app.mediadown.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.Permission;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

import sune.app.mediadown.util.Utils.Ignore;

public final class URLStreamIntercepter {
	
	private static URLStreamHandler defaultHandlerHTTPS;
	private static URLStreamHandlerFactory factory;
	private static final AtomicBoolean isActive = new AtomicBoolean();
	private static final Map<String, ByteArrayOutputStream> interceptedOutput = new HashMap<>();
	private static final Map<String, ByteArrayOutputStream> interceptedInput = new HashMap<>();
	
	private static final class InterceptURLStreamHandlerFactory implements URLStreamHandlerFactory {
		
		private static final class InterceptOutputStream extends OutputStream {
			
			OutputStream o;
			ByteArrayOutputStream s;
			
			public InterceptOutputStream(URL url, OutputStream out) {
				o = out;
				s = new ByteArrayOutputStream();
				putInterceptedOutput(url, s);
			}
			
			@Override
			public void write(int b) throws IOException {
				s.write(b);
				o.write(b);
			}
		}
		
		private static final class InterceptInputStream extends InputStream {
			
			InputStream i;
			ByteArrayOutputStream s;
			
			public InterceptInputStream(URL url, InputStream in) {
				i = in;
				s = new ByteArrayOutputStream();
				putInterceptedInput(url, s);
			}
			
			@Override
			public int read() throws IOException {
				int r = i.read();
				s.write(r);
				return r;
			}
		}
		
		private static class InterceptHttpsURLConnection extends HttpsURLConnection {
			
			HttpsURLConnection c;
			InterceptOutputStream o;
			InterceptInputStream i;
			URL u;
			
			public InterceptHttpsURLConnection(URL url) throws IOException {
				super(url);
				u = url;
				boolean allowed = isActive();
				try {
					setActive(false);
					c = (HttpsURLConnection) copyURL(u).openConnection();
				} finally {
					if((allowed))
						setActive(true);
				}
			}
			
			@Override
			public OutputStream getOutputStream() throws IOException {
				return o == null ? o = new InterceptOutputStream(u, c.getOutputStream()) : o;
			}
			
			@Override
			public InputStream getInputStream() throws IOException {
				return i == null ? i = new InterceptInputStream(u, c.getInputStream()) : i;
			}
			
			@Override public void connect() throws IOException { c.connect(); }
			@Override public int hashCode() { return c.hashCode(); }
			@Override public boolean equals(Object obj) { return c.equals(obj); }
			@Override public String toString() { return c.toString(); }
			@Override public java.net.URL getURL() { return c.getURL(); }
			@Override public void setConnectTimeout(int timeout) { c.setConnectTimeout(timeout); }
			@Override public int getConnectTimeout() { return c.getConnectTimeout(); }
			@Override public void setReadTimeout(int timeout) { c.setReadTimeout(timeout); }
			@Override public int getReadTimeout() { return c.getReadTimeout(); }
			@Override public int getContentLength() { return c.getContentLength(); }
			@Override public long getContentLengthLong() { return c.getContentLengthLong(); }
			@Override public String getContentType() { return c.getContentType(); }
			@Override public String getContentEncoding() { return c.getContentEncoding(); }
			@Override public long getExpiration() { return c.getExpiration(); }
			@Override public long getDate() { return c.getDate(); }
			@Override public long getLastModified() { return c.getLastModified(); }
			@Override public String getHeaderField(String name) { return c.getHeaderField(name); }
			@Override public Map<String, List<String>> getHeaderFields() { return c.getHeaderFields(); }
			@Override public int getHeaderFieldInt(String name, int Default) { return c.getHeaderFieldInt(name, Default); }
			@Override public long getHeaderFieldLong(String name, long Default) { return c.getHeaderFieldLong(name, Default); }
			@Override public long getHeaderFieldDate(String name, long Default) { return c.getHeaderFieldDate(name, Default); }
			@Override public String getHeaderFieldKey(int n) { return c.getHeaderFieldKey(n); }
			@Override public String getHeaderField(int n) { return c.getHeaderField(n); }
			@Override public Object getContent() throws IOException { return c.getContent(); }
			@Override public Permission getPermission() throws IOException { return c.getPermission(); }
			@Override public void setDoInput(boolean doinput) { c.setDoInput(doinput); }
			@Override public boolean getDoInput() { return c.getDoInput(); }
			@Override public void setDoOutput(boolean dooutput) { c.setDoOutput(dooutput); }
			@Override public boolean getDoOutput() { return c.getDoOutput(); }
			@Override public void setAllowUserInteraction(boolean allowuserinteraction) { c.setAllowUserInteraction(allowuserinteraction); }
			@Override public boolean getAllowUserInteraction() { return c.getAllowUserInteraction(); }
			@Override public void setUseCaches(boolean usecaches) { c.setUseCaches(usecaches); }
			@Override public boolean getUseCaches() { return c.getUseCaches(); }
			@Override public void setIfModifiedSince(long ifmodifiedsince) { c.setIfModifiedSince(ifmodifiedsince); }
			@Override public long getIfModifiedSince() { return c.getIfModifiedSince(); }
			@Override public boolean getDefaultUseCaches() { return c.getDefaultUseCaches(); }
			@Override public void setDefaultUseCaches(boolean defaultusecaches) { c.setDefaultUseCaches(defaultusecaches); }
			@Override public void setRequestProperty(String key, String value) { c.setRequestProperty(key, value); }
			@Override public void addRequestProperty(String key, String value) { c.addRequestProperty(key, value); }
			@Override public String getRequestProperty(String key) { return c.getRequestProperty(key); }
			@Override public Map<String, List<String>> getRequestProperties() { return c.getRequestProperties(); }
			@SuppressWarnings("rawtypes")
			@Override public Object getContent(Class[] classes) throws IOException { return c.getContent(classes); }
			@Override public String getCipherSuite() { return c.getCipherSuite(); }
			@Override public Certificate[] getLocalCertificates() { return c.getLocalCertificates(); }
			@Override public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException { return c.getServerCertificates(); }
			@Override public void disconnect() { c.disconnect(); }
			@Override public boolean usingProxy() { return c.usingProxy(); }
			@Override public InputStream getErrorStream() { return c.getErrorStream(); }
			@Override public HostnameVerifier getHostnameVerifier() { return c.getHostnameVerifier(); }
			@Override public boolean getInstanceFollowRedirects() { return c.getInstanceFollowRedirects(); }
			@Override public Principal getLocalPrincipal() { return c.getLocalPrincipal(); }
			@Override public Principal getPeerPrincipal() throws SSLPeerUnverifiedException { return c.getPeerPrincipal(); }
			@Override public void setChunkedStreamingMode(int chunklen) { c.setChunkedStreamingMode(chunklen); }
			@Override public void setInstanceFollowRedirects(boolean followRedirects) { c.setInstanceFollowRedirects(followRedirects); }
			@Override public void setRequestMethod(String method) throws ProtocolException { c.setRequestMethod(method); }
			@Override public String getRequestMethod() { return c.getRequestMethod(); }
			@Override public int getResponseCode() throws IOException { return c.getResponseCode(); }
			@Override public String getResponseMessage() throws IOException { return c.getResponseMessage(); }
			@Override public SSLSocketFactory getSSLSocketFactory() { return c.getSSLSocketFactory(); }
			@Override public void setFixedLengthStreamingMode(int contentLength) { c.setFixedLengthStreamingMode(contentLength); }
			@Override public void setFixedLengthStreamingMode(long contentLength) { c.setFixedLengthStreamingMode(contentLength); }
			@Override public void setHostnameVerifier(HostnameVerifier v) { c.setHostnameVerifier(v); }
			@Override public void setSSLSocketFactory(SSLSocketFactory sf) { c.setSSLSocketFactory(sf); }
		}
		
		class InterceptURLStreamHandler extends URLStreamHandler {
			
			@Override
			protected URLConnection openConnection(java.net.URL u) throws IOException {
				if(!isActive()) {
					Reflection2.setField(URL.class, u, "handler", defaultHandlerHTTPS);
					return u.openConnection();
				}
				return new InterceptHttpsURLConnection(u);
			}
		}
		
		@Override
		public URLStreamHandler createURLStreamHandler(String protocol) {
			return protocol.equalsIgnoreCase("https")
						? (isActive()
							? new InterceptURLStreamHandler()
							: defaultHandlerHTTPS)
						: null;
		}
		
		private static final java.net.URL copyURL(java.net.URL u) throws IOException {
			try { return u.toURI().toURL(); } catch(Exception ex) { throw new IOException(ex); }
		}
	}
	
	// Forbid anyone to create an instance of this class
	private URLStreamIntercepter() {
	}
	
	private static final URLStreamHandler getURLStreamHandler(String protocol) {
		return Ignore.call(() -> (URLStreamHandler) Reflection3.invokeStatic(URL.class, "getURLStreamHandler",
			Utils.<Class<?>>array(String.class), protocol));
	}
	
	private static final void putInterceptedOutput(URL url, ByteArrayOutputStream stream) {
		interceptedOutput.put(url.toExternalForm(), stream);
	}
	
	private static final void putInterceptedInput(URL url, ByteArrayOutputStream stream) {
		interceptedInput.put(url.toExternalForm(), stream);
	}
	
	public static final ByteArrayOutputStream getInterceptedOutput(String url) {
		return interceptedOutput.get(url);
	}
	
	public static final ByteArrayOutputStream getInterceptedInput(String url) {
		return interceptedInput.get(url);
	}
	
	public static final Map<String, ByteArrayOutputStream> getInterceptedOutputs() {
		return Collections.unmodifiableMap(interceptedOutput);
	}
	
	public static final Map<String, ByteArrayOutputStream> getInterceptedInputs() {
		return Collections.unmodifiableMap(interceptedInput);
	}
	
	public static final void setActive(boolean active) {
		if((factory == null)) {
			defaultHandlerHTTPS = getURLStreamHandler("https");
			factory = new InterceptURLStreamHandlerFactory();
			java.net.URL.setURLStreamHandlerFactory(factory);
		}
		isActive.set(active);
	}
	
	public static final boolean isActive() {
		return isActive.get();
	}
}