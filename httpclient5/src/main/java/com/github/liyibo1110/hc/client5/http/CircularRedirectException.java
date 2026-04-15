package com.github.liyibo1110.hc.client5.http;

/**
 * 循环重定向的异常。
 * @author liyibo
 * @date 2026-04-14 13:28
 */
public class CircularRedirectException extends RedirectException {

    private static final long serialVersionUID = 6830063487001091803L;

    public CircularRedirectException() {
        super();
    }

    public CircularRedirectException(final String message) {
        super(message);
    }

    public CircularRedirectException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
