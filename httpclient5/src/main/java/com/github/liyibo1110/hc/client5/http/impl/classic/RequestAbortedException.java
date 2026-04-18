package com.github.liyibo1110.hc.client5.http.impl.classic;

import java.io.InterruptedIOException;

/**
 * @author liyibo
 * @date 2026-04-17 12:03
 */
public class RequestAbortedException extends InterruptedIOException {
    private static final long serialVersionUID = 4973849966012490112L;

    public RequestAbortedException(final String message) {
        super(message);
    }

    public RequestAbortedException(final String message, final Throwable cause) {
        super(message);
        if (cause != null)
            initCause(cause);
    }
}
