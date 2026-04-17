package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.UserTokenHandler;
import com.github.liyibo1110.hc.client5.http.auth.AuthExchange;
import com.github.liyibo1110.hc.client5.http.auth.AuthScheme;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

import javax.net.ssl.SSLSession;
import java.security.Principal;

/**
 * UserTokenHandler的默认实现。
 * 如果能从给定的执行上下文中获取到Principal实例，该类将使用该实例作为HTTP连接的状态对象。
 * 这有助于确保在特定安全上下文中使用特定用户身份建立的持久连接仅能被同一用户重用。
 *
 * DefaultUserTokenHandler将使用基于连接的身份验证方案（如NTLM）的用户主体，或启用了客户端身份验证的SSL会话的用户主体。
 * 如果两者均不可用，则返回null令牌。
 * @author liyibo
 * @date 2026-04-16 16:41
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class DefaultUserTokenHandler implements UserTokenHandler {
    public static final DefaultUserTokenHandler INSTANCE = new DefaultUserTokenHandler();

    @Override
    public Object getUserToken(final HttpRoute route, final HttpContext context) {
        return getUserToken(route, null, context);
    }

    @Override
    public Object getUserToken(final HttpRoute route, final HttpRequest request, final HttpContext context) {
        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final HttpHost target = request != null ? new HttpHost(request.getScheme(), request.getAuthority()) : route.getTargetHost();
        final AuthExchange targetAuthExchange = clientContext.getAuthExchange(target);
        if (targetAuthExchange != null) {
            final Principal authPrincipal = getAuthPrincipal(targetAuthExchange);
            if (authPrincipal != null)
                return authPrincipal;
        }
        final HttpHost proxy = route.getProxyHost();
        if (proxy != null) {
            final AuthExchange proxyAuthExchange = clientContext.getAuthExchange(proxy);
            if (proxyAuthExchange != null) {
                final Principal authPrincipal = getAuthPrincipal(proxyAuthExchange);
                if (authPrincipal != null)
                    return authPrincipal;
            }
        }
        final SSLSession sslSession = clientContext.getSSLSession();
        if (sslSession != null)
            return sslSession.getLocalPrincipal();
        return null;
    }

    private static Principal getAuthPrincipal(final AuthExchange authExchange) {
        final AuthScheme scheme = authExchange.getAuthScheme();
        if (scheme != null && scheme.isConnectionBased())
            return scheme.getPrincipal();
        return null;
    }
}
