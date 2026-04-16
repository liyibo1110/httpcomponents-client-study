package com.github.liyibo1110.hc.client5.http.entity;

import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.io.entity.HttpEntityWrapper;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 用于解压HttpEntity实现类的通用基类。
 * @author liyibo
 * @date 2026-04-15 17:56
 */
public class DecompressingEntity extends HttpEntityWrapper {
    private static final int BUFFER_SIZE = 1024 * 2;
    private final InputStreamFactory inputStreamFactory;

    /** 当DecompressingEntity包装一个流式实体时，getContent()方法必须返回相同的InputStream实例。 */
    private InputStream content;

    public DecompressingEntity(final HttpEntity wrapped, final InputStreamFactory inputStreamFactory) {
        super(wrapped);
        this.inputStreamFactory = inputStreamFactory;
    }

    private InputStream getDecompressingStream() throws IOException {
        return new LazyDecompressingInputStream(super.getContent(), inputStreamFactory);
    }

    @Override
    public InputStream getContent() throws IOException {
        if (super.isStreaming()) {
            if (content == null)
                content = getDecompressingStream();
            return content;
        }
        return getDecompressingStream();
    }

    @Override
    public void writeTo(final OutputStream outStream) throws IOException {
        Args.notNull(outStream, "Output stream");
        try (InputStream inStream = getContent()) {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int l;
            while ((l = inStream.read(buffer)) != -1)
                outStream.write(buffer, 0, l);
        }
    }

    @Override
    public String getContentEncoding() {
        /* Content encoding is now 'identity' */
        return null;
    }

    @Override
    public long getContentLength() {
        /* length of decompressed content is not known */
        return -1;
    }
}
