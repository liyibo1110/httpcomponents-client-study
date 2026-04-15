package com.github.liyibo1110.hc.client5.http.classic.methods;

import java.net.URI;

/**
 * HTTP POST请求。
 * @author liyibo
 * @date 2026-04-14 18:16
 */
public class HttpPost extends HttpUriRequestBase {
    private static final long serialVersionUID = 1L;

    public final static String METHOD_NAME = "POST";

    public HttpPost(final URI uri) {
        super(METHOD_NAME, uri);
    }

    public HttpPost(final String uri) {
        this(URI.create(uri));
    }
}
