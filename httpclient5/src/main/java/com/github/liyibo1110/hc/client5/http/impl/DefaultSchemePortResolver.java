package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.URIScheme;
import com.github.liyibo1110.hc.core5.net.NamedEndpoint;
import com.github.liyibo1110.hc.core5.util.Args;

/**
 * SchemePortResolver接口的默认实现类。
 * @author liyibo
 * @date 2026-04-14 21:47
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class DefaultSchemePortResolver implements SchemePortResolver {

    public static final DefaultSchemePortResolver INSTANCE = new DefaultSchemePortResolver();

    @Override
    public int resolve(final HttpHost host) {
        Args.notNull(host, "HTTP host");
        return resolve(host.getSchemeName(), host);
    }

    /**
     * NamedEndpoint里的port是正常的正值，就直接用，否则根据schema返回其默认port值。
     */
    @Override
    public int resolve(final String scheme, final NamedEndpoint endpoint) {
        Args.notNull(endpoint, "Endpoint");
        final int port = endpoint.getPort();
        if (port > 0)
            return port;
        if (URIScheme.HTTP.same(scheme))
            return 80;
        else if (URIScheme.HTTPS.same(scheme))
            return 443;
        else
            return -1;
    }
}
