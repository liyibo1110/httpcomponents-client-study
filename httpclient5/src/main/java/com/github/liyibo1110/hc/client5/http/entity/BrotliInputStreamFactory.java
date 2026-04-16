package com.github.liyibo1110.hc.client5.http.entity;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import org.brotli.dec.BrotliInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * BrotliInputStream对象的工厂。
 * @author liyibo
 * @date 2026-04-15 18:01
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class BrotliInputStreamFactory implements InputStreamFactory {

    private static final BrotliInputStreamFactory INSTANCE = new BrotliInputStreamFactory();

    public static BrotliInputStreamFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public InputStream create(final InputStream inputStream) throws IOException {
        return new BrotliInputStream(inputStream);
    }
}
