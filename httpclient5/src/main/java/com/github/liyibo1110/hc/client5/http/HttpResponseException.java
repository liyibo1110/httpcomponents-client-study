package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.core5.util.TextUtils;

/**
 * 代表响应值非2xx的异常。
 * @author liyibo
 * @date 2026-04-14 13:44
 */
public class HttpResponseException extends ClientProtocolException {
    private static final long serialVersionUID = -7186627969477257933L;

    private final int statusCode;
    private final String reasonPhrase;

    public HttpResponseException(final int statusCode, final String reasonPhrase) {
        super(String.format("status code: %d" + (TextUtils.isBlank(reasonPhrase) ? "" : ", reason phrase: %s"), statusCode, reasonPhrase));
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getReasonPhrase() {
        return this.reasonPhrase;
    }
}

