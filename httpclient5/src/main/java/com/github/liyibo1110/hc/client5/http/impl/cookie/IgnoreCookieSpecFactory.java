package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.CookieSpec;
import com.github.liyibo1110.hc.client5.http.cookie.CookieSpecFactory;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

/**
 * 生成IgnoreCookieSpec对象的工厂。
 * @author liyibo
 * @date 2026-04-20 10:26
 */
public class IgnoreCookieSpecFactory implements CookieSpecFactory {

    /** 饿汉式单例 */
    private volatile CookieSpec cookieSpec;

    public IgnoreCookieSpecFactory() {
        super();
    }

    @Override
    public CookieSpec create(final HttpContext context) {
        if (cookieSpec == null) {
            synchronized (this) {
                if (cookieSpec == null)
                    this.cookieSpec = IgnoreSpecSpec.INSTANCE;
            }
        }
        return this.cookieSpec;
    }
}
