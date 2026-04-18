package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.core5.concurrent.Cancellable;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;

import java.util.concurrent.FutureTask;

/**
 * @author liyibo
 * @date 2026-04-17 14:33
 */
public class HttpRequestFutureTask<V> extends FutureTask<V> {
    private final ClassicHttpRequest request;
    private final HttpRequestTaskCallable<V> callable;

    HttpRequestFutureTask(final ClassicHttpRequest request, final HttpRequestTaskCallable<V> httpCallable) {
        super(httpCallable);
        this.request = request;
        this.callable = httpCallable;
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        callable.cancel();
        if (mayInterruptIfRunning && request instanceof Cancellable)
            ((Cancellable) request).cancel();
        return super.cancel(mayInterruptIfRunning);
    }

    public long scheduledTime() {
        return callable.getScheduled();
    }

    public long startedTime() {
        return callable.getStarted();
    }

    public long endedTime() {
        if (isDone())
            return callable.getEnded();
        else
            throw new IllegalStateException("Task is not done yet");
    }

    public long requestDuration() {
        if (isDone())
            return endedTime() - startedTime();
        else
            throw new IllegalStateException("Task is not done yet");
    }

    public long taskDuration() {
        if (isDone())
            return endedTime() - scheduledTime();
        else
            throw new IllegalStateException("Task is not done yet");
    }

    @Override
    public String toString() {
        return request.toString();
    }
}
