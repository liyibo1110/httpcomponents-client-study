package com.github.liyibo1110.hc.client5.http.entity.mime;

import com.github.liyibo1110.hc.core5.http.NameValuePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 类似这样的信息：Content-Disposition: form-data; name="title"
 * @author liyibo
 * @date 2026-04-15 22:26
 */
public class MimeField {
    private final String name;
    private final String value;
    private final List<NameValuePair> parameters;

    public MimeField(final String name, final String value) {
        super();
        this.name = name;
        this.value = value;
        this.parameters = Collections.emptyList();
    }

    public MimeField(final String name, final String value, final List<NameValuePair> parameters) {
        this.name = name;
        this.value = value;
        this.parameters = parameters != null
                ? Collections.unmodifiableList(new ArrayList<>(parameters))
                : Collections.emptyList();
    }

    public MimeField(final MimeField from) {
        this(from.name, from.value, from.parameters);
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public List<NameValuePair> getParameters() {
        return this.parameters;
    }

    public String getBody() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.value);
        for (int i = 0; i < this.parameters.size(); i++) {
            final NameValuePair parameter = this.parameters.get(i);
            sb.append("; ");
            sb.append(parameter.getName());
            sb.append("=\"");
            final String v = parameter.getValue();
            for (int n = 0; n < v.length(); n++) {
                final char ch = v.charAt(n);
                if (ch == '"' || ch == '\\' )
                    sb.append("\\");
                sb.append(ch);
            }
            sb.append("\"");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(this.name);
        buffer.append(": ");
        buffer.append(this.getBody());
        return buffer.toString();
    }
}
