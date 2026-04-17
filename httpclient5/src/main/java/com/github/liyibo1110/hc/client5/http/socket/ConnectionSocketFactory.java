package com.github.liyibo1110.hc.client5.http.socket;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.TimeValue;
import com.github.liyibo1110.hc.core5.util.Timeout;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

/**
 * 生成connection socket对象的工厂接口。
 * @author liyibo
 * @date 2026-04-16 11:48
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface ConnectionSocketFactory {

    /**
     * 创建一个新的、未连接的Socket，随后应传递给connectSocket方法。
     */
    Socket createSocket(HttpContext context) throws IOException;

    /**
     * 通过代理创建一个新的、未连接的Socket（通常预期使用 SOCKS），随后应传递给connectSocket方法。
     */
    @Internal
    default Socket createSocket(Proxy proxy, HttpContext context) throws IOException {
        return createSocket(context);
    }

    /**
     * 使用给定的解析后的远程地址，将Socket连接到目标主机。
     */
    Socket connectSocket(
            TimeValue connectTimeout,
            Socket socket,
            HttpHost host,
            InetSocketAddress remoteAddress,
            InetSocketAddress localAddress,
            HttpContext context) throws IOException;

    default Socket connectSocket(
            Socket socket,
            HttpHost host,
            InetSocketAddress remoteAddress,
            InetSocketAddress localAddress,
            Timeout connectTimeout,
            Object attachment,
            HttpContext context) throws IOException {
        return connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
    }
}
