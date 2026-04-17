package com.github.liyibo1110.hc.client5.http.socket;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.io.Closer;
import com.github.liyibo1110.hc.core5.util.Asserts;
import com.github.liyibo1110.hc.core5.util.TimeValue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * 用于创建普通（未加密）Socket的默认类。
 * @author liyibo
 * @date 2026-04-16 11:57
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class PlainConnectionSocketFactory implements ConnectionSocketFactory {

    public static final PlainConnectionSocketFactory INSTANCE = new PlainConnectionSocketFactory();

    public static PlainConnectionSocketFactory getSocketFactory() {
        return INSTANCE;
    }

    public PlainConnectionSocketFactory() {
        super();
    }

    @Override
    public Socket createSocket(final HttpContext context) throws IOException {
        return new Socket();
    }

    @Override
    public Socket connectSocket(final TimeValue connectTimeout,
                                final Socket socket,
                                final HttpHost host,
                                final InetSocketAddress remoteAddress,
                                final InetSocketAddress localAddress,
                                final HttpContext context) throws IOException {
        final Socket sock = socket != null ? socket : createSocket(context);
        if (localAddress != null)
            sock.bind(localAddress);

        try {
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                    sock.connect(remoteAddress, TimeValue.isPositive(connectTimeout) ? connectTimeout.toMillisecondsIntBound() : 0);
                    return null;
                });
            } catch (final PrivilegedActionException e) {
                Asserts.check(e.getCause() instanceof  IOException, "method contract violation only checked exceptions are wrapped: " + e.getCause());
                // only checked exceptions are wrapped - error and RTExceptions are rethrown by doPrivileged
                throw (IOException) e.getCause();
            }
        } catch (final IOException ex) {
            Closer.closeQuietly(sock);
            throw ex;
        }
        return sock;
    }
}
