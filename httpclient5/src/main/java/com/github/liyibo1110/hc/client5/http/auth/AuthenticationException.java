package com.github.liyibo1110.hc.client5.http.auth;

import com.github.liyibo1110.hc.core5.http.ProtocolException;

/**
 * 认证过程相关的异常。
 * @author liyibo
 * @date 2026-04-15 13:32
 */
public class AuthenticationException extends ProtocolException {
    private static final long serialVersionUID = -6794031905674764776L;

    public AuthenticationException() {
        super();
    }

    public AuthenticationException(final String message) {
        super(message);
    }

    public AuthenticationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
