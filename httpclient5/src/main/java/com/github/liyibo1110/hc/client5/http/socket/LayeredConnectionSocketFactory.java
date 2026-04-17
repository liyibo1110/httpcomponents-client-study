package com.github.liyibo1110.hc.client5.http.socket;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.net.Socket;

/**
 * 扩展了ConnectionSocketFactory接口，用于支持分层Sockets（如SSL/TLS）。
 * @author liyibo
 * @date 2026-04-16 11:54
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface LayeredConnectionSocketFactory extends ConnectionSocketFactory {

    /**
     * 返回一个连接到指定主机的Socket，该Socket基于现有Socket构建。主要用于通过代理创建secure Socket。
     */
    Socket createLayeredSocket(Socket socket, String target, int port, HttpContext context) throws IOException;

    default Socket createLayeredSocket(Socket socket, String target, int port, Object attachment, HttpContext context) throws IOException {
        return createLayeredSocket(socket, target, port, context);
    }
}
