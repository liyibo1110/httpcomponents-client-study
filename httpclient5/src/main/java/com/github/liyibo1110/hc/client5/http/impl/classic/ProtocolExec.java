package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.AuthenticationStrategy;
import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.client5.http.auth.AuthExchange;
import com.github.liyibo1110.hc.client5.http.auth.ChallengeType;
import com.github.liyibo1110.hc.client5.http.classic.ExecChain;
import com.github.liyibo1110.hc.client5.http.classic.ExecChainHandler;
import com.github.liyibo1110.hc.client5.http.classic.ExecRuntime;
import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.client5.http.impl.DefaultSchemePortResolver;
import com.github.liyibo1110.hc.client5.http.impl.RequestSupport;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;
import com.github.liyibo1110.hc.core5.http.ClassicHttpResponse;
import com.github.liyibo1110.hc.core5.http.Header;
import com.github.liyibo1110.hc.core5.http.HttpEntity;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHeaders;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.HttpResponse;
import com.github.liyibo1110.hc.core5.http.Method;
import com.github.liyibo1110.hc.core5.http.ProtocolException;
import com.github.liyibo1110.hc.core5.http.io.entity.EntityUtils;
import com.github.liyibo1110.hc.core5.http.io.support.ClassicRequestBuilder;
import com.github.liyibo1110.hc.core5.net.URIAuthority;
import com.github.liyibo1110.hc.core5.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

/**
 * 把一次HTTP请求按协议要求修整好，并处理认证往返：
 * 1、修正请求形式（尤其是代理场景）。
 * 2、确保schema/authority合法。
 * 3、处理target/proxy两套认证状态。
 * 4、在收到401/407后决定是否重试。
 * 5、把最终response包装成和连接释放联动的对象。
 * @author liyibo
 * @date 2026-04-17 11:08
 */
