package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.CircularRedirectException;
import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.RedirectException;
import com.github.liyibo1110.hc.client5.http.auth.AuthExchange;
import com.github.liyibo1110.hc.client5.http.classic.ExecChain;
import com.github.liyibo1110.hc.client5.http.classic.ExecChainHandler;
import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.client5.http.protocol.RedirectLocations;
import com.github.liyibo1110.hc.client5.http.protocol.RedirectStrategy;
import com.github.liyibo1110.hc.client5.http.routing.HttpRoutePlanner;
import com.github.liyibo1110.hc.client5.http.utils.URIUtils;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.HttpStatus;
import com.github.liyibo1110.hc.core5.http.Method;
import com.github.liyibo1110.hc.core5.http.ProtocolException;
import com.github.liyibo1110.hc.core5.http.io.entity.EntityUtils;
import com.github.liyibo1110.hc.core5.http.io.support.ClassicRequestBuilder;
import com.github.liyibo1110.hc.core5.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

/**
 * @author liyibo
 * @date 2026-04-17 12:01
 */
@Contract(threading = ThreadingBehavior.STATELESS)
@Internal
public class RedirectExec implements ExecChainHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RedirectExec.class);

    private final RedirectStrategy redirectStrategy;
    private final HttpRoutePlanner routePlanner;

    public RedirectExec(final HttpRoutePlanner routePlanner, final RedirectStrategy redirectStrategy) {
        super();
        Args.notNull(routePlanner, "HTTP route planner");
        Args.notNull(redirectStrategy, "HTTP redirect strategy");
        this.routePlanner = routePlanner;
        this.redirectStrategy = redirectStrategy;
    }

    @Override
    public ClassicHttpResponse execute(final ClassicHttpRequest request, final ExecChain.Scope scope, final ExecChain chain)
            throws IOException, HttpException {
        Args.notNull(request, "HTTP request");
        Args.notNull(scope, "Scope");

        final HttpClientContext context = scope.clientContext;
        RedirectLocations redirectLocations = context.getRedirectLocations();
        if (redirectLocations == null) {
            redirectLocations = new RedirectLocations();
            context.setAttribute(HttpClientContext.REDIRECT_LOCATIONS, redirectLocations);
        }
        redirectLocations.clear();

        final RequestConfig config = context.getRequestConfig();
        final int maxRedirects = config.getMaxRedirects() > 0 ? config.getMaxRedirects() : 50;
        ClassicHttpRequest currentRequest = request;
        ExecChain.Scope currentScope = scope;
        for (int redirectCount = 0;;) {
            final String exchangeId = currentScope.exchangeId;
            final ClassicHttpResponse response = chain.proceed(currentRequest, currentScope);
            try {
                if (config.isRedirectsEnabled() && this.redirectStrategy.isRedirected(request, response, context)) {
                    final HttpEntity requestEntity = request.getEntity();
                    if (requestEntity != null && !requestEntity.isRepeatable()) {
                        if (LOG.isDebugEnabled())
                            LOG.debug("{} cannot redirect non-repeatable request", exchangeId);
                        return response;
                    }
                    if (redirectCount >= maxRedirects)
                        throw new RedirectException("Maximum redirects ("+ maxRedirects + ") exceeded");

                    redirectCount++;

                    final URI redirectUri = this.redirectStrategy.getLocationURI(currentRequest, response, context);
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} redirect requested to location '{}'", exchangeId, redirectUri);

                    final HttpHost newTarget = URIUtils.extractHost(redirectUri);
                    if (newTarget == null)
                        throw new ProtocolException("Redirect URI does not specify a valid host name: " + redirectUri);

                    if (!config.isCircularRedirectsAllowed()) {
                        if (redirectLocations.contains(redirectUri))
                            throw new CircularRedirectException("Circular redirect to '" + redirectUri + "'");
                    }
                    redirectLocations.add(redirectUri);

                    final int statusCode = response.getCode();
                    final ClassicRequestBuilder redirectBuilder;
                    switch (statusCode) {
                        case HttpStatus.SC_MOVED_PERMANENTLY:
                        case HttpStatus.SC_MOVED_TEMPORARILY:
                            if (Method.POST.isSame(request.getMethod()))
                                redirectBuilder = ClassicRequestBuilder.get();
                            else
                                redirectBuilder = ClassicRequestBuilder.copy(scope.originalRequest);

                            break;
                        case HttpStatus.SC_SEE_OTHER:
                            if (!Method.GET.isSame(request.getMethod()) && !Method.HEAD.isSame(request.getMethod()))
                                redirectBuilder = ClassicRequestBuilder.get();
                            else
                                redirectBuilder = ClassicRequestBuilder.copy(scope.originalRequest);

                            break;
                        default:
                            redirectBuilder = ClassicRequestBuilder.copy(scope.originalRequest);
                    }
                    redirectBuilder.setUri(redirectUri);

                    final HttpRoute currentRoute = currentScope.route;
                    if (!Objects.equals(currentRoute.getTargetHost(), newTarget)) {
                        final HttpRoute newRoute = this.routePlanner.determineRoute(newTarget, context);
                        if (!Objects.equals(currentRoute, newRoute)) {
                            if (LOG.isDebugEnabled())
                                LOG.debug("{} new route required", exchangeId);

                            final AuthExchange targetAuthExchange = context.getAuthExchange(currentRoute.getTargetHost());
                            if (LOG.isDebugEnabled())
                                LOG.debug("{} resetting target auth state", exchangeId);

                            targetAuthExchange.reset();
                            if (currentRoute.getProxyHost() != null) {
                                final AuthExchange proxyAuthExchange = context.getAuthExchange(currentRoute.getProxyHost());
                                if (proxyAuthExchange.isConnectionBased()) {
                                    if (LOG.isDebugEnabled())
                                        LOG.debug("{} resetting proxy auth state", exchangeId);
                                    proxyAuthExchange.reset();
                                }
                            }
                            currentScope = new ExecChain.Scope(
                                    currentScope.exchangeId,
                                    newRoute,
                                    currentScope.originalRequest,
                                    currentScope.execRuntime,
                                    currentScope.clientContext);
                        }
                    }

                    if (LOG.isDebugEnabled())
                        LOG.debug("{} redirecting to '{}' via {}", exchangeId, redirectUri, currentRoute);

                    currentRequest = redirectBuilder.build();
                    RequestEntityProxy.enhance(currentRequest);

                    EntityUtils.consume(response.getEntity());
                    response.close();
                } else {
                    return response;
                }
            } catch (final RuntimeException | IOException ex) {
                response.close();
                throw ex;
            } catch (final HttpException ex) {
                // Protocol exception related to a direct.
                // The underlying connection may still be salvaged.
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (final IOException ioex) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} I/O error while releasing connection", exchangeId, ioex);
                } finally {
                    response.close();
                }
                throw ex;
            }
        }
    }
}
