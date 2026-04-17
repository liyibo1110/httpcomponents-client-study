package com.github.liyibo1110.hc.client5.http.entity.mime;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author liyibo
 * @date 2026-04-16 10:36
 */
class HttpRFC6532Multipart extends AbstractMultipartFormat {
    private final List<MultipartPart> parts;

    public HttpRFC6532Multipart(final Charset charset, final String boundary, final List<MultipartPart> parts) {
        super(charset, boundary);
        this.parts = parts;
    }

    @Override
    public List<MultipartPart> getParts() {
        return this.parts;
    }

    @Override
    protected void formatMultipartHeader(final MultipartPart part, final OutputStream out) throws IOException {
        // RFC 6532就是UTF-8版本的field
        final Header header = part.getHeader();
        for (final MimeField field: header)
            writeField(field, StandardCharsets.UTF_8, out);
    }
}
