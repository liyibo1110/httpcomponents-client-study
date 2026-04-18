package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.AuthenticationStrategy;
import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.RouteTracker;
import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.client5.http.auth.AuthExchange;
import com.github.liyibo1110.hc.client5.http.auth.ChallengeType;
import com.github.liyibo1110.hc.client5.http.classic.ExecChain;
import com.github.liyibo1110.hc.client5.http.classic.ExecChainHandler;
import com.github.liyibo1110.hc.client5.http.classic.ExecRuntime;
import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.client5.http.impl.TunnelRefusedException;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.client5.http.routing.HttpRouteDirector;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.ConnectionReuseStrategy;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHeaders;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.HttpStatus;
import com.github.liyibo1110.hc.core5.http.HttpVersion;
import com.github.liyibo1110.hc.core5.http.Method;
import com.github.liyibo1110.hc.core5.http.io.entity.EntityUtils;
import com.github.liyibo1110.hc.core5.http.message.BasicClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.message.StatusLine;
import com.github.liyibo1110.hc.core5.http.protocol.HttpProcessor;
import com.github.liyibo1110.hc.core5.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * classic request execution链中的请求执行处理程序，负责根据当前连接路由的指定，与目标源服务器建立连接。
 *
 * 职责是：保证当前请求在真正进入后续执行链之前，已经把路由对应的物理连接形态给搭建好了。
 * @author liyibo
 * @date 2026-04-17 10:35
 */
