package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.Cookie;
import com.github.liyibo1110.hc.client5.http.cookie.CookieOrigin;
import com.github.liyibo1110.hc.client5.http.cookie.CookieSpec;
import com.github.liyibo1110.hc.client5.http.cookie.CookieSpecFactory;
import com.github.liyibo1110.hc.client5.http.cookie.MalformedCookieException;
import com.github.liyibo1110.hc.client5.http.psl.PublicSuffixMatcher;
import com.github.liyibo1110.hc.client5.http.utils.DateUtils;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.http.protocol.HttpContext;

/**
 * CookieSpecFactory的实现，用于提供符合RFC 6265标准的Cookie策略实例。
 * 该工厂返回的实例可供多个线程共享。
 * @author liyibo
 * @date 2026-04-19 15:14
 */
@Contract(threading = ThreadingBehavior.SAFE)
public class RFC6265CookieSpecFactory implements CookieSpecFactory {

    public enum CompatibilityLevel {
        STRICT,
        RELAXED,
        IE_MEDIUM_SECURITY
    }

    private final CompatibilityLevel compatibilityLevel;
    private final PublicSuffixMatcher publicSuffixMatcher;

    /** 饿汉式单例 */
    private volatile CookieSpec cookieSpec;

    public RFC6265CookieSpecFactory(final CompatibilityLevel compatibilityLevel, final PublicSuffixMatcher publicSuffixMatcher) {
        super();
        this.compatibilityLevel = compatibilityLevel != null ? compatibilityLevel : CompatibilityLevel.RELAXED;
        this.publicSuffixMatcher = publicSuffixMatcher;
    }

    public RFC6265CookieSpecFactory(final PublicSuffixMatcher publicSuffixMatcher) {
        this(CompatibilityLevel.RELAXED, publicSuffixMatcher);
    }

    public RFC6265CookieSpecFactory() {
        this(CompatibilityLevel.RELAXED, null);
    }

    @Override
    public CookieSpec create(final HttpContext context) {
        if (cookieSpec == null) {
            synchronized (this) {
                if (cookieSpec == null) {
                    switch (this.compatibilityLevel) {
                        case STRICT:
                            this.cookieSpec = new RFC6265StrictSpec(
                                    BasicPathHandler.INSTANCE,
                                    PublicSuffixDomainFilter.decorate(BasicDomainHandler.INSTANCE, this.publicSuffixMatcher),
                                    BasicMaxAgeHandler.INSTANCE,
                                    BasicSecureHandler.INSTANCE,
                                    BasicHttpOnlyHandler.INSTANCE,
                                    new BasicExpiresHandler(DateUtils.STANDARD_PATTERNS));
                            break;
                        case IE_MEDIUM_SECURITY:
                            this.cookieSpec = new RFC6265LaxSpec(
                                    new BasicPathHandler() {
                                        @Override
                                        public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
                                            // No validation
                                        }
                                    },
                                    PublicSuffixDomainFilter.decorate(BasicDomainHandler.INSTANCE, this.publicSuffixMatcher),
                                    BasicMaxAgeHandler.INSTANCE,
                                    BasicSecureHandler.INSTANCE,
                                    BasicHttpOnlyHandler.INSTANCE,
                                    new BasicExpiresHandler(DateUtils.STANDARD_PATTERNS));
                            break;
                        default:
                            this.cookieSpec = new RFC6265LaxSpec(
                                    BasicPathHandler.INSTANCE,
                                    PublicSuffixDomainFilter.decorate(BasicDomainHandler.INSTANCE, this.publicSuffixMatcher),
                                    LaxMaxAgeHandler.INSTANCE,
                                    BasicSecureHandler.INSTANCE,
                                    LaxExpiresHandler.INSTANCE);
                    }
                }
            }
        }
        return this.cookieSpec;
    }
}
