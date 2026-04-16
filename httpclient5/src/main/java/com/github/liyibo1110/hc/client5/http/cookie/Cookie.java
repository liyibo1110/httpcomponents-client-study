package com.github.liyibo1110.hc.client5.http.cookie;

import java.time.Instant;
import java.util.Date;

/**
 * Cookie接口代表一种令牌或状态信息的短数据包（也称为“magic cookie”），
 * HTTP代理和目标服务器可以通过交换该数据包来维持会话。
 * 在最简单的形式下，HTTP Cookie仅仅是一对名称/值对。
 * @author liyibo
 * @date 2026-04-15 14:51
 */
public interface Cookie {
    String PATH_ATTR = "path";
    String DOMAIN_ATTR = "domain";
    String MAX_AGE_ATTR = "max-age";
    String SECURE_ATTR = "secure";
    String EXPIRES_ATTR = "expires";
    String HTTP_ONLY_ATTR = "httpOnly";

    String getAttribute(String name);

    boolean containsAttribute(String name);

    String getName();

    String getValue();

    /**
     * 返回Cookie的过期时间，如果不存在则返回null。
     * 注意：此方法返回的对象被视为不可变的。对其进行修改（例如使用setTime()）可能会导致未定义的行为。请自行承担风险。
     * @deprecated Use {{@link #getExpiryInstant()}}
     */
    @Deprecated
    Date getExpiryDate();

    /**
     * 返回Cookie的过期时间，如果不存在则返回null。
     * 注意：此方法返回的对象被视为不可变的。对其进行修改（例如使用setTime()）可能会导致未定义的行为。请自行承担风险。
     */
    @SuppressWarnings("deprecated")
    default Instant getExpiryInstant() {
        final Date date = getExpiryDate();
        return date != null ? Instant.ofEpochMilli(date.getTime()) : null;
    }

    /**
     * 如果该Cookie应在session结束时被丢弃，则返回false，否则返回true。
     */
    boolean isPersistent();

    String getDomain();

    String getPath();

    boolean isSecure();

    @Deprecated
    boolean isExpired(final Date date);

    @SuppressWarnings("deprecation")
    default boolean isExpired(final Instant date) {
        return isExpired(date != null ? new Date(date.toEpochMilli()) : null);
    }

    @Deprecated
    Date getCreationDate();

    default Instant getCreationInstant() {
        return null;
    }

    default boolean isHttpOnly(){
        return false;
    }
}
