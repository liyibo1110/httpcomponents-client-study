package com.github.liyibo1110.hc.client5.http.cookie;

/**
 * 表示该Cookie违反了Cookie规范所设定限制的异常。
 * @author liyibo
 * @date 2026-04-15 15:29
 */
public class CookieRestrictionViolationException extends MalformedCookieException {
    private static final long serialVersionUID = 7371235577078589013L;

    public CookieRestrictionViolationException() {
        super();
    }

    public CookieRestrictionViolationException(final String message) {
        super(message);
    }
}
