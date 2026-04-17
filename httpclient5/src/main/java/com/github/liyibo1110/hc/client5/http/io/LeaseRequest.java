package com.github.liyibo1110.hc.client5.http.io;

import com.github.liyibo1110.hc.core5.concurrent.Cancellable;
import com.github.liyibo1110.hc.core5.util.Timeout;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 表示对一个由连接管理器管理其生命周期的ConnectionEndpoint的请求。
 * @author liyibo
 * @date 2026-04-16 13:21
 */
public interface LeaseRequest extends Cancellable {

    /**
     * 在给定时间内返回ConnectionEndpoint。
     * 该方法将阻塞，直到连接可用、超时结束或连接管理器关闭，超时处理精确到毫秒。
     * 如果在阻塞期间或开始前调用cancel()，将抛出InterruptedException。
     */
    ConnectionEndpoint get(Timeout timeout) throws InterruptedException, ExecutionException, TimeoutException;
}
