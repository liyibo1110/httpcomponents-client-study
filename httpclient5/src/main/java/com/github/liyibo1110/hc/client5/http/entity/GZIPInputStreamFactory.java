package com.github.liyibo1110.hc.client5.http.entity;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * GZIPInputStream对象的工厂。
 * @author liyibo
 * @date 2026-04-15 17:24
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class GZIPInputStreamFactory implements InputStreamFactory {
    private static final GZIPInputStreamFactory INSTANCE = new GZIPInputStreamFactory();

    public static GZIPInputStreamFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public InputStream create(final InputStream inputStream) throws IOException {
        return new GZIPInputStream(inputStream);
    }
}
