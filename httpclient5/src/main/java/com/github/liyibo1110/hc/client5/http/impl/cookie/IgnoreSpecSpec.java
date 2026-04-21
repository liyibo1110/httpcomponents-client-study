package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.Cookie;
import com.github.liyibo1110.hc.client5.http.cookie.CookieOrigin;
import com.github.liyibo1110.hc.client5.http.cookie.MalformedCookieException;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.Header;

import java.util.Collections;
import java.util.List;

/**
 * 忽略所有cookie的CookieSpec实现。
 * @author liyibo
 * @date 2026-04-20 10:19
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class IgnoreSpecSpec extends CookieSpecBase {

    public static final IgnoreSpecSpec INSTANCE = new IgnoreSpecSpec();

    @Override
    public List<Cookie> parse(final Header header, final CookieOrigin origin) throws MalformedCookieException {
        return Collections.emptyList();
    }

    @Override
    public boolean match(final Cookie cookie, final CookieOrigin origin) {
        return false;
    }

    @Override
    public List<Header> formatCookies(final List<Cookie> cookies) {
        return Collections.emptyList();
    }
}
