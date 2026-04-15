package com.github.liyibo1110.hc.client5.http.classic.methods;

import java.net.URI;

/**
 * HTTP GET请求。
 * @author liyibo
 * @date 2026-04-14 18:14
 */
public class HttpGet extends HttpUriRequestBase {
    private static final long serialVersionUID = 1L;

    public final static String METHOD_NAME = "GET";

    public HttpGet(final URI uri) {
        super(METHOD_NAME, uri);
    }

    public HttpGet(final String uri) {
        this(URI.create(uri));
    }
}
