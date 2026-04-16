package com.github.liyibo1110.hc.client5.http.cookie;

import com.github.liyibo1110.hc.core5.http.ProtocolException;

/**
 * 表示该Cookie在特定情况下以某种方式无效或非法。
 * @author liyibo
 * @date 2026-04-15 15:12
 */
public class MalformedCookieException extends ProtocolException {
    private static final long serialVersionUID = -6695462944287282185L;

    public MalformedCookieException() {
        super();
    }

    public MalformedCookieException(final String message) {
        super(message);
    }

    public MalformedCookieException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
