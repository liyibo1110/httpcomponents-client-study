package com.github.liyibo1110.hc.client5.http.entity;

import java.io.IOException;
import java.io.InputStream;

/**
 * DeflateInputStream对象的工厂。
 * @author liyibo
 * @date 2026-04-15 17:22
 */
public class DeflateInputStreamFactory implements InputStreamFactory {

    private static final DeflateInputStreamFactory INSTANCE = new DeflateInputStreamFactory();

    public static DeflateInputStreamFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public InputStream create(final InputStream inputStream) throws IOException {
        return new DeflateInputStream(inputStream);
    }
}
