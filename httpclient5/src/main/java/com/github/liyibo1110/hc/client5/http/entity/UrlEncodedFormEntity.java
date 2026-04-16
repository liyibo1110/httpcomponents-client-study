package com.github.liyibo1110.hc.client5.http.entity;

import com.github.liyibo1110.hc.core5.http.ContentType;
import com.github.liyibo1110.hc.core5.http.NameValuePair;
import com.github.liyibo1110.hc.core5.http.io.entity.StringEntity;
import com.github.liyibo1110.hc.core5.net.WWWFormCodec;

import java.nio.charset.Charset;
import java.util.List;

/**
 * 由一组URL编码的键值对组成的实体。这在发送HTTP POST请求时通常非常有用。
 * @author liyibo
 * @date 2026-04-15 18:06
 */
public class UrlEncodedFormEntity extends StringEntity {

    public UrlEncodedFormEntity(final Iterable<? extends NameValuePair> parameters, final Charset charset) {
        super(WWWFormCodec.format(parameters, charset != null ? charset : ContentType.APPLICATION_FORM_URLENCODED.getCharset()),
                charset != null ? ContentType.APPLICATION_FORM_URLENCODED.withCharset(charset) : ContentType.APPLICATION_FORM_URLENCODED);
    }

    public UrlEncodedFormEntity (final List<? extends NameValuePair> parameters){
        this(parameters, null);
    }

    public UrlEncodedFormEntity (final Iterable <? extends NameValuePair> parameters) {
        this(parameters, null);
    }
}
