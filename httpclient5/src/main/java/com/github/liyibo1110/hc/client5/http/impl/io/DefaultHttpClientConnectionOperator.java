package com.github.liyibo1110.hc.client5.http.impl.io;

import com.github.liyibo1110.hc.client5.http.ConnectExceptionSupport;
import com.github.liyibo1110.hc.client5.http.DnsResolver;
import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.client5.http.SystemDefaultDnsResolver;
import com.github.liyibo1110.hc.client5.http.UnsupportedSchemeException;
import com.github.liyibo1110.hc.client5.http.impl.ConnPoolSupport;
import com.github.liyibo1110.hc.client5.http.impl.DefaultSchemePortResolver;
import com.github.liyibo1110.hc.client5.http.io.HttpClientConnectionOperator;
import com.github.liyibo1110.hc.client5.http.io.ManagedHttpClientConnection;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.client5.http.socket.ConnectionSocketFactory;
import com.github.liyibo1110.hc.client5.http.socket.LayeredConnectionSocketFactory;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ConnectionClosedException;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.config.Lookup;
import com.github.liyibo1110.hc.core5.http.io.SocketConfig;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.TimeValue;
import com.github.liyibo1110.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;

/**
 * HttpClientConnectionOperator接口的默认实现，
 * 当用户未向BasicHttpClientConnectionManager或PoolingHttpClientConnectionManager的构造函数提供实例时，
 * 该实现将作为HTTP客户端的默认实现使用。
 *
 * 负责真正的底层连接操作，例如socket connect和TLS upgrade。
 * @author liyibo
 * @date 2026-04-21 11:46
 */
