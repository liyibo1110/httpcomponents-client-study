package com.github.liyibo1110.hc.client5.http.protocol;

import com.github.liyibo1110.hc.client5.http.RouteInfo;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.EntityDetails;
import com.github.liyibo1110.hc.core5.http.HeaderElements;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHeaders;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.HttpRequestInterceptor;
import com.github.liyibo1110.hc.core5.http.Method;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 该协议拦截器负责在发出的请求中添加Connection头部，这对管理HTTP/1.0连接的持久性至关重要。
 * @author liyibo
 * @date 2026-04-15 17:06
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class RequestClientConnControl implements HttpRequestInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(RequestClientConnControl.class);

    public RequestClientConnControl() {
        super();
    }

    @Override
    public void process(final HttpRequest request, final EntityDetails entity, final HttpContext context) throws HttpException, IOException {
        Args.notNull(request, "HTTP request");

        // 一些method不需要附带cookie
        final String method = request.getMethod();
        if (Method.CONNECT.isSame(method))
            return;

        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final String exchangeId = clientContext.getExchangeId();

        // 检查RouteInfo是否存在，没有直接退出
        final RouteInfo route = clientContext.getHttpRoute();
        if (route == null) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} Connection route not set in the context", exchangeId);
            return;
        }

        if (route.getHopCount() == 1 || route.isTunnelled()) {
            if (!request.containsHeader(HttpHeaders.CONNECTION))
                request.addHeader(HttpHeaders.CONNECTION, HeaderElements.KEEP_ALIVE);
        }
    }
}
