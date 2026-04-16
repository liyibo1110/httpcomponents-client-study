package com.github.liyibo1110.hc.client5.http.cookie;

import com.github.liyibo1110.hc.client5.http.utils.DateUtils;

import java.time.Instant;
import java.util.Date;

/**
 * 该接口表示源服务器为维持会话状态而发送给HTTP代理的Set-Cookie响应头。
 * @author liyibo
 * @date 2026-04-15 14:56
 */
public interface SetCookie extends Cookie {

    void setValue(String value);

    @Deprecated
    void setExpiryDate (Date expiryDate);

    @SuppressWarnings("deprecated")
    default void setExpiryDate(Instant expiryDate) {
        setExpiryDate(DateUtils.toDate(expiryDate));
    }

    void setDomain(String domain);

    void setPath(String path);

    void setSecure(boolean secure);

    default void setHttpOnly (final boolean httpOnly) {}
}
