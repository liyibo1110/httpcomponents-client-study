package com.github.liyibo1110.hc.client5.http.cookie;

import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * 该接口表示一个用于存储Cookie对象的抽象存储。
 * @author liyibo
 * @date 2026-04-15 15:16
 */
public interface CookieStore {

    /**
     * 添加一个 Cookie，并替换任何现有的同类Cookie。
     * 如果指定的Cookie已过期，则不会添加，但现有的值仍会被移除。
     */
    void addCookie(Cookie cookie);

    /**
     * 返回该存储中包含的所有Cookie。
     */
    List<Cookie> getCookies();

    @Deprecated
    boolean clearExpired(Date date);

    @SuppressWarnings("deprecation")
    default boolean clearExpired(Instant date) {
        return clearExpired(date != null ? new Date(date.toEpochMilli()) : null);
    }

    void clear();
}
