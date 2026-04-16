package com.github.liyibo1110.hc.client5.http.entity.mime;

import com.github.liyibo1110.hc.core5.http.ContentType;
import com.github.liyibo1110.hc.core5.util.Args;

import java.nio.charset.Charset;

/**
 * @author liyibo
 * @date 2026-04-15 21:39
 */
public abstract class AbstractContentBody implements ContentBody {

    private final ContentType contentType;

    public AbstractContentBody(final ContentType contentType) {
        super();
        Args.notNull(contentType, "Content type");
        this.contentType = contentType;
    }

    public ContentType getContentType() {
        return this.contentType;
    }

    @Override
    public String getMimeType() {
        return this.contentType.getMimeType();
    }

    @Override
    public String getMediaType() {
        final String mimeType = this.contentType.getMimeType();
        // 尝试返回“/”字符前面的部分
        final int i = mimeType.indexOf('/');
        if (i != -1)
            return mimeType.substring(0, i);
        return mimeType;
    }

    @Override
    public String getSubType() {
        final String mimeType = this.contentType.getMimeType();
        // 尝试返回“/”字符后面的部分
        final int i = mimeType.indexOf('/');
        if (i != -1)
            return mimeType.substring(i + 1);
        return null;
    }

    @Override
    public String getCharset() {
        final Charset charset = this.contentType.getCharset();
        return charset != null ? charset.name() : null;
    }
}
