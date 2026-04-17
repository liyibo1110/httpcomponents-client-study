package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.client5.http.DnsResolver;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DnsResolver接口的基于内存的实现类。
 * @author liyibo
 * @date 2026-04-16 17:42
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class InMemoryDnsResolver implements DnsResolver {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDnsResolver.class);

    /** 存储主机名与InetAddress实例数组之间的关联 */
    private final Map<String, InetAddress[]> dnsMap;

    public InMemoryDnsResolver() {
        dnsMap = new ConcurrentHashMap<>();
    }

    public void add(final String host, final InetAddress... ips) {
        Args.notNull(host, "Host name");
        Args.notNull(ips, "Array of IP addresses");
        dnsMap.put(host, ips);
    }

    @Override
    public InetAddress[] resolve(final String host) throws UnknownHostException {
        final InetAddress[] resolvedAddresses = dnsMap.get(host);
        if (LOG.isInfoEnabled())
            LOG.info("Resolving {} to {}", host, Arrays.deepToString(resolvedAddresses));
        if(resolvedAddresses == null)
            throw new UnknownHostException(host + " cannot be resolved");
        return resolvedAddresses;
    }

    @Override
    public String resolveCanonicalHostname(final String host) throws UnknownHostException {
        final InetAddress[] resolvedAddresses = resolve(host);
        if (resolvedAddresses.length > 0)
            return resolvedAddresses[0].getCanonicalHostName();
        return host;
    }
}
