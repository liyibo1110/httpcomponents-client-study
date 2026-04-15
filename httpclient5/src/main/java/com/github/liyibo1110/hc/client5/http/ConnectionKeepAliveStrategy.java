package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpResponse;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.TimeValue;

/**
 * 用于确定连接在被重用前可以处于空闲状态多长时间的接口。
 * 该接口的实现必须是线程安全的。由于该接口的方法可能由多个线程执行，因此必须对共享数据的访问进行同步。
 * @author liyibo
 * @date 2026-04-14 14:32
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface ConnectionKeepAliveStrategy {

    /**
     * 返回该连接可安全处于空闲状态的时间长度。如果连接处于空闲状态的时间超过此时间段，则该连接绝不能被重用。
     * 返回值为0或更小，表示没有合适的建议。
     * 当与org.apache.hc.core5.http.ConnectionReuseStrategy配合使用时，如果org.apache.hc.core5.http.ConnectionReuseStrategy.keepAlive(org.apache.hc.core5.http.HttpRequest, HttpResponse, HttpContext)返回 true，则可通过此方法控制连接复用的持续时间。
     * 如果keepAlive返回false，则此方法不应产生实质性影响。
     */
    TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context);
}
