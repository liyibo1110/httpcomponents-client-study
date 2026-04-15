package com.github.liyibo1110.hc.client5.http;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.HttpRequest;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

/**
 * 一个用于判断给定执行上下文是否为特定于用户的处理程序。
 * 如果上下文是特定于用户的，则该处理程序返回的token对象应唯一标识当前用户。
 * 如果上下文不包含任何特定于当前用户的资源或详细信息，则该token对象应为null。
 *
 * 用户token将用于确保特定于用户的资源不会与其他用户共享或被其他用户重复使用。
 * @author liyibo
 * @date 2026-04-14 15:48
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface UserTokenHandler {

    /**
     * 如果上下文是用户特定的，则该方法返回的令牌对象应唯一标识当前用户，否则应为null。
     */
    Object getUserToken(HttpRoute route, HttpContext context);

    default Object getUserToken(HttpRoute route, HttpRequest request, HttpContext context) {
        return getUserToken(route, context);
    }
}
