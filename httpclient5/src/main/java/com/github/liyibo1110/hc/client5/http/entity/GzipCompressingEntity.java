package com.github.liyibo1110.hc.client5.http.entity;

import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.io.entity.HttpEntityWrapper;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 在写入时对内容进行压缩的wrapper entity。
 * @author liyibo
 * @date 2026-04-15 17:54
 */
public class GzipCompressingEntity extends HttpEntityWrapper {
    private static final String GZIP_CODEC = "gzip";

    public GzipCompressingEntity(final HttpEntity entity) {
        super(entity);
    }

    @Override
    public String getContentEncoding() {
        return GZIP_CODEC;
    }

    @Override
    public long getContentLength() {
        return -1;
    }

    @Override
    public boolean isChunked() {
        // force content chunking
        return true;
    }

    @Override
    public InputStream getContent() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeTo(final OutputStream outStream) throws IOException {
        Args.notNull(outStream, "Output stream");
        final GZIPOutputStream gzip = new GZIPOutputStream(outStream);
        super.writeTo(gzip);
        gzip.close();
    }
}
