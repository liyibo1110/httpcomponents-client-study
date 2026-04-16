package com.github.liyibo1110.hc.client5.http.auth;

/**
 * 用于响应身份验证Challenge的身份验证凭据无效
 * @author liyibo
 * @date 2026-04-15 13:41
 */
public class InvalidCredentialsException extends AuthenticationException {
    private static final long serialVersionUID = -4834003835215460648L;

    public InvalidCredentialsException() {
        super();
    }

    public InvalidCredentialsException(final String message) {
        super(message);
    }

    public InvalidCredentialsException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
