package com.github.liyibo1110.hc.client5.http.io;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.io.SocketConfig;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.TimeValue;
import com.github.liyibo1110.hc.core5.util.Timeout;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * 用于执行连接建立和upgrade操作的连接操作。
 * @author liyibo
 * @date 2026-04-16 13:49
 */
@Contract(threading = ThreadingBehavior.STATELESS)
@Internal
public interface HttpClientConnectionOperator {

    /**
     * 将给定的托管连接连接到远程端点。
     */
    void connect(ManagedHttpClientConnection conn,
                 HttpHost host,
                 InetSocketAddress localAddress,
                 TimeValue connectTimeout,
                 SocketConfig socketConfig,
                 HttpContext context) throws IOException;

    default void connect(ManagedHttpClientConnection conn,
                         HttpHost host,
                         InetSocketAddress localAddress,
                         Timeout connectTimeout,
                         SocketConfig socketConfig,
                         Object attachment,
                         HttpContext context) throws IOException {
        connect(conn, host, localAddress, connectTimeout, socketConfig, context);
    }

    /**
     * 通过使用TLS安全协议，提升指定受管连接的传输安全性。
     */
    void upgrade(ManagedHttpClientConnection conn, HttpHost host, HttpContext context) throws IOException;

    default void upgrade(ManagedHttpClientConnection conn, HttpHost host, Object attachment, HttpContext context) throws IOException {
        upgrade(conn, host, context);
    }
}
