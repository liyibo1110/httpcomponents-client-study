package com.github.liyibo1110.hc.client5.http.impl.io;

import com.github.liyibo1110.hc.client5.http.DnsResolver;
import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.client5.http.config.ConnectionConfig;
import com.github.liyibo1110.hc.client5.http.config.TlsConfig;
import com.github.liyibo1110.hc.client5.http.impl.ConnPoolSupport;
import com.github.liyibo1110.hc.client5.http.impl.ConnectionShutdownException;
import com.github.liyibo1110.hc.client5.http.io.ConnectionEndpoint;
import com.github.liyibo1110.hc.client5.http.io.HttpClientConnectionManager;
import com.github.liyibo1110.hc.client5.http.io.HttpClientConnectionOperator;
import com.github.liyibo1110.hc.client5.http.io.LeaseRequest;
import com.github.liyibo1110.hc.client5.http.io.ManagedHttpClientConnection;
import com.github.liyibo1110.hc.client5.http.socket.ConnectionSocketFactory;
import com.github.liyibo1110.hc.client5.http.socket.PlainConnectionSocketFactory;
import com.github.liyibo1110.hc.client5.http.ssl.SSLConnectionSocketFactory;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.URIScheme;
import com.github.liyibo1110.hc.core5.http.config.Lookup;
import com.github.liyibo1110.hc.core5.http.config.Registry;
import com.github.liyibo1110.hc.core5.http.config.RegistryBuilder;
import com.github.liyibo1110.hc.core5.http.impl.io.HttpRequestExecutor;
import com.github.liyibo1110.hc.core5.http.io.HttpConnectionFactory;
import com.github.liyibo1110.hc.core5.http.io.SocketConfig;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.io.CloseMode;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.Asserts;
import com.github.liyibo1110.hc.core5.util.Deadline;
import com.github.liyibo1110.hc.core5.util.TimeValue;
import com.github.liyibo1110.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 用于单个连接的连接管理器。该连接管理器仅维护一个活动连接。
 * 尽管该类完全线程安全，但应仅由一个执行线程使用，因为每次只能有一个线程租用该连接。
 *
 * 该连接管理器会尝试在后续针对相同路由的请求中复用该连接。
 * 但是，如果持久连接的路由与连接请求的路由不匹配，它将关闭现有连接并为给定的路由重新建立连接。
 * 如果连接已被分配，则会抛出IllegalStateException。
 *
 * 应将此连接管理器实现用于EJB容器中，而非PoolingHttpClientConnectionManager。
 * @author liyibo
 * @date 2026-04-21 11:35
 */
@Contract(threading = ThreadingBehavior.SAFE)
public class BasicHttpClientConnectionManager implements HttpClientConnectionManager {
    private static final Logger LOG = LoggerFactory.getLogger(BasicHttpClientConnectionManager.class);

    private static final AtomicLong COUNT = new AtomicLong(0);

    private final HttpClientConnectionOperator connectionOperator;
    private final HttpConnectionFactory<ManagedHttpClientConnection> connFactory;
    private final String id;

    private ManagedHttpClientConnection conn;
    private HttpRoute route;
    private Object state;
    private long created;
    private long updated;
    private long expiry;
    private boolean leased;
    private SocketConfig socketConfig;
    private ConnectionConfig connectionConfig;
    private TlsConfig tlsConfig;

    private final AtomicBoolean closed;

    private static Registry<ConnectionSocketFactory> getDefaultRegistry() {
        return RegistryBuilder.<ConnectionSocketFactory>create()
                .register(URIScheme.HTTP.id, PlainConnectionSocketFactory.getSocketFactory())
                .register(URIScheme.HTTPS.id, SSLConnectionSocketFactory.getSocketFactory())
                .build();
    }

    public BasicHttpClientConnectionManager(final Lookup<ConnectionSocketFactory> socketFactoryRegistry,
                                            final HttpConnectionFactory<ManagedHttpClientConnection> connFactory,
                                            final SchemePortResolver schemePortResolver,
                                            final DnsResolver dnsResolver) {
        this(new DefaultHttpClientConnectionOperator(socketFactoryRegistry, schemePortResolver, dnsResolver), connFactory);
    }

