package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.core5.concurrent.Cancellable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 常见的cancellable操作。
 * @author liyibo
 * @date 2026-04-16 17:45
 */
public final class Operations {

    private final static Cancellable NOOP_CANCELLABLE = () -> false;

    /**
     * 该类表示处于已完成状态且结果固定的Future。
     * 该Future的结果无法更改，也无法取消。
     */
    public static class CompletedFuture<T> implements Future<T> {
        private final T result;

        public CompletedFuture(final T result) {
            this.result = result;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return result;
        }

        @Override
        public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return result;
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }
    }

    /**
     * 为正在进行的进程或无法取消的操作handle，创建一个可取消的操作。
     * 使用此handle尝试取消该操作将无效。
     */
    public static Cancellable nonCancellable() {
        return NOOP_CANCELLABLE;
    }

    /**
     * 为由Future表示的正在进行的进程或操作创建一个可取消的操作handle。
     */
    public static Cancellable cancellable(final Future<?> future) {
        if (future == null)
            return NOOP_CANCELLABLE;

        if (future instanceof Cancellable)
            return (Cancellable) future;

        return () -> future.cancel(true);
    }
}
