package com.github.liyibo1110.hc.client5.http.protocol;

import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.EntityDetails;
import com.github.liyibo1110.hc.core5.http.HeaderElements;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHeaders;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.HttpRequestInterceptor;
import com.github.liyibo1110.hc.core5.http.HttpVersion;
import com.github.liyibo1110.hc.core5.http.ProtocolVersion;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.IOException;

/**
 * RequestExpectContinue负责通过添加Expect头部来启用expect-continue握手。
 * 该拦截器会参考RequestConfig.isExpectContinueEnabled()的设置。
 * @author liyibo
 * @date 2026-04-15 17:08
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class RequestExpectContinue implements HttpRequestInterceptor {

    public RequestExpectContinue() {
        super();
    }

    @Override
    public void process(final HttpRequest request, final EntityDetails entity, final HttpContext context) throws HttpException, IOException {
        Args.notNull(request, "HTTP request");

        // 如果没有Expect的header信息，则尝试添加
        if (!request.containsHeader(HttpHeaders.EXPECT)) {
            final ProtocolVersion version = request.getVersion() != null ? request.getVersion() : HttpVersion.HTTP_1_1;
            // 有entity，并且HTTP协议版本要大于1.0
            if (entity != null && entity.getContentLength() != 0 && !version.lessEquals(HttpVersion.HTTP_1_0)) {
                final HttpClientContext clientContext = HttpClientContext.adapt(context);
                final RequestConfig config = clientContext.getRequestConfig();
                if (config.isExpectContinueEnabled())
                    request.addHeader(HttpHeaders.EXPECT, HeaderElements.CONTINUE);
            }
        }
    }
}
