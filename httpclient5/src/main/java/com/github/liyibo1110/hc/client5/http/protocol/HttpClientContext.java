package com.github.liyibo1110.hc.client5.http.protocol;

import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.RouteInfo;
import com.github.liyibo1110.hc.client5.http.auth.AuthCache;
import com.github.liyibo1110.hc.client5.http.auth.AuthExchange;
import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.auth.AuthSchemeFactory;
import com.github.liyibo1110.hc.client5.http.auth.CredentialsProvider;
import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.client5.http.cookie.CookieOrigin;
import com.github.liyibo1110.hc.client5.http.cookie.CookieSpec;
import com.github.liyibo1110.hc.client5.http.cookie.CookieSpecFactory;
import com.github.liyibo1110.hc.client5.http.cookie.CookieStore;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.config.Lookup;
import com.github.liyibo1110.hc.core5.http.protocol.BasicHttpContext;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.http.protocol.HttpCoreContext;
import com.github.liyibo1110.hc.core5.util.Args;

import java.util.HashMap;
import java.util.Map;

/**
 * 一个适配器类，为HTTP请求执行过程中常用的HttpContext属性提供方便且类型安全的setter和getter方法。
 * @author liyibo
 * @date 2026-04-15 16:06
 */
public class HttpClientContext extends HttpCoreContext {

    /** RouteInfo对象中表示实际连接路径的属性名称 */
    public static final String HTTP_ROUTE = "http.route";

    /** RedirectLocations对象的属性名称，该对象表示在请求执行过程中接收到的所有重定向位置的集合 */
    public static final String REDIRECT_LOCATIONS = "http.protocol.redirect-locations";

    /** 表示实际CookieSpecFactory注册表的Lookup对象的属性名称 */
    public static final String COOKIESPEC_REGISTRY = "http.cookiespec-registry";

    /** CookieSpec对象的属性名称，该对象表示实际的Cookie规范。 */
    public static final String COOKIE_SPEC = "http.cookie-spec";

    /** CookieOrigin对象的属性名称，该对象表示源服务器的实际详细信息 */
    public static final String COOKIE_ORIGIN = "http.cookie-origin";

    /** CookieStore对象的属性名称，该对象表示实际的Cookie存储 */
    public static final String COOKIE_STORE = "http.cookie-store";

    /** 代表实际凭据提供程序的CredentialsProvider对象的属性名称。 */
    public static final String CREDS_PROVIDER = "http.auth.credentials-provider";

    /** 表示身份验证方案缓存的AuthCache对象的属性名称 */
    public static final String AUTH_CACHE = "http.auth.auth-cache";

    /** 包含实际AuthExchanges的映射的属性名称，这些AuthExchanges按各自的HttpHost作为键进行索引 */
    public static final String AUTH_EXCHANGE_MAP = "http.auth.exchanges";

    /** 表示实际用户身份的Object对象的属性名称，例如user java.security.Principal*/
    public static final String USER_TOKEN = "http.user-token";

    /** 表示实际AuthSchemeFactory注册表的Lookup对象的属性名称 */
    public static final String AUTHSCHEME_REGISTRY = "http.authscheme-registry";

    /** 表示实际请求配置的RequestConfig对象的属性名称 */
    public static final String REQUEST_CONFIG = "http.request-config";

    /** 一个String对象的属性名称，该对象表示当前消息交换的ID */
    public static final String EXCHANGE_ID = "http.exchange-id";

    public static HttpClientContext adapt(final HttpContext context) {
        Args.notNull(context, "HTTP context");
        if (context instanceof HttpClientContext)
            return (HttpClientContext) context;
        return new HttpClientContext(context);
    }

    public static HttpClientContext create() {
        return new HttpClientContext(new BasicHttpContext());
    }

    public HttpClientContext(final HttpContext context) {
        super(context);
    }

    public HttpClientContext() {
        super();
    }

    public RouteInfo getHttpRoute() {
        return getAttribute(HTTP_ROUTE, HttpRoute.class);
    }

    public RedirectLocations getRedirectLocations() {
        return getAttribute(REDIRECT_LOCATIONS, RedirectLocations.class);
    }

