package com.github.liyibo1110.hc.client5.http.entity.mime;

import com.github.liyibo1110.hc.core5.http.ContentType;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 由byte[]支持的二进制body部分。
 * @author liyibo
 * @date 2026-04-15 21:58
 */
public class ByteArrayBody extends AbstractContentBody {
    private final byte[] data;
    private final String filename;

    public ByteArrayBody(final byte[] data, final ContentType contentType, final String filename) {
        super(contentType);
        this.data = Args.notNull(data, "data");
        this.filename = filename;
    }

    public ByteArrayBody(final byte[] data, final ContentType contentType) {
        this(data, contentType, null);
    }

    public ByteArrayBody(final byte[] data, final String filename) {
        this(data, ContentType.APPLICATION_OCTET_STREAM, filename);
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void writeTo(final OutputStream out) throws IOException {
        out.write(data);
    }

    @Override
    public String getCharset() {
        return null;
    }

    @Override
    public long getContentLength() {
        return data.length;
    }
}
