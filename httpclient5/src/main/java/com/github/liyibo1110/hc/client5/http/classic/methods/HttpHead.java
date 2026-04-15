package com.github.liyibo1110.hc.client5.http.classic.methods;

import java.net.URI;

/**
 * HTTP HEAD请求。
 * @author liyibo
 * @date 2026-04-14 19:18
 */
public class HttpHead extends HttpUriRequestBase {
    private static final long serialVersionUID = 1L;

    public final static String METHOD_NAME = "HEAD";

    public HttpHead(final URI uri) {
        super(METHOD_NAME, uri);
    }

    public HttpHead(final String uri) {
        this(URI.create(uri));
    }
}
