package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.CommonCookieAttributeHandler;
import com.github.liyibo1110.hc.client5.http.cookie.Cookie;
import com.github.liyibo1110.hc.client5.http.cookie.CookieAttributeHandler;
import com.github.liyibo1110.hc.client5.http.cookie.CookieOrigin;
import com.github.liyibo1110.hc.client5.http.cookie.MalformedCookieException;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HeaderElement;
import com.github.liyibo1110.hc.core5.http.NameValuePair;
import com.github.liyibo1110.hc.core5.util.Args;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * 所有spec共有的Cookie管理功能。
 * @author liyibo
 * @date 2026-04-19 11:22
 */
@Contract(threading = ThreadingBehavior.SAFE)
public abstract class CookieSpecBase extends AbstractCookieSpec {

    public CookieSpecBase() {
        super();
    }

    protected CookieSpecBase(final HashMap<String, CookieAttributeHandler> map) {
        super(map);
    }

    protected CookieSpecBase(final CommonCookieAttributeHandler... handlers) {
        super(handlers);
    }

    protected static String getDefaultPath(final CookieOrigin origin) {
        String defaultPath = origin.getPath();
        int lastSlashIndex = defaultPath.lastIndexOf('/');
        if (lastSlashIndex >= 0) {
            if (lastSlashIndex == 0) {
                //Do not remove the very first slash
                lastSlashIndex = 1;
            }
            defaultPath = defaultPath.substring(0, lastSlashIndex);
        }
        return defaultPath;
    }

    protected static String getDefaultDomain(final CookieOrigin origin) {
        return origin.getHost();
    }

    protected List<Cookie> parse(final HeaderElement[] elems, final CookieOrigin origin) throws MalformedCookieException {
        final List<Cookie> cookies = new ArrayList<>(elems.length);
        for (final HeaderElement headerelement : elems) {
            final String name = headerelement.getName();
            final String value = headerelement.getValue();
            if (name == null || name.isEmpty())
                throw new MalformedCookieException("Cookie name may not be empty");

            final BasicClientCookie cookie = new BasicClientCookie(name, value);
            cookie.setPath(getDefaultPath(origin));
            cookie.setDomain(getDefaultDomain(origin));

            // 循环处理参数
            final NameValuePair[] attribs = headerelement.getParameters();
            for (int j = attribs.length - 1; j >= 0; j--) {
                final NameValuePair attrib = attribs[j];
                final String s = attrib.getName().toLowerCase(Locale.ROOT);

                cookie.setAttribute(s, attrib.getValue());

                final CookieAttributeHandler handler = findAttribHandler(s);
                if (handler != null)
                    handler.parse(cookie, attrib.getValue());
            }
            cookies.add(cookie);
        }
        return cookies;
    }

    @Override
    public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
        Args.notNull(cookie, "Cookie");
        Args.notNull(origin, "Cookie origin");
        for (final CookieAttributeHandler handler: getAttribHandlers())
            handler.validate(cookie, origin);
    }

    @Override
    public boolean match(final Cookie cookie, final CookieOrigin origin) {
        Args.notNull(cookie, "Cookie");
        Args.notNull(origin, "Cookie origin");
        for (final CookieAttributeHandler handler: getAttribHandlers()) {
            if (!handler.match(cookie, origin))
                return false;
        }
        return true;
    }
}
