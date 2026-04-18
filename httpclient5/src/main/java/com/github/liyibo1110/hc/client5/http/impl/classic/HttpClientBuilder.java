package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.AuthenticationStrategy;
import com.github.liyibo1110.hc.client5.http.ConnectionKeepAliveStrategy;
import com.github.liyibo1110.hc.client5.http.HttpRequestRetryStrategy;
import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.client5.http.UserTokenHandler;
import com.github.liyibo1110.hc.client5.http.auth.AuthSchemeFactory;
import com.github.liyibo1110.hc.client5.http.auth.CredentialsProvider;
import com.github.liyibo1110.hc.client5.http.auth.StandardAuthScheme;
import com.github.liyibo1110.hc.client5.http.classic.BackoffManager;
import com.github.liyibo1110.hc.client5.http.classic.ConnectionBackoffStrategy;
import com.github.liyibo1110.hc.client5.http.classic.ExecChainHandler;
import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.client5.http.cookie.BasicCookieStore;
import com.github.liyibo1110.hc.client5.http.cookie.CookieSpecFactory;
import com.github.liyibo1110.hc.client5.http.cookie.CookieStore;
import com.github.liyibo1110.hc.client5.http.entity.InputStreamFactory;
import com.github.liyibo1110.hc.client5.http.impl.ChainElement;
import com.github.liyibo1110.hc.client5.http.impl.DefaultAuthenticationStrategy;
import com.github.liyibo1110.hc.client5.http.impl.DefaultClientConnectionReuseStrategy;
import com.github.liyibo1110.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import com.github.liyibo1110.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import com.github.liyibo1110.hc.client5.http.impl.DefaultRedirectStrategy;
import com.github.liyibo1110.hc.client5.http.impl.DefaultSchemePortResolver;
import com.github.liyibo1110.hc.client5.http.impl.DefaultUserTokenHandler;
import com.github.liyibo1110.hc.client5.http.impl.IdleConnectionEvictor;
import com.github.liyibo1110.hc.client5.http.impl.NoopUserTokenHandler;
import com.github.liyibo1110.hc.client5.http.io.HttpClientConnectionManager;
import com.github.liyibo1110.hc.client5.http.protocol.RedirectStrategy;
import com.github.liyibo1110.hc.client5.http.protocol.RequestAddCookies;
import com.github.liyibo1110.hc.client5.http.protocol.RequestClientConnControl;
import com.github.liyibo1110.hc.client5.http.protocol.RequestDefaultHeaders;
import com.github.liyibo1110.hc.client5.http.protocol.RequestExpectContinue;
import com.github.liyibo1110.hc.client5.http.protocol.ResponseProcessCookies;
import com.github.liyibo1110.hc.client5.http.routing.HttpRoutePlanner;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.http.ConnectionReuseStrategy;
import com.github.liyibo1110.hc.core5.http.Header;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.HttpRequestInterceptor;
import com.github.liyibo1110.hc.core5.http.HttpResponseInterceptor;
import com.github.liyibo1110.hc.core5.http.config.Lookup;
import com.github.liyibo1110.hc.core5.http.config.NamedElementChain;
import com.github.liyibo1110.hc.core5.http.config.Registry;
import com.github.liyibo1110.hc.core5.http.config.RegistryBuilder;
import com.github.liyibo1110.hc.core5.http.impl.io.HttpRequestExecutor;
import com.github.liyibo1110.hc.core5.http.protocol.DefaultHttpProcessor;
import com.github.liyibo1110.hc.core5.http.protocol.HttpProcessor;
import com.github.liyibo1110.hc.core5.http.protocol.HttpProcessorBuilder;
import com.github.liyibo1110.hc.core5.http.protocol.RequestContent;
import com.github.liyibo1110.hc.core5.http.protocol.RequestTargetHost;
import com.github.liyibo1110.hc.core5.http.protocol.RequestUserAgent;
import com.github.liyibo1110.hc.core5.pool.ConnPoolControl;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.TimeValue;
import com.github.liyibo1110.hc.core5.util.Timeout;
import com.github.liyibo1110.hc.core5.util.VersionInfo;

