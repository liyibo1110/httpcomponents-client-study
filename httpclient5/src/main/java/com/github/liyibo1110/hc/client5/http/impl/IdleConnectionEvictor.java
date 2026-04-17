package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.concurrent.DefaultThreadFactory;
import com.github.liyibo1110.hc.core5.pool.ConnPoolControl;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.TimeValue;
import com.github.liyibo1110.hc.core5.util.Timeout;

import java.util.concurrent.ThreadFactory;

/**
 * 该类维护一个后台线程，用于对连接池中已过期或处于空闲状态的持久连接执行驱逐策略。
 * @author liyibo
 * @date 2026-04-16 17:26
 */
@Contract(threading = ThreadingBehavior.SAFE_CONDITIONAL)
public class IdleConnectionEvictor {
    private final ThreadFactory threadFactory;
    private final Thread thread;

    public IdleConnectionEvictor(final ConnPoolControl<?> connectionManager, final ThreadFactory threadFactory,
                                 final TimeValue sleepTime, final TimeValue maxIdleTime) {
        Args.notNull(connectionManager, "Connection manager");
        this.threadFactory = threadFactory != null ? threadFactory : new DefaultThreadFactory("idle-connection-evictor", true);
        final TimeValue localSleepTime = sleepTime != null ? sleepTime : TimeValue.ofSeconds(5);
        this.thread = this.threadFactory.newThread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    localSleepTime.sleep();
                    connectionManager.closeExpired();
                    if (maxIdleTime != null)
                        connectionManager.closeIdle(maxIdleTime);
                }
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (final Exception ex) {

            }
        });
    }

    public IdleConnectionEvictor(final ConnPoolControl<?> connectionManager, final TimeValue sleepTime, final TimeValue maxIdleTime) {
        this(connectionManager, null, sleepTime, maxIdleTime);
    }

    public IdleConnectionEvictor(final ConnPoolControl<?> connectionManager, final TimeValue maxIdleTime) {
        this(connectionManager, null, maxIdleTime, maxIdleTime);
    }

    public void start() {
        thread.start();
    }

    public void shutdown() {
        thread.interrupt();
    }

    public boolean isRunning() {
        return thread.isAlive();
    }

    public void awaitTermination(final Timeout timeout) throws InterruptedException {
        thread.join(timeout != null ? timeout.toMilliseconds() : Long.MAX_VALUE);
    }
}
