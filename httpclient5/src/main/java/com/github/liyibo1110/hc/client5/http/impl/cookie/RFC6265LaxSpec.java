package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.CommonCookieAttributeHandler;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;

/**
 * CookieSpec的实现类。
 * 该实现对HTTP状态管理规范（RFC 6265，第5节）采取了更为宽松的解释，以便与不遵循“良好行为”配置文件（RFC 6265，第4节）的现有服务器实现互操作。
 * @author liyibo
 * @date 2026-04-19 15:08
 */
@Contract(threading = ThreadingBehavior.SAFE)
public class RFC6265LaxSpec extends RFC6265CookieSpecBase {

    public RFC6265LaxSpec() {
        super(BasicPathHandler.INSTANCE,
              BasicDomainHandler.INSTANCE,
              LaxMaxAgeHandler.INSTANCE,
              BasicSecureHandler.INSTANCE,
              BasicHttpOnlyHandler.INSTANCE,
              LaxExpiresHandler.INSTANCE);
    }

    RFC6265LaxSpec(final CommonCookieAttributeHandler... handlers) {
        super(handlers);
    }

    @Override
    public String toString() {
        return "rfc6265-lax";
    }
}
