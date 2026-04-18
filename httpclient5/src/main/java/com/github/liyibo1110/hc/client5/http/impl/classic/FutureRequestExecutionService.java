package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.classic.HttpClient;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.concurrent.FutureCallback;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.io.HttpClientResponseHandler;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 该类使用提供的ExecutorService将消息的执行和处理作为FutureTasks进行调度。
 * @author liyibo
 * @date 2026-04-17 14:49
 */
@Contract(threading = ThreadingBehavior.SAFE_CONDITIONAL)
public class FutureRequestExecutionService implements Closeable {
    private final HttpClient httpclient;
    private final ExecutorService executorService;
    private final FutureRequestExecutionMetrics metrics = new FutureRequestExecutionMetrics();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public FutureRequestExecutionService(final HttpClient httpclient, final ExecutorService executorService) {
        this.httpclient = httpclient;
        this.executorService = executorService;
    }

    public <T> FutureTask<T> execute(final ClassicHttpRequest request,
                                     final HttpContext context,
                                     final HttpClientResponseHandler<T> HttpClientResponseHandler) {
        return execute(request, context, HttpClientResponseHandler, null);
    }

    public <T> FutureTask<T> execute(final ClassicHttpRequest request,
                                     final HttpContext context,
                                     final HttpClientResponseHandler<T> HttpClientResponseHandler,
                                     final FutureCallback<T> callback) {
        if(closed.get())
            throw new IllegalStateException("Close has been called on this httpclient instance.");

        metrics.getScheduledConnections().incrementAndGet();
        final HttpRequestTaskCallable<T> callable = new HttpRequestTaskCallable<>(
                httpclient, request, context, HttpClientResponseHandler, callback, metrics);
        final HttpRequestFutureTask<T> httpRequestFutureTask = new HttpRequestFutureTask<>(request, callable);
        executorService.execute(httpRequestFutureTask);
        return httpRequestFutureTask;
    }

    public FutureRequestExecutionMetrics metrics() {
        return metrics;
    }

    @Override
    public void close() throws IOException {
        closed.set(true);
        executorService.shutdownNow();
        if (httpclient instanceof Closeable)
            ((Closeable) httpclient).close();
    }
}
