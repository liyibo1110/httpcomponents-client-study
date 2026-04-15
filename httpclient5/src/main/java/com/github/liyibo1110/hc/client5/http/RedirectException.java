package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.core5.http.ProtocolException;

/**
 * 表示因无效重定向导致的HTTP规范违规
 * @author liyibo
 * @date 2026-04-14 12:44
 */
public class RedirectException extends ProtocolException {
    private static final long serialVersionUID = 4418824536372559326L;

    public RedirectException() {
        super();
    }

    public RedirectException(final String message) {
        super(message);
    }

    public RedirectException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
