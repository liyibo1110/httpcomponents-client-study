package com.github.liyibo1110.hc.client5.http.io;

import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.io.ModalCloseable;
import com.github.liyibo1110.hc.core5.util.TimeValue;
import com.github.liyibo1110.hc.core5.util.Timeout;

import java.io.IOException;

/**
 * 表示持久客户端连接的manager。
 * HTTP connection manager的目的是作为新建HTTP连接的工厂，管理持久连接并同步对持久连接的访问，以确保每次只有一个执行线程可以访问一个连接。
 * 此接口的实现必须是线程安全的。由于此接口的方法可能由多个线程执行，因此必须对共享数据的访问进行同步。
 * @author liyibo
 * @date 2026-04-16 13:44
 */
public interface HttpClientConnectionManager extends ModalCloseable {

    /**
     * 返回一个LeaseRequest对象，该对象可用于获取ConnectionEndpoint，并通过调用LeaseRequest.cancel()来取消请求。
     * 请注意，新分配的端点可以在断开连接的状态下被租用。
     * 端点的消费者有责任通过调用connect(ConnectionEndpoint, TimeValue, HttpContext)方法来完全建立通往端点目标的路径，
     * 以便直接连接到目标或第一个代理跳点，并且在向所有中间代理跳点执行CONNECT方法后，
     * 可选地调用upgrade(ConnectionEndpoint, HttpContext)方法将底层传输升级为传输层安全（TLS）。
     */
    LeaseRequest lease(String id, HttpRoute route, Timeout requestTimeout, Object state);

    /**
     * 将端点释放回管理器，使其可能被其他消费者重复使用。
     * 此外，还可以通过validDuration和timeUnit参数来定义管理器应保持连接存活的最长时长。
     */
    void release(ConnectionEndpoint endpoint, Object newState, TimeValue validDuration);

    /**
     * 将端点连接到初始跳点（如果是直连路由，则连接到连接目标；如果是通过代理或多个代理的路由，则连接到第一个代理跳点）。
     */
    void connect(ConnectionEndpoint endpoint, TimeValue connectTimeout, HttpContext context) throws IOException;

    /**
     * 通过使用TLS安全协议，提升指定端点的传输安全性。
     */
    void upgrade(ConnectionEndpoint endpoint, HttpContext context) throws IOException;
}
