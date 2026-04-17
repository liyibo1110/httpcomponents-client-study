package com.github.liyibo1110.hc.client5.http.classic;

import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.concurrent.CancellableDependency;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.util.TimeValue;

import java.io.IOException;

/**
 * 一个执行运行时，用于访问底层连接端点并协助管理其生命周期。
 * 该接口被视为内部接口，通常不应由自定义请求执行处理程序使用或访问。
 * @author liyibo
 * @date 2026-04-16 18:11
 */
@Internal
public interface ExecRuntime {

    /**
     * 确定请求的执行已被中止。
     */
    boolean isExecutionAborted();

    /**
     * 已获取连接端点的详细信息。
     */
    boolean isEndpointAcquired();

    /**
     * 获取连接端点，端点可以从池中租用，也可以创建未连接的新端点。
     */
    void acquireEndpoint(String id, HttpRoute route, Object state, HttpClientContext context) throws IOException;

    /**
     * 释放已获取的端点，使其可能可供再次使用。
     */
    void releaseEndpoint();

    /**
     * 关闭并释放已获取的端点。
     */
    void discardEndpoint();

    /**
     * 确定该端点是否与初始跳点相连（如果是直连路由，则为连接目标；如果是通过一个或多个代理的路由，则为第一个代理跳点）。
     */
    boolean isEndpointConnected();

    /**
     * 将本地端点从连接路由中的初始跳点断开。
     */
    void disconnectEndpoint() throws IOException;

    /**
     * 将本地端点连接到初始跳点（如果是直连路由，则连接到连接目标；如果是通过代理或多个代理的路由，则连接到第一个代理跳点）。
     */
    void connectEndpoint(HttpClientContext context) throws IOException;

    /**
     * 通过使用TLS安全协议，增强当前连接的传输安全性。
     */
    void upgradeTls(HttpClientContext context) throws IOException;

    /**
     * 使用给定的上下文执行HTTP请求。
     */
    ClassicHttpResponse execute(String id, ClassicHttpRequest request, HttpClientContext context)
            throws IOException, HttpException;

    /**
     * 确定该连接是否可重复使用。
     */
    boolean isConnectionReusable();

    /**
     * 将连接标记为在给定时间段内可重复使用，如果提供了状态表示，则同时将其标记为有状态的。
     */
    void markConnectionReusable(Object state, TimeValue validityTime);

    /**
     * 将该连接标记为不可重复使用。
     */
    void markConnectionNonReusable();

    /**
     * 此runtime会进行fork，以实现并行执行。
     */
    ExecRuntime fork(CancellableDependency cancellableAware);
}
