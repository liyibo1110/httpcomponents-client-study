package com.github.liyibo1110.hc.client5.http.impl.io;

import com.github.liyibo1110.hc.client5.http.DnsResolver;
import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.client5.http.config.ConnectionConfig;
import com.github.liyibo1110.hc.client5.http.config.TlsConfig;
import com.github.liyibo1110.hc.client5.http.impl.ConnPoolSupport;
import com.github.liyibo1110.hc.client5.http.impl.ConnectionShutdownException;
import com.github.liyibo1110.hc.client5.http.impl.PrefixedIncrementingId;
import com.github.liyibo1110.hc.client5.http.io.ConnectionEndpoint;
import com.github.liyibo1110.hc.client5.http.io.HttpClientConnectionManager;
import com.github.liyibo1110.hc.client5.http.io.HttpClientConnectionOperator;
import com.github.liyibo1110.hc.client5.http.io.LeaseRequest;
import com.github.liyibo1110.hc.client5.http.io.ManagedHttpClientConnection;
import com.github.liyibo1110.hc.client5.http.socket.ConnectionSocketFactory;
import com.github.liyibo1110.hc.client5.http.socket.PlainConnectionSocketFactory;
import com.github.liyibo1110.hc.client5.http.ssl.SSLConnectionSocketFactory;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.function.Resolver;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.URIScheme;
import com.github.liyibo1110.hc.core5.http.config.Registry;
import com.github.liyibo1110.hc.core5.http.config.RegistryBuilder;
import com.github.liyibo1110.hc.core5.http.impl.io.HttpRequestExecutor;
import com.github.liyibo1110.hc.core5.http.io.HttpConnectionFactory;
import com.github.liyibo1110.hc.core5.http.io.SocketConfig;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.io.CloseMode;
import com.github.liyibo1110.hc.core5.pool.ConnPoolControl;
import com.github.liyibo1110.hc.core5.pool.LaxConnPool;
import com.github.liyibo1110.hc.core5.pool.ManagedConnPool;
import com.github.liyibo1110.hc.core5.pool.PoolConcurrencyPolicy;
import com.github.liyibo1110.hc.core5.pool.PoolEntry;
import com.github.liyibo1110.hc.core5.pool.PoolReusePolicy;
import com.github.liyibo1110.hc.core5.pool.PoolStats;
import com.github.liyibo1110.hc.core5.pool.StrictConnPool;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.Asserts;
import com.github.liyibo1110.hc.core5.util.Deadline;
import com.github.liyibo1110.hc.core5.util.Identifiable;
import com.github.liyibo1110.hc.core5.util.TimeValue;
import com.github.liyibo1110.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PoolingClientConnectionPoolManager维护一个ManagedHttpClientConnections连接池，并能够处理来自多个执行线程的连接请求。
 * 连接按路由进行池化。如果针对某个路由的请求，管理器在连接池中已有可用的持久连接，则会通过从连接池中租用连接来处理该请求，而非创建新连接。
 *
 * PoolingClientConnectionPoolManager针对每条路由以及整体维护连接的最大限制。不过，可以通过ConnPoolControl方法调整连接限制。
 * 在构造时设置的总生存时间(TTL)定义了持久连接的最大生命周期，无论其过期设置如何。任何持久连接在超过其TTL值后都不会被重复使用。
 *
 * 职责：负责按route维度来维护连接池，并向上层提供：租出endpoint -> 建立/升级连接 -> 执行请求 -> 归还/回收连接，这一整套能力。
 * 以HttpRoute为key来管理可复用的连接池，并把池中的PoolEntry包装成上层可操作的ConnectionEndpoint，提供给执行链租用、连接、升级、执行和释放。
 * 同时实现了HttpClientConnectionManager和ConnPoolControl接口，所以既是连接管理器，又是连接池配额控制器。
 * @author liyibo
 * @date 2026-04-21 13:33
 */
@Contract(threading = ThreadingBehavior.SAFE_CONDITIONAL)
public class PoolingHttpClientConnectionManager implements HttpClientConnectionManager, ConnPoolControl<HttpRoute> {
    private static final Logger LOG = LoggerFactory.getLogger(PoolingHttpClientConnectionManager.class);