@Contract(threading = ThreadingBehavior.STATELESS)
@Internal
public class ProtocolExec implements ExecChainHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ProtocolExec.class);

    private final AuthenticationStrategy targetAuthStrategy;
    private final AuthenticationStrategy proxyAuthStrategy;
    private final HttpAuthenticator authenticator;
    private final SchemePortResolver schemePortResolver;
    private final AuthCacheKeeper authCacheKeeper;

    public ProtocolExec(final AuthenticationStrategy targetAuthStrategy,
                        final AuthenticationStrategy proxyAuthStrategy,
                        final SchemePortResolver schemePortResolver,
                        final boolean authCachingDisabled) {
        this.targetAuthStrategy = Args.notNull(targetAuthStrategy, "Target authentication strategy");
        this.proxyAuthStrategy = Args.notNull(proxyAuthStrategy, "Proxy authentication strategy");
        this.authenticator = new HttpAuthenticator();
        this.schemePortResolver = schemePortResolver != null ? schemePortResolver : DefaultSchemePortResolver.INSTANCE;
        this.authCacheKeeper = authCachingDisabled ? null : new AuthCacheKeeper(this.schemePortResolver);
    }

    /**
     * 先把用户请求修整成符合当前路由和HTTP规范的形式，
     * 再根据target/proxy的认证状态给请求补认证头，交给下游执行。
     * 如果响应里出现认证挑战，就更新认证状态并重试，否则返回最终响应。
     */
    @Override
    public ClassicHttpResponse execute(final ClassicHttpRequest userRequest, final ExecChain.Scope scope, final ExecChain chain)
            throws IOException, HttpException {
        Args.notNull(userRequest, "HTTP request");
        Args.notNull(scope, "Scope");

        /**
         * CONNECT代表：为代理建tunnel的专用协议动作，不是普通业务请求。
         */
        if (Method.CONNECT.isSame(userRequest.getMethod()))
            throw new ProtocolException("Direct execution of CONNECT is not allowed");

        final String exchangeId = scope.exchangeId;
        final HttpRoute route = scope.route;
        final HttpClientContext context = scope.clientContext;
        final ExecRuntime execRuntime = scope.execRuntime;

        final HttpHost routeTarget = route.getTargetHost();
        final HttpHost proxy = route.getProxyHost();

        try {
            /**
             * 根据route修整request的形式。
             */
            final ClassicHttpRequest request;
            if (proxy != null && !route.isTunnelled()) {
                final ClassicRequestBuilder requestBuilder = ClassicRequestBuilder.copy(userRequest);
                if (requestBuilder.getAuthority() == null)
                    requestBuilder.setAuthority(new URIAuthority(routeTarget));

                requestBuilder.setAbsoluteRequestUri(true);
                request = requestBuilder.build();
            } else {
                request = userRequest;
            }

            /**
             * 补齐schema/authority，并校验URI语义（确保request至少有schema和authority）。
             */
            if (request.getScheme() == null)
                request.setScheme(routeTarget.getSchemeName());
            if (request.getAuthority() == null)
                request.setAuthority(new URIAuthority(routeTarget));

            // 拒绝URI authority里携带userinfo，例如http://user:pass@example.com/...这种形式，已经这种方式被认定为淘汰
            final URIAuthority authority = request.getAuthority();
            if (authority.getUserInfo() != null)
                throw new ProtocolException("Request URI authority contains deprecated userinfo component");

            /**
             * 构造target / pathPrefix / authExchange
             */
            // 构造规范化的target host
            final HttpHost target = new HttpHost(
                    request.getScheme(),
                    authority.getHostName(),
                    schemePortResolver.resolve(request.getScheme(), authority));

            // 提取pathPrefix
            final String pathPrefix = RequestSupport.extractPathPrefix(request);

            // 拿两套认证交换状态
            final AuthExchange targetAuthExchange = context.getAuthExchange(target);
            final AuthExchange proxyAuthExchange = proxy != null ? context.getAuthExchange(proxy) : new AuthExchange();

            if (!targetAuthExchange.isConnectionBased() &&
                    targetAuthExchange.getPathPrefix() != null &&
                    !pathPrefix.startsWith(targetAuthExchange.getPathPrefix())) {
                // force re-authentication if the current path prefix does not match
                // that of the previous authentication exchange.
                targetAuthExchange.reset();
            }
            if (targetAuthExchange.getPathPrefix() == null)
                targetAuthExchange.setPathPrefix(pathPrefix);

            // 加载预认证缓存
            if (authCacheKeeper != null) {
                authCacheKeeper.loadPreemptively(target, pathPrefix, targetAuthExchange, context);
                if (proxy != null)
                    authCacheKeeper.loadPreemptively(proxy, null, proxyAuthExchange, context);
            }

            // 对请求实体做增强包装，接下来有可能进入认证的重试循环，所以这里是为后面的重试判断做准备。
            RequestEntityProxy.enhance(request);

            /**
             * 认证循环：发请求 -> 看是否需要认证 -> 如果需要就更新认证状态并重发，否则返回。
             */
            while (true) {
                // 尝试给target补上认证头
                if (!request.containsHeader(HttpHeaders.AUTHORIZATION)) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} target auth state: {}", exchangeId, targetAuthExchange.getState());
                    authenticator.addAuthResponse(target, ChallengeType.TARGET, request, targetAuthExchange, context);
                }
                // 再给proxy加认证头
                if (!request.containsHeader(HttpHeaders.PROXY_AUTHORIZATION) && !route.isTunnelled()) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} proxy auth state: {}", exchangeId, proxyAuthExchange.getState());
                    authenticator.addAuthResponse(proxy, ChallengeType.PROXY, request, proxyAuthExchange, context);
                }
                // 请求交给下游执行
                final ClassicHttpResponse response = chain.proceed(request, scope);

                // 排除TRACE类型的请求，这个不需要认证
                if (Method.TRACE.isSame(request.getMethod())) {
                    // Do not perform authentication for TRACE request
                    ResponseEntityProxy.enhance(response, execRuntime);
                    return response;
                }

                // 如果请求entity不能重复发送，即使响应提示需要认证，也不能再发过去
                final HttpEntity requestEntity = request.getEntity();
                if (requestEntity != null && !requestEntity.isRepeatable()) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("{} Cannot retry non-repeatable request", exchangeId);
                    ResponseEntityProxy.enhance(response, execRuntime);
                    return response;
                }
                // 要不要重试的判断方法
                if (needAuthentication(
                        targetAuthExchange,
                        proxyAuthExchange,
                        proxy != null ? proxy : target,
                        target,
                        pathPrefix,
                        response,
                        context)) {
                    // Make sure the response body is fully consumed, if present

                    // 进入了说明响应返回了可处理的认证挑战，并且认证状态已经更新，应该重发请求
                    final HttpEntity responseEntity = response.getEntity();
                    if (execRuntime.isConnectionReusable()) {
                        EntityUtils.consume(responseEntity);
                    } else {
                        execRuntime.disconnectEndpoint();
                        if (proxyAuthExchange.getState() == AuthExchange.State.SUCCESS && proxyAuthExchange.isConnectionBased()) {
                            if (LOG.isDebugEnabled())
                                LOG.debug("{} resetting proxy auth state", exchangeId);
                            proxyAuthExchange.reset();
                        }
                        if (targetAuthExchange.getState() == AuthExchange.State.SUCCESS && targetAuthExchange.isConnectionBased()) {
                            if (LOG.isDebugEnabled())
                                LOG.debug("{} resetting target auth state", exchangeId);
                            targetAuthExchange.reset();
                        }
                    }
                    // Reset request headers
                    final ClassicHttpRequest original = scope.originalRequest;
                    request.setHeaders();
                    for (final Iterator<Header> it = original.headerIterator(); it.hasNext(); )
                        request.addHeader(it.next());

                } else {
                    // 进入了说明当前响应就是最终响应了，包装后直接返回
                    ResponseEntityProxy.enhance(response, execRuntime);
                    return response;
                }
            }
        } catch (final HttpException ex) {
            execRuntime.discardEndpoint();
            throw ex;
        } catch (final RuntimeException | IOException ex) {
            execRuntime.discardEndpoint();
            for (final AuthExchange authExchange : context.getAuthExchanges().values()) {
                if (authExchange.isConnectionBased())
                    authExchange.reset();
            }
            throw ex;
        }
    }

    private boolean needAuthentication(final AuthExchange targetAuthExchange,
                                       final AuthExchange proxyAuthExchange,
                                       final HttpHost proxy,
                                       final HttpHost target,
                                       final String pathPrefix,
                                       final HttpResponse response,
                                       final HttpClientContext context) {
        final RequestConfig config = context.getRequestConfig();
        if (config.isAuthenticationEnabled()) {
            final boolean targetAuthRequested = authenticator.isChallenged(target, ChallengeType.TARGET, response, targetAuthExchange, context);

            if (authCacheKeeper != null) {
                if (targetAuthRequested)
                    authCacheKeeper.updateOnChallenge(target, pathPrefix, targetAuthExchange, context);
                else
                    authCacheKeeper.updateOnNoChallenge(target, pathPrefix, targetAuthExchange, context);
            }

            final boolean proxyAuthRequested = authenticator.isChallenged(proxy, ChallengeType.PROXY, response, proxyAuthExchange, context);

            if (authCacheKeeper != null) {
                if (proxyAuthRequested)
                    authCacheKeeper.updateOnChallenge(proxy, null, proxyAuthExchange, context);
                else
                    authCacheKeeper.updateOnNoChallenge(proxy, null, proxyAuthExchange, context);
            }

            if (targetAuthRequested) {
                final boolean updated = authenticator.updateAuthState(target, ChallengeType.TARGET, response,
                        targetAuthStrategy, targetAuthExchange, context);
                if (authCacheKeeper != null)
                    authCacheKeeper.updateOnResponse(target, pathPrefix, targetAuthExchange, context);
                return updated;
            }
            if (proxyAuthRequested) {
                final boolean updated = authenticator.updateAuthState(proxy, ChallengeType.PROXY, response,
                        proxyAuthStrategy, proxyAuthExchange, context);
                if (authCacheKeeper != null)
                    authCacheKeeper.updateOnResponse(proxy, null, proxyAuthExchange, context);
                return updated;
            }
        }
        return false;
    }
}
