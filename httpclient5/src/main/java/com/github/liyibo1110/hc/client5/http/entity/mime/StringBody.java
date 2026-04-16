package com.github.liyibo1110.hc.client5.http.entity.mime;

import com.github.liyibo1110.hc.core5.http.ContentType;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 由String（其实内部还是byte[]）支持的二进制body部分。
 * @author liyibo
 * @date 2026-04-15 22:19
 */
public class StringBody extends AbstractContentBody {
    private final byte[] content;

    public StringBody(final String text, final ContentType contentType) {
        super(contentType);
        Args.notNull(text, "Text");
        final Charset charset = contentType.getCharset();
        this.content = text.getBytes(charset != null ? charset : StandardCharsets.US_ASCII);
    }

    public Reader getReader() {
        final Charset charset = getContentType().getCharset();
        return new InputStreamReader(new ByteArrayInputStream(this.content), charset != null ? charset : StandardCharsets.US_ASCII);
    }

    @Override
    public void writeTo(final OutputStream out) throws IOException {
        Args.notNull(out, "Output stream");
        out.write(this.content);
    }

    @Override
    public long getContentLength() {
        return this.content.length;
    }

    @Override
    public String getFilename() {
        return null;
    }
}
