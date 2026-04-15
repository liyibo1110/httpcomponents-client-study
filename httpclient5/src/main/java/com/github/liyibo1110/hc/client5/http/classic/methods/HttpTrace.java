package com.github.liyibo1110.hc.client5.http.classic.methods;

import com.github.liyibo1110.hc.core5.http.HttpEntity;

import java.net.URI;

/**
 * HTTP TRACE请求。
 * @author liyibo
 * @date 2026-04-14 19:23
 */
public class HttpTrace extends HttpUriRequestBase {
    private static final long serialVersionUID = 1L;

    public final static String METHOD_NAME = "TRACE";

    public HttpTrace(final URI uri) {
        super(METHOD_NAME, uri);
    }

    public HttpTrace(final String uri) {
        this(URI.create(uri));
    }

    @Override
    public void setEntity(final HttpEntity entity) {
        throw new IllegalStateException(METHOD_NAME + " requests may not include an entity.");
    }
}
