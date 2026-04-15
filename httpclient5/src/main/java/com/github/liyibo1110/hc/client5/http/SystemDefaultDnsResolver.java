package com.github.liyibo1110.hc.client5.http;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 使用操作系统默认实现来解析主机名的DNS解析器。
 * @author liyibo
 * @date 2026-04-14 13:59
 */
public class SystemDefaultDnsResolver implements DnsResolver {

    public static final SystemDefaultDnsResolver INSTANCE = new SystemDefaultDnsResolver();

    @Override
    public InetAddress[] resolve(final String host) throws UnknownHostException {
        return InetAddress.getAllByName(host);
    }

    @Override
    public String resolveCanonicalHostname(final String host) throws UnknownHostException {
        if (host == null)
            return null;
        final InetAddress in = InetAddress.getByName(host);
        final String canonicalServer = in.getCanonicalHostName();
        if(in.getHostAddress().contentEquals(canonicalServer))
            return host;
        return canonicalServer;
    }
}
