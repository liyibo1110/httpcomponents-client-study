package com.github.liyibo1110.hc.client5.http.auth;

import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

/**
 * 身份验证凭据提供者。
 * 此接口的实现必须是线程安全的。由于此接口的方法可能由多个线程执行，因此必须对共享数据的访问进行同步。
 * @author liyibo
 * @date 2026-04-15 13:07
 */
public interface CredentialsProvider {

    /**
     * 如果可用，则返回指定身份验证范围的凭据。
     */
    Credentials getCredentials(AuthScope authScope, HttpContext context);
}