import java.io.Closeable;
import java.net.ProxySelector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 用于创建CloseableHttpClient对象的builder。
 * 当未显式设置特定组件时，该类将使用其默认实现。
 * 如果在调用build()之前先调用了useSystemProperties()方法，则在配置默认实现时会考虑系统属性。
 * @author liyibo
 * @date 2026-04-17 13:21
 */
public class HttpClientBuilder {

    private static class RequestInterceptorEntry {
        enum Position { FIRST, LAST }

        final RequestInterceptorEntry.Position position;
        final HttpRequestInterceptor interceptor;

        private RequestInterceptorEntry(final RequestInterceptorEntry.Position position, final HttpRequestInterceptor interceptor) {
            this.position = position;
            this.interceptor = interceptor;
        }
    }

    private static class ResponseInterceptorEntry {
        enum Position { FIRST, LAST }

        final ResponseInterceptorEntry.Position position;
        final HttpResponseInterceptor interceptor;

        private ResponseInterceptorEntry(final ResponseInterceptorEntry.Position position, final HttpResponseInterceptor interceptor) {
            this.position = position;
            this.interceptor = interceptor;
        }
    }

    private static class ExecInterceptorEntry {
        enum Position { BEFORE, AFTER, REPLACE, FIRST, LAST }

        final ExecInterceptorEntry.Position position;
        final String name;
        final ExecChainHandler interceptor;
        final String existing;

        private ExecInterceptorEntry(final ExecInterceptorEntry.Position position,
                                     final String name,
                                     final ExecChainHandler interceptor,
                                     final String existing) {
            this.position = position;
            this.name = name;
            this.interceptor = interceptor;
            this.existing = existing;
        }
    }

    private HttpRequestExecutor requestExec;
    private HttpClientConnectionManager connManager;
    private boolean connManagerShared;
    private SchemePortResolver schemePortResolver;
    private ConnectionReuseStrategy reuseStrategy;
    private ConnectionKeepAliveStrategy keepAliveStrategy;
    private AuthenticationStrategy targetAuthStrategy;
    private AuthenticationStrategy proxyAuthStrategy;
    private UserTokenHandler userTokenHandler;

    private LinkedList<RequestInterceptorEntry> requestInterceptors;
    private LinkedList<ResponseInterceptorEntry> responseInterceptors;
    private LinkedList<ExecInterceptorEntry> execInterceptors;

    private HttpRequestRetryStrategy retryStrategy;
    private HttpRoutePlanner routePlanner;
    private RedirectStrategy redirectStrategy;
    private ConnectionBackoffStrategy connectionBackoffStrategy;
    private BackoffManager backoffManager;
    private Lookup<AuthSchemeFactory> authSchemeRegistry;
    private Lookup<CookieSpecFactory> cookieSpecRegistry;
    private LinkedHashMap<String, InputStreamFactory> contentDecoderMap;
    private CookieStore cookieStore;
    private CredentialsProvider credentialsProvider;
    private String userAgent;
    private HttpHost proxy;
    private Collection<? extends Header> defaultHeaders;
    private RequestConfig defaultRequestConfig;
    private boolean evictExpiredConnections;
    private boolean evictIdleConnections;
    private TimeValue maxIdleTime;

    private boolean systemProperties;
    private boolean redirectHandlingDisabled;
    private boolean automaticRetriesDisabled;
    private boolean contentCompressionDisabled;
    private boolean cookieManagementDisabled;
    private boolean authCachingDisabled;
    private boolean connectionStateDisabled;
    private boolean defaultUserAgentDisabled;

    private List<Closeable> closeables;

    public static HttpClientBuilder create() {
        return new HttpClientBuilder();
    }

    protected HttpClientBuilder() {
        super();
    }

    public final HttpClientBuilder setRequestExecutor(final HttpRequestExecutor requestExec) {
        this.requestExec = requestExec;
        return this;
    }

    public final HttpClientBuilder setConnectionManager(final HttpClientConnectionManager connManager) {
        this.connManager = connManager;
        return this;
    }

    public final HttpClientBuilder setConnectionManagerShared(final boolean shared) {
        this.connManagerShared = shared;
        return this;
    }