    public static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 25;
    public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 5;

    private final HttpClientConnectionOperator connectionOperator;
    private final ManagedConnPool<HttpRoute, ManagedHttpClientConnection> pool;
    private final HttpConnectionFactory<ManagedHttpClientConnection> connFactory;

    /** manager自身的关闭状态 */
    private final AtomicBoolean closed;

    /** 负责socket层的参数 */
    private volatile Resolver<HttpRoute, SocketConfig> socketConfigResolver;

    /** 负责连接层的参数，例如connectTimeout和TTL */
    private volatile Resolver<HttpRoute, ConnectionConfig> connectionConfigResolver;

    /** 负责TLS层的参数 */
    private volatile Resolver<HttpHost, TlsConfig> tlsConfigResolver;

    public PoolingHttpClientConnectionManager() {
        this(RegistryBuilder.<ConnectionSocketFactory>create()
                .register(URIScheme.HTTP.id, PlainConnectionSocketFactory.getSocketFactory())
                .register(URIScheme.HTTPS.id, SSLConnectionSocketFactory.getSocketFactory())
                .build());
    }

    public PoolingHttpClientConnectionManager(final Registry<ConnectionSocketFactory> socketFactoryRegistry) {
        this(socketFactoryRegistry, null);
    }

    public PoolingHttpClientConnectionManager(final Registry<ConnectionSocketFactory> socketFactoryRegistry,
                                              final HttpConnectionFactory<ManagedHttpClientConnection> connFactory) {
        this(socketFactoryRegistry, PoolConcurrencyPolicy.STRICT, TimeValue.NEG_ONE_MILLISECOND, connFactory);
    }

    public PoolingHttpClientConnectionManager(final Registry<ConnectionSocketFactory> socketFactoryRegistry,
                                              final PoolConcurrencyPolicy poolConcurrencyPolicy,
                                              final TimeValue timeToLive,
                                              final HttpConnectionFactory<ManagedHttpClientConnection> connFactory) {
        this(socketFactoryRegistry, poolConcurrencyPolicy, PoolReusePolicy.LIFO, timeToLive, connFactory);
    }

    public PoolingHttpClientConnectionManager(final Registry<ConnectionSocketFactory> socketFactoryRegistry,
                                              final PoolConcurrencyPolicy poolConcurrencyPolicy,
                                              final PoolReusePolicy poolReusePolicy,
                                              final TimeValue timeToLive) {
        this(socketFactoryRegistry, poolConcurrencyPolicy, poolReusePolicy, timeToLive, null);
    }

    public PoolingHttpClientConnectionManager(final Registry<ConnectionSocketFactory> socketFactoryRegistry,
                                              final PoolConcurrencyPolicy poolConcurrencyPolicy,
                                              final PoolReusePolicy poolReusePolicy,
                                              final TimeValue timeToLive,
                                              final HttpConnectionFactory<ManagedHttpClientConnection> connFactory) {
        this(socketFactoryRegistry, poolConcurrencyPolicy, poolReusePolicy, timeToLive, null, null, connFactory);
    }

    public PoolingHttpClientConnectionManager(final Registry<ConnectionSocketFactory> socketFactoryRegistry,
                                              final PoolConcurrencyPolicy poolConcurrencyPolicy,
                                              final PoolReusePolicy poolReusePolicy,
                                              final TimeValue timeToLive,
                                              final SchemePortResolver schemePortResolver,
                                              final DnsResolver dnsResolver,
                                              final HttpConnectionFactory<ManagedHttpClientConnection> connFactory) {
        this(new DefaultHttpClientConnectionOperator(socketFactoryRegistry, schemePortResolver, dnsResolver),
                poolConcurrencyPolicy,
                poolReusePolicy,
                timeToLive,
                connFactory);
    }

