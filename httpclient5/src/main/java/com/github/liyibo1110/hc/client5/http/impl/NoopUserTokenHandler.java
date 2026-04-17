package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.client5.http.HttpRoute;
import com.github.liyibo1110.hc.client5.http.UserTokenHandler;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

/**
 * 总是返回null的UserTokenHandler实现类。
 * @author liyibo
 * @date 2026-04-16 16:39
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class NoopUserTokenHandler implements UserTokenHandler {
    public static final NoopUserTokenHandler INSTANCE = new NoopUserTokenHandler();

    @Override
    public Object getUserToken(final HttpRoute route, final HttpContext context) {
        return null;
    }
}
