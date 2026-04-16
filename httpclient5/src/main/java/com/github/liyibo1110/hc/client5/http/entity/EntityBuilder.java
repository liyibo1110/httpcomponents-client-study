package com.github.liyibo1110.hc.client5.http.entity;

import com.github.liyibo1110.hc.core5.http.ContentType;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.NameValuePair;
import com.github.liyibo1110.hc.core5.http.io.entity.AbstractHttpEntity;
import com.github.liyibo1110.hc.core5.http.io.entity.ByteArrayEntity;
import com.github.liyibo1110.hc.core5.http.io.entity.FileEntity;
import com.github.liyibo1110.hc.core5.http.io.entity.InputStreamEntity;
import com.github.liyibo1110.hc.core5.http.io.entity.SerializableEntity;
import com.github.liyibo1110.hc.core5.http.io.entity.StringEntity;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * HttpEntity实例的构建器。
 * 该构建器的若干设置方法具有互斥性。如果多次调用以下方法，仅最后一次调用生效：
 * <ul>
 *   <li>{@link #setText(String)}</li>
 *   <li>{@link #setBinary(byte[])}</li>
 *   <li>{@link #setStream(java.io.InputStream)}</li>
 *   <li>{@link #setSerializable(java.io.Serializable)}</li>
 *   <li>{@link #setParameters(java.util.List)}</li>
 *   <li>{@link #setParameters(NameValuePair...)}</li>
 *   <li>{@link #setFile(java.io.File)}</li>
 * </ul>
 * @author liyibo
 * @date 2026-04-15 18:16
 */
public class EntityBuilder {
    private String text;
    private byte[] binary;
    private InputStream stream;
    private List<NameValuePair> parameters;
    private Serializable serializable;
    private File file;
    private ContentType contentType;
    private String contentEncoding;
    private boolean chunked;
    private boolean gzipCompressed;

    EntityBuilder() {
        super();
    }

    public static EntityBuilder create() {
        return new EntityBuilder();
    }

    private void clearContent() {
        this.text = null;
        this.binary = null;
        this.stream = null;
        this.parameters = null;
        this.serializable = null;
        this.file = null;
    }

    public String getText() {
        return text;
    }

    public EntityBuilder setText(final String text) {
        clearContent();
        this.text = text;
        return this;
    }

    public byte[] getBinary() {
        return binary;
    }

    public EntityBuilder setBinary(final byte[] binary) {
        clearContent();
        this.binary = binary;
        return this;
    }

    public InputStream getStream() {
        return stream;
    }

    public EntityBuilder setStream(final InputStream stream) {
        clearContent();
        this.stream = stream;
        return this;
    }

    public List<NameValuePair> getParameters() {
        return parameters;
    }

    public EntityBuilder setParameters(final List<NameValuePair> parameters) {
        clearContent();
        this.parameters = parameters;
        return this;
    }

    public EntityBuilder setParameters(final NameValuePair... parameters) {
        return setParameters(Arrays.asList(parameters));
    }

    public Serializable getSerializable() {
        return serializable;
    }

    public EntityBuilder setSerializable(final Serializable serializable) {
        clearContent();
        this.serializable = serializable;
        return this;
    }

    public File getFile() {
        return file;
    }

    public EntityBuilder setFile(final File file) {
        clearContent();
        this.file = file;
        return this;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public EntityBuilder setContentType(final ContentType contentType) {
        this.contentType = contentType;
        return this;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public EntityBuilder setContentEncoding(final String contentEncoding) {
        this.contentEncoding = contentEncoding;
        return this;
    }

    public boolean isChunked() {
        return chunked;
    }

    public EntityBuilder chunked() {
        this.chunked = true;
        return this;
    }

    public boolean isGzipCompressed() {
        return gzipCompressed;
    }

    public EntityBuilder gzipCompressed() {
        this.gzipCompressed = true;
        return this;
    }

    private ContentType getContentOrDefault(final ContentType def) {
        return this.contentType != null ? this.contentType : def;
    }

    public HttpEntity build() {
        final AbstractHttpEntity e;
        if (this.text != null)
            e = new StringEntity(this.text, getContentOrDefault(ContentType.DEFAULT_TEXT), this.contentEncoding, this.chunked);
        else if (this.binary != null)
            e = new ByteArrayEntity(this.binary, getContentOrDefault(ContentType.DEFAULT_BINARY), this.contentEncoding, this.chunked);
        else if (this.stream != null)
            e = new InputStreamEntity(this.stream, -1, getContentOrDefault(ContentType.DEFAULT_BINARY), this.contentEncoding);
        else if (this.parameters != null)
            e = new UrlEncodedFormEntity(this.parameters, this.contentType != null ? this.contentType.getCharset() : null);
        else if (this.serializable != null)
            e = new SerializableEntity(this.serializable, ContentType.DEFAULT_BINARY, this.contentEncoding);
        else if (this.file != null)
            e = new FileEntity(this.file, getContentOrDefault(ContentType.DEFAULT_BINARY), this.contentEncoding);
        else
            throw new IllegalStateException("No entity set");

        if (this.gzipCompressed)
            return new GzipCompressingEntity(e);
        return e;
    }
}
