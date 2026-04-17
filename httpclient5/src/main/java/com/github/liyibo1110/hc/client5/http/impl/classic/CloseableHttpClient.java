package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.ClientProtocolException;
import com.github.liyibo1110.hc.client5.http.classic.HttpClient;
import com.github.liyibo1110.hc.client5.http.routing.RoutingSupport;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.io.HttpClientResponseHandler;
import com.github.liyibo1110.hc.core5.http.io.entity.EntityUtils;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.io.ModalCloseable;
import com.github.liyibo1110.hc.core5.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * HttpClient接口的基础实现。
 * @author liyibo
 * @date 2026-04-16 18:00
 */
public abstract class CloseableHttpClient implements HttpClient, ModalCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CloseableHttpClient.class);

    protected abstract CloseableHttpResponse doExecute(HttpHost target, ClassicHttpRequest request, HttpContext context) throws IOException;

    private static HttpHost determineTarget(final ClassicHttpRequest request) throws ClientProtocolException {
        try {
            return RoutingSupport.determineHost(request);
        } catch (final HttpException ex) {
            throw new ClientProtocolException(ex);
        }
    }

    @Deprecated
    @Override
    public CloseableHttpResponse execute(final HttpHost target, final ClassicHttpRequest request, final HttpContext context) throws IOException {
        return doExecute(target, request, context);
    }

    @Deprecated
    @Override
    public CloseableHttpResponse execute(final ClassicHttpRequest request, final HttpContext context) throws IOException {
        Args.notNull(request, "HTTP request");
        return doExecute(determineTarget(request), request, context);
    }

    @Deprecated
    @Override
    public CloseableHttpResponse execute(final ClassicHttpRequest request) throws IOException {
        return doExecute(determineTarget(request), request, null);
    }

    @Deprecated
    @Override
    public CloseableHttpResponse execute(final HttpHost target, final ClassicHttpRequest request) throws IOException {
        return doExecute(target, request, null);
    }

    @Override
    public <T> T execute(final ClassicHttpRequest request, final HttpClientResponseHandler<? extends T> responseHandler) throws IOException {
        return execute(request, null, responseHandler);
    }

    @Override
    public <T> T execute(final ClassicHttpRequest request, final HttpContext context, final HttpClientResponseHandler<? extends T> responseHandler)
            throws IOException {
        final HttpHost target = determineTarget(request);
        return execute(target, request, context, responseHandler);
    }

    @Override
    public <T> T execute(final HttpHost target, final ClassicHttpRequest request, final HttpClientResponseHandler<? extends T> responseHandler)
            throws IOException {
        return execute(target, request, null, responseHandler);
    }

    /**
     * 最新版本建议使用这种显式传入HttpClientResponseHandler的方法来进行http请求。
     * 用CloseableHttpResponse的版本是为了兼容4.x版本。
     */
    @Override
    public <T> T execute(final HttpHost target, final ClassicHttpRequest request, final HttpContext context, final HttpClientResponseHandler<? extends T> responseHandler)
            throws IOException {
        Args.notNull(responseHandler, "Response handler");

        try (final ClassicHttpResponse response = doExecute(target, request, context)) {
            try {
                final T result = responseHandler.handleResponse(response);
                final HttpEntity entity = response.getEntity();
                EntityUtils.consume(entity);
                return result;
            } catch (final HttpException t) {
                // Try to salvage the underlying connection in case of a protocol exception
                final HttpEntity entity = response.getEntity();
                try {
                    EntityUtils.consume(entity);
                } catch (final Exception t2) {
                    // Log this exception. The original exception is more
                    // important and will be thrown to the caller.
                    LOG.warn("Error consuming content after an exception.", t2);
                }
                throw new ClientProtocolException(t);
            }
        }
    }
}
