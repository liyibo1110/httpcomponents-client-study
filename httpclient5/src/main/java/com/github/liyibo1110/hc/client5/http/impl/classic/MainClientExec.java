package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.ConnectionKeepAliveStrategy;
import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.UserTokenHandler;
import com.github.liyibo1110.hc.client5.http.classic.ExecChain;
import com.github.liyibo1110.hc.client5.http.classic.ExecChainHandler;
import com.github.liyibo1110.hc.client5.http.classic.ExecRuntime;
import com.github.liyibo1110.hc.client5.http.impl.ConnectionShutdownException;
import com.github.liyibo1110.hc.client5.http.io.HttpClientConnectionManager;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.ConnectionReuseStrategy;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.message.RequestLine;
import com.github.liyibo1110.hc.core5.http.protocol.HttpCoreContext;
import com.github.liyibo1110.hc.core5.http.protocol.HttpProcessor;
import com.github.liyibo1110.hc.core5.io.CloseMode;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * 通常是classic request execution链中负责与对端执行请求/响应交互的最后一个请求执行处理程序
 * @author liyibo
 * @date 2026-04-17 9:59
 */
@Contract(threading = ThreadingBehavior.STATELESS)
@Internal
public class MainClientExec implements ExecChainHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MainClientExec.class);

    private final HttpClientConnectionManager connectionManager;
    private final HttpProcessor httpProcessor;
    private final ConnectionReuseStrategy reuseStrategy;
    private final ConnectionKeepAliveStrategy keepAliveStrategy;
    private final UserTokenHandler userTokenHandler;

    public MainClientExec(final HttpClientConnectionManager connectionManager,
                          final HttpProcessor httpProcessor,
                          final ConnectionReuseStrategy reuseStrategy,
                          final ConnectionKeepAliveStrategy keepAliveStrategy,
                          final UserTokenHandler userTokenHandler) {
        this.connectionManager = Args.notNull(connectionManager, "Connection manager");
        this.httpProcessor = Args.notNull(httpProcessor, "HTTP protocol processor");
        this.reuseStrategy = Args.notNull(reuseStrategy, "Connection reuse strategy");
        this.keepAliveStrategy = Args.notNull(keepAliveStrategy, "Connection keep alive strategy");
        this.userTokenHandler = Args.notNull(userTokenHandler, "User token handler");
    }

    @Override
    public ClassicHttpResponse execute(final ClassicHttpRequest request,
                                       final ExecChain.Scope scope,
                                       final ExecChain chain) throws IOException, HttpException {
        Args.notNull(request, "HTTP request");
        Args.notNull(scope, "Scope");
        final String exchangeId = scope.exchangeId;
        final HttpRoute route = scope.route;
        final HttpClientContext context = scope.clientContext;
        final ExecRuntime execRuntime = scope.execRuntime;

        if (LOG.isDebugEnabled())
            LOG.debug("{} executing {}", exchangeId, new RequestLine(request));
        try {
            context.setAttribute(HttpClientContext.HTTP_ROUTE, route);
            context.setAttribute(HttpCoreContext.HTTP_REQUEST, request);

            // 首先执行各种request protocol interceptors
            httpProcessor.process(request, request.getEntity(), context);

            // 开始发起http请求
            final ClassicHttpResponse response = execRuntime.execute(exchangeId, request, context);
            context.setAttribute(HttpCoreContext.HTTP_RESPONSE, response);
            // 执行各种response protocol interceptors
            httpProcessor.process(response, response.getEntity(), context);

            // 调用UserTokenHandler，生成userToken
            Object userToken = context.getUserToken();
            if (userToken == null) {
                userToken = userTokenHandler.getUserToken(route, request, context);
                context.setAttribute(HttpClientContext.USER_TOKEN, userToken);
            }

            // 决定连接是否能重用
            if (reuseStrategy.keepAlive(request, response, context)) {
                final TimeValue duration = keepAliveStrategy.getKeepAliveDuration(response, context);
                if (LOG.isDebugEnabled()) {
                    final String s;
                    if (duration != null) {
                        s = "for " + duration;
                    } else {
                        s = "indefinitely";
                    }
                    LOG.debug("{} connection can be kept alive {}", exchangeId, s);
                }
                execRuntime.markConnectionReusable(userToken, duration);
            } else {
                execRuntime.markConnectionNonReusable();
            }

            //
            final HttpEntity entity = response.getEntity();
            if (entity == null || !entity.isStreaming()) {
                // 没有响应entity，或entity不是流式，即一次性获取内容完毕，会走这个没有execRuntime的路线
                // connection not needed and (assumed to be) in re-usable state
                execRuntime.releaseEndpoint();
                return new CloseableHttpResponse(response, null);
            }
            return new CloseableHttpResponse(response, execRuntime);

        } catch (final ConnectionShutdownException ex) {
            final InterruptedIOException ioex = new InterruptedIOException(
                    "Connection has been shut down");
            ioex.initCause(ex);
            execRuntime.discardEndpoint();
            throw ioex;
        } catch (final HttpException | RuntimeException | IOException ex) {
            execRuntime.discardEndpoint();
            throw ex;
        } catch (final Error error) {
            connectionManager.close(CloseMode.IMMEDIATE);
            throw error;
        }
    }
}