    public CookieStore getCookieStore() {
        return getAttribute(COOKIE_STORE, CookieStore.class);
    }

    public void setCookieStore(final CookieStore cookieStore) {
        setAttribute(COOKIE_STORE, cookieStore);
    }

    public CookieSpec getCookieSpec() {
        return getAttribute(COOKIE_SPEC, CookieSpec.class);
    }

    public CookieOrigin getCookieOrigin() {
        return getAttribute(COOKIE_ORIGIN, CookieOrigin.class);
    }

    @SuppressWarnings("unchecked")
    private <T> Lookup<T> getLookup(final String name) {
        return (Lookup<T>) getAttribute(name, Lookup.class);
    }

    public Lookup<CookieSpecFactory> getCookieSpecRegistry() {
        return getLookup(COOKIESPEC_REGISTRY);
    }

    public void setCookieSpecRegistry(final Lookup<CookieSpecFactory> lookup) {
        setAttribute(COOKIESPEC_REGISTRY, lookup);
    }

    public Lookup<AuthSchemeFactory> getAuthSchemeRegistry() {
        return getLookup(AUTHSCHEME_REGISTRY);
    }

    public void setAuthSchemeRegistry(final Lookup<AuthSchemeFactory> lookup) {
        setAttribute(AUTHSCHEME_REGISTRY, lookup);
    }

    public CredentialsProvider getCredentialsProvider() {
        return getAttribute(CREDS_PROVIDER, CredentialsProvider.class);
    }

    public void setCredentialsProvider(final CredentialsProvider credentialsProvider) {
        setAttribute(CREDS_PROVIDER, credentialsProvider);
    }

    public AuthCache getAuthCache() {
        return getAttribute(AUTH_CACHE, AuthCache.class);
    }

    public void setAuthCache(final AuthCache authCache) {
        setAttribute(AUTH_CACHE, authCache);
    }

    @SuppressWarnings("unchecked")
    public Map<HttpHost, AuthExchange> getAuthExchanges() {
        Map<HttpHost, AuthExchange> map = (Map<HttpHost, AuthExchange>) getAttribute(AUTH_EXCHANGE_MAP);
        if (map == null) {
            map = new HashMap<>();
            setAttribute(AUTH_EXCHANGE_MAP, map);
        }
        return map;
    }

    public AuthExchange getAuthExchange(final HttpHost host) {
        final Map<HttpHost, AuthExchange> authExchangeMap = getAuthExchanges();
        AuthExchange authExchange = authExchangeMap.get(host);
        if (authExchange == null) {
            authExchange = new AuthExchange();
            authExchangeMap.put(host, authExchange);
        }
        return authExchange;
    }

    public void setAuthExchange(final HttpHost host, final AuthExchange authExchange) {
        final Map<HttpHost, AuthExchange> authExchangeMap = getAuthExchanges();
        authExchangeMap.put(host, authExchange);
    }

    public void resetAuthExchange(final HttpHost host, final AuthScheme authScheme) {
        final AuthExchange authExchange = new AuthExchange();
        authExchange.select(authScheme);
        final Map<HttpHost, AuthExchange> authExchangeMap = getAuthExchanges();
        authExchangeMap.put(host, authExchange);
    }

    public <T> T getUserToken(final Class<T> clazz) {
        return getAttribute(USER_TOKEN, clazz);
    }

    public Object getUserToken() {
        return getAttribute(USER_TOKEN);
    }

    public void setUserToken(final Object obj) {
        setAttribute(USER_TOKEN, obj);
    }

    public RequestConfig getRequestConfig() {
        final RequestConfig config = getAttribute(REQUEST_CONFIG, RequestConfig.class);
        return config != null ? config : RequestConfig.DEFAULT;
    }

    public void setRequestConfig(final RequestConfig config) {
        setAttribute(REQUEST_CONFIG, config);
    }

    public String getExchangeId() {
        return getAttribute(EXCHANGE_ID, String.class);
    }

    public void setExchangeId(final String id) {
        setAttribute(EXCHANGE_ID, id);
    }
}
