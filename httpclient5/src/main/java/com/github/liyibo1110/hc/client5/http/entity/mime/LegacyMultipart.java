package com.github.liyibo1110.hc.client5.http.entity.mime;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * HttpBrowserCompatibleMultipart表示一组采用MIME多部分编码的内容主体。
 * 该类模拟了浏览器兼容性，例如IE 5或更早版本。
 * @author liyibo
 * @date 2026-04-16 10:40
 */
class LegacyMultipart extends AbstractMultipartFormat {
    private final List<MultipartPart> parts;

    public LegacyMultipart(final Charset charset, final String boundary, final List<MultipartPart> parts) {
        super(charset, boundary);
        this.parts = parts;
    }

    @Override
    public List<MultipartPart> getParts() {
        return this.parts;
    }

    @Override
    protected void formatMultipartHeader(final MultipartPart part, final OutputStream out) throws IOException {
        // For browser-compatible, only write Content-Disposition
        // Use content charset
        final Header header = part.getHeader();
        final MimeField cd = header.getField(MimeConsts.CONTENT_DISPOSITION);
        if (cd != null)
            writeField(cd, this.charset, out);

        final String filename = part.getBody().getFilename();
        if (filename != null) {
            final MimeField ct = header.getField(MimeConsts.CONTENT_TYPE);
            writeField(ct, this.charset, out);
        }

    }
}
