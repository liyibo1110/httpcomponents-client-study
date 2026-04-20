package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.CommonCookieAttributeHandler;
import com.github.liyibo1110.hc.client5.http.cookie.Cookie;
import com.github.liyibo1110.hc.client5.http.cookie.CookieOrigin;
import com.github.liyibo1110.hc.client5.http.cookie.MalformedCookieException;
import com.github.liyibo1110.hc.client5.http.cookie.SetCookie;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.TextUtils;

/**
 * path attribute handler。
 * @author liyibo
 * @date 2026-04-19 13:31
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class BasicPathHandler implements CommonCookieAttributeHandler {

    public static final BasicPathHandler INSTANCE = new BasicPathHandler();

    public BasicPathHandler() {
        super();
    }

    @Override
    public void parse(final SetCookie cookie, final String value) throws MalformedCookieException {
        Args.notNull(cookie, "Cookie");
        cookie.setPath(!TextUtils.isBlank(value) ? value : "/");
    }

    @Override
    public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
        // nothing to do
    }

    static boolean pathMatch(final String uriPath, final String cookiePath) {
        String normalizedCookiePath = cookiePath;
        if (normalizedCookiePath == null)
            normalizedCookiePath = "/";

        if (normalizedCookiePath.length() > 1 && normalizedCookiePath.endsWith("/"))
            normalizedCookiePath = normalizedCookiePath.substring(0, normalizedCookiePath.length() - 1);

        if (uriPath.startsWith(normalizedCookiePath)) {
            if (normalizedCookiePath.equals("/"))
                return true;
            if (uriPath.length() == normalizedCookiePath.length())
                return true;
            return uriPath.charAt(normalizedCookiePath.length()) == '/';
        }
        return false;
    }

    @Override
    public boolean match(final Cookie cookie, final CookieOrigin origin) {
        Args.notNull(cookie, "Cookie");
        Args.notNull(origin, "Cookie origin");
        return pathMatch(origin.getPath(), cookie.getPath());
    }

    @Override
    public String getAttributeName() {
        return Cookie.PATH_ATTR;
    }
}