    public BasicHttpClientConnectionManager(final HttpClientConnectionOperator httpClientConnectionOperator,
                                            final HttpConnectionFactory<ManagedHttpClientConnection> connFactory) {
        super();
        this.connectionOperator = Args.notNull(httpClientConnectionOperator, "Connection operator");
        this.connFactory = connFactory != null ? connFactory : ManagedHttpClientConnectionFactory.INSTANCE;
        this.id = String.format("ep-%010d", COUNT.getAndIncrement());
        this.expiry = Long.MAX_VALUE;
        this.socketConfig = SocketConfig.DEFAULT;
        this.connectionConfig = ConnectionConfig.DEFAULT;
        this.tlsConfig = TlsConfig.DEFAULT;
        this.closed = new AtomicBoolean(false);
    }

    public BasicHttpClientConnectionManager(final Lookup<ConnectionSocketFactory> socketFactoryRegistry,
                                            final HttpConnectionFactory<ManagedHttpClientConnection> connFactory) {
        this(socketFactoryRegistry, connFactory, null, null);
    }

    public BasicHttpClientConnectionManager(final Lookup<ConnectionSocketFactory> socketFactoryRegistry) {
        this(socketFactoryRegistry, null, null, null);
    }

    public BasicHttpClientConnectionManager() {
        this(getDefaultRegistry(), null, null, null);
    }

    @Override
    public void close() {
        close(CloseMode.GRACEFUL);
    }

    @Override
    public void close(final CloseMode closeMode) {
        if (this.closed.compareAndSet(false, true))
            closeConnection(closeMode);
    }

    HttpRoute getRoute() {
        return route;
    }

    Object getState() {
        return state;
    }

    public synchronized SocketConfig getSocketConfig() {
        return socketConfig;
    }

    public synchronized void setSocketConfig(final SocketConfig socketConfig) {
        this.socketConfig = socketConfig != null ? socketConfig : SocketConfig.DEFAULT;
    }

    public synchronized ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public synchronized void setConnectionConfig(final ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig != null ? connectionConfig : ConnectionConfig.DEFAULT;
    }

    public synchronized TlsConfig getTlsConfig() {
        return tlsConfig;
    }

    public synchronized void setTlsConfig(final TlsConfig tlsConfig) {
        this.tlsConfig = tlsConfig != null ? tlsConfig : TlsConfig.DEFAULT;
    }

    public LeaseRequest lease(final String id, final HttpRoute route, final Object state) {
        return lease(id, route, Timeout.DISABLED, state);
    }

    @Override
    public LeaseRequest lease(final String id, final HttpRoute route, final Timeout requestTimeout, final Object state) {
        return new LeaseRequest() {
            @Override
            public ConnectionEndpoint get(final Timeout timeout) throws InterruptedException, ExecutionException, TimeoutException {
                try {
                    return new InternalConnectionEndpoint(route, getConnection(route, state));
                } catch (final IOException ex) {
                    throw new ExecutionException(ex.getMessage(), ex);
                }
            }

            @Override
            public boolean cancel() {
                return false;
            }

        };
    }

