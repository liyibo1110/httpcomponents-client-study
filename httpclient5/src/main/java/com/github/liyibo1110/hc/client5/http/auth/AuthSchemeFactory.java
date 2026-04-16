package com.github.liyibo1110.hc.client5.http.auth;

import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

/**
 * AuthScheme对象的工厂。
 * @author liyibo
 * @date 2026-04-15 14:21
 */
public interface AuthSchemeFactory {

    AuthScheme create(HttpContext context);
}
