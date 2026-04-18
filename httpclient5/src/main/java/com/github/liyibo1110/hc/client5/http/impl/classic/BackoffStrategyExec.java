package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.classic.BackoffManager;
import com.github.liyibo1110.hc.client5.http.classic.ConnectionBackoffStrategy;
import com.github.liyibo1110.hc.client5.http.classic.ExecChain;
import com.github.liyibo1110.hc.client5.http.classic.ExecChainHandler;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.Experimental;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.util.Args;

import java.io.IOException;

/**
 * @author liyibo
 * @date 2026-04-17 12:08
 */
@Contract(threading = ThreadingBehavior.STATELESS)
@Experimental
public class BackoffStrategyExec implements ExecChainHandler {

    private final ConnectionBackoffStrategy connectionBackoffStrategy;
    private final BackoffManager backoffManager;

    public BackoffStrategyExec(final ConnectionBackoffStrategy connectionBackoffStrategy,
                               final BackoffManager backoffManager) {
        super();
        Args.notNull(connectionBackoffStrategy, "Connection backoff strategy");
        Args.notNull(backoffManager, "Backoff manager");
        this.connectionBackoffStrategy = connectionBackoffStrategy;
        this.backoffManager = backoffManager;
    }

    @Override
    public ClassicHttpResponse execute(final ClassicHttpRequest request, final ExecChain.Scope scope, final ExecChain chain)
            throws IOException, HttpException {
        Args.notNull(request, "HTTP request");
        Args.notNull(scope, "Scope");
        final HttpRoute route = scope.route;

        final ClassicHttpResponse response;
        try {
            response = chain.proceed(request, scope);
        } catch (final IOException | HttpException ex) {
            if (this.connectionBackoffStrategy.shouldBackoff(ex))
                this.backoffManager.backOff(route);
            throw ex;
        }
        if (this.connectionBackoffStrategy.shouldBackoff(response))
            this.backoffManager.backOff(route);
        else
            this.backoffManager.probe(route);

        return response;
    }
}
