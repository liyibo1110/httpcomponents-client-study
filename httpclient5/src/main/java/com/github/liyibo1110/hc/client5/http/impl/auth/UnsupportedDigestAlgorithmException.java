package com.github.liyibo1110.hc.client5.http.impl.auth;

/**
 * 用于响应身份验证挑战的身份验证凭据无效。
 * @author liyibo
 * @date 2026-04-17 17:00
 */
public class UnsupportedDigestAlgorithmException extends RuntimeException {
    private static final long serialVersionUID = 319558534317118022L;

    public UnsupportedDigestAlgorithmException() {
        super();
    }

    public UnsupportedDigestAlgorithmException(final String message) {
        super(message);
    }

    public UnsupportedDigestAlgorithmException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
