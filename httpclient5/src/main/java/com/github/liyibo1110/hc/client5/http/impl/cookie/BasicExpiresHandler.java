package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.CommonCookieAttributeHandler;
import com.github.liyibo1110.hc.client5.http.cookie.Cookie;
import com.github.liyibo1110.hc.client5.http.cookie.MalformedCookieException;
import com.github.liyibo1110.hc.client5.http.cookie.SetCookie;
import com.github.liyibo1110.hc.client5.http.utils.DateUtils;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.Args;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * expires attribute handler。
 * @author liyibo
 * @date 2026-04-19 15:12
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class BasicExpiresHandler extends AbstractCookieAttributeHandler implements CommonCookieAttributeHandler {

    private final DateTimeFormatter[] datePatterns;

    public BasicExpiresHandler(final DateTimeFormatter... datePatterns) {
        this.datePatterns = datePatterns;
    }

    @Deprecated
    public BasicExpiresHandler(final String[] datePatterns) {
        Args.notNull(datePatterns, "Array of date patterns");
        this.datePatterns = new DateTimeFormatter[datePatterns.length];
        for (int i = 0; i < datePatterns.length; i++) {
            this.datePatterns[i] = new DateTimeFormatterBuilder()
                    .parseLenient()
                    .parseCaseInsensitive()
                    .appendPattern(datePatterns[i])
                    .toFormatter();
        }
    }

    @Override
    public void parse(final SetCookie cookie, final String value) throws MalformedCookieException {
        Args.notNull(cookie, "Cookie");
        if (value == null)
            throw new MalformedCookieException("Missing value for 'expires' attribute");

        final Instant expiry = DateUtils.parseDate(value, this.datePatterns);
        if (expiry == null)
            throw new MalformedCookieException("Invalid 'expires' attribute: " + value);

        cookie.setExpiryDate(expiry);
    }

    @Override
    public String getAttributeName() {
        return Cookie.EXPIRES_ATTR;
    }
}
