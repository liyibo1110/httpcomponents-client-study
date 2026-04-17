package com.github.liyibo1110.hc.client5.http.entity.mime;

import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.ByteArrayBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 基于模板方法，用来解决多个part怎么样被统一写出去。
 * @author liyibo
 * @date 2026-04-16 10:04
 */
abstract class AbstractMultipartFormat {

    /**
     * String -> ByteArrayBuffer(encoded)
     */
    static ByteArrayBuffer encode(final Charset charset, final CharSequence string) {
        final ByteBuffer encoded = charset.encode(CharBuffer.wrap(string));
        final ByteArrayBuffer bab = new ByteArrayBuffer(encoded.remaining());
        bab.append(encoded.array(), encoded.arrayOffset() + encoded.position(), encoded.remaining());
        return bab;
    }

    static void writeBytes(final ByteArrayBuffer b, final OutputStream out) throws IOException {
        out.write(b.array(), 0, b.length());
    }

    static void writeBytes(final CharSequence s, final Charset charset, final OutputStream out) throws IOException {
        final ByteArrayBuffer b = encode(charset, s);
        writeBytes(b, out);
    }

    static void writeBytes(final CharSequence s, final OutputStream out) throws IOException {
        final ByteArrayBuffer b = encode(StandardCharsets.ISO_8859_1, s);
        writeBytes(b, out);
    }

    static boolean isLineBreak(final char ch) {
        return ch == '\r' || ch == '\n' || ch == '\f' || ch == 11;
    }

    static CharSequence stripLineBreaks(final CharSequence s) {
        if (s == null)
            return null;

        boolean requiresRewrite = false;
        int n = 0;
        for (; n < s.length(); n++) {
            final char ch = s.charAt(n);
            if (isLineBreak(ch)) {
                requiresRewrite = true;
                break;
            }
        }
        if (!requiresRewrite) {
            return s;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append(s, 0, n);
        for (; n < s.length(); n++) {
            final char ch = s.charAt(n);
            if (isLineBreak(ch))
                buf.append(' ');
            else
                buf.append(ch);
        }
        return buf.toString();
    }

    static void writeField(final MimeField field, final OutputStream out) throws IOException {
        writeBytes(stripLineBreaks(field.getName()), out);
        writeBytes(FIELD_SEP, out);
        writeBytes(stripLineBreaks(field.getBody()), out);
        writeBytes(CR_LF, out);
    }

    static void writeField(final MimeField field, final Charset charset, final OutputStream out) throws IOException {
        writeBytes(stripLineBreaks(field.getName()), charset, out);
        writeBytes(FIELD_SEP, out);
        writeBytes(stripLineBreaks(field.getBody()), charset, out);
        writeBytes(CR_LF, out);
    }

    static final ByteArrayBuffer FIELD_SEP = encode(StandardCharsets.ISO_8859_1, ": ");
    static final ByteArrayBuffer CR_LF = encode(StandardCharsets.ISO_8859_1, "\r\n");
    static final ByteArrayBuffer TWO_HYPHENS = encode(StandardCharsets.ISO_8859_1, "--");

    final Charset charset;
    final String boundary;

    public AbstractMultipartFormat(final Charset charset, final String boundary) {
        super();
        Args.notNull(boundary, "Multipart boundary");
        this.charset = charset != null ? charset : StandardCharsets.ISO_8859_1;
        this.boundary = boundary;
    }

    public AbstractMultipartFormat(final String boundary) {
        this(null, boundary);
    }

    /**
     * 子类负责提供具体的MultipartPart。
     */
    public abstract List<MultipartPart> getParts();

    void doWriteTo(final OutputStream out, final boolean writeContent) throws IOException {
        final ByteArrayBuffer boundaryEncoded = encode(this.charset, this.boundary);
        for (final MultipartPart part: getParts()) {
            writeBytes(TWO_HYPHENS, out);
            writeBytes(boundaryEncoded, out);
            writeBytes(CR_LF, out);

            formatMultipartHeader(part, out);

            writeBytes(CR_LF, out);

            if (writeContent)
                part.getBody().writeTo(out);
            writeBytes(CR_LF, out);
        }
        writeBytes(TWO_HYPHENS, out);
        writeBytes(boundaryEncoded, out);
        writeBytes(TWO_HYPHENS, out);
        writeBytes(CR_LF, out);
    }

    /**
     * 输出Header。
     */
    protected abstract void formatMultipartHeader(final MultipartPart part, final OutputStream out) throws IOException;

    public void writeTo(final OutputStream out) throws IOException {
        doWriteTo(out, true);
    }

    /**
     * 确定MultipartPart的总长度（即各部分的内容长度加上用于区分各部分所需的额外元素的长度）。
     * 如果该对象中包含的任何@{link BodyPart}属于长度未知的流式实体，则总长度同样未知。
     * 此方法仅缓冲少量数据以确定整个实体的总长度。各部分的内容不会被缓冲。
     * @return
     */
    public long getTotalLength() {
        long contentLen = 0;
        for (final MultipartPart part: getParts()) {
            final ContentBody body = part.getBody();
            final long len = body.getContentLength();
            if (len >= 0)
                contentLen += len;
            else
                return -1;
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            doWriteTo(out, false);
            final byte[] extra = out.toByteArray();
            return contentLen + extra.length;
        } catch (final IOException ex) {
            // Should never happen
            return -1;
        }
    }
}
