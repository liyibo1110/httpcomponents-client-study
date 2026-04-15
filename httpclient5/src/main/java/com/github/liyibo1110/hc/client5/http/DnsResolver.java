package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 用户可以实现此接口，以覆盖操作系统提供的常规DNS查询功能
 * @author liyibo
 * @date 2026-04-14 13:46
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface DnsResolver {

    /**
     * 返回指定主机名的IP地址；如果无法识别给定主机，或者无法使用关联的IP地址构建InetAddress实例，则返回 null。
     */
    InetAddress[] resolve(String host) throws UnknownHostException;

    /**
     * 获取给定主机名的完全合格域名。
     */
    String resolveCanonicalHostname(String host) throws UnknownHostException;
}
