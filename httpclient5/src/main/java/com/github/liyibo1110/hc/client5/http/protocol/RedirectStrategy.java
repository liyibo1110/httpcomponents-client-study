package com.github.liyibo1110.hc.client5.http.protocol;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.HttpResponse;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

import java.net.URI;

/**
 * 一种策略，用于根据从目标服务器接收到的HTTP响应，确定是否应将HTTP请求重定向到新位置。
 * 此接口的实现必须是线程安全的。由于此接口的方法可能由多个线程执行，因此必须对共享数据的访问进行同步。
 * @author liyibo
 * @date 2026-04-15 16:38
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface RedirectStrategy {

    /**
     * 根据目标服务器的响应，确定是否应将请求重定向到新位置。
     */
    boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException;

    URI getLocationURI(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException;
}
