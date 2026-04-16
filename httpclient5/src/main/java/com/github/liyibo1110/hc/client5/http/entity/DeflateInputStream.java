package com.github.liyibo1110.hc.client5.http.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

/**
 * 对输入流进行Deflate压缩。
 * 该类包含各种RFC所需的逻辑，以便合理地实现deflate压缩格式。
 * @author liyibo
 * @date 2026-04-15 17:19
 */
public class DeflateInputStream extends InputStream {

    private final InputStream sourceStream;

    public DeflateInputStream(final InputStream wrapped) throws IOException {
        final PushbackInputStream pushback = new PushbackInputStream(wrapped, 2);
        final int i1 = pushback.read();
        final int i2 = pushback.read();
        if (i1 == -1 || i2 == -1)
            throw new ZipException("Unexpected end of stream");

        pushback.unread(i2);
        pushback.unread(i1);

        boolean nowrap = true;
        final int b1 = i1 & 0xFF;
        final int compressionMethod = b1 & 0xF;
        final int compressionInfo = b1 >> 4 & 0xF;
        final int b2 = i2 & 0xFF;
        if (compressionMethod == 8 && compressionInfo <= 7 && ((b1 << 8) | b2) % 31 == 0)
            nowrap = false;
        sourceStream = new DeflateStream(pushback, new Inflater(nowrap));
    }

    @Override
    public int read() throws IOException {
        return sourceStream.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return sourceStream.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return sourceStream.read(b, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
        return sourceStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return sourceStream.available();
    }

    @Override
    public void mark(final int readLimit) {
        sourceStream.mark(readLimit);
    }

    @Override
    public void reset() throws IOException {
        sourceStream.reset();
    }

    @Override
    public boolean markSupported() {
        return sourceStream.markSupported();
    }

    @Override
    public void close() throws IOException {
        sourceStream.close();
    }

    static class DeflateStream extends InflaterInputStream {
        private boolean closed;

        public DeflateStream(final InputStream in, final Inflater inflater) {
            super(in, inflater);
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            inf.end();
            super.close();
        }
    }
}
