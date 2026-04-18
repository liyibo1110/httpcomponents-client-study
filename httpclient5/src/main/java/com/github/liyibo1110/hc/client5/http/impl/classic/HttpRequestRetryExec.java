package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.HttpRequestRetryStrategy;
import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.classic.ExecChain;
import com.github.liyibo1110.hc.client5.http.classic.ExecChainHandler;
import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.NoHttpResponseException;
import com.github.liyibo1110.hc.core5.http.io.support.ClassicRequestBuilder;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.TimeValue;
import com.github.liyibo1110.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * @author liyibo
 * @date 2026-04-17 11:48
 */
@Contract(threading = ThreadingBehavior.STATELESS)
@Internal
public class HttpRequestRetryExec implements ExecChainHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestRetryExec.class);

    private final HttpRequestRetryStrategy retryStrategy;

    public HttpRequestRetryExec(final HttpRequestRetryStrategy retryStrategy) {
        Args.notNull(retryStrategy, "retryStrategy");
        this.retryStrategy = retryStrategy;
    }

    @Override
    public ClassicHttpResponse execute(final ClassicHttpRequest request, final ExecChain.Scope scope, final ExecChain chain)
            throws IOException, HttpException {
        Args.notNull(request, "request");
        Args.notNull(scope, "scope");
        final String exchangeId = scope.exchangeId;
        final HttpRoute route = scope.route;
        final HttpClientContext context = scope.clientContext;
        ClassicHttpRequest currentRequest = request;

        for (int execCount = 1;; execCount++) {
            final ClassicHttpResponse response;
            try {
                response = chain.proceed(currentRequest, scope);
            } catch (final IOException ex) {
                if (scope.execRuntime.isExecutionAborted())
                    throw new RequestFailedException("Request aborted");

                final HttpEntity requestEntity = request.getEntity();
                if (requestEntity != null && !requestEntity.isRepeatable()) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} cannot retry non-repeatable request", exchangeId);
                    throw ex;
                }
                if (retryStrategy.retryRequest(request, ex, execCount, context)) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} {}", exchangeId, ex.getMessage(), ex);
                    if (LOG.isInfoEnabled())
                        LOG.info("Recoverable I/O exception ({}) caught when processing request to {}", ex.getClass().getName(), route);

                    final TimeValue nextInterval = retryStrategy.getRetryInterval(request, ex, execCount, context);
                    if (TimeValue.isPositive(nextInterval)) {
                        try {
                            if (LOG.isDebugEnabled())
                                LOG.debug("{} wait for {}", exchangeId, nextInterval);
                            nextInterval.sleep();
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new InterruptedIOException();
                        }
                    }
                    currentRequest = ClassicRequestBuilder.copy(scope.originalRequest).build();
                    continue;
                } else {
                    if (ex instanceof NoHttpResponseException) {
                        final NoHttpResponseException updatedex = new NoHttpResponseException(
                                route.getTargetHost().toHostString() + " failed to respond");
                        updatedex.setStackTrace(ex.getStackTrace());
                        throw updatedex;
                    }
                    throw ex;
                }
            }

            try {
                final HttpEntity entity = request.getEntity();
                if (entity != null && !entity.isRepeatable()) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} cannot retry non-repeatable request", exchangeId);
                    return response;
                }
                if (retryStrategy.retryRequest(response, execCount, context)) {
                    final TimeValue nextInterval = retryStrategy.getRetryInterval(response, execCount, context);
                    // Make sure the retry interval does not exceed the response timeout
                    if (TimeValue.isPositive(nextInterval)) {
                        final RequestConfig requestConfig = context.getRequestConfig();
                        final Timeout responseTimeout = requestConfig.getResponseTimeout();
                        if (responseTimeout != null && nextInterval.compareTo(responseTimeout) > 0)
                            return response;
                    }
                    response.close();
                    if (TimeValue.isPositive(nextInterval)) {
                        try {
                            if (LOG.isDebugEnabled())
                                LOG.debug("{} wait for {}", exchangeId, nextInterval);
                            nextInterval.sleep();
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new InterruptedIOException();
                        }
                    }
                    currentRequest = ClassicRequestBuilder.copy(scope.originalRequest).build();
                } else {
                    return response;
                }
            } catch (final RuntimeException ex) {
                response.close();
                throw ex;
            }
        }
    }
}
