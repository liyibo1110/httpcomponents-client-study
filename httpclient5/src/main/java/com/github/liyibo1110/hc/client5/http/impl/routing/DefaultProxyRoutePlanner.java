package com.github.liyibo1110.hc.client5.http.impl.routing;

import com.github.liyibo1110.hc.client5.http.SchemePortResolver;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpException;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;
import com.github.liyibo1110.hc.core5.util.Args;

/**
 * 通过给定的proxy HttpPost组件的HttpRoutePlanner实现。
 * @author liyibo
 * @date 2026-04-20 16:44
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class DefaultProxyRoutePlanner extends DefaultRoutePlanner {

    private final HttpHost proxy;

    public DefaultProxyRoutePlanner(final HttpHost proxy, final SchemePortResolver schemePortResolver) {
        super(schemePortResolver);
        this.proxy = Args.notNull(proxy, "Proxy host");
    }

    public DefaultProxyRoutePlanner(final HttpHost proxy) {
        this(proxy, null);
    }

    @Override
    protected HttpHost determineProxy(final HttpHost target, final HttpContext context) throws HttpException {
        return proxy;
    }
}
