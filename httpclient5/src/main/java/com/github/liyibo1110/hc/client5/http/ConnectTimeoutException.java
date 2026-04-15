package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.core5.net.NamedEndpoint;

import java.net.SocketTimeoutException;

/**
 * 在连接到HTTP服务器或等待连接管理器提供可用连接时发生超时。
 * @author liyibo
 * @date 2026-04-14 13:30
 */
public class ConnectTimeoutException extends SocketTimeoutException {

    private static final long serialVersionUID = -4816682903149535989L;

    private final NamedEndpoint namedEndpoint;

    public ConnectTimeoutException(final String message) {
        super(message);
        this.namedEndpoint = null;
    }

    public ConnectTimeoutException(final String message, final NamedEndpoint namedEndpoint) {
        super(message);
        this.namedEndpoint = namedEndpoint;
    }

    public NamedEndpoint getHost() {
        return this.namedEndpoint;
    }
}
