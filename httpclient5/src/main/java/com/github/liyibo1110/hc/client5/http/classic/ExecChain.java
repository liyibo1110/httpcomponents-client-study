package com.github.liyibo1110.hc.client5.http.classic;

import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.IOException;

/**
 * 表示client classic request执行链中的单个元素。
 * @author liyibo
 * @date 2026-04-16 18:18
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface ExecChain {

    final class Scope {
        public final String exchangeId;
        public final HttpRoute route;
        public final ClassicHttpRequest originalRequest;
        public final ExecRuntime execRuntime;
        public final HttpClientContext clientContext;

        public Scope(final String exchangeId, final HttpRoute route, final ClassicHttpRequest originalRequest, final ExecRuntime execRuntime, final HttpClientContext clientContext) {
            this.exchangeId = Args.notNull(exchangeId, "Exchange id");
            this.route = Args.notNull(route, "Route");
            this.originalRequest = Args.notNull(originalRequest, "Original request");
            this.execRuntime = Args.notNull(execRuntime, "Exec runtime");
            this.clientContext = clientContext != null ? clientContext : HttpClientContext.create();
        }
    }

    ClassicHttpResponse proceed(ClassicHttpRequest request, Scope scope) throws IOException, HttpException;
}
