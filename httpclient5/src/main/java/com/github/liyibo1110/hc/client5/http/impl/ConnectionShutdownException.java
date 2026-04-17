package com.github.liyibo1110.hc.client5.http.impl;

/**
 * 表示连接已被关闭或释放回连接池的异常。
 * @author liyibo
 * @date 2026-04-16 16:14
 */
public class ConnectionShutdownException extends IllegalStateException {
    private static final long serialVersionUID = 5868657401162844497L;

    public ConnectionShutdownException() {
        super();
    }
}
