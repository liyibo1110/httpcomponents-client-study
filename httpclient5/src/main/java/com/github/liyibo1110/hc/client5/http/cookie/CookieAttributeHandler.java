package com.github.liyibo1110.hc.client5.http.cookie;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;

/**
 * 该接口代表一个Cookie属性处理器，负责解析、验证和匹配特定的Cookie属性，例如路径、域名、端口等。
 * 不同的Cookie规范可以根据其Cookie处理规则，为该类提供具体的实现。
 * @author liyibo
 * @date 2026-04-15 15:20
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface CookieAttributeHandler {

    /**
     * 解析给定的Cookie属性值，并处理相应的Cookie属性。
     */
    void parse(SetCookie cookie, String value) throws MalformedCookieException;

    /**
     * 对给定的属性值执行Cookie验证。
     */
    void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException;

    /**
     * 将给定的值（请求提交目标主机的属性）与相应的Cookie属性进行匹配。
     */
    boolean match(Cookie cookie, CookieOrigin origin);
}
