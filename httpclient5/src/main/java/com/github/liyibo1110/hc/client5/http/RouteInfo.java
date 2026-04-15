package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.core5.http.HttpHost;

import java.net.InetAddress;

/**
 * 连接路由相关基础接口。
 * @author liyibo
 * @date 2026-04-14 14:49
 */
public interface RouteInfo {

    /**
     * 路由的隧道类型。
     * 普通路由是通过连接到目标或第一个代理来建立的。
     * 隧道路由是通过连接到第一个代理，并经由所有代理隧道传输至目标来建立的。不包含代理的路由无法建立隧道。
     */
    enum TunnelType { PLAIN, TUNNELLED }

    /**
     * 路由的分层类型。
     * 普通路由是通过连接或隧道建立的。
     * 分层路由则是通过在现有连接上叠加TLS/SSL等协议来建立的。
     * 协议只能叠加在通往目标的隧道上，或者叠加在没有代理的直接连接上。
     * 在直接连接上叠加协议意义不大，因为连接本可以直接使用新协议建立。但我们不希望排除这种用例。
     */
    enum LayerType { PLAIN, LAYERED }

    /**
     * 获取目标主机的HttpHost对象。
     */
    HttpHost getTargetHost();

    /**
     * 获取本机地址。
     */
    InetAddress getLocalAddress();

    /**
     * 获取此路由的跳数。直连路由有1个跳。经过代理的路由有2个跳。经过n个代理链的路由有n+1个跳。
     */
    int getHopCount();

    /**
     * 获取该路由中某跳的目标。
     * 最后一个跳的目标是目标主机，之前各跳的目标则是链路中的相应代理。
     * 对于仅经过一个代理的路由，第0跳的目标是该代理，第1跳的目标则是目标主机。
     */
    HttpHost getHopTarget(int hop);

    /**
     * 获取第一个proxy主机地址。
     */
    HttpHost getProxyHost();

    /**
     * 获取此路由的隧道类型。如果存在代理链，则仅考虑端到端隧道。
     */
    TunnelType getTunnelType();

    /**
     * 检查此路由是否通过代理进行隧道传输。如果存在代理链，则仅考虑端到端隧道。
     */
    boolean isTunnelled();

    /**
     * 获取此路由的分层类型。如果存在代理，则仅考虑通过端到端隧道进行的分层。
     */
    LayerType getLayerType();

    /**
     * 检查此路由是否包含分层协议。若存在代理，则仅考虑在端到端隧道上进行分层。
     */
    boolean isLayered();

    /**
     * 检查此路由是否为安全方式。
     */
    boolean isSecure();
}
