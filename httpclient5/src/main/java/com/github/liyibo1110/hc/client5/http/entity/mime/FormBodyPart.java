package com.github.liyibo1110.hc.client5.http.entity.mime;

import com.github.liyibo1110.hc.core5.util.Args;

/**
 * FormBodyPart类表示一个内容主体，可作为多部分编码实体的组成部分。
 * 该类会根据所包含主体的内容描述，自动在头部填充标准字段。
 * @author liyibo
 * @date 2026-04-16 10:32
 */
public class FormBodyPart extends MultipartPart {

    private final String name;

    FormBodyPart(final String name, final ContentBody body, final Header header) {
        super(body, header);
        Args.notNull(name, "Name");
        Args.notNull(body, "Body");
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void addField(final String name, final String value) {
        Args.notNull(name, "Field name");
        super.addField(name, value);
    }
}
