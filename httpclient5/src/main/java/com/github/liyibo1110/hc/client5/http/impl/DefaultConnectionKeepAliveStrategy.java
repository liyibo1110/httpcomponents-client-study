package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.client5.http.ConnectionKeepAliveStrategy;
import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HeaderElement;
import com.github.liyibo1110.hc.core5.http.HeaderElements;
import com.github.liyibo1110.hc.core5.http.HttpResponse;
import com.github.liyibo1110.hc.core5.http.message.MessageSupport;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.TimeValue;

import java.util.Iterator;

/**
 * 决定连接可处于空闲状态时长的策略的默认实现。
 * 该默认实现仅关注Keep-Alive标头的超时标记。
 * @author liyibo
 * @date 2026-04-16 17:02
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class DefaultConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {

    public static final DefaultConnectionKeepAliveStrategy INSTANCE = new DefaultConnectionKeepAliveStrategy();

    @Override
    public TimeValue getKeepAliveDuration(final HttpResponse response, final HttpContext context) {
        Args.notNull(response, "HTTP response");
        // 先尝试从keep-alive的header上找timeout值
        final Iterator<HeaderElement> it = MessageSupport.iterate(response, HeaderElements.KEEP_ALIVE);
        while (it.hasNext()) {
            final HeaderElement he = it.next();
            final String param = he.getName();
            final String value = he.getValue();
            if (value != null && param.equalsIgnoreCase("timeout")) {
                try {
                    return TimeValue.ofSeconds(Long.parseLong(value));
                } catch(final NumberFormatException ignore) {

                }
            }
        }
        // header上找不到就用RequestConfig里面的配置值
        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final RequestConfig requestConfig = clientContext.getRequestConfig();
        return requestConfig.getConnectionKeepAlive();
    }
}
