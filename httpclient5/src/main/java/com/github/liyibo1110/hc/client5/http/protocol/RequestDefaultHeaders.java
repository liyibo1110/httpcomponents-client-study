package com.github.liyibo1110.hc.client5.http.protocol;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.EntityDetails;
import com.github.liyibo1110.hc.core5.http.Header;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.HttpRequestInterceptor;
import com.github.liyibo1110.hc.core5.http.Method;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.IOException;
import java.util.Collection;

/**
 * 添加默认请求头部的请求拦截器（注意这个要增加的header，是在构造时动态传进来的）。
 * @author liyibo
 * @date 2026-04-15 17:12
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class RequestDefaultHeaders implements HttpRequestInterceptor {
    public static final RequestDefaultHeaders INSTANCE = new RequestDefaultHeaders();

    private final Collection<? extends Header> defaultHeaders;

    public RequestDefaultHeaders(final Collection<? extends Header> defaultHeaders) {
        super();
        this.defaultHeaders = defaultHeaders;
    }

    public RequestDefaultHeaders() {
        this(null);
    }

    @Override
    public void process(final HttpRequest request, final EntityDetails entity, final HttpContext context) throws HttpException, IOException {
        Args.notNull(request, "HTTP request");

        final String method = request.getMethod();
        if (Method.CONNECT.isSame(method))
            return;

        if (this.defaultHeaders != null) {
            for (final Header defHeader : this.defaultHeaders) {
                if(!request.containsHeader(defHeader.getName()))
                    request.addHeader(defHeader);
            }
        }
    }
}
