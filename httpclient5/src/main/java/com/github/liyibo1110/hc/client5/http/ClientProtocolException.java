package com.github.liyibo1110.hc.client5.http;

import java.io.IOException;

/**
 * 客户端HTTP协议的相关异常。
 * @author liyibo
 * @date 2026-04-14 13:29
 */
public class ClientProtocolException extends IOException {
    private static final long serialVersionUID = -5596590843227115865L;

    public ClientProtocolException() {
        super();
    }

    public ClientProtocolException(final String s) {
        super(s);
    }

    public ClientProtocolException(final Throwable cause) {
        initCause(cause);
    }

    public ClientProtocolException(final String message, final Throwable cause) {
        super(message);
        initCause(cause);
    }
}
