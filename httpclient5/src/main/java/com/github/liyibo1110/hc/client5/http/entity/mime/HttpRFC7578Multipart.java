package com.github.liyibo1110.hc.client5.http.entity.mime;

import com.github.liyibo1110.hc.core5.http.NameValuePair;
import com.github.liyibo1110.hc.core5.util.ByteArrayBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.List;

/**
 * @author liyibo
 * @date 2026-04-16 10:38
 */
class HttpRFC7578Multipart extends AbstractMultipartFormat {
    private static final PercentCodec PERCENT_CODEC = new PercentCodec();

    private final List<MultipartPart> parts;

    public HttpRFC7578Multipart(final Charset charset, final String boundary, final List<MultipartPart> parts) {
        super(charset, boundary);
        this.parts = parts;
    }

    @Override
    public List<MultipartPart> getParts() {
        return parts;
    }

    @Override
    protected void formatMultipartHeader(final MultipartPart part, final OutputStream out) throws IOException {
        for (final MimeField field: part.getHeader()) {
            if (MimeConsts.CONTENT_DISPOSITION.equalsIgnoreCase(field.getName())) {
                writeBytes(field.getName(), charset, out);
                writeBytes(FIELD_SEP, out);
                writeBytes(field.getValue(), out);
                final List<NameValuePair> parameters = field.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    final NameValuePair parameter = parameters.get(i);
                    final String name = parameter.getName();
                    final String value = parameter.getValue();
                    writeBytes("; ", out);
                    writeBytes(name, out);
                    writeBytes("=\"", out);
                    if (value != null) {
                        if (name.equalsIgnoreCase(MimeConsts.FIELD_PARAM_FILENAME))
                            out.write(PERCENT_CODEC.encode(value.getBytes(charset)));
                        else
                            writeBytes(value, out);
                    }
                    writeBytes("\"", out);
                }
                writeBytes(CR_LF, out);
            } else {
                writeField(field, charset, out);
            }
        }
    }

    static class PercentCodec {
        private static final byte ESCAPE_CHAR = '%';

        private static final BitSet ALWAYSENCODECHARS = new BitSet();

        static {
            ALWAYSENCODECHARS.set(' ');
            ALWAYSENCODECHARS.set('%');
        }

        public byte[] encode(final byte[] bytes) {
            if (bytes == null)
                return null;

            final CharsetEncoder characterSetEncoder = StandardCharsets.US_ASCII.newEncoder();
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            for (final byte c : bytes) {
                int b = c;
                if (b < 0)
                    b = 256 + b;

                if (characterSetEncoder.canEncode((char) b) && !ALWAYSENCODECHARS.get(c)) {
                    buffer.write(b);
                } else {
                    buffer.write(ESCAPE_CHAR);
                    final char hex1 = hexDigit(b >> 4);
                    final char hex2 = hexDigit(b);
                    buffer.write(hex1);
                    buffer.write(hex2);
                }
            }
            return buffer.toByteArray();
        }

        public byte[] decode(final byte[] bytes) {
            if (bytes == null)
                return null;

            final ByteArrayBuffer buffer = new ByteArrayBuffer(bytes.length);
            for (int i = 0; i < bytes.length; i++) {
                final int b = bytes[i];
                if (b == ESCAPE_CHAR) {
                    if (i >= bytes.length - 2)
                        throw new IllegalArgumentException("Invalid encoding: too short");

                    final int u = digit16(bytes[++i]);
                    final int l = digit16(bytes[++i]);
                    buffer.append((char) ((u << 4) + l));
                } else {
                    buffer.append(b);
                }
            }
            return buffer.toByteArray();
        }
    }

    private static final int RADIX = 16;

    static int digit16(final byte b) {
        final int i = Character.digit((char) b, RADIX);
        if (i == -1)
            throw new IllegalArgumentException("Invalid encoding: not a valid digit (radix " + RADIX + "): " + b);
        return i;
    }

    static char hexDigit(final int b) {
        return Character.toUpperCase(Character.forDigit(b & 0xF, RADIX));
    }
}
