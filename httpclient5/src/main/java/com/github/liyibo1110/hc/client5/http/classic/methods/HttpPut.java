package com.github.liyibo1110.hc.client5.http.classic.methods;

import java.net.URI;

/**
 * HTTP PUT请求。
 * @author liyibo
 * @date 2026-04-14 19:17
 */
public class HttpPut extends HttpUriRequestBase {
    private static final long serialVersionUID = 1L;

    public final static String METHOD_NAME = "PUT";

    public HttpPut(final URI uri) {
        super(METHOD_NAME, uri);
    }

    public HttpPut(final String uri) {
        this(URI.create(uri));
    }
}
