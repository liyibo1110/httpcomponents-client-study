package com.github.liyibo1110.hc.client5.http.cookie;

import com.github.liyibo1110.hc.core5.http.Header;

import java.util.List;

/**
 * 定义了Cookie管理规范，Cookie管理规范必须定义：
 * 1、解析Set-Cookie头部的规则。
 * 2、解析后Cookie的验证规则。
 * 3、Cookie头部的格式化。
 * 针对给定的主机、端口和源路径。
 * @author liyibo
 * @date 2026-04-15 15:09
 */
public interface CookieSpec {

    /**
     * 将Set-Cookie头解析为一个Cookie数组。
     * 此方法不会对生成的Cookie进行验证。
     */
    List<Cookie> parse(Header header, CookieOrigin origin) throws MalformedCookieException;

    /**
     * 根据Cookie规范中定义的验证规则对Cookie进行验证。
     */
    void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException;

    /**
     * 判断一个Cookie是否与CookieOrigin匹配。
     */
    boolean match(Cookie cookie, CookieOrigin origin);

    /**
     * List<Cookie> -> List<Header>
     */
    List<Header> formatCookies(List<Cookie> cookies);
}
