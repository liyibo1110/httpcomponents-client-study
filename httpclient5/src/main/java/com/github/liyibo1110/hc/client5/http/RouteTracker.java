package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.core5.http.HttpHost;
import com.github.liyibo1110.hc.core5.util.Args;
import com.github.liyibo1110.hc.core5.util.Asserts;
import com.github.liyibo1110.hc.core5.util.LangUtils;

import java.net.InetAddress;
import java.util.Objects;

/**
 * 帮助跟踪规划路线的各个步骤的RouteInfo实现。
 * @author liyibo
 * @date 2026-04-14 15:53
 */
public final class RouteTracker implements RouteInfo, Cloneable {

    private final HttpHost targetHost;

    private final InetAddress localAddress;

    /** 该路由的第一跳是否已建立。 */
    private boolean connected;

    private HttpHost[] proxyChain;

    private TunnelType tunnelled;

    private LayerType layered;

    private boolean secure;

    public RouteTracker(final HttpHost target, final InetAddress local) {
        Args.notNull(target, "Target host");
        this.targetHost = target;
        this.localAddress = local;
        this.tunnelled = TunnelType.PLAIN;
        this.layered = LayerType.PLAIN;
    }

    public void reset() {
        this.connected = false;
        this.proxyChain = null;
        this.tunnelled = TunnelType.PLAIN;
        this.layered = LayerType.PLAIN;
        this.secure = false;
    }

    public RouteTracker(final HttpRoute route) {
        this(route.getTargetHost(), route.getLocalAddress());
    }

    public void connectTarget(final boolean secure) {
        Asserts.check(!this.connected, "Already connected");
        this.connected = true;
        this.secure = secure;
    }

    public void connectProxy(final HttpHost proxy, final boolean secure) {
        Args.notNull(proxy, "Proxy host");
        Asserts.check(!this.connected, "Already connected");
        this.connected = true;
        this.proxyChain = new HttpHost[]{ proxy };
        this.secure = secure;
    }

    public void tunnelTarget(final boolean secure) {
        Asserts.check(this.connected, "No tunnel unless connected");
        Asserts.notNull(this.proxyChain, "No tunnel without proxy");
        this.tunnelled = TunnelType.TUNNELLED;
        this.secure    = secure;
    }

    public void tunnelProxy(final HttpHost proxy, final boolean secure) {
        Args.notNull(proxy, "Proxy host");
        Asserts.check(this.connected, "No tunnel unless connected");
        Asserts.notNull(this.proxyChain, "No tunnel without proxy");
        // prepare an extended proxy chain
        final HttpHost[] proxies = new HttpHost[this.proxyChain.length + 1];
        System.arraycopy(this.proxyChain, 0, proxies, 0, this.proxyChain.length);
        proxies[proxies.length - 1] = proxy;
        this.proxyChain = proxies;
        this.secure = secure;
    }

    public void layerProtocol(final boolean secure) {
        // it is possible to layer a protocol over a direct connection,
        // although this case is probably not considered elsewhere
        Asserts.check(this.connected, "No layered protocol unless connected");
        this.layered = LayerType.LAYERED;
        this.secure  = secure;
    }

    @Override
    public HttpHost getTargetHost() {
        return this.targetHost;
    }

    @Override
    public InetAddress getLocalAddress() {
        return this.localAddress;
    }

    @Override
    public int getHopCount() {
        int hops = 0;
        if (this.connected) {
            if (proxyChain == null)
                hops = 1;
            else
                hops = proxyChain.length + 1;
        }
        return hops;
    }

    @Override
    public HttpHost getHopTarget(final int hop) {
        Args.notNegative(hop, "Hop index");
        final int hopcount = getHopCount();
        Args.check(hop < hopcount, "Hop index exceeds tracked route length");
        HttpHost result = null;
        if (hop < hopcount-1)
            result = this.proxyChain[hop];
        else
            result = this.targetHost;
        return result;
    }

    @Override
    public HttpHost getProxyHost() {
        return this.proxyChain == null ? null : this.proxyChain[0];
    }

    public boolean isConnected() {
        return this.connected;
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

    public HttpRoute toRoute() {
        return !this.connected
                ? null
                : new HttpRoute(this.targetHost, this.localAddress, this.proxyChain, this.secure, this.tunnelled, this.layered);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RouteTracker))
            return false;

        final RouteTracker that = (RouteTracker) o;
        return
                // Do the cheapest checks first
                (this.connected == that.connected) &&
                (this.secure    == that.secure) &&
                (this.tunnelled == that.tunnelled) &&
                (this.layered   == that.layered) &&
                Objects.equals(this.targetHost, that.targetHost) &&
                Objects.equals(this.localAddress, that.localAddress) &&
                Objects.equals(this.proxyChain, that.proxyChain);
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
        hash = LangUtils.hashCode(hash, this.connected);
        hash = LangUtils.hashCode(hash, this.secure);
        hash = LangUtils.hashCode(hash, this.tunnelled);
        hash = LangUtils.hashCode(hash, this.layered);
        return hash;
    }

    @Override
    public String toString() {
        final StringBuilder cab = new StringBuilder(50 + getHopCount() * 30);
        cab.append("RouteTracker[");
        if (this.localAddress != null) {
            cab.append(this.localAddress);
            cab.append("->");
        }
        cab.append('{');
        if (this.connected)
            cab.append('c');
        if (this.tunnelled == TunnelType.TUNNELLED)
            cab.append('t');
        if (this.layered == LayerType.LAYERED)
            cab.append('l');
        if (this.secure)
            cab.append('s');
        cab.append("}->");
        if (this.proxyChain != null) {
            for (final HttpHost element : this.proxyChain) {
                cab.append(element);
                cab.append("->");
            }
        }
        cab.append(this.targetHost);
        cab.append(']');
        return cab.toString();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
