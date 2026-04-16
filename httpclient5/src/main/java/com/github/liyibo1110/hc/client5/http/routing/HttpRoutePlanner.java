package com.github.liyibo1110.hc.client5.http.routing;

import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

/**
 * 封装了用于计算目标主机的HttpRoute的逻辑。实现可以基于参数，也可以基于标准的Java系统属性。
 * 此接口的实现必须是线程安全的。由于此接口的方法可能由多个线程执行，因此对共享数据的访问必须进行同步。
 * @author liyibo
 * @date 2026-04-15 11:00
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface HttpRoutePlanner {

    HttpRoute determineRoute(HttpHost target, HttpContext context) throws HttpException;
}
