package com.github.liyibo1110.hc.client5.http.auth;

import com.github.liyibo1110.hc.core5.http.HttpHost;

/**
 * 该接口表示一个AuthScheme状态信息的缓存，后续请求可重用该缓存进行预先认证。
 * @author liyibo
 * @date 2026-04-15 14:22
 */
public interface AuthCache {

    /**
     * 将具有指定认证范围的认证状态存储到缓存中。
     */
    void put(HttpHost host, AuthScheme authScheme);

    AuthScheme get(HttpHost host);

    void remove(HttpHost host);

    void clear();

    default void put(HttpHost host, String pathPrefix, AuthScheme authScheme) {
        put(host, authScheme);
    }

    default AuthScheme get(HttpHost host, String pathPrefix) {
        return get(host);
    }

    default void remove(HttpHost host, String pathPrefix) {
        remove(host);
    }
}
