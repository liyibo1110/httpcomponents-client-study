package com.github.liyibo1110.hc.client5.http.impl.io;

import com.github.liyibo1110.hc.client5.http.DnsResolver;
import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.client5.http.config.ConnectionConfig;
import com.github.liyibo1110.hc.client5.http.config.TlsConfig;
import com.github.liyibo1110.hc.client5.http.io.ManagedHttpClientConnection;
import com.github.liyibo1110.hc.client5.http.socket.ConnectionSocketFactory;
import com.github.liyibo1110.hc.client5.http.socket.LayeredConnectionSocketFactory;
import com.github.liyibo1110.hc.client5.http.socket.PlainConnectionSocketFactory;
import com.github.liyibo1110.hc.client5.http.ssl.SSLConnectionSocketFactory;
import com.github.liyibo1110.hc.core5.function.Resolver;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.URIScheme;
import com.github.liyibo1110.hc.core5.http.config.RegistryBuilder;
import com.github.liyibo1110.hc.core5.http.io.HttpConnectionFactory;
import com.github.liyibo1110.hc.core5.http.io.SocketConfig;
import com.github.liyibo1110.hc.core5.pool.PoolConcurrencyPolicy;
import com.github.liyibo1110.hc.core5.pool.PoolReusePolicy;
import com.github.liyibo1110.hc.core5.util.TimeValue;

/**
 * 生成PoolingHttpClientConnectionManager的builder。
 * @author liyibo
 * @date 2026-04-21 14:17
 */
public class PoolingHttpClientConnectionManagerBuilder {
    private HttpConnectionFactory<ManagedHttpClientConnection> connectionFactory;
    private LayeredConnectionSocketFactory sslSocketFactory;
    private SchemePortResolver schemePortResolver;
    private DnsResolver dnsResolver;
    private PoolConcurrencyPolicy poolConcurrencyPolicy;
    private PoolReusePolicy poolReusePolicy;
    private Resolver<HttpRoute, SocketConfig> socketConfigResolver;
    private Resolver<HttpRoute, ConnectionConfig> connectionConfigResolver;
    private Resolver<HttpHost, TlsConfig> tlsConfigResolver;

    private boolean systemProperties;

    private int maxConnTotal;
    private int maxConnPerRoute;

    public static PoolingHttpClientConnectionManagerBuilder create() {
        return new PoolingHttpClientConnectionManagerBuilder();
    }

    PoolingHttpClientConnectionManagerBuilder() {
        super();
    }

    public final PoolingHttpClientConnectionManagerBuilder setConnectionFactory(
            final HttpConnectionFactory<ManagedHttpClientConnection> connectionFactory) {
        this.connectionFactory = connectionFactory;
        return this;
    }

    public final PoolingHttpClientConnectionManagerBuilder setSSLSocketFactory(
            final LayeredConnectionSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
        return this;
    }

    public final PoolingHttpClientConnectionManagerBuilder setDnsResolver(final DnsResolver dnsResolver) {
        this.dnsResolver = dnsResolver;
        return this;
    }

    public final PoolingHttpClientConnectionManagerBuilder setSchemePortResolver(final SchemePortResolver schemePortResolver) {
        this.schemePortResolver = schemePortResolver;
        return this;
    }

    public final PoolingHttpClientConnectionManagerBuilder setPoolConcurrencyPolicy(final PoolConcurrencyPolicy poolConcurrencyPolicy) {
        this.poolConcurrencyPolicy = poolConcurrencyPolicy;
        return this;
    }

    public final PoolingHttpClientConnectionManagerBuilder setConnPoolPolicy(final PoolReusePolicy poolReusePolicy) {
        this.poolReusePolicy = poolReusePolicy;
        return this;
    }

    public final PoolingHttpClientConnectionManagerBuilder setMaxConnTotal(final int maxConnTotal) {
        this.maxConnTotal = maxConnTotal;
        return this;
    }

    public final PoolingHttpClientConnectionManagerBuilder setMaxConnPerRoute(final int maxConnPerRoute) {
        this.maxConnPerRoute = maxConnPerRoute;
        return this;
    }

    public final PoolingHttpClientConnectionManagerBuilder setDefaultSocketConfig(final SocketConfig config) {
        this.socketConfigResolver = (route) -> config;
        return this;
    }

    public final PoolingHttpClientConnectionManagerBuilder setSocketConfigResolver(
            final Resolver<HttpRoute, SocketConfig> socketConfigResolver) {
        this.socketConfigResolver = socketConfigResolver;
        return this;
    }

    public final PoolingHttpClientConnectionManagerBuilder setDefaultConnectionConfig(final ConnectionConfig config) {
        this.connectionConfigResolver = (route) -> config;
        return this;
    }

    public final PoolingHttpClientConnectionManagerBuilder setConnectionConfigResolver(
            final Resolver<HttpRoute, ConnectionConfig> connectionConfigResolver) {
        this.connectionConfigResolver = connectionConfigResolver;
        return this;
    }

    public final PoolingHttpClientConnectionManagerBuilder setDefaultTlsConfig(final TlsConfig config) {
        this.tlsConfigResolver = (host) -> config;
        return this;
    }

    public final PoolingHttpClientConnectionManagerBuilder setTlsConfigResolver(
            final Resolver<HttpHost, TlsConfig> tlsConfigResolver) {
        this.tlsConfigResolver = tlsConfigResolver;
        return this;
    }

    @Deprecated
    public final PoolingHttpClientConnectionManagerBuilder setConnectionTimeToLive(final TimeValue timeToLive) {
        setDefaultConnectionConfig(ConnectionConfig.custom()
                .setTimeToLive(timeToLive)
                .build());
        return this;
    }

    @Deprecated
    public final PoolingHttpClientConnectionManagerBuilder setValidateAfterInactivity(final TimeValue validateAfterInactivity) {
        setDefaultConnectionConfig(ConnectionConfig.custom()
                .setValidateAfterInactivity(validateAfterInactivity)
                .build());
        return this;
    }

    public final PoolingHttpClientConnectionManagerBuilder useSystemProperties() {
        this.systemProperties = true;
        return this;
    }

    public PoolingHttpClientConnectionManager build() {
        @SuppressWarnings("resource") final PoolingHttpClientConnectionManager poolingmgr = new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register(URIScheme.HTTP.id, PlainConnectionSocketFactory.getSocketFactory())
                        .register(URIScheme.HTTPS.id, sslSocketFactory != null ? sslSocketFactory :
                                (systemProperties ?
                                        SSLConnectionSocketFactory.getSystemSocketFactory() :
                                        SSLConnectionSocketFactory.getSocketFactory()))
                        .build(),
                poolConcurrencyPolicy,
                poolReusePolicy,
                null,
                schemePortResolver,
                dnsResolver,
                connectionFactory);
        poolingmgr.setSocketConfigResolver(socketConfigResolver);
        poolingmgr.setConnectionConfigResolver(connectionConfigResolver);
        poolingmgr.setTlsConfigResolver(tlsConfigResolver);
        if (maxConnTotal > 0)
            poolingmgr.setMaxTotal(maxConnTotal);

        if (maxConnPerRoute > 0)
            poolingmgr.setDefaultMaxPerRoute(maxConnPerRoute);
        return poolingmgr;
    }
}
