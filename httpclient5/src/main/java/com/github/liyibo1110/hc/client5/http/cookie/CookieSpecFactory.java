package com.github.liyibo1110.hc.client5.http.cookie;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

/**
 * CookieSpec对象的工厂。
 * @author liyibo
 * @date 2026-04-15 15:15
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface CookieSpecFactory {

    CookieSpec create(HttpContext context);
}
