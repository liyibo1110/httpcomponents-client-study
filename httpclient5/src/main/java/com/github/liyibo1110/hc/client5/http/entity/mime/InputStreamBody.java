package com.github.liyibo1110.hc.client5.http.entity.mime;

import com.github.liyibo1110.hc.core5.http.ContentType;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 由InputStream支持的二进制body部分。
 * @author liyibo
 * @date 2026-04-15 22:15
 */
public class InputStreamBody extends AbstractContentBody {
    private final InputStream in;
    private final String filename;
    private final long contentLength;

    public InputStreamBody(final InputStream in, final String filename) {
        this(in, ContentType.DEFAULT_BINARY, filename);
    }

    public InputStreamBody(final InputStream in, final ContentType contentType, final String filename) {
        this(in, contentType, filename, -1);
    }

    public InputStreamBody(final InputStream in, final ContentType contentType, final String filename, final long contentLength) {
        super(contentType);
        Args.notNull(in, "Input stream");
        this.in = in;
        this.filename = filename;
        this.contentLength = contentLength >= 0 ? contentLength : -1;
    }

    public InputStreamBody(final InputStream in, final ContentType contentType) {
        this(in, contentType, null);
    }

    public InputStream getInputStream() {
        return this.in;
    }

    @Override
    public void writeTo(final OutputStream out) throws IOException {
        Args.notNull(out, "Output stream");
        try {
            final byte[] tmp = new byte[4096];
            int l;
            while ((l = this.in.read(tmp)) != -1)
                out.write(tmp, 0, l);
            out.flush();
        } finally {
            this.in.close();
        }
    }

    @Override
    public long getContentLength() {
        return this.contentLength;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }
}