@Internal
@Contract(threading = ThreadingBehavior.STATELESS)
public class DefaultHttpClientConnectionOperator implements HttpClientConnectionOperator {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultHttpClientConnectionOperator.class);
    static final String SOCKET_FACTORY_REGISTRY = "http.socket-factory-registry";

    private final Lookup<ConnectionSocketFactory> socketFactoryRegistry;
    private final SchemePortResolver schemePortResolver;
    private final DnsResolver dnsResolver;

    public DefaultHttpClientConnectionOperator(final Lookup<ConnectionSocketFactory> socketFactoryRegistry,
                                               final SchemePortResolver schemePortResolver,
                                               final DnsResolver dnsResolver) {
        super();
        Args.notNull(socketFactoryRegistry, "Socket factory registry");
        this.socketFactoryRegistry = socketFactoryRegistry;
        this.schemePortResolver = schemePortResolver != null ? schemePortResolver : DefaultSchemePortResolver.INSTANCE;
        this.dnsResolver = dnsResolver != null ? dnsResolver : SystemDefaultDnsResolver.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    private Lookup<ConnectionSocketFactory> getSocketFactoryRegistry(final HttpContext context) {
        Lookup<ConnectionSocketFactory> reg = (Lookup<ConnectionSocketFactory>) context.getAttribute(SOCKET_FACTORY_REGISTRY);
        if (reg == null)
            reg = this.socketFactoryRegistry;
        return reg;
    }

    @Override
    public void connect(final ManagedHttpClientConnection conn,
                        final HttpHost host,
                        final InetSocketAddress localAddress,
                        final TimeValue connectTimeout,
                        final SocketConfig socketConfig,
                        final HttpContext context) throws IOException {
        final Timeout timeout = connectTimeout != null ? Timeout.of(connectTimeout.getDuration(), connectTimeout.getTimeUnit()) : null;
        connect(conn, host, localAddress, timeout, socketConfig, null, context);
    }

    @Override
    public void connect(final ManagedHttpClientConnection conn,
                        final HttpHost host,
                        final InetSocketAddress localAddress,
                        final Timeout connectTimeout,
                        final SocketConfig socketConfig,
                        final Object attachment,
                        final HttpContext context) throws IOException {
        Args.notNull(conn, "Connection");
        Args.notNull(host, "Host");
        Args.notNull(socketConfig, "Socket config");
        Args.notNull(context, "Context");
        final Lookup<ConnectionSocketFactory> registry = getSocketFactoryRegistry(context);
        final ConnectionSocketFactory sf = registry.lookup(host.getSchemeName());
        if (sf == null)
            throw new UnsupportedSchemeException(host.getSchemeName() + " protocol is not supported");

        final InetAddress[] remoteAddresses;
        if (host.getAddress() != null) {
            remoteAddresses = new InetAddress[] { host.getAddress() };
        } else {
            if (LOG.isDebugEnabled())
                LOG.debug("{} resolving remote address", host.getHostName());

            remoteAddresses = this.dnsResolver.resolve(host.getHostName());

            if (LOG.isDebugEnabled())
                LOG.debug("{} resolved to {}", host.getHostName(), Arrays.asList(remoteAddresses));
        }

        final Timeout soTimeout = socketConfig.getSoTimeout();
        final SocketAddress socksProxyAddress = socketConfig.getSocksProxyAddress();
        final Proxy proxy = socksProxyAddress != null ? new Proxy(Proxy.Type.SOCKS, socksProxyAddress) : null;
        final int port = this.schemePortResolver.resolve(host);
        for (int i = 0; i < remoteAddresses.length; i++) {
            final InetAddress address = remoteAddresses[i];
            final boolean last = i == remoteAddresses.length - 1;

            Socket sock = sf.createSocket(proxy, context);
            if (soTimeout != null)
                sock.setSoTimeout(soTimeout.toMillisecondsIntBound());

            sock.setReuseAddress(socketConfig.isSoReuseAddress());
            sock.setTcpNoDelay(socketConfig.isTcpNoDelay());
            sock.setKeepAlive(socketConfig.isSoKeepAlive());
            if (socketConfig.getRcvBufSize() > 0)
                sock.setReceiveBufferSize(socketConfig.getRcvBufSize());

            if (socketConfig.getSndBufSize() > 0)
                sock.setSendBufferSize(socketConfig.getSndBufSize());


            final int linger = socketConfig.getSoLinger().toMillisecondsIntBound();
            if (linger >= 0)
                sock.setSoLinger(true, linger);

            conn.bind(sock);

            final InetSocketAddress remoteAddress = new InetSocketAddress(address, port);
            if (LOG.isDebugEnabled())
                LOG.debug("{}:{} connecting {}->{} ({})", host.getHostName(), host.getPort(), localAddress, remoteAddress, connectTimeout);

            try {
                sock = sf.connectSocket(sock, host, remoteAddress, localAddress, connectTimeout, attachment, context);
                conn.bind(sock);
                conn.setSocketTimeout(soTimeout);
                if (LOG.isDebugEnabled())
                    LOG.debug("{}:{} connected {}->{} as {}", host.getHostName(), host.getPort(), localAddress, remoteAddress, ConnPoolSupport.getId(conn));

                return;
            } catch (final IOException ex) {
                if (last) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{}:{} connection to {} failed ({}); terminating operation", host.getHostName(), host.getPort(), remoteAddress, ex.getClass());

                    throw ConnectExceptionSupport.enhance(ex, host, remoteAddresses);
                } else {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{}:{} connection to {} failed ({}); retrying connection to the next address", host.getHostName(), host.getPort(), remoteAddress, ex.getClass());
                }
            }
        }
    }

    @Override
    public void upgrade(final ManagedHttpClientConnection conn,
                        final HttpHost host,
                        final HttpContext context)
            throws IOException {
        upgrade(conn, host, null, context);
    }

    @Override
    public void upgrade(final ManagedHttpClientConnection conn,
                        final HttpHost host,
                        final Object attachment,
                        final HttpContext context) throws IOException {
        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final Lookup<ConnectionSocketFactory> registry = getSocketFactoryRegistry(clientContext);
        final ConnectionSocketFactory sf = registry.lookup(host.getSchemeName());
        if (sf == null)
            throw new UnsupportedSchemeException(host.getSchemeName() + " protocol is not supported");

        if (!(sf instanceof LayeredConnectionSocketFactory))
            throw new UnsupportedSchemeException(host.getSchemeName() + " protocol does not support connection upgrade");

        final LayeredConnectionSocketFactory lsf = (LayeredConnectionSocketFactory) sf;
        Socket sock = conn.getSocket();
        if (sock == null)
            throw new ConnectionClosedException("Connection is closed");

        final int port = this.schemePortResolver.resolve(host);
        sock = lsf.createLayeredSocket(sock, host.getHostName(), port, attachment, context);
        conn.bind(sock);
    }
}
