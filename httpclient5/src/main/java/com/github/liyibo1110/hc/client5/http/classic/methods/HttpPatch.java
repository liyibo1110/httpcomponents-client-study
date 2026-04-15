package com.github.liyibo1110.hc.client5.http.classic.methods;

import java.net.URI;

/**
 * HTTP PATCH请求。
 * @author liyibo
 * @date 2026-04-14 19:22
 */
public class HttpPatch extends HttpUriRequestBase {
    private static final long serialVersionUID = 1L;

    public final static String METHOD_NAME = "PATCH";

    public HttpPatch(final URI uri) {
        super(METHOD_NAME, uri);
    }

    public HttpPatch(final String uri) {
        this(URI.create(uri));
    }
}
