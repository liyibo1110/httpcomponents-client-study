package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.client5.http.auth.AuthCache;
import com.github.liyibo1110.hc.client5.http.auth.AuthExchange;
import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.auth.AuthStateCacheable;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 一个实现client端authentication cache管理通用功能的辅助类。
 * @author liyibo
 * @date 2026-04-17 22:20
 */
@Internal
@Contract(threading = ThreadingBehavior.STATELESS)
public class AuthCacheKeeper {
    private static final Logger LOG = LoggerFactory.getLogger(AuthCacheKeeper.class);

    private final SchemePortResolver schemePortResolver;

    public AuthCacheKeeper(final SchemePortResolver schemePortResolver) {
        this.schemePortResolver = schemePortResolver;
    }

    public void updateOnChallenge(final HttpHost host,
                                  final String pathPrefix,
                                  final AuthExchange authExchange,
                                  final HttpContext context) {
        clearCache(host, pathPrefix, HttpClientContext.adapt(context));
    }

    public void updateOnNoChallenge(final HttpHost host,
                                    final String pathPrefix,
                                    final AuthExchange authExchange,
                                    final HttpContext context) {
        if (authExchange.getState() == AuthExchange.State.SUCCESS)
            updateCache(host, pathPrefix, authExchange.getAuthScheme(), HttpClientContext.adapt(context));
    }

    public void updateOnResponse(final HttpHost host,
                                 final String pathPrefix,
                                 final AuthExchange authExchange,
                                 final HttpContext context) {
        if (authExchange.getState() == AuthExchange.State.FAILURE)
            clearCache(host, pathPrefix, HttpClientContext.adapt(context));
    }

    public void loadPreemptively(final HttpHost host,
                                 final String pathPrefix,
                                 final AuthExchange authExchange,
                                 final HttpContext context) {
        if (authExchange.getState() == AuthExchange.State.UNCHALLENGED) {
            AuthScheme authScheme = loadFromCache(host, pathPrefix, HttpClientContext.adapt(context));
            if (authScheme == null && pathPrefix != null)
                authScheme = loadFromCache(host, null, HttpClientContext.adapt(context));
            if (authScheme != null)
                authExchange.select(authScheme);
        }
    }

    private AuthScheme loadFromCache(final HttpHost host, final String pathPrefix, final HttpClientContext clientContext) {
        final AuthCache authCache = clientContext.getAuthCache();
        if (authCache != null) {
            final AuthScheme authScheme = authCache.get(host, pathPrefix);
            if (authScheme != null) {
                if (LOG.isDebugEnabled()) {
                    final String exchangeId = clientContext.getExchangeId();
                    LOG.debug("{} Re-using cached '{}' auth scheme for {}{}", exchangeId, authScheme.getName(), host, pathPrefix != null ? pathPrefix : "");
                }
                return authScheme;
            }
        }
        return null;
    }

    private void updateCache(final HttpHost host,
                             final String pathPrefix,
                             final AuthScheme authScheme,
                             final HttpClientContext clientContext) {
        final boolean cacheable = authScheme.getClass().getAnnotation(AuthStateCacheable.class) != null;
        if (cacheable) {
            AuthCache authCache = clientContext.getAuthCache();
            if (authCache == null) {
                authCache = new BasicAuthCache(schemePortResolver);
                clientContext.setAuthCache(authCache);
            }
            if (LOG.isDebugEnabled()) {
                final String exchangeId = clientContext.getExchangeId();
                LOG.debug("{} Caching '{}' auth scheme for {}{}", exchangeId, authScheme.getName(), host, pathPrefix != null ? pathPrefix : "");
            }
            authCache.put(host, pathPrefix, authScheme);
        }
    }

    private void clearCache(final HttpHost host, final String pathPrefix, final HttpClientContext clientContext) {
        final AuthCache authCache = clientContext.getAuthCache();
        if(authCache != null) {
            if (LOG.isDebugEnabled()) {
                final String exchangeId = clientContext.getExchangeId();
                LOG.debug("{} Clearing cached auth scheme for {}{}", exchangeId, host, pathPrefix != null ? pathPrefix : "");
            }
            authCache.remove(host, pathPrefix);
        }
    }
}
