package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.Cookie;
import com.github.liyibo1110.hc.client5.http.cookie.CookieAttributeHandler;
import com.github.liyibo1110.hc.client5.http.cookie.CookieOrigin;
import com.github.liyibo1110.hc.client5.http.cookie.MalformedCookieException;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;

/**
 * 只提供了validate和match方法的默认实现。
 * @author liyibo
 * @date 2026-04-19 11:08
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public abstract class AbstractCookieAttributeHandler implements CookieAttributeHandler {

    @Override
    public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
        // nothing to do
    }

    @Override
    public boolean match(final Cookie cookie, final CookieOrigin origin) {
        // Always match
        return true;
    }
}
