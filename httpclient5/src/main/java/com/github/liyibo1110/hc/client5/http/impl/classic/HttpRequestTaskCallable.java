package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.classic.HttpClient;
import com.github.liyibo1110.hc.core5.concurrent.FutureCallback;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.io.HttpClientResponseHandler;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author liyibo
 * @date 2026-04-17 14:34
 */
class HttpRequestTaskCallable<V> implements Callable<V> {
    private final ClassicHttpRequest request;
    private final HttpClient httpclient;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private final long scheduled = System.currentTimeMillis();
    private long started = -1;
    private long ended = -1;

    private final HttpContext context;
    private final HttpClientResponseHandler<V> responseHandler;
    private final FutureCallback<V> callback;

    private final FutureRequestExecutionMetrics metrics;

    HttpRequestTaskCallable(final HttpClient httpClient,
                            final ClassicHttpRequest request,
                            final HttpContext context,
                            final HttpClientResponseHandler<V> responseHandler,
                            final FutureCallback<V> callback,
                            final FutureRequestExecutionMetrics metrics) {
        this.httpclient = httpClient;
        this.responseHandler = responseHandler;
        this.request = request;
        this.context = context;
        this.callback = callback;
        this.metrics = metrics;
    }

    public long getScheduled() {
        return scheduled;
    }

    public long getStarted() {
        return started;
    }

    public long getEnded() {
        return ended;
    }

    @Override
    public V call() throws Exception {
        if (!cancelled.get()) {
            try {
                metrics.getActiveConnections().incrementAndGet();
                started = System.currentTimeMillis();
                try {
                    metrics.getScheduledConnections().decrementAndGet();
                    final V result = httpclient.execute(request, context, responseHandler);
                    ended = System.currentTimeMillis();
                    metrics.getSuccessfulConnections().increment(started);
                    if (callback != null)
                        callback.completed(result);
                    return result;
                } catch (final Exception e) {
                    metrics.getFailedConnections().increment(started);
                    ended = System.currentTimeMillis();
                    if (callback != null)
                        callback.failed(e);
                    throw e;
                }
            } finally {
                metrics.getRequests().increment(started);
                metrics.getTasks().increment(started);
                metrics.getActiveConnections().decrementAndGet();
            }
        }
    }

    public void cancel() {
        cancelled.set(true);
        if (callback != null) {
            callback.cancelled();
        }
    }
}
