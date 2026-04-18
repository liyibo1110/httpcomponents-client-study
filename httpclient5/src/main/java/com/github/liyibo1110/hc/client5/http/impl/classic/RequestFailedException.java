package com.github.liyibo1110.hc.client5.http.impl.classic;

import java.io.InterruptedIOException;

/**
 * @author liyibo
 * @date 2026-04-17 12:04
 */
public class RequestFailedException extends InterruptedIOException {
    private static final long serialVersionUID = 4973849966012490112L;

    public RequestFailedException(final String message) {
        super(message);
    }

    public RequestFailedException(final String message, final Throwable cause) {
        super(message);
        if (cause != null) {
            initCause(cause);
        }
    }
}
