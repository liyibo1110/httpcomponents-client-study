package com.github.liyibo1110.hc.client5.http.classic.methods;

import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.core5.concurrent.Cancellable;
import com.github.liyibo1110.hc.core5.concurrent.CancellableDependency;
import com.github.liyibo1110.hc.core5.http.message.BasicClassicHttpRequest;

import java.net.URI;
import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * @author liyibo
 * @date 2026-04-14 17:58
 */
public class HttpUriRequestBase extends BasicClassicHttpRequest implements HttpUriRequest, CancellableDependency {
    private static final long serialVersionUID = 1L;

    /** 依赖的Cancellable对象，这个状态为true说明HttpUriRequestBase本身也cancel了 */
    private final AtomicMarkableReference<Cancellable> cancellableRef;
    private RequestConfig requestConfig;

    public HttpUriRequestBase(final String method, final URI requestUri) {
        super(method, requestUri);
        this.cancellableRef = new AtomicMarkableReference<>(null, false);
    }

    @Override
    public boolean cancel() {
        // 依赖Cancellable如果没有cancel，就进循环
        while (!cancellableRef.isMarked()) {
            final Cancellable actualCancellable = cancellableRef.getReference();
            if(cancellableRef.compareAndSet(actualCancellable, actualCancellable, false, true)) {
                if(actualCancellable != null)
                    actualCancellable.cancel();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        return cancellableRef.isMarked();
    }

    @Override
    public void setDependency(final Cancellable cancellable) {
        final Cancellable actualCancellable = cancellableRef.getReference();
        // 注意这里如果CAS失败了，要调用一下传入Cancellable的cancel方法
        if (!cancellableRef.compareAndSet(actualCancellable, cancellable, false, false))
            cancellable.cancel();
    }

    /**
     * 重置请求的内部状态，使其可重复使用。
     */
    public void reset() {
        while (true) {
            final boolean marked = cancellableRef.isMarked();
            final Cancellable actualCancellable = cancellableRef.getReference();
            // 先尝试cancel，然后把cancellableRef置空
            if (actualCancellable != null)
                actualCancellable.cancel();
            if (cancellableRef.compareAndSet(actualCancellable, null, marked, false))
                break;
        }
    }

    @Override
    public void abort() throws UnsupportedOperationException {
        cancel();
    }

    @Override
    public boolean isAborted() {
        return isCancelled();
    }

    public void setConfig(final RequestConfig requestConfig) {
        this.requestConfig = requestConfig;
    }

    @Override
    public RequestConfig getConfig() {
        return requestConfig;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getMethod()).append(" ").append(getRequestUri());
        return sb.toString();
    }
}
