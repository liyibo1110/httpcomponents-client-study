package com.github.liyibo1110.hc.client5.http.entity.mime;

/**
 * MultipartPart类表示一个内容主体，该主体可作为多部分编码实体的组成部分。
 * 该类会根据所包含主体的内容描述，自动在头部填充标准字段。
 * @author liyibo
 * @date 2026-04-15 22:34
 */
public class MultipartPart {
    private final Header header;
    private final ContentBody body;

    MultipartPart(final ContentBody body, final Header header) {
        super();
        this.header = header != null ? header : new Header();
        this.body = body;
    }

    public ContentBody getBody() {
        return this.body;
    }

    public Header getHeader() {
        return this.header;
    }

    void addField(final String name, final String value) {
        addField(new MimeField(name, value));
    }

    void addField(final MimeField field) {
        this.header.addField(field);
    }
}
