package com.github.liyibo1110.hc.client5.http.impl.io;

import com.github.liyibo1110.hc.client5.http.impl.Wire;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream装饰增强组件，增加了write之后额外输出log的功能。
 * @author liyibo
 * @date 2026-04-20 16:53
 */
public class LoggingOutputStream extends OutputStream {
    private final OutputStream out;
    private final Wire wire;

    public LoggingOutputStream(final OutputStream out, final Wire wire) {
        super();
        this.out = out;
        this.wire = wire;
    }

    @Override
    public void write(final int b) throws IOException {
        try {
            out.write(b);
            wire.output(b);
        } catch (final IOException e) {
            wire.output("[write] I/O error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void write(final byte[] b) throws IOException {
        try {
            wire.output(b);
            out.write(b);
        } catch (final IOException e) {
            wire.output("[write] I/O error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        try {
            wire.output(b, off, len);
            out.write(b, off, len);
        } catch (final IOException e) {
            wire.output("[write] I/O error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            out.flush();
        } catch (final IOException e) {
            wire.output("[flush] I/O error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            out.close();
        } catch (final IOException e) {
            wire.output("[close] I/O error: " + e.getMessage());
            throw e;
        }
    }
}
