package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.CommonCookieAttributeHandler;
import com.github.liyibo1110.hc.client5.http.cookie.Cookie;
import com.github.liyibo1110.hc.client5.http.cookie.CookieOrigin;
import com.github.liyibo1110.hc.client5.http.cookie.MalformedCookieException;
import com.github.liyibo1110.hc.client5.http.cookie.SetCookie;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.Args;

/**
 * HttpOnly attribute handler。
 * @author liyibo
 * @date 2026-04-19 14:29
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class BasicHttpOnlyHandler implements CommonCookieAttributeHandler {

    public static final BasicHttpOnlyHandler INSTANCE = new BasicHttpOnlyHandler();

    public BasicHttpOnlyHandler() {
        super();
    }

    @Override
    public void parse(final SetCookie cookie, final String value) throws MalformedCookieException {
        Args.notNull(cookie, "Cookie");
        cookie.setHttpOnly(true);
    }

    @Override
    public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
        // nothing to do
    }

    @Override
    public boolean match(final Cookie cookie, final CookieOrigin origin) {
        return true;
    }

    @Override
    public String getAttributeName() {
        return Cookie.HTTP_ONLY_ATTR;
    }
}