@Contract(threading = ThreadingBehavior.STATELESS)
@Internal
public class ConnectExec implements ExecChainHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectExec.class);

    private final ConnectionReuseStrategy reuseStrategy;
    private final HttpProcessor proxyHttpProcessor;
    private final AuthenticationStrategy proxyAuthStrategy;
    private final HttpAuthenticator authenticator;
    private final AuthCacheKeeper authCacheKeeper;

    /** 比较planned route和current fact route，来决定下一步的动作 */
    private final HttpRouteDirector routeDirector;

    public ConnectExec(final ConnectionReuseStrategy reuseStrategy,
                       final HttpProcessor proxyHttpProcessor,
                       final AuthenticationStrategy proxyAuthStrategy,
                       final SchemePortResolver schemePortResolver,
                       final boolean authCachingDisabled) {
        Args.notNull(reuseStrategy, "Connection reuse strategy");
        Args.notNull(proxyHttpProcessor, "Proxy HTTP processor");
        Args.notNull(proxyAuthStrategy, "Proxy authentication strategy");
        this.reuseStrategy = reuseStrategy;
        this.proxyHttpProcessor = proxyHttpProcessor;
        this.proxyAuthStrategy = proxyAuthStrategy;
        this.authenticator = new HttpAuthenticator();
        this.authCacheKeeper = authCachingDisabled ? null : new AuthCacheKeeper(schemePortResolver);
        this.routeDirector = BasicRouteDirector.INSTANCE;
    }

    /**
     * 1、先确保endpoint已经获取。
     * 2、如果还没连通，就按照routeDirector给出的步骤，一步步完成connect/tunnel/layer protocol。
     * 3、连接打通后，再把原始业务请求交给下游的chain.proceed()方法。
     */
    @Override
    public ClassicHttpResponse execute(final ClassicHttpRequest request, final ExecChain.Scope scope, final ExecChain chain)
            throws IOException, HttpException {
        Args.notNull(request, "HTTP request");
        Args.notNull(scope, "Scope");

        final String exchangeId = scope.exchangeId;
        final HttpRoute route = scope.route;
        final HttpClientContext context = scope.clientContext;
        /** 真正干底层活的对象，负责操作socket */
        final ExecRuntime execRuntime = scope.execRuntime;

        /**
         * 先从连接管理体系中，拿出一个endpoint。
         * 可能只是先占个位置，不一定已经真正连上的远端。
         */
        if (!execRuntime.isEndpointAcquired()) {
            final Object userToken = context.getUserToken();
            if (LOG.isDebugEnabled())
                LOG.debug("{} acquiring connection with route {}", exchangeId, route);
            execRuntime.acquireEndpoint(exchangeId, route, userToken, context);
        }

        try {
            if (!execRuntime.isEndpointConnected()) {
                if (LOG.isDebugEnabled())
                    LOG.debug("{} opening connection {}", exchangeId, route);

                /**
                 * 当前这条连接实际上已经走到了哪一步，相当于是个实时记录器。
                 */
                final RouteTracker tracker = new RouteTracker(route);
                int step;
                do {
                    /**
                     * 开始做最重要的事情：planned route和fact route的差量推进，每一轮循环做3件事：
                     * 1、拿到当前事实路线fact。
                     * 2、去问routeDirector，下一步要做什么，也就是比较planned和fact，最终得出一个动作指令。
                     * 3、执行这个动作，并更新tracker。
                     */
                    final HttpRoute fact = tracker.toRoute();
                    step = this.routeDirector.nextStep(route, fact);

                    // 算出了下一步的指令
                    switch (step) {
                        case HttpRouteDirector.CONNECT_TARGET:
                            // 直接连接目标服务器
                            execRuntime.connectEndpoint(context);   // 真正建立底层连接
                            tracker.connectTarget(route.isSecure());    // 告诉tracker，已经连到target了，以及是否安全
                            break;
                        case HttpRouteDirector.CONNECT_PROXY:
                            // 先连接代理
                            execRuntime.connectEndpoint(context);
                            final HttpHost proxy  = route.getProxyHost();
                            // 事实连接只到proxy，没有到target
                            tracker.connectProxy(proxy, route.isSecure() && !route.isTunnelled());
                            break;
                        case HttpRouteDirector.TUNNEL_TARGET: {
                            // 已经到了proxy了，下一步要通过代理开启一个tunnel到target，即典型的HTTPS走HTTP proxy的场景
                            final boolean secure = createTunnelToTarget(exchangeId, route, request, execRuntime, context);
                            if (LOG.isDebugEnabled())
                                LOG.debug("{} tunnel to target created.", exchangeId);
                            tracker.tunnelTarget(secure);
                        }   break;

                        case HttpRouteDirector.TUNNEL_PROXY: {
                            // The most simple example for this case is a proxy chain
                            // of two proxies, where P1 must be tunnelled to P2.
                            // route: Source -> P1 -> P2 -> Target (3 hops)
                            // fact:  Source -> P1 -> Target       (2 hops)

                            /**
                             * 更复杂的代理链场景：不是一个proxy，而是多个proxy串起来，例如：
                             * source -> p1 -> p2 -> target，需要对中间代理继续开启tunnel。
                             * 但是这里面并不支持，说明目前只是个预留扩展。
                             */
                            final int hop = fact.getHopCount()-1; // the hop to establish
                            final boolean secure = createTunnelToProxy(route, hop, context);
                            if (LOG.isDebugEnabled())
                                LOG.debug("{} tunnel to proxy created.", exchangeId);
                            tracker.tunnelProxy(route.getHopTarget(hop), secure);
                        }   break;

                        case HttpRouteDirector.LAYER_PROTOCOL:
                            /**
                             * 说明当前物理通路已经有了，还需要再叠一层协议，典型场景就是：升级成TLS。
                             */
                            execRuntime.upgradeTls(context);
                            tracker.layerProtocol(route.isSecure());
                            break;

                        case HttpRouteDirector.UNREACHABLE:
                            // 当前事实路线，不可能再推进到计划路线。
                            throw new HttpException("Unable to establish route: planned = " + route + "; current = " + fact);
                        case HttpRouteDirector.COMPLETE:
                            // 什么也不做，说明planned route已经完全被落实了
                            break;
                        default:
                            throw new IllegalStateException("Unknown step indicator " + step + " from RouteDirector.");
                    }

                } while (step > HttpRouteDirector.COMPLETE);
            }
            // 连接已经按route的要求建好了，执行ExecChain上下一个执行节点（一般就是MainClientExec了）
            return chain.proceed(request, scope);

        } catch (final IOException | HttpException | RuntimeException ex) {
            execRuntime.discardEndpoint();
            throw ex;
        }
    }

    /**
     * 建立通往目标服务器的隧道。必须先与（最后一个）代理服务器建立连接。
     * 系统将生成并发送一个用于通过代理建立隧道的CONNECT请求，接收并验证响应。
     * 此方法不会使用隧道相关信息对连接进行验证，该操作由调用方负责。
     */
    private boolean createTunnelToTarget(final String exchangeId,
                                         final HttpRoute route,
                                         final HttpRequest request,
                                         final ExecRuntime execRuntime,
                                         final HttpClientContext context) throws HttpException, IOException {
        final RequestConfig config = context.getRequestConfig();

        final HttpHost target = route.getTargetHost();
        final HttpHost proxy = route.getProxyHost();
        final AuthExchange proxyAuthExchange = context.getAuthExchange(proxy);

        if (authCacheKeeper != null)
            authCacheKeeper.loadPreemptively(proxy, null, proxyAuthExchange, context);

        ClassicHttpResponse response = null;

        final String authority = target.toHostString();
        final ClassicHttpRequest connect = new BasicClassicHttpRequest(Method.CONNECT, target, authority);
        connect.setVersion(HttpVersion.HTTP_1_1);

        this.proxyHttpProcessor.process(connect, null, context);

        while (response == null) {
            connect.removeHeaders(HttpHeaders.PROXY_AUTHORIZATION);
            this.authenticator.addAuthResponse(proxy, ChallengeType.PROXY, connect, proxyAuthExchange, context);

            // 发起method为connect的请求
            response = execRuntime.execute(exchangeId, connect, context);
            this.proxyHttpProcessor.process(response, response.getEntity(), context);

            final int status = response.getCode();
            if (status < HttpStatus.SC_SUCCESS)
                throw new HttpException("Unexpected response to CONNECT request: " + new StatusLine(response));

            if (config.isAuthenticationEnabled()) {
                final boolean proxyAuthRequested = authenticator.isChallenged(proxy, ChallengeType.PROXY, response, proxyAuthExchange, context);

                if (authCacheKeeper != null) {
                    if (proxyAuthRequested)
                        authCacheKeeper.updateOnChallenge(proxy, null, proxyAuthExchange, context);
                    else
                        authCacheKeeper.updateOnNoChallenge(proxy, null, proxyAuthExchange, context);
                }

                if (proxyAuthRequested) {
                    final boolean updated = authenticator.updateAuthState(proxy, ChallengeType.PROXY, response,
                            proxyAuthStrategy, proxyAuthExchange, context);

                    if (authCacheKeeper != null)
                        authCacheKeeper.updateOnResponse(proxy, null, proxyAuthExchange, context);

                    if (updated) {
                        // Retry request
                        if (this.reuseStrategy.keepAlive(connect, response, context)) {
                            if (LOG.isDebugEnabled())
                                LOG.debug("{} connection kept alive", exchangeId);
                            // Consume response content
                            final HttpEntity entity = response.getEntity();
                            EntityUtils.consume(entity);
                        } else {
                            execRuntime.disconnectEndpoint();
                        }
                        response = null;
                    }
                }
            }
        }

        final int status = response.getCode();
        if (status != HttpStatus.SC_OK) {
            // Buffer response content
            final HttpEntity entity = response.getEntity();
            final String responseMessage = entity != null ? EntityUtils.toString(entity) : null;
            execRuntime.disconnectEndpoint();
            throw new TunnelRefusedException("CONNECT refused by proxy: " + new StatusLine(response), responseMessage);
        }
        return false;
    }

    /**
     * 创建通往中间代理的隧道。此方法在此类中未实现，只会抛出异常。
     */
    private boolean createTunnelToProxy(final HttpRoute route, final int hop, final HttpClientContext context) throws HttpException {
        throw new HttpException("Proxy chains are not supported.");
    }
}
