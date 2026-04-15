package com.github.liyibo1110.hc.client5.http.routing;

import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.client5.http.impl.DefaultSchemePortResolver;
import com.github.liyibo1110.hc.client5.http.utils.URIUtils;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.ProtocolException;
import com.github.liyibo1110.hc.core5.net.URIAuthority;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * HTTP路由相关支持方法。
 * @author liyibo
 * @date 2026-04-14 21:26
 */
public final class RoutingSupport {

    private RoutingSupport() {}

    /**
     * HttpRequest -> HttpHost
     */
    public static HttpHost determineHost(final HttpRequest request) throws HttpException {
        if (request == null)
            return null;
        final URIAuthority authority = request.getAuthority();
        if (authority != null) {
            final String scheme = request.getScheme();
            if (scheme == null)
                throw new ProtocolException("Protocol scheme is not specified");
            return new HttpHost(scheme, authority);
        }
        // 没有authority则继续，根据带有schema的URI来生成HttpHost
        try {
            final URI requestURI = request.getUri();
            if (requestURI.isAbsolute()) {
                final HttpHost httpHost = URIUtils.extractHost(requestURI);
                if (httpHost == null)
                    throw new ProtocolException("URI does not specify a valid host name: " + requestURI);
                return httpHost;
            }
        } catch (final URISyntaxException ignore) {

        }
        return null;
    }

    /**
     * 规范化HttpHost，其实就是调整了HttpHost里的port值。
     */
    public static HttpHost normalize(final HttpHost host, final SchemePortResolver schemePortResolver) {
        if (host == null)
            return null;
        if (host.getPort() < 0) {
            final int port = (schemePortResolver != null ? schemePortResolver: DefaultSchemePortResolver.INSTANCE).resolve(host);
            if (port > 0)
                return new HttpHost(host.getSchemeName(), host.getAddress(), host.getHostName(), port);
        }
        return host;
    }
}
