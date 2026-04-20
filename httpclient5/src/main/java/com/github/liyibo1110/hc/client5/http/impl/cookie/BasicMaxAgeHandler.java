package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.CommonCookieAttributeHandler;
import com.github.liyibo1110.hc.client5.http.cookie.Cookie;
import com.github.liyibo1110.hc.client5.http.cookie.MalformedCookieException;
import com.github.liyibo1110.hc.client5.http.cookie.SetCookie;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.Args;

import java.time.Instant;

/**
 * max-age attribute handler。
 * @author liyibo
 * @date 2026-04-19 15:11
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class BasicMaxAgeHandler extends AbstractCookieAttributeHandler implements CommonCookieAttributeHandler {

    public static final BasicMaxAgeHandler INSTANCE = new BasicMaxAgeHandler();

    public BasicMaxAgeHandler() {
        super();
    }

    @Override
    public void parse(final SetCookie cookie, final String value) throws MalformedCookieException {
        Args.notNull(cookie, "Cookie");
        if (value == null)
            throw new MalformedCookieException("Missing value for 'max-age' attribute");

        final int age;
        try {
            age = Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            throw new MalformedCookieException ("Invalid 'max-age' attribute: " + value);
        }
        if (age < 0)
            throw new MalformedCookieException ("Negative 'max-age' attribute: " + value);

        cookie.setExpiryDate(Instant.now().plusSeconds(age));
    }

    @Override
    public String getAttributeName() {
        return Cookie.MAX_AGE_ATTR;
    }
}
