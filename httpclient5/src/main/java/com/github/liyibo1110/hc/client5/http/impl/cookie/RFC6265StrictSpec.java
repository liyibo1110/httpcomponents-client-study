package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.CommonCookieAttributeHandler;
import com.github.liyibo1110.hc.client5.http.utils.DateUtils;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;

/**
 * CookieSpec的实现类。
 * 该实现强制遵守HTTP状态管理规范（RFC 6265，第4节）中“良好行为”配置文件的语法和语义。
 * @author liyibo
 * @date 2026-04-19 15:10
 */
@Contract(threading = ThreadingBehavior.SAFE)
public class RFC6265StrictSpec extends RFC6265CookieSpecBase {

    public RFC6265StrictSpec() {
        super(BasicPathHandler.INSTANCE,
              BasicDomainHandler.INSTANCE,
              BasicMaxAgeHandler.INSTANCE,
              BasicSecureHandler.INSTANCE,
              BasicHttpOnlyHandler.INSTANCE,
              new BasicExpiresHandler(DateUtils.STANDARD_PATTERNS));
    }

    RFC6265StrictSpec(final CommonCookieAttributeHandler... handlers) {
        super(handlers);
    }

    @Override
    public String toString() {
        return "rfc6265-strict";
    }
}
