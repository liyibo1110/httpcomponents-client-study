package com.github.liyibo1110.hc.client5.http.protocol;

import com.github.liyibo1110.hc.client5.http.cookie.Cookie;
import com.github.liyibo1110.hc.client5.http.cookie.CookieOrigin;
import com.github.liyibo1110.hc.client5.http.cookie.CookieSpec;
import com.github.liyibo1110.hc.client5.http.cookie.CookieStore;
import com.github.liyibo1110.hc.client5.http.cookie.MalformedCookieException;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.EntityDetails;
import com.github.liyibo1110.hc.core5.http.Header;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHeaders;
import com.github.liyibo1110.hc.core5.http.HttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpResponseInterceptor;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * 响应拦截器，用于将给定HTTP响应中接收到的响应Cookie中的数据填充到当前的CookieStore中。
 * @author liyibo
 * @date 2026-04-15 17:13
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class ResponseProcessCookies implements HttpResponseInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseProcessCookies.class);

    public static final ResponseProcessCookies INSTANCE = new ResponseProcessCookies();

    public ResponseProcessCookies() {
        super();
    }

    @Override
    public void process(final HttpResponse response, final EntityDetails entity, final HttpContext context) throws HttpException, IOException {
        Args.notNull(response, "HTTP request");
        Args.notNull(context, "HTTP context");

        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final String exchangeId = clientContext.getExchangeId();

        // 获取CookieSpec
        final CookieSpec cookieSpec = clientContext.getCookieSpec();
        if (cookieSpec == null) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} Cookie spec not specified in HTTP context", exchangeId);
            return;
        }

        // 获取CookieStore
        final CookieStore cookieStore = clientContext.getCookieStore();
        if (cookieStore == null) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} Cookie store not specified in HTTP context", exchangeId);
            return;
        }

        // 获取CookieOrigin
        final CookieOrigin cookieOrigin = clientContext.getCookieOrigin();
        if (cookieOrigin == null) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} Cookie origin not specified in HTTP context", exchangeId);
            return;
        }

        // 获取response里面的Set-Cookie头
        final Iterator<Header> it = response.headerIterator(HttpHeaders.SET_COOKIE);
        processCookies(exchangeId, it, cookieSpec, cookieOrigin, cookieStore);
    }

    private void processCookies(final String exchangeId, final Iterator<Header> iterator, final CookieSpec cookieSpec,
            final CookieOrigin cookieOrigin, final CookieStore cookieStore) {
        while (iterator.hasNext()) {
            final Header header = iterator.next();
            try {
                // header -> Cookies
                final List<Cookie> cookies = cookieSpec.parse(header, cookieOrigin);
                for (final Cookie cookie : cookies) {
                    try {
                        cookieSpec.validate(cookie, cookieOrigin);
                        cookieStore.addCookie(cookie);
                        if (LOG.isDebugEnabled())
                            LOG.debug("{} Cookie accepted [{}]", exchangeId, formatCookie(cookie));
                    } catch (final MalformedCookieException ex) {
                        if (LOG.isWarnEnabled())
                            LOG.warn("{} Cookie rejected [{}] {}", exchangeId, formatCookie(cookie), ex.getMessage());
                    }
                }
            } catch (final MalformedCookieException ex) {
                if (LOG.isWarnEnabled())
                    LOG.warn("{} Invalid cookie header: \"{}\". {}", exchangeId, header, ex.getMessage());
            }
        }
    }

    private static String formatCookie(final Cookie cookie) {
        final StringBuilder buf = new StringBuilder();
        buf.append(cookie.getName());
        buf.append("=\"");
        String v = cookie.getValue();
        if (v != null) {
            if (v.length() > 100)
                v = v.substring(0, 100) + "...";
            buf.append(v);
        }
        buf.append("\"");
        buf.append(", domain:");
        buf.append(cookie.getDomain());
        buf.append(", path:");
        buf.append(cookie.getPath());
        buf.append(", expiry:");
        buf.append(cookie.getExpiryInstant());
        return buf.toString();
    }
}
