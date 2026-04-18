package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.ClientProtocolException;
import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.client5.http.classic.ExecRuntime;
import com.github.liyibo1110.hc.client5.http.config.Configurable;
import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.client5.http.impl.ConnectionShutdownException;
import com.github.liyibo1110.hc.client5.http.impl.DefaultClientConnectionReuseStrategy;
import com.github.liyibo1110.hc.client5.http.impl.DefaultSchemePortResolver;
import com.github.liyibo1110.hc.client5.http.impl.ExecSupport;
import com.github.liyibo1110.hc.client5.http.io.HttpClientConnectionManager;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.client5.http.protocol.RequestClientConnControl;
import com.github.liyibo1110.hc.client5.http.routing.RoutingSupport;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.concurrent.CancellableDependency;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.ConnectionReuseStrategy;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.impl.io.HttpRequestExecutor;
import com.github.liyibo1110.hc.core5.http.protocol.BasicHttpContext;
import com.github.liyibo1110.hc.core5.http.protocol.DefaultHttpProcessor;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.http.protocol.HttpCoreContext;
import com.github.liyibo1110.hc.core5.http.protocol.HttpProcessor;
import com.github.liyibo1110.hc.core5.http.protocol.RequestContent;
import com.github.liyibo1110.hc.core5.http.protocol.RequestTargetHost;
import com.github.liyibo1110.hc.core5.http.protocol.RequestUserAgent;
import com.github.liyibo1110.hc.core5.io.CloseMode;
import com.github.liyibo1110.hc.core5.net.URIAuthority;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.TimeValue;
import com.github.liyibo1110.hc.core5.util.VersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * CloseableHttpClient的精简实现。
 * 该客户端针对HTTP/1.1消息传输进行了优化，不支持通过代理执行请求、状态管理、身份验证和请求重定向等高级HTTP协议功能。
 * 由该客户端执行的并发消息交换将被分配到从连接池中租用的独立连接上。
 * @author liyibo
 * @date 2026-04-17 13:59
 */
@Contract(threading = ThreadingBehavior.SAFE_CONDITIONAL)
public class MinimalHttpClient extends CloseableHttpClient {
    private static final Logger LOG = LoggerFactory.getLogger(MinimalHttpClient.class);

    private final HttpClientConnectionManager connManager;
    private final ConnectionReuseStrategy reuseStrategy;
    private final SchemePortResolver schemePortResolver;
    private final HttpRequestExecutor requestExecutor;
    private final HttpProcessor httpProcessor;

    MinimalHttpClient(final HttpClientConnectionManager connManager) {
        super();
        this.connManager = Args.notNull(connManager, "HTTP connection manager");
        this.reuseStrategy = DefaultClientConnectionReuseStrategy.INSTANCE;
        this.schemePortResolver = DefaultSchemePortResolver.INSTANCE;
        this.requestExecutor = new HttpRequestExecutor(this.reuseStrategy);
        this.httpProcessor = new DefaultHttpProcessor(
                new RequestContent(),
                new RequestTargetHost(),
                new RequestClientConnControl(),
                new RequestUserAgent(VersionInfo.getSoftwareInfo(
                        "Apache-HttpClient", "org.apache.hc.client5", getClass())));
    }

    @Override
    protected CloseableHttpResponse doExecute(final HttpHost target, final ClassicHttpRequest request, final HttpContext context)
            throws IOException {
        Args.notNull(target, "Target host");
        Args.notNull(request, "HTTP request");
        if (request.getScheme() == null)
            request.setScheme(target.getSchemeName());

        if (request.getAuthority() == null)
            request.setAuthority(new URIAuthority(target));

        final HttpClientContext clientContext = HttpClientContext.adapt(context != null ? context : new BasicHttpContext());
        RequestConfig config = null;
        if (request instanceof Configurable)
            config = ((Configurable) request).getConfig();

        if (config != null)
            clientContext.setRequestConfig(config);

        final HttpRoute route = new HttpRoute(RoutingSupport.normalize(target, schemePortResolver));
        final String exchangeId = ExecSupport.getNextExchangeId();
        clientContext.setExchangeId(exchangeId);
        final ExecRuntime execRuntime = new InternalExecRuntime(LOG, connManager, requestExecutor,
                request instanceof CancellableDependency ? (CancellableDependency) request : null);
        try {
            if (!execRuntime.isEndpointAcquired())
                execRuntime.acquireEndpoint(exchangeId, route, null, clientContext);

            if (!execRuntime.isEndpointConnected())
                execRuntime.connectEndpoint(clientContext);

            clientContext.setAttribute(HttpCoreContext.HTTP_REQUEST, request);
            clientContext.setAttribute(HttpClientContext.HTTP_ROUTE, route);

            httpProcessor.process(request, request.getEntity(), clientContext);
            final ClassicHttpResponse response = execRuntime.execute(exchangeId, request, clientContext);
            httpProcessor.process(response, response.getEntity(), clientContext);

            if (reuseStrategy.keepAlive(request, response, clientContext))
                execRuntime.markConnectionReusable(null, TimeValue.NEG_ONE_MILLISECOND);
            else
                execRuntime.markConnectionNonReusable();

            // check for entity, release connection if possible
            final HttpEntity entity = response.getEntity();
            if (entity == null || !entity.isStreaming()) {
                // connection not needed and (assumed to be) in re-usable state
                execRuntime.releaseEndpoint();
                return new CloseableHttpResponse(response, null);
            }
            ResponseEntityProxy.enhance(response, execRuntime);
            return new CloseableHttpResponse(response, execRuntime);
        } catch (final ConnectionShutdownException ex) {
            final InterruptedIOException ioex = new InterruptedIOException("Connection has been shut down");
            ioex.initCause(ex);
            execRuntime.discardEndpoint();
            throw ioex;
        } catch (final HttpException httpException) {
            execRuntime.discardEndpoint();
            throw new ClientProtocolException(httpException);
        } catch (final RuntimeException | IOException ex) {
            execRuntime.discardEndpoint();
            throw ex;
        } catch (final Error error) {
            connManager.close(CloseMode.IMMEDIATE);
            throw error;
        }
    }

    @Override
    public void close() throws IOException {
        this.connManager.close();
    }

    @Override
    public void close(final CloseMode closeMode) {
        this.connManager.close(closeMode);
    }
}
