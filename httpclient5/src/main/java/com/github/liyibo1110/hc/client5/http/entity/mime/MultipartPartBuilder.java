package com.github.liyibo1110.hc.client5.http.entity.mime;

import com.github.liyibo1110.hc.core5.http.ContentType;
import com.github.liyibo1110.hc.core5.http.NameValuePair;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.Asserts;

import java.util.List;

/**
 * 生成MultipartPart对象的工厂。
 * @author liyibo
 * @date 2026-04-16 10:42
 */
public class MultipartPartBuilder {
    private ContentBody body;
    private final Header header;

    MultipartPartBuilder(final ContentBody body) {
        this();
        this.body = body;
    }

    MultipartPartBuilder() {
        this.header = new Header();
    }

    public static MultipartPartBuilder create(final ContentBody body) {
        return new MultipartPartBuilder(body);
    }

    public static MultipartPartBuilder create() {
        return new MultipartPartBuilder();
    }

    public MultipartPartBuilder setBody(final ContentBody body) {
        this.body = body;
        return this;
    }

    public MultipartPartBuilder addHeader(final String name, final String value, final List<NameValuePair> parameters) {
        Args.notNull(name, "Header name");
        this.header.addField(new MimeField(name, value, parameters));
        return this;
    }

    public MultipartPartBuilder addHeader(final String name, final String value) {
        Args.notNull(name, "Header name");
        this.header.addField(new MimeField(name, value));
        return this;
    }

    public MultipartPartBuilder setHeader(final String name, final String value) {
        Args.notNull(name, "Header name");
        this.header.setField(new MimeField(name, value));
        return this;
    }

    public MultipartPartBuilder removeHeaders(final String name) {
        Args.notNull(name, "Header name");
        this.header.removeFields(name);
        return this;
    }

    public MultipartPart build() {
        Asserts.notNull(this.body, "Content body");
        final Header headerCopy = new Header();
        final List<MimeField> fields = this.header.getFields();
        for (final MimeField field: fields)
            headerCopy.addField(field);

        if (headerCopy.getField(MimeConsts.CONTENT_TYPE) == null) {
            final ContentType contentType;
            if (body instanceof AbstractContentBody)
                contentType = ((AbstractContentBody) body).getContentType();
            else
                contentType = null;

            if (contentType != null) {
                headerCopy.addField(new MimeField(MimeConsts.CONTENT_TYPE, contentType.toString()));
            } else {
                final StringBuilder buffer = new StringBuilder();
                buffer.append(this.body.getMimeType()); // MimeType cannot be null
                if (this.body.getCharset() != null) { // charset may legitimately be null
                    buffer.append("; charset=");
                    buffer.append(this.body.getCharset());
                }
                headerCopy.addField(new MimeField(MimeConsts.CONTENT_TYPE, buffer.toString()));
            }
        }
        return new MultipartPart(this.body, headerCopy);
    }
}
