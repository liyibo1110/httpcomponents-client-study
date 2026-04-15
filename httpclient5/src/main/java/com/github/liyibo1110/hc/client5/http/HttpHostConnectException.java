package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.core5.net.NamedEndpoint;

import java.net.ConnectException;

/**
 * 一个ConnectException，其中指定了正在连接的NamedEndpoint。
 * @author liyibo
 * @date 2026-04-14 13:42
 */
public class HttpHostConnectException extends ConnectException {
    private static final long serialVersionUID = -3194482710275220224L;

    private final NamedEndpoint namedEndpoint;

    public HttpHostConnectException(final String message) {
        super(message);
        this.namedEndpoint = null;
    }

    public HttpHostConnectException(final String message, final NamedEndpoint namedEndpoint) {
        super(message);
        this.namedEndpoint = namedEndpoint;
    }

    public NamedEndpoint getHost() {
        return this.namedEndpoint;
    }
}
