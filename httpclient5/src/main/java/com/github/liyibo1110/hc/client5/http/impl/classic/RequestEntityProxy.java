package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.core5.function.Supplier;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.Header;
import com.github.liyibo1110.hc.core5.http.HttpEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

/**
 * @author liyibo
 * @date 2026-04-17 13:09
 */
class RequestEntityProxy implements HttpEntity {

    static void enhance(final ClassicHttpRequest request) {
        final HttpEntity entity = request.getEntity();
        if (entity != null && !entity.isRepeatable() && !isEnhanced(entity))
            request.setEntity(new RequestEntityProxy(entity));
    }

    static boolean isEnhanced(final HttpEntity entity) {
        return entity instanceof RequestEntityProxy;
    }

    private final HttpEntity original;
    private boolean consumed;

    RequestEntityProxy(final HttpEntity original) {
        super();
        this.original = original;
    }

    public HttpEntity getOriginal() {
        return original;
    }

    public boolean isConsumed() {
        return consumed;
    }

    @Override
    public boolean isRepeatable() {
        if (!consumed)
            return true;
        else
            return original.isRepeatable();
    }

    @Override
    public boolean isChunked() {
        return original.isChunked();
    }

    @Override
    public long getContentLength() {
        return original.getContentLength();
    }

    @Override
    public String getContentType() {
        return original.getContentType();
    }

    @Override
    public String getContentEncoding() {
        return original.getContentEncoding();
    }

    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
        return original.getContent();
    }

    @Override
    public void writeTo(final OutputStream outStream) throws IOException {
        consumed = true;
        original.writeTo(outStream);
    }

    @Override
    public boolean isStreaming() {
        return original.isStreaming();
    }

    @Override
    public Supplier<List<? extends Header>> getTrailers() {
        return original.getTrailers();
    }

    @Override
    public Set<String> getTrailerNames() {
        return original.getTrailerNames();
    }

    @Override
    public void close() throws IOException {
        original.close();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RequestEntityProxy{");
        sb.append(original);
        sb.append('}');
        return sb.toString();
    }
}
