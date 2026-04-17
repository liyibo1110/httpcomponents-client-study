package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.client5.http.auth.AuthCache;
import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.auth.AuthSchemeFactory;
import com.github.liyibo1110.hc.client5.http.auth.CredentialsProvider;
import com.github.liyibo1110.hc.client5.http.auth.UsernamePasswordCredentials;
import com.github.liyibo1110.hc.client5.http.cookie.CookieSpecFactory;
import com.github.liyibo1110.hc.client5.http.cookie.CookieStore;
import com.github.liyibo1110.hc.client5.http.impl.DefaultSchemePortResolver;
import com.github.liyibo1110.hc.client5.http.impl.auth.BasicScheme;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.client5.http.routing.RoutingSupport;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.config.Lookup;
import com.github.liyibo1110.hc.core5.http.protocol.BasicHttpContext;
import com.github.liyibo1110.hc.core5.util.Args;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 生成HttpClientContext对象的工厂。
 * @author liyibo
 * @date 2026-04-16 10:59
 */
public class ContextBuilder {
    private final SchemePortResolver schemePortResolver;
    private Lookup<CookieSpecFactory> cookieSpecRegistry;
    private Lookup<AuthSchemeFactory> authSchemeRegistry;
    private CookieStore cookieStore;
    private CredentialsProvider credentialsProvider;
    private AuthCache authCache;
    private Map<HttpHost, AuthScheme> authSchemeMap;

    ContextBuilder(final SchemePortResolver schemePortResolver) {
        this.schemePortResolver = schemePortResolver != null ? schemePortResolver : DefaultSchemePortResolver.INSTANCE;
    }

    public static ContextBuilder create(final SchemePortResolver schemePortResolver) {
        return new ContextBuilder(schemePortResolver);
    }

    public static ContextBuilder create() {
        return new ContextBuilder(DefaultSchemePortResolver.INSTANCE);
    }

    public ContextBuilder useCookieSpecRegistry(final Lookup<CookieSpecFactory> cookieSpecRegistry) {
        this.cookieSpecRegistry = cookieSpecRegistry;
        return this;
    }

    public ContextBuilder useAuthSchemeRegistry(final Lookup<AuthSchemeFactory> authSchemeRegistry) {
        this.authSchemeRegistry = authSchemeRegistry;
        return this;
    }

    public ContextBuilder useCookieStore(final CookieStore cookieStore) {
        this.cookieStore = cookieStore;
        return this;
    }

    public ContextBuilder useCredentialsProvider(final CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        return this;
    }

    public ContextBuilder useAuthCache(final AuthCache authCache) {
        this.authCache = authCache;
        return this;
    }

    public ContextBuilder preemptiveAuth(final HttpHost host, final AuthScheme authScheme) {
        Args.notNull(host, "HTTP host");
        if (authSchemeMap == null) {
            authSchemeMap = new HashMap<>();
        }
        authSchemeMap.put(RoutingSupport.normalize(host, schemePortResolver), authScheme);
        return this;
    }

    public ContextBuilder preemptiveBasicAuth(final HttpHost host, final UsernamePasswordCredentials credentials) {
        Args.notNull(host, "HTTP host");
        final BasicScheme authScheme = new BasicScheme(StandardCharsets.UTF_8);
        authScheme.initPreemptive(credentials);
        preemptiveAuth(host, authScheme);
        return this;
    }

    public HttpClientContext build() {
        final HttpClientContext context = new HttpClientContext(new BasicHttpContext());
        context.setCookieSpecRegistry(cookieSpecRegistry);
        context.setAuthSchemeRegistry(authSchemeRegistry);
        context.setCookieStore(cookieStore);
        context.setCredentialsProvider(credentialsProvider);
        context.setAuthCache(authCache);
        if (authSchemeMap != null) {
            for (final Map.Entry<HttpHost, AuthScheme> entry : authSchemeMap.entrySet())
                context.resetAuthExchange(entry.getKey(), entry.getValue());
        }
        return context;
    }
}
