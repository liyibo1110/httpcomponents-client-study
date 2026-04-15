package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.LangUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * HTTP请求的连接路由定义。
 * @author liyibo
 * @date 2026-04-14 15:01
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public final class HttpRoute implements RouteInfo, Cloneable {

    /** 目标主机地址 */
    private final HttpHost targetHost;

    /** 本机地址 */
    private final InetAddress localAddress;

    /** 代理中间链 */
    private final List<HttpHost> proxyChain;

    private final TunnelType tunnelled;

    private final LayerType layered;

    private final boolean secure;

    private HttpRoute(final HttpHost targetHost,
                      final InetAddress local,
                      final List<HttpHost> proxies,
                      final boolean secure,
                      final TunnelType tunnelled,
                      final LayerType layered) {
        Args.notNull(targetHost, "Target host");
        Args.notNegative(targetHost.getPort(), "Target port");
        this.targetHost = targetHost;
        this.localAddress = local;
        if (proxies != null && !proxies.isEmpty())
            this.proxyChain = new ArrayList<>(proxies);
        else
            this.proxyChain = null;
        if (tunnelled == TunnelType.TUNNELLED)
            Args.check(this.proxyChain != null, "Proxy required if tunnelled");
        this.secure = secure;
        this.tunnelled = tunnelled != null ? tunnelled : TunnelType.PLAIN;
        this.layered = layered != null ? layered : LayerType.PLAIN;
    }

    public HttpRoute(final HttpHost target,
                     final InetAddress local,
                     final HttpHost[] proxies,
                     final boolean secure,
                     final TunnelType tunnelled,
                     final LayerType layered) {
        this(target, local, proxies != null ? Arrays.asList(proxies) : null, secure, tunnelled, layered);
    }

    public HttpRoute(final HttpHost target,
                     final InetAddress local,
                     final HttpHost proxy,
                     final boolean secure,
                     final TunnelType tunnelled,
                     final LayerType layered) {
        this(target, local, proxy != null ? Collections.singletonList(proxy) : null, secure, tunnelled, layered);
    }

    public HttpRoute(final HttpHost target, final InetAddress local, final boolean secure) {
        this(target, local, Collections.emptyList(), secure, TunnelType.PLAIN, LayerType.PLAIN);
    }

    public HttpRoute(final HttpHost target) {
        this(target, null, Collections.emptyList(), false, TunnelType.PLAIN, LayerType.PLAIN);
    }

    public HttpRoute(final HttpHost target, final InetAddress local, final HttpHost proxy, final boolean secure) {
        this(target, local, Collections.singletonList(Args.notNull(proxy, "Proxy host")), secure,
                secure ? TunnelType.TUNNELLED : TunnelType.PLAIN,
                secure ? LayerType.LAYERED    : LayerType.PLAIN);
    }

    public HttpRoute(final HttpHost target, final HttpHost proxy) {
        this(target, null, proxy, false);
    }

    @Override
    public HttpHost getTargetHost() {
        return this.targetHost;
    }

    @Override
    public InetAddress getLocalAddress() {
        return this.localAddress;
    }

    public InetSocketAddress getLocalSocketAddress() {
        return this.localAddress != null ? new InetSocketAddress(this.localAddress, 0) : null;
    }

    @Override
    public int getHopCount() {
        return proxyChain != null ? proxyChain.size() + 1 : 1;
    }

    @Override
    public HttpHost getHopTarget(final int hop) {
        Args.notNegative(hop, "Hop index");
        final int hopcount = getHopCount();
        Args.check(hop < hopcount, "Hop index exceeds tracked route length");
        if (hop < hopcount - 1)
            return this.proxyChain.get(hop);
        return this.targetHost;
    }

    @Override
    public HttpHost getProxyHost() {
        return proxyChain != null && !this.proxyChain.isEmpty() ? this.proxyChain.get(0) : null;
    }

    @Override
    public TunnelType getTunnelType() {
        return this.tunnelled;
    }

    @Override
    public boolean isTunnelled() {
        return this.tunnelled == TunnelType.TUNNELLED;
    }

    @Override
    public LayerType getLayerType() {
        return this.layered;
    }

    @Override
    public boolean isLayered() {
        return this.layered == LayerType.LAYERED;
    }

    @Override
    public boolean isSecure() {
        return this.secure;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof HttpRoute) {
            final HttpRoute that = (HttpRoute) obj;
            return
                    // Do the cheapest tests first
                    (this.secure == that.secure) &&
                            (this.tunnelled == that.tunnelled) &&
                            (this.layered   == that.layered) &&
                            Objects.equals(this.targetHost, that.targetHost) &&
                            Objects.equals(this.localAddress, that.localAddress) &&
                            Objects.equals(this.proxyChain, that.proxyChain);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, this.targetHost);
        hash = LangUtils.hashCode(hash, this.localAddress);
        if (this.proxyChain != null) {
            for (final HttpHost element : this.proxyChain)
                hash = LangUtils.hashCode(hash, element);
        }
        hash = LangUtils.hashCode(hash, this.secure);
        hash = LangUtils.hashCode(hash, this.tunnelled);
        hash = LangUtils.hashCode(hash, this.layered);
        return hash;
    }

    @Override
    public String toString() {
        final StringBuilder cab = new StringBuilder(50 + getHopCount()*30);
        if (this.localAddress != null) {
            cab.append(this.localAddress);
            cab.append("->");
        }
        cab.append('{');
        if (this.tunnelled == TunnelType.TUNNELLED)
            cab.append('t');
        if (this.layered == LayerType.LAYERED)
            cab.append('l');
        if (this.secure)
            cab.append('s');
        cab.append("}->");
        if (this.proxyChain != null) {
            for (final HttpHost aProxyChain : this.proxyChain) {
                cab.append(aProxyChain);
                cab.append("->");
            }
        }
        cab.append(this.targetHost);
        return cab.toString();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
