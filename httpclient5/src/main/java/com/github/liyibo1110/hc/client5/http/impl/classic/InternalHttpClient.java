package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.ClientProtocolException;
import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.auth.AuthSchemeFactory;
import com.github.liyibo1110.hc.client5.http.auth.CredentialsProvider;
import com.github.liyibo1110.hc.client5.http.classic.ExecChain;
import com.github.liyibo1110.hc.client5.http.classic.ExecRuntime;
import com.github.liyibo1110.hc.client5.http.config.Configurable;
import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.client5.http.cookie.CookieSpecFactory;
import com.github.liyibo1110.hc.client5.http.cookie.CookieStore;
import com.github.liyibo1110.hc.client5.http.impl.ExecSupport;
import com.github.liyibo1110.hc.client5.http.io.HttpClientConnectionManager;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.client5.http.routing.HttpRoutePlanner;
import com.github.liyibo1110.hc.client5.http.routing.RoutingSupport;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.concurrent.CancellableDependency;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.config.Lookup;
import com.github.liyibo1110.hc.core5.http.impl.io.HttpRequestExecutor;
import com.github.liyibo1110.hc.core5.http.io.support.ClassicRequestBuilder;
import com.github.liyibo1110.hc.core5.http.protocol.BasicHttpContext;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.io.CloseMode;
import com.github.liyibo1110.hc.core5.io.ModalCloseable;
import com.github.liyibo1110.hc.core5.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * classic客户端的总装配实现。
 * @author liyibo
 * @date 2026-04-16 22:04
 */
@Contract(threading = ThreadingBehavior.SAFE_CONDITIONAL)
@Internal
class InternalHttpClient extends CloseableHttpClient implements Configurable {
    private static final Logger LOG = LoggerFactory.getLogger(InternalHttpClient.class);

    private final HttpClientConnectionManager connManager;
    private final HttpRequestExecutor requestExecutor;
    private final ExecChainElement execChain;
    private final HttpRoutePlanner routePlanner;
    private final Lookup<CookieSpecFactory> cookieSpecRegistry;
    private final Lookup<AuthSchemeFactory> authSchemeRegistry;
    private final CookieStore cookieStore;
    private final CredentialsProvider credentialsProvider;
    private final RequestConfig defaultConfig;
    private final ConcurrentLinkedQueue<Closeable> closeables;

    public InternalHttpClient(final HttpClientConnectionManager connManager,
                              final HttpRequestExecutor requestExecutor,
                              final ExecChainElement execChain,
                              final HttpRoutePlanner routePlanner,
                              final Lookup<CookieSpecFactory> cookieSpecRegistry,
                              final Lookup<AuthSchemeFactory> authSchemeRegistry,
                              final CookieStore cookieStore,
                              final CredentialsProvider credentialsProvider,
                              final RequestConfig defaultConfig,
                              final List<Closeable> closeables) {
        super();
        this.connManager = Args.notNull(connManager, "Connection manager");
        this.requestExecutor = Args.notNull(requestExecutor, "Request executor");
        this.execChain = Args.notNull(execChain, "Execution chain");
        this.routePlanner = Args.notNull(routePlanner, "Route planner");
        this.cookieSpecRegistry = cookieSpecRegistry;
        this.authSchemeRegistry = authSchemeRegistry;
        this.cookieStore = cookieStore;
        this.credentialsProvider = credentialsProvider;
        this.defaultConfig = defaultConfig;
        this.closeables = closeables != null ?  new ConcurrentLinkedQueue<>(closeables) : null;
    }

    private HttpRoute determineRoute(final HttpHost target, final HttpContext context) throws HttpException {
        return this.routePlanner.determineRoute(target, context);
    }

    private void setupContext(final HttpClientContext context) {
        if (context.getAttribute(HttpClientContext.AUTHSCHEME_REGISTRY) == null)
            context.setAttribute(HttpClientContext.AUTHSCHEME_REGISTRY, this.authSchemeRegistry);

        if (context.getAttribute(HttpClientContext.COOKIESPEC_REGISTRY) == null)
            context.setAttribute(HttpClientContext.COOKIESPEC_REGISTRY, this.cookieSpecRegistry);

        if (context.getAttribute(HttpClientContext.COOKIE_STORE) == null)
            context.setAttribute(HttpClientContext.COOKIE_STORE, this.cookieStore);

        if (context.getAttribute(HttpClientContext.CREDS_PROVIDER) == null)
            context.setAttribute(HttpClientContext.CREDS_PROVIDER, this.credentialsProvider);

        if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null)
            context.setAttribute(HttpClientContext.REQUEST_CONFIG, this.defaultConfig);
    }

    @Override
    protected CloseableHttpResponse doExecute(final HttpHost target, final ClassicHttpRequest request, final HttpContext context)
            throws IOException {
        Args.notNull(request, "HTTP request");
        try {
            final HttpClientContext localcontext = HttpClientContext.adapt(
                    context != null ? context : new BasicHttpContext());
            RequestConfig config = null;
            if (request instanceof Configurable)
                config = ((Configurable) request).getConfig();
            if (config != null)
                localcontext.setRequestConfig(config);
            setupContext(localcontext);

            final HttpRoute route = determineRoute(
                    target != null ? target : RoutingSupport.determineHost(request),
                    localcontext);
            final String exchangeId = ExecSupport.getNextExchangeId();
            localcontext.setExchangeId(exchangeId);
            if (LOG.isDebugEnabled())
                LOG.debug("{} preparing request execution", exchangeId);

            final ExecRuntime execRuntime = new InternalExecRuntime(LOG, connManager, requestExecutor,
                    request instanceof CancellableDependency ? (CancellableDependency) request : null);
            final ExecChain.Scope scope = new ExecChain.Scope(exchangeId, route, request, execRuntime, localcontext);
            // 终于开始发出请求了，注意这里是阻塞的
            final ClassicHttpResponse response = this.execChain.execute(ClassicRequestBuilder.copy(request).build(), scope);
            return CloseableHttpResponse.adapt(response);
        } catch (final HttpException httpException) {
            throw new ClientProtocolException(httpException.getMessage(), httpException);
        }
    }

    @Override
    public RequestConfig getConfig() {
        return this.defaultConfig;
    }

    @Override
    public void close() {
        close(CloseMode.GRACEFUL);
    }

    @Override
    public void close(final CloseMode closeMode) {
        if (this.closeables != null) {
            Closeable closeable;
            while ((closeable = this.closeables.poll()) != null) {
                try {
                    if (closeable instanceof ModalCloseable)
                        ((ModalCloseable) closeable).close(closeMode);
                    else
                        closeable.close();
                } catch (final IOException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
        }
    }
}
