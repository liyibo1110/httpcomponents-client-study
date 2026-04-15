package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.HttpResponse;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.TimeValue;

import java.io.IOException;

/**
 * 一种策略接口，允许API用户接入自定义逻辑，以控制是否应自动重试、重试次数等。
 * @author liyibo
 * @date 2026-04-14 14:02
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface HttpRequestRetryStrategy {

    /**
     * 确定在执行过程中发生I/O异常后是否应重试该方法。
     */
    boolean retryRequest(HttpRequest request, IOException exception, int execCount, HttpContext context);

    /**
     * 根据目标服务器的响应，确定是否应重试该方法。
     */
    boolean retryRequest(HttpResponse response, int execCount, HttpContext context);

    /**
     * 确定两次重试之间的重试间隔。
     */
    default TimeValue getRetryInterval(HttpRequest request, IOException exception, int execCount, HttpContext context) {
        return TimeValue.ZERO_MILLISECONDS;
    }

    /**
     * 确定两次重试之间的重试间隔。
     */
    TimeValue getRetryInterval(HttpResponse response, int execCount, HttpContext context);
}
