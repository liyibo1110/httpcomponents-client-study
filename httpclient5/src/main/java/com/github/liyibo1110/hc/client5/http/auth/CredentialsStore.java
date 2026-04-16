package com.github.liyibo1110.hc.client5.http.auth;

/**
 * 身份验证凭据的抽象存储。
 * 该接口的实现必须是线程安全的。由于该接口的方法可能由多个线程执行，因此必须对共享数据的访问进行同步。
 * @author liyibo
 * @date 2026-04-15 13:08
 */
public interface CredentialsStore extends CredentialsProvider {

    /**
     * 设置指定身份验证范围的凭据。该范围下之前的任何凭据都将被覆盖。
     */
    void setCredentials(AuthScope authScope, Credentials credentials);

    /**
     * 清除所有凭据。
     */
    void clear();
}