    private synchronized void closeConnection(final CloseMode closeMode) {
        if (this.conn != null) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} Closing connection {}", id, closeMode);
            this.conn.close(closeMode);
            this.conn = null;
        }
    }

    private void checkExpiry() {
        if (this.conn != null && System.currentTimeMillis() >= this.expiry) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} Connection expired @ {}", id, Instant.ofEpochMilli(this.expiry));
            closeConnection(CloseMode.GRACEFUL);
        }
    }

    private void validate() {
        if (this.conn != null) {
            final TimeValue timeToLive = connectionConfig.getTimeToLive();
            if (TimeValue.isNonNegative(timeToLive)) {
                final Deadline deadline = Deadline.calculate(created, timeToLive);
                if (deadline.isExpired())
                    closeConnection(CloseMode.GRACEFUL);
            }
        }
        if (this.conn != null) {
            final TimeValue timeValue = connectionConfig.getValidateAfterInactivity() != null
                    ? connectionConfig.getValidateAfterInactivity()
                    : TimeValue.ofSeconds(2);
            if (TimeValue.isNonNegative(timeValue)) {
                final Deadline deadline = Deadline.calculate(updated, timeValue);
                if (deadline.isExpired()) {
                    boolean stale;
                    try {
                        stale = conn.isStale();
                    } catch (final IOException ignore) {
                        stale = true;
                    }
                    if (stale) {
                        if (LOG.isDebugEnabled())
                            LOG.debug("{} connection {} is stale", id, ConnPoolSupport.getId(conn));
                        closeConnection(CloseMode.GRACEFUL);
                    }
                }
            }
        }
    }

    synchronized ManagedHttpClientConnection getConnection(final HttpRoute route, final Object state) throws IOException {
        Asserts.check(!isClosed(), "Connection manager has been shut down");
        if (LOG.isDebugEnabled())
            LOG.debug("{} Get connection for route {}", id, route);

        Asserts.check(!this.leased, "Connection %s is still allocated", conn);
        if (!Objects.equals(this.route, route) || !Objects.equals(this.state, state))
            closeConnection(CloseMode.GRACEFUL);

        this.route = route;
        this.state = state;
        checkExpiry();
        validate();
        if (this.conn == null) {
            this.conn = this.connFactory.createConnection(null);
            this.created = System.currentTimeMillis();
        } else {
            this.conn.activate();
        }
        this.leased = true;
        if (LOG.isDebugEnabled())
            LOG.debug("{} Using connection {}", id, conn);
        return this.conn;
    }

    private InternalConnectionEndpoint cast(final ConnectionEndpoint endpoint) {
        if (endpoint instanceof InternalConnectionEndpoint)
            return (InternalConnectionEndpoint) endpoint;
        throw new IllegalStateException("Unexpected endpoint class: " + endpoint.getClass());
    }

    @Override
    public synchronized void release(final ConnectionEndpoint endpoint, final Object state, final TimeValue keepAlive) {
        Args.notNull(endpoint, "Managed endpoint");
        final InternalConnectionEndpoint internalEndpoint = cast(endpoint);
        final ManagedHttpClientConnection conn = internalEndpoint.detach();
        if (LOG.isDebugEnabled())
            LOG.debug("{} Releasing connection {}", id, conn);
        if (isClosed())
            return;

        try {
            if (keepAlive == null)
                this.conn.close(CloseMode.GRACEFUL);

            this.updated = System.currentTimeMillis();
            if (!this.conn.isOpen() && !this.conn.isConsistent()) {
                this.route = null;
                this.conn = null;
                this.expiry = Long.MAX_VALUE;
                if (LOG.isDebugEnabled())
                    LOG.debug("{} Connection is not kept alive", id);
            } else {
                this.state = state;
                if (conn != null)
                    conn.passivate();

                if (TimeValue.isPositive(keepAlive)) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} Connection can be kept alive for {}", id, keepAlive);
                    this.expiry = this.updated + keepAlive.toMilliseconds();
                } else {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} Connection can be kept alive indefinitely", id);
                    this.expiry = Long.MAX_VALUE;
                }
            }
        } finally {
            this.leased = false;
        }
    }

    @Override
    public synchronized void connect(final ConnectionEndpoint endpoint, final TimeValue timeout, final HttpContext context) throws IOException {
        Args.notNull(endpoint, "Endpoint");

        final InternalConnectionEndpoint internalEndpoint = cast(endpoint);
        if (internalEndpoint.isConnected())
            return;

        final HttpRoute route = internalEndpoint.getRoute();
        final HttpHost host;
        if (route.getProxyHost() != null)
            host = route.getProxyHost();
        else
            host = route.getTargetHost();

        final Timeout connectTimeout = timeout != null ? Timeout.of(timeout.getDuration(), timeout.getTimeUnit()) : connectionConfig.getConnectTimeout();
        final ManagedHttpClientConnection connection = internalEndpoint.getConnection();
        if (LOG.isDebugEnabled())
            LOG.debug("{} connecting endpoint to {} ({})", ConnPoolSupport.getId(endpoint), host, connectTimeout);

        this.connectionOperator.connect(
                connection,
                host,
                route.getLocalSocketAddress(),
                connectTimeout,
                socketConfig,
                tlsConfig,
                context);
        if (LOG.isDebugEnabled())
            LOG.debug("{} connected {}", ConnPoolSupport.getId(endpoint), ConnPoolSupport.getId(conn));
        final Timeout socketTimeout = connectionConfig.getSocketTimeout();
        if (socketTimeout != null)
            connection.setSocketTimeout(socketTimeout);
    }

    @Override
    public synchronized void upgrade(final ConnectionEndpoint endpoint, final HttpContext context) throws IOException {
        Args.notNull(endpoint, "Endpoint");
        Args.notNull(route, "HTTP route");
        final InternalConnectionEndpoint internalEndpoint = cast(endpoint);
        this.connectionOperator.upgrade(internalEndpoint.getConnection(),
                                        internalEndpoint.getRoute().getTargetHost(),
                                        tlsConfig,
                                        context);
    }

    public synchronized void closeExpired() {
        if (isClosed())
            return;
        if (!this.leased)
            checkExpiry();
    }

    public synchronized void closeIdle(final TimeValue idleTime) {
        Args.notNull(idleTime, "Idle time");
        if (isClosed())
            return;

        if (!this.leased) {
            long time = idleTime.toMilliseconds();
            if (time < 0)
                time = 0;
            final long deadline = System.currentTimeMillis() - time;
            if (this.updated <= deadline)
                closeConnection(CloseMode.GRACEFUL);
        }
    }

    @Deprecated
    public TimeValue getValidateAfterInactivity() {
        return connectionConfig.getValidateAfterInactivity();
    }

    @Deprecated
    public void setValidateAfterInactivity(final TimeValue validateAfterInactivity) {
        this.connectionConfig = ConnectionConfig.custom()
                .setValidateAfterInactivity(validateAfterInactivity)
                .build();
    }

    boolean isClosed() {
        return this.closed.get();
    }

    class InternalConnectionEndpoint extends ConnectionEndpoint {
        private final HttpRoute route;
        private final AtomicReference<ManagedHttpClientConnection> connRef;

        public InternalConnectionEndpoint(final HttpRoute route, final ManagedHttpClientConnection conn) {
            this.route = route;
            this.connRef = new AtomicReference<>(conn);
        }

        HttpRoute getRoute() {
            return route;
        }

        ManagedHttpClientConnection getConnection() {
            final ManagedHttpClientConnection conn = this.connRef.get();
            if (conn == null)
                throw new ConnectionShutdownException();
            return conn;
        }

        ManagedHttpClientConnection getValidatedConnection() {
            final ManagedHttpClientConnection conn = getConnection();
            Asserts.check(conn.isOpen(), "Endpoint is not connected");
            return conn;
        }

        ManagedHttpClientConnection detach() {
            return this.connRef.getAndSet(null);
        }

        @Override
        public boolean isConnected() {
            final ManagedHttpClientConnection conn = getConnection();
            return conn != null && conn.isOpen();
        }

        @Override
        public void close(final CloseMode closeMode) {
            final ManagedHttpClientConnection conn = detach();
            if (conn != null)
                conn.close(closeMode);
        }

        @Override
        public void close() throws IOException {
            final ManagedHttpClientConnection conn = detach();
            if (conn != null)
                conn.close();
        }

        @Override
        public void setSocketTimeout(final Timeout timeout) {
            getValidatedConnection().setSocketTimeout(timeout);
        }

        @Override
        public ClassicHttpResponse execute(final String exchangeId,
                                           final ClassicHttpRequest request,
                                           final HttpRequestExecutor requestExecutor,
                                           final HttpContext context) throws IOException, HttpException {
            Args.notNull(request, "HTTP request");
            Args.notNull(requestExecutor, "Request executor");
            if (LOG.isDebugEnabled())
                LOG.debug("{} Executing exchange {}", id, exchangeId);
            return requestExecutor.execute(request, getValidatedConnection(), context);
        }
    }
}
