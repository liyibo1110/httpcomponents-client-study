package com.github.liyibo1110.hc.client5.http.impl.routing;

import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.client5.http.config.RequestConfig;
import com.github.liyibo1110.hc.client5.http.impl.DefaultSchemePortResolver;
import com.github.liyibo1110.hc.client5.http.protocol.HttpClientContext;
import com.github.liyibo1110.hc.client5.http.routing.HttpRoutePlanner;
import com.github.liyibo1110.hc.client5.http.routing.RoutingSupport;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.ProtocolException;
import com.github.liyibo1110.hc.core5.http.URIScheme;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

import java.net.InetAddress;

/**
 * HttpRoutePlanner接口的默认实现类，不会使用任何Java系统属性，也不会使用系统或浏览器的代理设置。
 * @author liyibo
 * @date 2026-04-20 15:58
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class DefaultRoutePlanner implements HttpRoutePlanner {

    private final SchemePortResolver schemePortResolver;

    public DefaultRoutePlanner(final SchemePortResolver schemePortResolver) {
        super();
        this.schemePortResolver = schemePortResolver != null ? schemePortResolver : DefaultSchemePortResolver.INSTANCE;
    }

    /**
     * 先确定目标host是否合法，再从请求配置或扩展点里找proxy，然后把target规范化、判断是否secure，
     * 最后根据有没有proxy构造出一个直连或代理的HttpRoute。
     */
    @Override
    public final HttpRoute determineRoute(final HttpHost host, final HttpContext context) throws HttpException {
        if (host == null)
            throw new ProtocolException("Target host is not specified");
        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final RequestConfig config = clientContext.getRequestConfig();
        HttpHost proxy = config.getProxy();
        if (proxy == null)  // 尝试调用子类扩展获取proxy版本的HttpHost
            proxy = determineProxy(host, context);

        final HttpHost target = RoutingSupport.normalize(host, schemePortResolver);
        if (target.getPort() < 0)
            throw new ProtocolException("Unroutable protocol scheme: " + target);
        final boolean secure = target.getSchemeName().equalsIgnoreCase(URIScheme.HTTPS.getId());

        if (proxy == null)
            return new HttpRoute(target, determineLocalAddress(target, context), secure);
        return new HttpRoute(target, determineLocalAddress(proxy, context), proxy, secure);
    }

    /**
     * 留给子类扩展实现。
     */
    protected HttpHost determineProxy(final HttpHost target, final HttpContext context) throws HttpException {
        return null;
    }

    /**
     * 留给子类扩展实现。
     */
    protected InetAddress determineLocalAddress(final HttpHost firstHop, final HttpContext context) throws HttpException {
        return null;
    }
}
