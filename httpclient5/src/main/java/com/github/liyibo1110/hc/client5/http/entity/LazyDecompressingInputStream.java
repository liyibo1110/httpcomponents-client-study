package com.github.liyibo1110.hc.client5.http.entity;

import java.io.IOException;
import java.io.InputStream;

/**
 * 延迟初始化的InputStream包装类。
 * @author liyibo
 * @date 2026-04-15 17:24
 */
class LazyDecompressingInputStream extends InputStream {

    /** 要被包装修饰的原始InputStream */
    private final InputStream wrappedStream;

    /** 装饰器工厂 */
    private final InputStreamFactory inputStreamFactory;

    /** 被包装修饰后的InputStream */
    private InputStream wrapperStream;

    public LazyDecompressingInputStream(final InputStream wrappedStream, final InputStreamFactory inputStreamFactory) {
        this.wrappedStream = wrappedStream;
        this.inputStreamFactory = inputStreamFactory;
    }

    /**
     * 延迟包装方法，会在对InputStream进行读取之类的操作时，才会进行装饰。
     */
    private void initWrapper() throws IOException {
        if (wrapperStream == null)
            wrapperStream = inputStreamFactory.create(wrappedStream);
    }

    @Override
    public int read() throws IOException {
        initWrapper();
        return wrapperStream.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        initWrapper();
        return wrapperStream.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        initWrapper();
        return wrapperStream.read(b, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
        initWrapper();
        return wrapperStream.skip(n);
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int available() throws IOException {
        initWrapper();
        return wrapperStream.available();
    }

    @Override
    public void close() throws IOException {
        try {
            if (wrapperStream != null)
                wrapperStream.close();
        } finally {
            wrappedStream.close();
        }
    }
}