    @Internal
    protected PoolingHttpClientConnectionManager(final HttpClientConnectionOperator httpClientConnectionOperator,
                                                 final PoolConcurrencyPolicy poolConcurrencyPolicy,
                                                 final PoolReusePolicy poolReusePolicy,
                                                 final TimeValue timeToLive,
                                                 final HttpConnectionFactory<ManagedHttpClientConnection> connFactory) {
        super();
        this.connectionOperator = Args.notNull(httpClientConnectionOperator, "Connection operator");
        /**
         * 根据PoolConcurrencyPolicy，来选择底层连接池的实现（StrictConnPool or LaxConnPool）。
         * 并且重写了closeExpired()方法，也就是manager把过期连接怎么判定的逻辑，接到了自己的closeIfExpired()方法上面了。
         */
        switch (poolConcurrencyPolicy != null ? poolConcurrencyPolicy : PoolConcurrencyPolicy.STRICT) {
            case STRICT:
                this.pool = new StrictConnPool<HttpRoute, ManagedHttpClientConnection>(
                        DEFAULT_MAX_CONNECTIONS_PER_ROUTE,
                        DEFAULT_MAX_TOTAL_CONNECTIONS,
                        timeToLive,
                        poolReusePolicy,
                        null) {

                    @Override
                    public void closeExpired() {
                        enumAvailable(e -> closeIfExpired(e));
                    }
                };
                break;
            case LAX:
                this.pool = new LaxConnPool<HttpRoute, ManagedHttpClientConnection>(
                        DEFAULT_MAX_CONNECTIONS_PER_ROUTE,
                        timeToLive,
                        poolReusePolicy,
                        null) {

                    @Override
                    public void closeExpired() {
                        enumAvailable(e -> closeIfExpired(e));
                    }
                };
                break;
            default:
                throw new IllegalArgumentException("Unexpected PoolConcurrencyPolicy value: " + poolConcurrencyPolicy);
        }
        // 参数没传就用默认的连接工厂
        this.connFactory = connFactory != null ? connFactory : ManagedHttpClientConnectionFactory.INSTANCE;
        this.closed = new AtomicBoolean(false);
    }

    @Internal
    protected PoolingHttpClientConnectionManager(final HttpClientConnectionOperator httpClientConnectionOperator,
                                                 final ManagedConnPool<HttpRoute, ManagedHttpClientConnection> pool,
                                                 final HttpConnectionFactory<ManagedHttpClientConnection> connFactory) {
        super();
        this.connectionOperator = Args.notNull(httpClientConnectionOperator, "Connection operator");
        this.pool = Args.notNull(pool, "Connection pool");
        this.connFactory = connFactory != null ? connFactory : ManagedHttpClientConnectionFactory.INSTANCE;
        this.closed = new AtomicBoolean(false);
    }

    @Override
    public void close() {
        close(CloseMode.GRACEFUL);
    }

    @Override
    public void close(final CloseMode closeMode) {
        if (this.closed.compareAndSet(false, true)) {
            if (LOG.isDebugEnabled())
                LOG.debug("Shutdown connection pool {}", closeMode);
            this.pool.close(closeMode);
            LOG.debug("Connection pool shut down");
        }
    }

    private InternalConnectionEndpoint cast(final ConnectionEndpoint endpoint) {
        if (endpoint instanceof InternalConnectionEndpoint)
            return (InternalConnectionEndpoint) endpoint;
        throw new IllegalStateException("Unexpected endpoint class: " + endpoint.getClass());
    }

    /**
     * 先用resolver按route解析，没有就回退使用SocketConfig.Default。
     */
    private SocketConfig resolveSocketConfig(final HttpRoute route) {
        final Resolver<HttpRoute, SocketConfig> resolver = this.socketConfigResolver;
        final SocketConfig socketConfig = resolver != null ? resolver.resolve(route) : null;
        return socketConfig != null ? socketConfig : SocketConfig.DEFAULT;
    }

    /**
     * 先用resolver按route解析，没有就回退使用ConnectionConfig.Default。
     */
    private ConnectionConfig resolveConnectionConfig(final HttpRoute route) {
        final Resolver<HttpRoute, ConnectionConfig> resolver = this.connectionConfigResolver;
        final ConnectionConfig connectionConfig = resolver != null ? resolver.resolve(route) : null;
        return connectionConfig != null ? connectionConfig : ConnectionConfig.DEFAULT;
    }

