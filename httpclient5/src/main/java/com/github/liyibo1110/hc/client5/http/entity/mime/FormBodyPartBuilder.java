package com.github.liyibo1110.hc.client5.http.entity.mime;

import com.github.liyibo1110.hc.core5.http.ContentType;
import com.github.liyibo1110.hc.core5.http.NameValuePair;
import com.github.liyibo1110.hc.core5.http.message.BasicNameValuePair;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.Asserts;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成FormBodyPart对象的工厂。
 * @author liyibo
 * @date 2026-04-16 10:33
 */
public class FormBodyPartBuilder {
    private String name;
    private ContentBody body;
    private final Header header;

    FormBodyPartBuilder(final String name, final ContentBody body) {
        this();
        this.name = name;
        this.body = body;
    }

    FormBodyPartBuilder() {
        this.header = new Header();
    }

    public static FormBodyPartBuilder create(final String name, final ContentBody body) {
        return new FormBodyPartBuilder(name, body);
    }

    public static FormBodyPartBuilder create() {
        return new FormBodyPartBuilder();
    }

    public FormBodyPartBuilder setName(final String name) {
        this.name = name;
        return this;
    }

    public FormBodyPartBuilder setBody(final ContentBody body) {
        this.body = body;
        return this;
    }

    public FormBodyPartBuilder addField(final String name, final String value, final List<NameValuePair> parameters) {
        Args.notNull(name, "Field name");
        this.header.addField(new MimeField(name, value, parameters));
        return this;
    }

    public FormBodyPartBuilder addField(final String name, final String value) {
        Args.notNull(name, "Field name");
        this.header.addField(new MimeField(name, value));
        return this;
    }

    public FormBodyPartBuilder setField(final String name, final String value) {
        Args.notNull(name, "Field name");
        this.header.setField(new MimeField(name, value));
        return this;
    }

    public FormBodyPartBuilder removeFields(final String name) {
        Args.notNull(name, "Field name");
        this.header.removeFields(name);
        return this;
    }

    public FormBodyPart build() {
        Asserts.notBlank(this.name, "Name");
        Asserts.notNull(this.body, "Content body");
        // 构造新的Header
        final Header headerCopy = new Header();
        final List<MimeField> fields = this.header.getFields();
        for (final MimeField field: fields)
            headerCopy.addField(field);

        if (headerCopy.getField(MimeConsts.CONTENT_DISPOSITION) == null) {
            final List<NameValuePair> fieldParameters = new ArrayList<>();
            fieldParameters.add(new BasicNameValuePair(MimeConsts.FIELD_PARAM_NAME, this.name));
            if (this.body.getFilename() != null)
                fieldParameters.add(new BasicNameValuePair(MimeConsts.FIELD_PARAM_FILENAME, this.body.getFilename()));

            headerCopy.addField(new MimeField(MimeConsts.CONTENT_DISPOSITION, "form-data", fieldParameters));
        }
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
        return new FormBodyPart(this.name, this.body, headerCopy);
    }
}
