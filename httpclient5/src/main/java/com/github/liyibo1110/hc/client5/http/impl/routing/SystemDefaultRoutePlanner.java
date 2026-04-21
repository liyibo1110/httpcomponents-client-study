package com.github.liyibo1110.hc.client5.http.impl.routing;

import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * 基于ProxySelector的HttpRoutePlanner实现。
 * 默认情况下，该类将从系统属性或运行应用程序的浏览器中获取JVM的代理设置。
 * @author liyibo
 * @date 2026-04-20 16:34
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class SystemDefaultRoutePlanner extends DefaultRoutePlanner {

    private final ProxySelector proxySelector;

    public SystemDefaultRoutePlanner(final SchemePortResolver schemePortResolver, final ProxySelector proxySelector) {
        super(schemePortResolver);
        this.proxySelector = proxySelector;
    }

    public SystemDefaultRoutePlanner(final ProxySelector proxySelector) {
        this(null, proxySelector);
    }

    @Override
    protected HttpHost determineProxy(final HttpHost target, final HttpContext context) throws HttpException {
        final URI targetURI;
        try {
            targetURI = new URI(target.toURI());
        } catch (final URISyntaxException ex) {
            throw new HttpException("Cannot convert host to URI: " + target, ex);
        }

        ProxySelector proxySelectorInstance = this.proxySelector;
        if (proxySelectorInstance == null)
            proxySelectorInstance = ProxySelector.getDefault();
        if (proxySelectorInstance == null)
            return null;

        final List<Proxy> proxies = proxySelectorInstance.select(targetURI);
        final Proxy p = chooseProxy(proxies);
        HttpHost result = null;
        if (p.type() == Proxy.Type.HTTP) {
            // convert the socket address to an HttpHost
            if (!(p.address() instanceof InetSocketAddress))
                throw new HttpException("Unable to handle non-Inet proxy address: " + p.address());

            final InetSocketAddress isa = (InetSocketAddress) p.address();
            // assume default scheme (http)
            result = new HttpHost(null, isa.getAddress(), isa.getHostString(), isa.getPort());
        }
        return result;
    }

    private Proxy chooseProxy(final List<Proxy> proxies) {
        Proxy result = null;
        // check the list for one we can use
        for (int i = 0; (result == null) && (i < proxies.size()); i++) {
            final Proxy p = proxies.get(i);
            switch (p.type()) {
                case DIRECT:
                case HTTP:
                    result = p;
                    break;
                case SOCKS:
                    // SOCKS hosts are not handled on the route level.
                    // The socket may make use of the SOCKS host though.
                    break;
            }
        }
        if (result == null) {
            //@@@ log as warning or info that only a socks proxy is available?
            // result can only be null if all proxies are socks proxies
            // socks proxies are not handled on the route planning level
            result = Proxy.NO_PROXY;
        }
        return result;
    }
}
