package com.github.liyibo1110.hc.client5.http.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示 AuthScheme的状态可以被缓存，并供后续请求用于预先认证。
 * @author liyibo
 * @date 2026-04-15 14:02
 */
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthStateCacheable {
}
