package com.github.liyibo1110.hc.client5.http.impl.auth;

import com.github.liyibo1110.hc.client5.http.auth.AuthenticationException;
import com.github.liyibo1110.hc.core5.annotation.Internal;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * AuthScheme实现类，需要的通用工具。
 * @author liyibo
 * @date 2026-04-16 11:41
 */
@Internal
public class AuthSchemeSupport {

    /**
     * String -> Charset
     */
    public static Charset parseCharset(final String charsetName, final Charset defaultCharset) throws AuthenticationException {
        try {
            return charsetName != null ? Charset.forName(charsetName) : defaultCharset;
        } catch (final UnsupportedCharsetException ex) {
            throw new AuthenticationException("Unsupported charset: " + charsetName);
        }
    }
}
