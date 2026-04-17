package com.github.liyibo1110.hc.client5.http.io;

import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.http.io.HttpClientConnection;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Socket;

/**
 * 表示一种受管理的连接，其状态和生命周期由连接管理器进行管理。
 * 该接口继承自HttpClientConnection，并提供了将连接绑定到任意套接字以及获取SSL会话详细信息的方法。
 * @author liyibo
 * @date 2026-04-16 13:41
 */
@Internal
public interface ManagedHttpClientConnection extends HttpClientConnection {

    /**
     * 将此连接绑定到指定的socket。
     * 如果连接已绑定且底层socket已连接到远程主机，则该连接被视为已建立。
     */
    void bind(Socket socket) throws IOException;

    Socket getSocket();

    /**
     * 获取底层连接的SSL会话（如有）。
     * 如果该连接已打开，且底层套接字是SSLSocket，则获取该套接字的SSL会话。
     * 这是一项可能阻塞的操作。
     */
    @Override
    SSLSession getSSLSession();

    /**
     * 将连接置于空闲模式。
     */
    void passivate();

    /**
     * 从空闲模式恢复连接。
     */
    void activate();
}
