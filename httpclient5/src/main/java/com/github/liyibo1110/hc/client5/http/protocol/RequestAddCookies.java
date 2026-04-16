package com.github.liyibo1110.hc.client5.http.protocol;

import com.github.liyibo1110.hc.client5.http.RouteInfo;
import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.client5.http.cookie.Cookie;
import com.github.liyibo1110.hc.client5.http.cookie.CookieOrigin;
import com.github.liyibo1110.hc.client5.http.cookie.CookieSpec;
import com.github.liyibo1110.hc.client5.http.cookie.CookieSpecFactory;
import com.github.liyibo1110.hc.client5.http.cookie.CookieStore;
import com.github.liyibo1110.hc.client5.http.cookie.StandardCookieSpec;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.EntityDetails;
import com.github.liyibo1110.hc.core5.http.Header;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.HttpRequestInterceptor;
import com.github.liyibo1110.hc.core5.http.Method;
import com.github.liyibo1110.hc.core5.http.config.Lookup;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.net.URIAuthority;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 请求拦截器，用于将当前CookieStore中可用的Cookie与正在执行的请求进行匹配，并生成相应的Cookie请求头。
 * @author liyibo
 * @date 2026-04-15 16:46
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class RequestAddCookies implements HttpRequestInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(RequestAddCookies.class);
    public static final RequestAddCookies INSTANCE = new RequestAddCookies();

    public RequestAddCookies() {
        super();
    }

    @Override
    public void process(final HttpRequest request, final EntityDetails entity, final HttpContext context) throws HttpException, IOException {
        Args.notNull(request, "HTTP request");
        Args.notNull(context, "HTTP context");

        final String method = request.getMethod();
        // 一些method不需要附带cookie
        if (Method.CONNECT.isSame(method) || Method.TRACE.isSame(method))
            return;

        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final String exchangeId = clientContext.getExchangeId();

        // 检查CookieStore是否存在，没有直接退出
        final CookieStore cookieStore = clientContext.getCookieStore();
        if (cookieStore == null) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} Cookie store not specified in HTTP context", exchangeId);
            return;
        }

        // 检查CookieSpecFactory是否存在，没有直接退出
        final Lookup<CookieSpecFactory> registry = clientContext.getCookieSpecRegistry();
        if (registry == null) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} CookieSpec registry not specified in HTTP context", exchangeId);
            return;
        }

        // 检查RouteInfo是否存在，没有直接退出
        final RouteInfo route = clientContext.getHttpRoute();
        if (route == null) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} Connection route not set in the context", exchangeId);
            return;
        }

        final RequestConfig config = clientContext.getRequestConfig();
        String cookieSpecName = config.getCookieSpec();
        // 没有配置特定的cookieSpecName，自动用strict版本
        if (cookieSpecName == null)
            cookieSpecName = StandardCookieSpec.STRICT;
        if (LOG.isDebugEnabled())
            LOG.debug("{} Cookie spec selected: {}", exchangeId, cookieSpecName);

        final URIAuthority authority = request.getAuthority();
        // 获取请求path
        String path = request.getPath();
        if (TextUtils.isEmpty(path))
            path = "/";

        // 获取请求hostname
        String hostName = authority != null ? authority.getHostName() : null;
        if (hostName == null)
            hostName = route.getTargetHost().getHostName();

        // 获取请求的服务port
        int port = authority != null ? authority.getPort() : -1;
        if (port < 0)
            port = route.getTargetHost().getPort();

        // 获取CookieOrigin
        final CookieOrigin cookieOrigin = new CookieOrigin(hostName, port, path, route.isSecure());

        // 获取CookieSpecFactory
        final CookieSpecFactory factory = registry.lookup(cookieSpecName);
        if (factory == null) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} Unsupported cookie spec: {}", exchangeId, cookieSpecName);
            return;
        }

        // 获取CookieSpec
        final CookieSpec cookieSpec = factory.create(clientContext);

        // 从CookieStore里获取所有的cookie
        final List<Cookie> cookies = cookieStore.getCookies();

        // 找出和特定CookieOrigin能匹配的cookie
        final List<Cookie> matchedCookies = new ArrayList<>();
        final Instant now = Instant.now();
        boolean expired = false;
        for (final Cookie cookie : cookies) {
            if (!cookie.isExpired(now)) {
                if (cookieSpec.match(cookie, cookieOrigin)) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} Cookie {} match {}", exchangeId, cookie, cookieOrigin);
                    matchedCookies.add(cookie);
                }
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("{} Cookie {} expired", exchangeId, cookie);
                expired = true;
            }
        }

        // 如果cookieStore里面有超时的cookie，顺便触发清理
        if (expired)
            cookieStore.clearExpired(now);

        // 调用红cookieSpec的formatCookies方法，将Cookie转换成Header，并附加在request上面
        if (!matchedCookies.isEmpty()) {
            final List<Header> headers = cookieSpec.formatCookies(matchedCookies);
            for (final Header header : headers)
                request.addHeader(header);
        }

        context.setAttribute(HttpClientContext.COOKIE_SPEC, cookieSpec);
        context.setAttribute(HttpClientContext.COOKIE_ORIGIN, cookieOrigin);
    }
}