    public final HttpClientBuilder setConnectionReuseStrategy(final ConnectionReuseStrategy reuseStrategy) {
        this.reuseStrategy = reuseStrategy;
        return this;
    }

    public final HttpClientBuilder setKeepAliveStrategy(final ConnectionKeepAliveStrategy keepAliveStrategy) {
        this.keepAliveStrategy = keepAliveStrategy;
        return this;
    }

    public final HttpClientBuilder setTargetAuthenticationStrategy(final AuthenticationStrategy targetAuthStrategy) {
        this.targetAuthStrategy = targetAuthStrategy;
        return this;
    }

    public final HttpClientBuilder setProxyAuthenticationStrategy(final AuthenticationStrategy proxyAuthStrategy) {
        this.proxyAuthStrategy = proxyAuthStrategy;
        return this;
    }

    public final HttpClientBuilder setUserTokenHandler(final UserTokenHandler userTokenHandler) {
        this.userTokenHandler = userTokenHandler;
        return this;
    }

    public final HttpClientBuilder disableConnectionState() {
        connectionStateDisabled = true;
        return this;
    }

    public final HttpClientBuilder setSchemePortResolver(final SchemePortResolver schemePortResolver) {
        this.schemePortResolver = schemePortResolver;
        return this;
    }

    public final HttpClientBuilder setUserAgent(final String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public final HttpClientBuilder setDefaultHeaders(final Collection<? extends Header> defaultHeaders) {
        this.defaultHeaders = defaultHeaders;
        return this;
    }

    public final HttpClientBuilder addResponseInterceptorFirst(final HttpResponseInterceptor interceptor) {
        Args.notNull(interceptor, "Interceptor");
        if (responseInterceptors == null)
            responseInterceptors = new LinkedList<>();
        responseInterceptors.add(new ResponseInterceptorEntry(ResponseInterceptorEntry.Position.FIRST, interceptor));
        return this;
    }

    public final HttpClientBuilder addResponseInterceptorLast(final HttpResponseInterceptor interceptor) {
        Args.notNull(interceptor, "Interceptor");
        if (responseInterceptors == null)
            responseInterceptors = new LinkedList<>();
        responseInterceptors.add(new ResponseInterceptorEntry(ResponseInterceptorEntry.Position.LAST, interceptor));
        return this;
    }

    public final HttpClientBuilder addRequestInterceptorFirst(final HttpRequestInterceptor interceptor) {
        Args.notNull(interceptor, "Interceptor");
        if (requestInterceptors == null)
            requestInterceptors = new LinkedList<>();
        requestInterceptors.add(new RequestInterceptorEntry(RequestInterceptorEntry.Position.FIRST, interceptor));
        return this;
    }

    public final HttpClientBuilder addRequestInterceptorLast(final HttpRequestInterceptor interceptor) {
        Args.notNull(interceptor, "Interceptor");
        if (requestInterceptors == null)
            requestInterceptors = new LinkedList<>();
        requestInterceptors.add(new RequestInterceptorEntry(RequestInterceptorEntry.Position.LAST, interceptor));
        return this;
    }

    public final HttpClientBuilder addExecInterceptorBefore(final String existing, final String name, final ExecChainHandler interceptor) {
        Args.notBlank(existing, "Existing");
        Args.notBlank(name, "Name");
        Args.notNull(interceptor, "Interceptor");
        if (execInterceptors == null)
            execInterceptors = new LinkedList<>();
        execInterceptors.add(new ExecInterceptorEntry(ExecInterceptorEntry.Position.BEFORE, name, interceptor, existing));
        return this;
    }

    public final HttpClientBuilder addExecInterceptorAfter(final String existing, final String name, final ExecChainHandler interceptor) {
        Args.notBlank(existing, "Existing");
        Args.notBlank(name, "Name");
        Args.notNull(interceptor, "Interceptor");
        if (execInterceptors == null)
            execInterceptors = new LinkedList<>();
        execInterceptors.add(new ExecInterceptorEntry(ExecInterceptorEntry.Position.AFTER, name, interceptor, existing));
        return this;
    }

    public final HttpClientBuilder replaceExecInterceptor(final String existing, final ExecChainHandler interceptor) {
        Args.notBlank(existing, "Existing");
        Args.notNull(interceptor, "Interceptor");
        if (execInterceptors == null)
            execInterceptors = new LinkedList<>();
        execInterceptors.add(new ExecInterceptorEntry(ExecInterceptorEntry.Position.REPLACE, existing, interceptor, existing));
        return this;
    }

    public final HttpClientBuilder addExecInterceptorFirst(final String name, final ExecChainHandler interceptor) {
        Args.notNull(name, "Name");
        Args.notNull(interceptor, "Interceptor");
        if (execInterceptors == null)
            execInterceptors = new LinkedList<>();
        execInterceptors.add(new ExecInterceptorEntry(ExecInterceptorEntry.Position.FIRST, name, interceptor, null));
        return this;
    }

    public final HttpClientBuilder addExecInterceptorLast(final String name, final ExecChainHandler interceptor) {
        Args.notNull(name, "Name");
        Args.notNull(interceptor, "Interceptor");
        if (execInterceptors == null)
            execInterceptors = new LinkedList<>();
        execInterceptors.add(new ExecInterceptorEntry(ExecInterceptorEntry.Position.LAST, name, interceptor, null));
        return this;
    }

    public final HttpClientBuilder disableCookieManagement() {
        this.cookieManagementDisabled = true;
        return this;
    }

    public final HttpClientBuilder disableContentCompression() {
        contentCompressionDisabled = true;
        return this;
    }

    public final HttpClientBuilder disableAuthCaching() {
        this.authCachingDisabled = true;
        return this;
    }

    public final HttpClientBuilder setRetryStrategy(final HttpRequestRetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
        return this;
    }

    public final HttpClientBuilder disableAutomaticRetries() {
        automaticRetriesDisabled = true;
        return this;
    }

    public final HttpClientBuilder setProxy(final HttpHost proxy) {
        this.proxy = proxy;
        return this;
    }

    public final HttpClientBuilder setRoutePlanner(final HttpRoutePlanner routePlanner) {
        this.routePlanner = routePlanner;
        return this;
    }

    public final HttpClientBuilder setRedirectStrategy(final RedirectStrategy redirectStrategy) {
        this.redirectStrategy = redirectStrategy;
        return this;
    }

    public final HttpClientBuilder disableRedirectHandling() {
        redirectHandlingDisabled = true;
        return this;
    }

    public final HttpClientBuilder setConnectionBackoffStrategy(final ConnectionBackoffStrategy connectionBackoffStrategy) {
        this.connectionBackoffStrategy = connectionBackoffStrategy;
        return this;
    }

    public final HttpClientBuilder setBackoffManager(final BackoffManager backoffManager) {
        this.backoffManager = backoffManager;
        return this;
    }

    public final HttpClientBuilder setDefaultCookieStore(final CookieStore cookieStore) {
        this.cookieStore = cookieStore;
        return this;
    }

    public final HttpClientBuilder setDefaultCredentialsProvider(final CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        return this;
    }

    public final HttpClientBuilder setDefaultAuthSchemeRegistry(final Lookup<AuthSchemeFactory> authSchemeRegistry) {
        this.authSchemeRegistry = authSchemeRegistry;
        return this;
    }

    public final HttpClientBuilder setDefaultCookieSpecRegistry(final Lookup<CookieSpecFactory> cookieSpecRegistry) {
        this.cookieSpecRegistry = cookieSpecRegistry;
        return this;
    }

    public final HttpClientBuilder setContentDecoderRegistry(final LinkedHashMap<String, InputStreamFactory> contentDecoderMap) {
        this.contentDecoderMap = contentDecoderMap;
        return this;
    }

    public final HttpClientBuilder setDefaultRequestConfig(final RequestConfig config) {
        this.defaultRequestConfig = config;
        return this;
    }

    public final HttpClientBuilder useSystemProperties() {
        this.systemProperties = true;
        return this;
    }

    public final HttpClientBuilder evictExpiredConnections() {
        evictExpiredConnections = true;
        return this;
    }

    public final HttpClientBuilder evictIdleConnections(final TimeValue maxIdleTime) {
        this.evictIdleConnections = true;
        this.maxIdleTime = maxIdleTime;
        return this;
    }

    public final HttpClientBuilder disableDefaultUserAgent() {
        this.defaultUserAgentDisabled = true;
        return this;
    }

    /**
     * 内部扩展用。
     */
    @Internal
    protected void customizeExecChain(final NamedElementChain<ExecChainHandler> execChainDefinition) {}

    @Internal
    protected void addCloseable(final Closeable closeable) {
        if (closeable == null)
            return;
        if (closeables == null)
            closeables = new ArrayList<>();
        closeables.add(closeable);
    }

    public CloseableHttpClient build() {
        // Create main request executor
        // We copy the instance fields to avoid changing them, and rename to avoid accidental use of the wrong version
        HttpRequestExecutor requestExecCopy = this.requestExec;
        if (requestExecCopy == null)
            requestExecCopy = new HttpRequestExecutor();

        HttpClientConnectionManager connManagerCopy = this.connManager;
        if (connManagerCopy == null)
            connManagerCopy = PoolingHttpClientConnectionManagerBuilder.create().build();

        ConnectionReuseStrategy reuseStrategyCopy = this.reuseStrategy;
        if (reuseStrategyCopy == null) {
            if (systemProperties) {
                final String s = System.getProperty("http.keepAlive", "true");
                if ("true".equalsIgnoreCase(s))
                    reuseStrategyCopy = DefaultClientConnectionReuseStrategy.INSTANCE;
                else
                    reuseStrategyCopy = (request, response, context) -> false;
            } else {
                reuseStrategyCopy = DefaultClientConnectionReuseStrategy.INSTANCE;
            }
        }

        ConnectionKeepAliveStrategy keepAliveStrategyCopy = this.keepAliveStrategy;
        if (keepAliveStrategyCopy == null)
            keepAliveStrategyCopy = DefaultConnectionKeepAliveStrategy.INSTANCE;

        AuthenticationStrategy targetAuthStrategyCopy = this.targetAuthStrategy;
        if (targetAuthStrategyCopy == null)
            targetAuthStrategyCopy = DefaultAuthenticationStrategy.INSTANCE;

        AuthenticationStrategy proxyAuthStrategyCopy = this.proxyAuthStrategy;
        if (proxyAuthStrategyCopy == null)
            proxyAuthStrategyCopy = DefaultAuthenticationStrategy.INSTANCE;

        UserTokenHandler userTokenHandlerCopy = this.userTokenHandler;
        if (userTokenHandlerCopy == null) {
            if (!connectionStateDisabled)
                userTokenHandlerCopy = DefaultUserTokenHandler.INSTANCE;
            else
                userTokenHandlerCopy = NoopUserTokenHandler.INSTANCE;
        }

        String userAgentCopy = this.userAgent;
        if (userAgentCopy == null) {
            if (systemProperties)
                userAgentCopy = System.getProperty("http.agent");

            if (userAgentCopy == null && !defaultUserAgentDisabled)
                userAgentCopy = VersionInfo.getSoftwareInfo("Apache-HttpClient", "org.apache.hc.client5", getClass());
        }

        final HttpProcessorBuilder b = HttpProcessorBuilder.create();
        if (requestInterceptors != null) {
            for (final RequestInterceptorEntry entry: requestInterceptors) {
                if (entry.position == RequestInterceptorEntry.Position.FIRST)
                    b.addFirst(entry.interceptor);
            }
        }
        if (responseInterceptors != null) {
            for (final ResponseInterceptorEntry entry: responseInterceptors) {
                if (entry.position == ResponseInterceptorEntry.Position.FIRST)
                    b.addFirst(entry.interceptor);
            }
        }
        b.addAll(new RequestDefaultHeaders(defaultHeaders),
                 new RequestContent(),
                 new RequestTargetHost(),
                 new RequestClientConnControl(),
                 new RequestUserAgent(userAgentCopy),
                 new RequestExpectContinue());
        if (!cookieManagementDisabled)
            b.add(RequestAddCookies.INSTANCE);

        if (!cookieManagementDisabled)
            b.add(ResponseProcessCookies.INSTANCE);

        if (requestInterceptors != null) {
            for (final RequestInterceptorEntry entry: requestInterceptors) {
                if (entry.position == RequestInterceptorEntry.Position.LAST)
                    b.addLast(entry.interceptor);
            }
        }
        if (responseInterceptors != null) {
            for (final ResponseInterceptorEntry entry: responseInterceptors) {
                if (entry.position == ResponseInterceptorEntry.Position.LAST)
                    b.addLast(entry.interceptor);
            }
        }
        final HttpProcessor httpProcessor = b.build();

        final NamedElementChain<ExecChainHandler> execChainDefinition = new NamedElementChain<>();
        execChainDefinition.addLast(
                new MainClientExec(connManagerCopy, httpProcessor, reuseStrategyCopy, keepAliveStrategyCopy, userTokenHandlerCopy),
                ChainElement.MAIN_TRANSPORT.name());
        execChainDefinition.addFirst(
                new ConnectExec(
                        reuseStrategyCopy,
                        new DefaultHttpProcessor(new RequestTargetHost(), new RequestUserAgent(userAgentCopy)),
                        proxyAuthStrategyCopy,
                        schemePortResolver != null ? schemePortResolver : DefaultSchemePortResolver.INSTANCE,
                        authCachingDisabled),
                ChainElement.CONNECT.name());

        execChainDefinition.addFirst(
                new ProtocolExec(
                        targetAuthStrategyCopy,
                        proxyAuthStrategyCopy,
                        schemePortResolver != null ? schemePortResolver : DefaultSchemePortResolver.INSTANCE,
                        authCachingDisabled),
                ChainElement.PROTOCOL.name());

        // Add request retry executor, if not disabled
        if (!automaticRetriesDisabled) {
            HttpRequestRetryStrategy retryStrategyCopy = this.retryStrategy;
            if (retryStrategyCopy == null)
                retryStrategyCopy = DefaultHttpRequestRetryStrategy.INSTANCE;

            execChainDefinition.addFirst(new HttpRequestRetryExec(retryStrategyCopy), ChainElement.RETRY.name());
        }

        HttpRoutePlanner routePlannerCopy = this.routePlanner;
        if (routePlannerCopy == null) {
            SchemePortResolver schemePortResolverCopy = this.schemePortResolver;
            if (schemePortResolverCopy == null)
                schemePortResolverCopy = DefaultSchemePortResolver.INSTANCE;

            if (proxy != null)
                routePlannerCopy = new DefaultProxyRoutePlanner(proxy, schemePortResolverCopy);
            else if (systemProperties)
                routePlannerCopy = new SystemDefaultRoutePlanner(schemePortResolverCopy, ProxySelector.getDefault());
            else
                routePlannerCopy = new DefaultRoutePlanner(schemePortResolverCopy);
        }

        if (!contentCompressionDisabled) {
            if (contentDecoderMap != null) {
                final List<String> encodings = new ArrayList<>(contentDecoderMap.keySet());
                final RegistryBuilder<InputStreamFactory> b2 = RegistryBuilder.create();
                for (final Map.Entry<String, InputStreamFactory> entry: contentDecoderMap.entrySet())
                    b2.register(entry.getKey(), entry.getValue());

                final Registry<InputStreamFactory> decoderRegistry = b2.build();
                execChainDefinition.addFirst(new ContentCompressionExec(encodings, decoderRegistry, true),
                        ChainElement.COMPRESS.name());
            } else {
                execChainDefinition.addFirst(new ContentCompressionExec(true), ChainElement.COMPRESS.name());
            }
        }

        // Add redirect executor, if not disabled
        if (!redirectHandlingDisabled) {
            RedirectStrategy redirectStrategyCopy = this.redirectStrategy;
            if (redirectStrategyCopy == null)
                redirectStrategyCopy = DefaultRedirectStrategy.INSTANCE;

            execChainDefinition.addFirst(new RedirectExec(routePlannerCopy, redirectStrategyCopy), ChainElement.REDIRECT.name());
        }

        // Optionally, add connection back-off executor
        if (this.backoffManager != null && this.connectionBackoffStrategy != null) {
            execChainDefinition.addFirst(new BackoffStrategyExec(this.connectionBackoffStrategy, this.backoffManager),
                    ChainElement.BACK_OFF.name());
        }

        if (execInterceptors != null) {
            for (final ExecInterceptorEntry entry: execInterceptors) {
                switch (entry.position) {
                    case AFTER:
                        execChainDefinition.addAfter(entry.existing, entry.interceptor, entry.name);
                        break;
                    case BEFORE:
                        execChainDefinition.addBefore(entry.existing, entry.interceptor, entry.name);
                        break;
                    case REPLACE:
                        execChainDefinition.replace(entry.existing, entry.interceptor);
                        break;
                    case FIRST:
                        execChainDefinition.addFirst(entry.interceptor, entry.name);
                        break;
                    case LAST:
                        // Don't add last, after MainClientExec, as that does not delegate to the chain
                        // Instead, add the interceptor just before it, making it effectively the last interceptor
                        execChainDefinition.addBefore(ChainElement.MAIN_TRANSPORT.name(), entry.interceptor, entry.name);
                        break;
                }
            }
        }

        customizeExecChain(execChainDefinition);

        NamedElementChain<ExecChainHandler>.Node current = execChainDefinition.getLast();
        ExecChainElement execChain = null;
        while (current != null) {
            execChain = new ExecChainElement(current.getValue(), execChain);
            current = current.getPrevious();
        }

        Lookup<AuthSchemeFactory> authSchemeRegistryCopy = this.authSchemeRegistry;
        if (authSchemeRegistryCopy == null) {
            authSchemeRegistryCopy = RegistryBuilder.<AuthSchemeFactory>create()
                    .register(StandardAuthScheme.BASIC, BasicSchemeFactory.INSTANCE)
                    .register(StandardAuthScheme.DIGEST, DigestSchemeFactory.INSTANCE)
                    .register(StandardAuthScheme.NTLM, NTLMSchemeFactory.INSTANCE)
                    .register(StandardAuthScheme.SPNEGO, SPNegoSchemeFactory.DEFAULT)
                    .register(StandardAuthScheme.KERBEROS, KerberosSchemeFactory.DEFAULT)
                    .build();
        }
        Lookup<CookieSpecFactory> cookieSpecRegistryCopy = this.cookieSpecRegistry;
        if (cookieSpecRegistryCopy == null)
            cookieSpecRegistryCopy = CookieSpecSupport.createDefault();

        CookieStore defaultCookieStore = this.cookieStore;
        if (defaultCookieStore == null)
            defaultCookieStore = new BasicCookieStore();

        CredentialsProvider defaultCredentialsProvider = this.credentialsProvider;
        if (defaultCredentialsProvider == null) {
            if (systemProperties)
                defaultCredentialsProvider = new SystemDefaultCredentialsProvider();
            else
                defaultCredentialsProvider = new BasicCredentialsProvider();
        }

        List<Closeable> closeablesCopy = closeables != null ? new ArrayList<>(closeables) : null;
        if (!this.connManagerShared) {
            if (closeablesCopy == null)
                closeablesCopy = new ArrayList<>(1);

            if (evictExpiredConnections || evictIdleConnections) {
                if (connManagerCopy instanceof ConnPoolControl) {
                    final IdleConnectionEvictor connectionEvictor = new IdleConnectionEvictor((ConnPoolControl<?>) connManagerCopy,
                            maxIdleTime, maxIdleTime);
                    closeablesCopy.add(() -> {
                        connectionEvictor.shutdown();
                        try {
                            connectionEvictor.awaitTermination(Timeout.ofSeconds(1));
                        } catch (final InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                        }
                    });
                    connectionEvictor.start();
                }
            }
            closeablesCopy.add(connManagerCopy);
        }

        return new InternalHttpClient(
                connManagerCopy,
                requestExecCopy,
                execChain,
                routePlannerCopy,
                cookieSpecRegistryCopy,
                authSchemeRegistryCopy,
                defaultCookieStore,
                defaultCredentialsProvider,
                defaultRequestConfig != null ? defaultRequestConfig : RequestConfig.DEFAULT,
                closeablesCopy);
    }
}
