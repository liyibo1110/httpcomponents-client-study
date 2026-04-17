package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.core5.http.HttpException;

/**
 * 表示隧道请求被代理主机拒绝的异常。
 * @author liyibo
 * @date 2026-04-16 16:27
 */
public class TunnelRefusedException extends HttpException {
    private static final long serialVersionUID = -8646722842745617323L;

    private final String responseMessage;

    public TunnelRefusedException(final String message, final String responseMessage) {
        super(message);
        this.responseMessage = responseMessage;
    }

    public String getResponseMessage() {
        return this.responseMessage;
    }
}
