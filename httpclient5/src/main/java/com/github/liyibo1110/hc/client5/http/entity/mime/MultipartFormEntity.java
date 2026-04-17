package com.github.liyibo1110.hc.client5.http.entity.mime;

import com.github.liyibo1110.hc.core5.function.Supplier;
import com.github.liyibo1110.hc.core5.http.ContentTooLongException;
import com.github.liyibo1110.hc.core5.http.ContentType;
import com.github.liyibo1110.hc.core5.http.Header;
import com.github.liyibo1110.hc.core5.http.HttpEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

/**
 * MultipartForm类型的entity实现。
 * @author liyibo
 * @date 2026-04-16 10:44
 */
class MultipartFormEntity implements HttpEntity {
    private final AbstractMultipartFormat multipart;
    private final ContentType contentType;
    private final long contentLength;

    MultipartFormEntity(final AbstractMultipartFormat multipart, final ContentType contentType, final long contentLength) {
        super();
        this.multipart = multipart;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    AbstractMultipartFormat getMultipart() {
        return this.multipart;
    }

    @Override
    public boolean isRepeatable() {
        return this.contentLength != -1;
    }

    @Override
    public boolean isChunked() {
        return !isRepeatable();
    }

    @Override
    public boolean isStreaming() {
        return !isRepeatable();
    }

    @Override
    public long getContentLength() {
        return this.contentLength;
    }

    @Override
    public String getContentType() {
        return this.contentType != null ? this.contentType.toString() : null;
    }

    @Override
    public String getContentEncoding() {
        return null;
    }

    @Override
    public InputStream getContent() throws IOException {
        if (this.contentLength < 0)
            throw new ContentTooLongException("Content length is unknown");
        else if (this.contentLength > 25 * 1024)
            throw new ContentTooLongException("Content length is too long: " + this.contentLength);

        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        writeTo(outStream);
        outStream.flush();
        return new ByteArrayInputStream(outStream.toByteArray());
    }

    @Override
    public void writeTo(final OutputStream outStream) throws IOException {
        this.multipart.writeTo(outStream);
    }

    @Override
    public Supplier<List<? extends Header>> getTrailers() {
        return null;
    }

    @Override
    public Set<String> getTrailerNames() {
        return null;
    }

    @Override
    public void close() throws IOException {}
}
