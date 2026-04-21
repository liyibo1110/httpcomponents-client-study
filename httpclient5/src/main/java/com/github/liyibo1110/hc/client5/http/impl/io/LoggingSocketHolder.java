package com.github.liyibo1110.hc.client5.http.impl.io;

import com.github.liyibo1110.hc.client5.http.impl.Wire;
import com.github.liyibo1110.hc.core5.http.impl.io.SocketHolder;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * 附带了Wire组件的SocketHolder扩展。
 * @author liyibo
 * @date 2026-04-20 16:55
 */
public class LoggingSocketHolder extends SocketHolder {
    private final Wire wire;

    public LoggingSocketHolder(final Socket socket, final String id, final Logger log) {
        super(socket);
        this.wire = new Wire(log, id);
    }

    @Override
    protected InputStream getInputStream(final Socket socket) throws IOException {
        return new LoggingInputStream(super.getInputStream(socket), wire);
    }

    @Override
    protected OutputStream getOutputStream(final Socket socket) throws IOException {
        return new LoggingOutputStream(super.getOutputStream(socket), wire);
    }
}
