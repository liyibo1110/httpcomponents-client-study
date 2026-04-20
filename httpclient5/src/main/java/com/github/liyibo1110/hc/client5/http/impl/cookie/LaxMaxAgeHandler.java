package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.CommonCookieAttributeHandler;
import com.github.liyibo1110.hc.client5.http.cookie.Cookie;
import com.github.liyibo1110.hc.client5.http.cookie.MalformedCookieException;
import com.github.liyibo1110.hc.client5.http.cookie.SetCookie;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.TextUtils;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 符合HTTP状态管理更宽松解释的Cookie max-age属性处理程序。
 * @author liyibo
 * @date 2026-04-19 14:27
 */
public class LaxMaxAgeHandler extends AbstractCookieAttributeHandler implements CommonCookieAttributeHandler {

    public static final LaxMaxAgeHandler INSTANCE = new LaxMaxAgeHandler();

    private final static Pattern MAX_AGE_PATTERN = Pattern.compile("^\\-?[0-9]+$");

    public LaxMaxAgeHandler() {
        super();
    }

    @Override
    public void parse(final SetCookie cookie, final String value) throws MalformedCookieException {
        Args.notNull(cookie, "Cookie");
        if (TextUtils.isBlank(value))
            return;

        final Matcher matcher = MAX_AGE_PATTERN.matcher(value);
        if (matcher.matches()) {
            final int age;
            try {
                age = Integer.parseInt(value);
            } catch (final NumberFormatException e) {
                return;
            }
            final Instant expiryDate = age >= 0 ? Instant.now().plusSeconds(age) : Instant.EPOCH;
            cookie.setExpiryDate(expiryDate);
        }
    }

    @Override
    public String getAttributeName() {
        return Cookie.MAX_AGE_ATTR;
    }
}