    /**
     * 先用resolver按host解析，没有就回退使用TlsConfig.Default。
     */
    private TlsConfig resolveTlsConfig(final HttpHost host) {
        final Resolver<HttpHost, TlsConfig> resolver = this.tlsConfigResolver;
        final TlsConfig tlsConfig = resolver != null ? resolver.resolve(host) : null;
        return tlsConfig != null ? tlsConfig : TlsConfig.DEFAULT;
    }

    /**
     * 如果传入的ConnectionConfig没有指定，则默认用2秒。
     */
    private TimeValue resolveValidateAfterInactivity(final ConnectionConfig connectionConfig) {
        final TimeValue timeValue = connectionConfig.getValidateAfterInactivity();
        return timeValue != null ? timeValue : TimeValue.ofSeconds(2);
    }

    public LeaseRequest lease(final String id, final HttpRoute route, final Object state) {
        return lease(id, route, Timeout.DISABLED, state);
    }

    /**
     * 向连接池申请一个route对应的连接入口，并最终返回一个上层可操作的ConnectionEndpoint封装。
     */
    @Override
    public LeaseRequest lease(final String id, final HttpRoute route, final Timeout requestTimeout, final Object state) {
        Args.notNull(route, "HTTP route");
        if (LOG.isDebugEnabled())
            LOG.debug("{} endpoint lease request ({}) {}", id, requestTimeout, ConnPoolSupport.formatStats(route, state, pool));

        final Future<PoolEntry<HttpRoute, ManagedHttpClientConnection>> leaseFuture = this.pool.lease(route, state, requestTimeout, null);
        return new LeaseRequest() {
            private volatile ConnectionEndpoint endpoint;

            /**
             * 这里才是真正取endpoint的地方，因为LeaseRequest对象本身代表异步获取。
             */
            @Override
            public synchronized ConnectionEndpoint get(final Timeout timeout) throws InterruptedException, ExecutionException, TimeoutException {
                Args.notNull(timeout, "Operation timeout");
                if (this.endpoint != null)
                    return this.endpoint;

                final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry;
                try {
                    poolEntry = leaseFuture.get(timeout.getDuration(), timeout.getTimeUnit());
                } catch (final TimeoutException ex) {
                    leaseFuture.cancel(true);
                    throw ex;
                }
                if (LOG.isDebugEnabled())
                    LOG.debug("{} endpoint leased {}", id, ConnPoolSupport.formatStats(route, state, pool));

                final ConnectionConfig connectionConfig = resolveConnectionConfig(route);
                try {
                    if (poolEntry.hasConnection()) {
                        final TimeValue timeToLive = connectionConfig.getTimeToLive();
                        if (TimeValue.isNonNegative(timeToLive)) {
                            if (timeToLive.getDuration() == 0 || Deadline.calculate(poolEntry.getCreated(), timeToLive).isExpired())
                                poolEntry.discardConnection(CloseMode.GRACEFUL);
                        }
                    }
                    /**
                     * 如果poolEntry已经带连接了，先看它有没有超过timeToLive。
                     * 意思是即使池里已经有老连接，也不能随便复用，如果已经超过TTL就要丢掉。
                     */
                    if (poolEntry.hasConnection()) {
                        final TimeValue timeValue = resolveValidateAfterInactivity(connectionConfig);
                        if (TimeValue.isNonNegative(timeValue)) {
                            if (timeValue.getDuration() == 0 || Deadline.calculate(poolEntry.getUpdated(), timeValue).isExpired()) {
                                final ManagedHttpClientConnection conn = poolEntry.getConnection();
                                boolean stale;
                                try {
                                    stale = conn.isStale();
                                } catch (final IOException ignore) {
                                    stale = true;
                                }
                                if (stale) {
                                    if (LOG.isDebugEnabled())
                                        LOG.debug("{} connection {} is stale", id, ConnPoolSupport.getId(conn));
                                    poolEntry.discardConnection(CloseMode.IMMEDIATE);
                                }
                            }
                        }
                    }
                    /**
                     * 如果entry里已有连接，就激活它，否则创建一个新连接并赋值给entry。
                     */
                    final ManagedHttpClientConnection conn = poolEntry.getConnection();
                    if (conn != null)
                        conn.activate();
                    else
                        poolEntry.assignConnection(connFactory.createConnection(null));

                    /**
                     * 包装成InternalConnectionEndpoint，ConnectionEndpoint才是暴露给上层的对象。
                     */
                    this.endpoint = new InternalConnectionEndpoint(poolEntry);
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} acquired {}", id, ConnPoolSupport.getId(endpoint));
                    return this.endpoint;
                } catch (final Exception ex) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} endpoint lease failed", id);
                    pool.release(poolEntry, false);
                    throw new ExecutionException(ex.getMessage(), ex);
                }
            }

            @Override
            public boolean cancel() {
                return leaseFuture.cancel(true);
            }
        };
    }

    /**
     * 把endpoint对应的poolEntry从上层拿回来，判断连接能不能复用，然后归还给连接池。
     */
    @Override
    public void release(final ConnectionEndpoint endpoint, final Object state, final TimeValue keepAlive) {
        Args.notNull(endpoint, "Managed endpoint");
        /**
         * detach就是去除endpoint里面的poolEntry引用。
         */
        final PoolEntry<HttpRoute, ManagedHttpClientConnection> entry = cast(endpoint).detach();
        if (entry == null)
            return;

        if (LOG.isDebugEnabled())
            LOG.debug("{} releasing endpoint", ConnPoolSupport.getId(endpoint));

        /**
         * 如果keepalive为null，则直接关闭连接。
         */
        final ManagedHttpClientConnection conn = entry.getConnection();
        if (conn != null && keepAlive == null)
            conn.close(CloseMode.GRACEFUL);

        /**
         * 判断连接是否reusable，isConsistent()语义是说明连接内部协议没有坏掉（例如没有读写异常导致半残状态）
         * 如果可以复用，则更新状态和过期时间。
         */
        boolean reusable = conn != null && conn.isOpen() && conn.isConsistent();
        try {
            if (reusable) {
                entry.updateState(state);
                entry.updateExpiry(keepAlive);
                conn.passivate();
                if (LOG.isDebugEnabled()) {
                    final String s;
                    if (TimeValue.isPositive(keepAlive))
                        s = "for " + keepAlive;
                    else
                        s = "indefinitely";

                    LOG.debug("{} connection {} can be kept alive {}", ConnPoolSupport.getId(endpoint), ConnPoolSupport.getId(conn), s);
                }
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("{} connection is not kept alive", ConnPoolSupport.getId(endpoint));
            }
        } catch (final RuntimeException ex) {
            reusable = false;
            throw ex;
        } finally {
            this.pool.release(entry, reusable);
            if (LOG.isDebugEnabled())
                LOG.debug("{} connection released {}", ConnPoolSupport.getId(endpoint), ConnPoolSupport.formatStats(entry.getRoute(), entry.getState(), pool));
        }
    }

    /**
     * 让一个已经Lease出来的endpoint真正建立到底层first hop的网络连接。
     */
    @Override
    public void connect(final ConnectionEndpoint endpoint, final TimeValue timeout, final HttpContext context) throws IOException {
        Args.notNull(endpoint, "Managed endpoint");
        final InternalConnectionEndpoint internalEndpoint = cast(endpoint);
        // 已经连接了，则直接返回
        if (internalEndpoint.isConnected())
            return;

        // 确保poolEntry里有connection对象
        final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = internalEndpoint.getPoolEntry();
        if (!poolEntry.hasConnection())
            poolEntry.assignConnection(connFactory.createConnection(null));

        final HttpRoute route = poolEntry.getRoute();
        // 确定first hop的host（如果有proxy则第一跳连proxy，否则连target）
        final HttpHost host = route.getProxyHost() != null ? route.getProxyHost() : route.getTargetHost();
        final SocketConfig socketConfig = resolveSocketConfig(route);
        final ConnectionConfig connectionConfig = resolveConnectionConfig(route);
        final TlsConfig tlsConfig = resolveTlsConfig(host);
        // 决定connect timeout
        final Timeout connectTimeout = timeout != null ? Timeout.of(timeout.getDuration(), timeout.getTimeUnit()) : connectionConfig.getConnectTimeout();
        if (LOG.isDebugEnabled())
            LOG.debug("{} connecting endpoint to {} ({})", ConnPoolSupport.getId(endpoint), host, connectTimeout);

        final ManagedHttpClientConnection conn = poolEntry.getConnection();
        // 委托给operator组件来连接
        this.connectionOperator.connect(
                conn,
                host,
                route.getLocalSocketAddress(),
                connectTimeout,
                socketConfig,
                tlsConfig,
                context);
        if (LOG.isDebugEnabled())
            LOG.debug("{} connected {}", ConnPoolSupport.getId(endpoint), ConnPoolSupport.getId(conn));

        // 建立连接后，设置socket timeout
        final Timeout socketTimeout = connectionConfig.getSocketTimeout();
        if (socketTimeout != null)
            conn.setSocketTimeout(socketTimeout);
    }

    /**
     * 把已有连接升级成TLS之类的安全连接。
     */
    @Override
    public void upgrade(final ConnectionEndpoint endpoint, final HttpContext context) throws IOException {
        Args.notNull(endpoint, "Managed endpoint");
        final InternalConnectionEndpoint internalEndpoint = cast(endpoint);
        final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = internalEndpoint.getValidatedPoolEntry();
        final HttpRoute route = poolEntry.getRoute();
        final HttpHost host = route.getProxyHost() != null ? route.getProxyHost() : route.getTargetHost();
        final TlsConfig tlsConfig = resolveTlsConfig(host);
        this.connectionOperator.upgrade(poolEntry.getConnection(), route.getTargetHost(), tlsConfig, context);
    }

    /**
     * 清理方法，关闭闲置超过指定时间的连接。
     */
    @Override
    public void closeIdle(final TimeValue idleTime) {
        Args.notNull(idleTime, "Idle time");
        if (LOG.isDebugEnabled())
            LOG.debug("Closing connections idle longer than {}", idleTime);
        this.pool.closeIdle(idleTime);
    }

    /**
     * 关闭已过期的连接，至于怎么算过期，由前面构造方法里，连接池重写的closeExpired和本来的closeIfExpired一起实现的。
     */
    @Override
    public void closeExpired() {
        LOG.debug("Closing expired connections");
        this.pool.closeExpired();
    }

    @Override
    public Set<HttpRoute> getRoutes() {
        return this.pool.getRoutes();
    }

    @Override
    public int getMaxTotal() {
        return this.pool.getMaxTotal();
    }

    @Override
    public void setMaxTotal(final int max) {
        this.pool.setMaxTotal(max);
    }

    @Override
    public int getDefaultMaxPerRoute() {
        return this.pool.getDefaultMaxPerRoute();
    }

    @Override
    public void setDefaultMaxPerRoute(final int max) {
        this.pool.setDefaultMaxPerRoute(max);
    }

    @Override
    public int getMaxPerRoute(final HttpRoute route) {
        return this.pool.getMaxPerRoute(route);
    }

    @Override
    public void setMaxPerRoute(final HttpRoute route, final int max) {
        this.pool.setMaxPerRoute(route, max);
    }

    @Override
    public PoolStats getTotalStats() {
        return this.pool.getTotalStats();
    }

    @Override
    public PoolStats getStats(final HttpRoute route) {
        return this.pool.getStats(route);
    }

    public void setDefaultSocketConfig(final SocketConfig config) {
        this.socketConfigResolver = (route) -> config;
    }

    public void setSocketConfigResolver(final Resolver<HttpRoute, SocketConfig> socketConfigResolver) {
        this.socketConfigResolver = socketConfigResolver;
    }

    public void setDefaultConnectionConfig(final ConnectionConfig config) {
        this.connectionConfigResolver = (route) -> config;
    }

    public void setConnectionConfigResolver(final Resolver<HttpRoute, ConnectionConfig> connectionConfigResolver) {
        this.connectionConfigResolver = connectionConfigResolver;
    }

    public void setDefaultTlsConfig(final TlsConfig config) {
        this.tlsConfigResolver = (host) -> config;
    }

    public void setTlsConfigResolver(final Resolver<HttpHost, TlsConfig> tlsConfigResolver) {
        this.tlsConfigResolver = tlsConfigResolver;
    }

    /**
     *
     */
    void closeIfExpired(final PoolEntry<HttpRoute, ManagedHttpClientConnection> entry) {
        final long now = System.currentTimeMillis();
        if (entry.getExpiryDeadline().isBefore(now)) {  // 先看entry自己的expiryDeadline
            entry.discardConnection(CloseMode.GRACEFUL);
        } else {    // 否则再看connection config的TTL
            final ConnectionConfig connectionConfig = resolveConnectionConfig(entry.getRoute());
            final TimeValue timeToLive = connectionConfig.getTimeToLive();
            if (timeToLive != null && Deadline.calculate(entry.getCreated(), timeToLive).isBefore(now))
                entry.discardConnection(CloseMode.GRACEFUL);
        }
    }

    @Deprecated
    public SocketConfig getDefaultSocketConfig() {
        return SocketConfig.DEFAULT;
    }

    @Deprecated
    public TimeValue getValidateAfterInactivity() {
        return ConnectionConfig.DEFAULT.getValidateAfterInactivity();
    }

    @Deprecated
    public void setValidateAfterInactivity(final TimeValue validateAfterInactivity) {
        setDefaultConnectionConfig(ConnectionConfig.custom()
                .setValidateAfterInactivity(validateAfterInactivity)
                .build());
    }

    private static final PrefixedIncrementingId INCREMENTING_ID = new PrefixedIncrementingId("ep-");

    static class InternalConnectionEndpoint extends ConnectionEndpoint implements Identifiable {
        private final AtomicReference<PoolEntry<HttpRoute, ManagedHttpClientConnection>> poolEntryRef;
        private final String id;

        InternalConnectionEndpoint(final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry) {
            this.poolEntryRef = new AtomicReference<>(poolEntry);
            this.id = INCREMENTING_ID.getNextId();
        }

        @Override
        public String getId() {
            return id;
        }

        PoolEntry<HttpRoute, ManagedHttpClientConnection> getPoolEntry() {
            final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = poolEntryRef.get();
            if (poolEntry == null)
                throw new ConnectionShutdownException();
            return poolEntry;
        }

        PoolEntry<HttpRoute, ManagedHttpClientConnection> getValidatedPoolEntry() {
            final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = getPoolEntry();
            final ManagedHttpClientConnection connection = poolEntry.getConnection();
            Asserts.check(connection != null && connection.isOpen(), "Endpoint is not connected");
            return poolEntry;
        }

        PoolEntry<HttpRoute, ManagedHttpClientConnection> detach() {
            return poolEntryRef.getAndSet(null);
        }

        @Override
        public void close(final CloseMode closeMode) {
            final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = poolEntryRef.get();
            if (poolEntry != null)
                poolEntry.discardConnection(closeMode);
        }

        @Override
        public void close() throws IOException {
            final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = poolEntryRef.get();
            if (poolEntry != null)
                poolEntry.discardConnection(CloseMode.GRACEFUL);
        }

        @Override
        public boolean isConnected() {
            final PoolEntry<HttpRoute, ManagedHttpClientConnection> poolEntry = getPoolEntry();
            final ManagedHttpClientConnection connection = poolEntry.getConnection();
            return connection != null && connection.isOpen();
        }

        @Override
        public void setSocketTimeout(final Timeout timeout) {
            getValidatedPoolEntry().getConnection().setSocketTimeout(timeout);
        }

        @Override
        public ClassicHttpResponse execute(final String exchangeId,
                                           final ClassicHttpRequest request,
                                           final HttpRequestExecutor requestExecutor,
                                           final HttpContext context) throws IOException, HttpException {
            Args.notNull(request, "HTTP request");
            Args.notNull(requestExecutor, "Request executor");
            final ManagedHttpClientConnection connection = getValidatedPoolEntry().getConnection();
            if (LOG.isDebugEnabled())
                LOG.debug("{} executing exchange {} over {}", id, exchangeId, ConnPoolSupport.getId(connection));
            return requestExecutor.execute(request, connection, context);
        }
    }
}
