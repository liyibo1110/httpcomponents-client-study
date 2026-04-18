package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.classic.ExecRuntime;
import com.github.liyibo1110.hc.core5.function.Supplier;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.Header;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.impl.io.ChunkedInputStream;
import com.github.liyibo1110.hc.core5.http.io.EofSensorInputStream;
import com.github.liyibo1110.hc.core5.http.io.EofSensorWatcher;
import com.github.liyibo1110.hc.core5.http.io.entity.HttpEntityWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;

/**
 * @author liyibo
 * @date 2026-04-17 13:11
 */
class ResponseEntityProxy extends HttpEntityWrapper implements EofSensorWatcher {

    private final ExecRuntime execRuntime;

    public static void enhance(final ClassicHttpResponse response, final ExecRuntime execRuntime) {
        final HttpEntity entity = response.getEntity();
        if (entity != null && entity.isStreaming() && execRuntime != null)
            response.setEntity(new ResponseEntityProxy(entity, execRuntime));
    }

    ResponseEntityProxy(final HttpEntity entity, final ExecRuntime execRuntime) {
        super(entity);
        this.execRuntime = execRuntime;
    }

    private void cleanup() throws IOException {
        if (this.execRuntime != null) {
            if (this.execRuntime.isEndpointConnected())
                this.execRuntime.disconnectEndpoint();
            this.execRuntime.discardEndpoint();
        }
    }

    private void discardConnection() {
        if (this.execRuntime != null)
            this.execRuntime.discardEndpoint();
    }

    public void releaseConnection() {
        if (this.execRuntime != null)
            this.execRuntime.releaseEndpoint();
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public InputStream getContent() throws IOException {
        return new EofSensorInputStream(super.getContent(), this);
    }

    @Override
    public void writeTo(final OutputStream outStream) throws IOException {
        try {
            super.writeTo(outStream != null ? outStream : NullOutputStream.INSTANCE);
            releaseConnection();
        } catch (final IOException | RuntimeException ex) {
            discardConnection();
            throw ex;
        } finally {
            cleanup();
        }
    }

    @Override
    public boolean eofDetected(final InputStream wrapped) throws IOException {
        try {
            // there may be some cleanup required, such as
            // reading trailers after the response body:
            if (wrapped != null)
                wrapped.close();
            releaseConnection();
        } catch (final IOException | RuntimeException ex) {
            discardConnection();
            throw ex;
        } finally {
            cleanup();
        }
        return false;
    }

    @Override
    public boolean streamClosed(final InputStream wrapped) throws IOException {
        try {
            final boolean open = execRuntime != null && execRuntime.isEndpointAcquired();
            // this assumes that closing the stream will
            // consume the remainder of the response body:
            try {
                if (wrapped != null)
                    wrapped.close();
                releaseConnection();
            } catch (final SocketException ex) {
                if (open)
                    throw ex;
            }
        } catch (final IOException | RuntimeException ex) {
            discardConnection();
            throw ex;
        } finally {
            cleanup();
        }
        return false;
    }

    @Override
    public boolean streamAbort(final InputStream wrapped) throws IOException {
        cleanup();
        return false;
    }

    @Override
    public Supplier<List<? extends Header>> getTrailers() {
        try {
            final InputStream underlyingStream = super.getContent();
            return () -> {
                final Header[] footers;
                if (underlyingStream instanceof ChunkedInputStream) {
                    final ChunkedInputStream chunkedInputStream = (ChunkedInputStream) underlyingStream;
                    footers = chunkedInputStream.getFooters();
                } else {
                    footers = new Header[0];
                }
                return Arrays.asList(footers);
            };
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to retrieve input stream", e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            // HttpEntity.close will close the underlying resource. Closing a reusable request stream results in
            // draining remaining data, allowing for connection reuse.
            super.close();
            releaseConnection();
        } catch (final IOException | RuntimeException ex) {
            discardConnection();
            throw ex;
        } finally {
            cleanup();
        }
    }

    private static final class NullOutputStream extends OutputStream {
        private static final NullOutputStream INSTANCE = new NullOutputStream();

        private NullOutputStream() {}

        @Override
        public void write(@SuppressWarnings("unused") final int byteValue) {
            // no-op
        }

        @Override
        public void write(@SuppressWarnings("unused") final byte[] buffer) {
            // no-op
        }

        @Override
        public void write(
                @SuppressWarnings("unused") final byte[] buffer,
                @SuppressWarnings("unused") final int off,
                @SuppressWarnings("unused") final int len) {
            // no-op
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }

        @Override
        public String toString() {
            return "NullOutputStream{}";
        }
    }
}
