package com.github.liyibo1110.hc.client5.http.ssl;

import com.github.liyibo1110.hc.client5.http.config.TlsConfig;
import com.github.liyibo1110.hc.client5.http.socket.LayeredConnectionSocketFactory;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.http.ssl.TLS;
import com.github.liyibo1110.hc.core5.http.ssl.TlsCiphers;
import com.github.liyibo1110.hc.core5.io.Closer;
import com.github.liyibo1110.hc.core5.ssl.SSLContexts;
import com.github.liyibo1110.hc.core5.ssl.SSLInitializationException;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.Asserts;
import com.github.liyibo1110.hc.core5.util.TimeValue;
import com.github.liyibo1110.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 用于 TLS/SSL连接的分层套接字工厂。
 * SSLSocketFactory可用于根据可信证书列表验证HTTPS服务器的身份，并使用私钥向HTTPS服务器进行身份验证。
 * @author liyibo
 * @date 2026-04-21 11:41
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class SSLConnectionSocketFactory implements LayeredConnectionSocketFactory {

    private static final String WEAK_KEY_EXCHANGES
            = "^(TLS|SSL)_(NULL|ECDH_anon|DH_anon|DH_anon_EXPORT|DHE_RSA_EXPORT|DHE_DSS_EXPORT|"
            + "DSS_EXPORT|DH_DSS_EXPORT|DH_RSA_EXPORT|RSA_EXPORT|KRB5_EXPORT)_(.*)";
    private static final String WEAK_CIPHERS
            = "^(TLS|SSL)_(.*)_WITH_(NULL|DES_CBC|DES40_CBC|DES_CBC_40|3DES_EDE_CBC|RC4_128|RC4_40|RC2_CBC_40)_(.*)";
    private static final List<Pattern> WEAK_CIPHER_SUITE_PATTERNS = Collections.unmodifiableList(Arrays.asList(
            Pattern.compile(WEAK_KEY_EXCHANGES, Pattern.CASE_INSENSITIVE),
            Pattern.compile(WEAK_CIPHERS, Pattern.CASE_INSENSITIVE)));

    private static final Logger LOG = LoggerFactory.getLogger(SSLConnectionSocketFactory.class);

    /**
     * Obtains default SSL socket factory with an SSL context based on the standard JSSE
     * trust material ({@code cacerts} file in the security properties directory).
     * System properties are not taken into consideration.
     *
     * @return default SSL socket factory
     */
    public static SSLConnectionSocketFactory getSocketFactory() throws SSLInitializationException {
        return new SSLConnectionSocketFactory(SSLContexts.createDefault(), HttpsSupport.getDefaultHostnameVerifier());
    }

    /**
     * Obtains default SSL socket factory with an SSL context based on system properties
     * as described in
     * <a href="http://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html">
     * Java&#x2122; Secure Socket Extension (JSSE) Reference Guide</a>.
     *
     * @return default system SSL socket factory
     */
    public static SSLConnectionSocketFactory getSystemSocketFactory() throws SSLInitializationException {
        return new SSLConnectionSocketFactory(
                (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault(),
                HttpsSupport.getSystemProtocols(),
                HttpsSupport.getSystemCipherSuits(),
                HttpsSupport.getDefaultHostnameVerifier());
    }

    static boolean isWeakCipherSuite(final String cipherSuite) {
        for (final Pattern pattern : WEAK_CIPHER_SUITE_PATTERNS) {
            if (pattern.matcher(cipherSuite).matches()) {
                return true;
            }
        }
        return false;
    }

    private final javax.net.ssl.SSLSocketFactory socketFactory;
    private final HostnameVerifier hostnameVerifier;
    private final String[] supportedProtocols;
    private final String[] supportedCipherSuites;
    private final TlsSessionValidator tlsSessionValidator;

    public SSLConnectionSocketFactory(final SSLContext sslContext) {
        this(sslContext, HttpsSupport.getDefaultHostnameVerifier());
    }

    /**
     * @since 4.4
     */
    public SSLConnectionSocketFactory(
            final SSLContext sslContext, final HostnameVerifier hostnameVerifier) {
        this(Args.notNull(sslContext, "SSL context").getSocketFactory(),
                null, null, hostnameVerifier);
    }

    /**
     * @since 4.4
     */
    public SSLConnectionSocketFactory(
            final SSLContext sslContext,
            final String[] supportedProtocols,
            final String[] supportedCipherSuites,
            final HostnameVerifier hostnameVerifier) {
        this(Args.notNull(sslContext, "SSL context").getSocketFactory(),
                supportedProtocols, supportedCipherSuites, hostnameVerifier);
    }

    /**
     * @since 4.4
     */
    public SSLConnectionSocketFactory(
            final javax.net.ssl.SSLSocketFactory socketFactory,
            final HostnameVerifier hostnameVerifier) {
        this(socketFactory, null, null, hostnameVerifier);
    }

    /**
     * @since 4.4
     */
    public SSLConnectionSocketFactory(
            final javax.net.ssl.SSLSocketFactory socketFactory,
            final String[] supportedProtocols,
            final String[] supportedCipherSuites,
            final HostnameVerifier hostnameVerifier) {
        this.socketFactory = Args.notNull(socketFactory, "SSL socket factory");
        this.supportedProtocols = supportedProtocols;
        this.supportedCipherSuites = supportedCipherSuites;
        this.hostnameVerifier = hostnameVerifier != null ? hostnameVerifier : HttpsSupport.getDefaultHostnameVerifier();
        this.tlsSessionValidator = new TlsSessionValidator(LOG);
    }

    /**
     * @deprecated Use {@link #prepareSocket(SSLSocket, HttpContext)}
     */
    @Deprecated
    protected void prepareSocket(final SSLSocket socket) throws IOException {
    }

    /**
     * Performs any custom initialization for a newly created SSLSocket
     * (before the SSL handshake happens).
     *
     * The default implementation is a no-op, but could be overridden to, e.g.,
     * call {@link javax.net.ssl.SSLSocket#setEnabledCipherSuites(String[])}.
     * @throws IOException may be thrown if overridden
     */
    @SuppressWarnings("deprecation")
    protected void prepareSocket(final SSLSocket socket, final HttpContext context) throws IOException {
        prepareSocket(socket);
    }

    @Override
    public Socket createSocket(final HttpContext context) throws IOException {
        return new Socket();
    }

    @Override
    public Socket createSocket(final Proxy proxy, final HttpContext context) throws IOException {
        return proxy != null ? new Socket(proxy) : new Socket();
    }

    @Override
    public Socket connectSocket(
            final TimeValue connectTimeout,
            final Socket socket,
            final HttpHost host,
            final InetSocketAddress remoteAddress,
            final InetSocketAddress localAddress,
            final HttpContext context) throws IOException {
        final Timeout timeout = connectTimeout != null ? Timeout.of(connectTimeout.getDuration(), connectTimeout.getTimeUnit()) : null;
        return connectSocket(socket, host, remoteAddress, localAddress, timeout, timeout, context);
    }

    @Override
    public Socket connectSocket(
            final Socket socket,
            final HttpHost host,
            final InetSocketAddress remoteAddress,
            final InetSocketAddress localAddress,
            final Timeout connectTimeout,
            final Object attachment,
            final HttpContext context) throws IOException {
        Args.notNull(host, "HTTP host");
        Args.notNull(remoteAddress, "Remote address");
        final Socket sock = socket != null ? socket : createSocket(context);
        if (localAddress != null) {
            sock.bind(localAddress);
        }
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Connecting socket to {} with timeout {}", remoteAddress, connectTimeout);
            }
            // Run this under a doPrivileged to support lib users that run under a SecurityManager this allows granting connect permissions
            // only to this library
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                    sock.connect(remoteAddress, Timeout.defaultsToDisabled(connectTimeout).toMillisecondsIntBound());
                    return null;
                });
            } catch (final PrivilegedActionException e) {
                Asserts.check(e.getCause() instanceof  IOException,
                        "method contract violation only checked exceptions are wrapped: " + e.getCause());
                // only checked exceptions are wrapped - error and RTExceptions are rethrown by doPrivileged
                throw (IOException) e.getCause();
            }
        } catch (final IOException ex) {
            Closer.closeQuietly(sock);
            throw ex;
        }
        // Setup SSL layering if necessary
        if (sock instanceof SSLSocket) {
            final SSLSocket sslsock = (SSLSocket) sock;
            executeHandshake(sslsock, host.getHostName(), attachment, context);
            return sock;
        }
        return createLayeredSocket(sock, host.getHostName(), remoteAddress.getPort(), attachment, context);
    }

    @Override
    public Socket createLayeredSocket(
            final Socket socket,
            final String target,
            final int port,
            final HttpContext context) throws IOException {
        return createLayeredSocket(socket, target, port, null, context);
    }

    @Override
    public Socket createLayeredSocket(
            final Socket socket,
            final String target,
            final int port,
            final Object attachment,
            final HttpContext context) throws IOException {
        final SSLSocket sslsock = (SSLSocket) this.socketFactory.createSocket(
                socket,
                target,
                port,
                true);
        executeHandshake(sslsock, target, attachment, context);
        return sslsock;
    }

    private void executeHandshake(
            final SSLSocket sslsock,
            final String target,
            final Object attachment,
            final HttpContext context) throws IOException {
        final TlsConfig tlsConfig = attachment instanceof TlsConfig ? (TlsConfig) attachment : TlsConfig.DEFAULT;
        if (supportedProtocols != null) {
            sslsock.setEnabledProtocols(supportedProtocols);
        } else {
            sslsock.setEnabledProtocols((TLS.excludeWeak(sslsock.getEnabledProtocols())));
        }
        if (supportedCipherSuites != null) {
            sslsock.setEnabledCipherSuites(supportedCipherSuites);
        } else {
            sslsock.setEnabledCipherSuites(TlsCiphers.excludeWeak(sslsock.getEnabledCipherSuites()));
        }
        final Timeout handshakeTimeout = tlsConfig.getHandshakeTimeout();
        if (handshakeTimeout != null) {
            sslsock.setSoTimeout(handshakeTimeout.toMillisecondsIntBound());
        }

        prepareSocket(sslsock, context);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Enabled protocols: {}", (Object) sslsock.getEnabledProtocols());
            LOG.debug("Enabled cipher suites: {}", (Object) sslsock.getEnabledCipherSuites());
            LOG.debug("Starting handshake ({})", handshakeTimeout);
        }
        sslsock.startHandshake();
        verifyHostname(sslsock, target);
    }

    private void verifyHostname(final SSLSocket sslsock, final String hostname) throws IOException {
        try {
            SSLSession session = sslsock.getSession();
            if (session == null) {
                // In our experience this only happens under IBM 1.4.x when
                // spurious (unrelated) certificates show up in the server'
                // chain.  Hopefully this will unearth the real problem:
                final InputStream in = sslsock.getInputStream();
                in.available();
                // If ssl.getInputStream().available() didn't cause an
                // exception, maybe at least now the session is available?
                session = sslsock.getSession();
                if (session == null) {
                    // If it's still null, probably a startHandshake() will
                    // unearth the real problem.
                    sslsock.startHandshake();
                    session = sslsock.getSession();
                }
            }
            if (session == null) {
                throw new SSLHandshakeException("SSL session not available");
            }
            verifySession(hostname, session);
        } catch (final IOException iox) {
            // close the socket before re-throwing the exception
            Closer.closeQuietly(sslsock);
            throw iox;
        }
    }

    protected void verifySession(
            final String hostname,
            final SSLSession sslSession) throws SSLException {
        tlsSessionValidator.verifySession(hostname, sslSession, hostnameVerifier);
    }
}
