package com.github.liyibo1110.hc.client5.http.config;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.util.TimeValue;
import com.github.liyibo1110.hc.core5.util.Timeout;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 封装请求配置项的不可变类。
 * @author liyibo
 * @date 2026-04-14 17:02
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class RequestConfig implements Cloneable {

    private static final Timeout DEFAULT_CONNECTION_REQUEST_TIMEOUT = Timeout.ofMinutes(3);
    private static final TimeValue DEFAULT_CONN_KEEP_ALIVE = TimeValue.ofMinutes(3);

    public static final RequestConfig DEFAULT = new Builder().build();

    private final boolean expectContinueEnabled;
    private final HttpHost proxy;
    private final String cookieSpec;
    private final boolean redirectsEnabled;
    private final boolean circularRedirectsAllowed;
    private final int maxRedirects;
    private final boolean authenticationEnabled;
    private final Collection<String> targetPreferredAuthSchemes;
    private final Collection<String> proxyPreferredAuthSchemes;
    private final Timeout connectionRequestTimeout;
    private final Timeout connectTimeout;
    private final Timeout responseTimeout;
    private final TimeValue connectionKeepAlive;
    private final boolean contentCompressionEnabled;
    private final boolean hardCancellationEnabled;

    protected RequestConfig() {
        this(false, null, null, false, false, 0, false, null, null,
                DEFAULT_CONNECTION_REQUEST_TIMEOUT, null, null, DEFAULT_CONN_KEEP_ALIVE, false, false);
    }

    RequestConfig(final boolean expectContinueEnabled,
                  final HttpHost proxy,
                  final String cookieSpec,
                  final boolean redirectsEnabled,
                  final boolean circularRedirectsAllowed,
                  final int maxRedirects,
                  final boolean authenticationEnabled,
                  final Collection<String> targetPreferredAuthSchemes,
                  final Collection<String> proxyPreferredAuthSchemes,
                  final Timeout connectionRequestTimeout,
                  final Timeout connectTimeout,
                  final Timeout responseTimeout,
                  final TimeValue connectionKeepAlive,
                  final boolean contentCompressionEnabled,
                  final boolean hardCancellationEnabled) {
        super();
        this.expectContinueEnabled = expectContinueEnabled;
        this.proxy = proxy;
        this.cookieSpec = cookieSpec;
        this.redirectsEnabled = redirectsEnabled;
        this.circularRedirectsAllowed = circularRedirectsAllowed;
        this.maxRedirects = maxRedirects;
        this.authenticationEnabled = authenticationEnabled;
        this.targetPreferredAuthSchemes = targetPreferredAuthSchemes;
        this.proxyPreferredAuthSchemes = proxyPreferredAuthSchemes;
        this.connectionRequestTimeout = connectionRequestTimeout;
        this.connectTimeout = connectTimeout;
        this.responseTimeout = responseTimeout;
        this.connectionKeepAlive = connectionKeepAlive;
        this.contentCompressionEnabled = contentCompressionEnabled;
        this.hardCancellationEnabled = hardCancellationEnabled;
    }

    public boolean isExpectContinueEnabled() {
        return expectContinueEnabled;
    }

    @Deprecated
    public HttpHost getProxy() {
        return proxy;
    }

    public String getCookieSpec() {
        return cookieSpec;
    }

    public boolean isRedirectsEnabled() {
        return redirectsEnabled;
    }

    public boolean isCircularRedirectsAllowed() {
        return circularRedirectsAllowed;
    }

    public int getMaxRedirects() {
        return maxRedirects;
    }

    public boolean isAuthenticationEnabled() {
        return authenticationEnabled;
    }

    public Collection<String> getTargetPreferredAuthSchemes() {
        return targetPreferredAuthSchemes;
    }

    public Collection<String> getProxyPreferredAuthSchemes() {
        return proxyPreferredAuthSchemes;
    }

    public Timeout getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    @Deprecated
    public Timeout getConnectTimeout() {
        return connectTimeout;
    }

    public Timeout getResponseTimeout() {
        return responseTimeout;
    }

    public TimeValue getConnectionKeepAlive() {
        return connectionKeepAlive;
    }

    public boolean isContentCompressionEnabled() {
        return contentCompressionEnabled;
    }

    public boolean isHardCancellationEnabled() {
        return hardCancellationEnabled;
    }

    @Override
    protected RequestConfig clone() throws CloneNotSupportedException {
        return (RequestConfig) super.clone();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append("expectContinueEnabled=").append(expectContinueEnabled);
        builder.append(", proxy=").append(proxy);
        builder.append(", cookieSpec=").append(cookieSpec);
        builder.append(", redirectsEnabled=").append(redirectsEnabled);
        builder.append(", maxRedirects=").append(maxRedirects);
        builder.append(", circularRedirectsAllowed=").append(circularRedirectsAllowed);
        builder.append(", authenticationEnabled=").append(authenticationEnabled);
        builder.append(", targetPreferredAuthSchemes=").append(targetPreferredAuthSchemes);
        builder.append(", proxyPreferredAuthSchemes=").append(proxyPreferredAuthSchemes);
        builder.append(", connectionRequestTimeout=").append(connectionRequestTimeout);
        builder.append(", connectTimeout=").append(connectTimeout);
        builder.append(", responseTimeout=").append(responseTimeout);
        builder.append(", connectionKeepAlive=").append(connectionKeepAlive);
        builder.append(", contentCompressionEnabled=").append(contentCompressionEnabled);
        builder.append(", hardCancellationEnabled=").append(hardCancellationEnabled);
        builder.append("]");
        return builder.toString();
    }

    public static RequestConfig.Builder custom() {
        return new Builder();
    }

    @SuppressWarnings("deprecation")
    public static RequestConfig.Builder copy(final RequestConfig config) {
        return new Builder()
                .setExpectContinueEnabled(config.isExpectContinueEnabled())
                .setProxy(config.getProxy())
                .setCookieSpec(config.getCookieSpec())
                .setRedirectsEnabled(config.isRedirectsEnabled())
                .setCircularRedirectsAllowed(config.isCircularRedirectsAllowed())
                .setMaxRedirects(config.getMaxRedirects())
                .setAuthenticationEnabled(config.isAuthenticationEnabled())
                .setTargetPreferredAuthSchemes(config.getTargetPreferredAuthSchemes())
                .setProxyPreferredAuthSchemes(config.getProxyPreferredAuthSchemes())
                .setConnectionRequestTimeout(config.getConnectionRequestTimeout())
                .setConnectTimeout(config.getConnectTimeout())
                .setResponseTimeout(config.getResponseTimeout())
                .setConnectionKeepAlive(config.getConnectionKeepAlive())
                .setContentCompressionEnabled(config.isContentCompressionEnabled())
                .setHardCancellationEnabled(config.isHardCancellationEnabled());
    }

    public static class Builder {
        private boolean expectContinueEnabled;
        private HttpHost proxy;
        private String cookieSpec;
        private boolean redirectsEnabled;
        private boolean circularRedirectsAllowed;
        private int maxRedirects;
        private boolean authenticationEnabled;
        private Collection<String> targetPreferredAuthSchemes;
        private Collection<String> proxyPreferredAuthSchemes;
        private Timeout connectionRequestTimeout;
        private Timeout connectTimeout;
        private Timeout responseTimeout;
        private TimeValue connectionKeepAlive;
        private boolean contentCompressionEnabled;
        private boolean hardCancellationEnabled;

        Builder() {
            super();
            this.redirectsEnabled = true;
            this.maxRedirects = 50;
            this.authenticationEnabled = true;
            this.connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;
            this.contentCompressionEnabled = true;
            this.hardCancellationEnabled = true;
        }

        public Builder setExpectContinueEnabled(final boolean expectContinueEnabled) {
            this.expectContinueEnabled = expectContinueEnabled;
            return this;
        }

        @Deprecated
        public Builder setProxy(final HttpHost proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder setCookieSpec(final String cookieSpec) {
            this.cookieSpec = cookieSpec;
            return this;
        }

        public Builder setRedirectsEnabled(final boolean redirectsEnabled) {
            this.redirectsEnabled = redirectsEnabled;
            return this;
        }

        public Builder setCircularRedirectsAllowed(final boolean circularRedirectsAllowed) {
            this.circularRedirectsAllowed = circularRedirectsAllowed;
            return this;
        }

        public Builder setMaxRedirects(final int maxRedirects) {
            this.maxRedirects = maxRedirects;
            return this;
        }

        public Builder setAuthenticationEnabled(final boolean authenticationEnabled) {
            this.authenticationEnabled = authenticationEnabled;
            return this;
        }

        public Builder setTargetPreferredAuthSchemes(final Collection<String> targetPreferredAuthSchemes) {
            this.targetPreferredAuthSchemes = targetPreferredAuthSchemes;
            return this;
        }

        public Builder setProxyPreferredAuthSchemes(final Collection<String> proxyPreferredAuthSchemes) {
            this.proxyPreferredAuthSchemes = proxyPreferredAuthSchemes;
            return this;
        }

        public Builder setConnectionRequestTimeout(final Timeout connectionRequestTimeout) {
            this.connectionRequestTimeout = connectionRequestTimeout;
            return this;
        }

        public Builder setConnectionRequestTimeout(final long connectionRequestTimeout, final TimeUnit timeUnit) {
            this.connectionRequestTimeout = Timeout.of(connectionRequestTimeout, timeUnit);
            return this;
        }

        @Deprecated
        public Builder setConnectTimeout(final Timeout connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        @Deprecated
        public Builder setConnectTimeout(final long connectTimeout, final TimeUnit timeUnit) {
            this.connectTimeout = Timeout.of(connectTimeout, timeUnit);
            return this;
        }

        public Builder setResponseTimeout(final Timeout responseTimeout) {
            this.responseTimeout = responseTimeout;
            return this;
        }

        public Builder setResponseTimeout(final long responseTimeout, final TimeUnit timeUnit) {
            this.responseTimeout = Timeout.of(responseTimeout, timeUnit);
            return this;
        }

        public Builder setConnectionKeepAlive(final TimeValue connectionKeepAlive) {
            this.connectionKeepAlive = connectionKeepAlive;
            return this;
        }

        public Builder setDefaultKeepAlive(final long defaultKeepAlive, final TimeUnit timeUnit) {
            this.connectionKeepAlive = TimeValue.of(defaultKeepAlive, timeUnit);
            return this;
        }

        public Builder setContentCompressionEnabled(final boolean contentCompressionEnabled) {
            this.contentCompressionEnabled = contentCompressionEnabled;
            return this;
        }

        public Builder setHardCancellationEnabled(final boolean hardCancellationEnabled) {
            this.hardCancellationEnabled = hardCancellationEnabled;
            return this;
        }

        public RequestConfig build() {
            return new RequestConfig(
                    expectContinueEnabled,
                    proxy,
                    cookieSpec,
                    redirectsEnabled,
                    circularRedirectsAllowed,
                    maxRedirects,
                    authenticationEnabled,
                    targetPreferredAuthSchemes,
                    proxyPreferredAuthSchemes,
                    connectionRequestTimeout != null ? connectionRequestTimeout : DEFAULT_CONNECTION_REQUEST_TIMEOUT,
                    connectTimeout,
                    responseTimeout,
                    connectionKeepAlive != null ? connectionKeepAlive : DEFAULT_CONN_KEEP_ALIVE,
                    contentCompressionEnabled,
                    hardCancellationEnabled);
        }
    }
}
