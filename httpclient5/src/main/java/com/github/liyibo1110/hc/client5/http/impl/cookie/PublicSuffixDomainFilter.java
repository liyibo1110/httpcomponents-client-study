package com.github.liyibo1110.hc.client5.http.impl.cookie;

import com.github.liyibo1110.hc.client5.http.cookie.CommonCookieAttributeHandler;
import com.github.liyibo1110.hc.client5.http.cookie.Cookie;
import com.github.liyibo1110.hc.client5.http.cookie.CookieOrigin;
import com.github.liyibo1110.hc.client5.http.cookie.MalformedCookieException;
import com.github.liyibo1110.hc.client5.http.cookie.SetCookie;
import com.github.liyibo1110.hc.client5.http.psl.PublicSuffixList;
import com.github.liyibo1110.hc.client5.http.psl.PublicSuffixMatcher;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.Args;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 封装了CookieAttributeHandler，并利用其match方法，确保永远不会匹配黑名单中的后缀。
 * 可用于防范来自非公开域名的Cookie，从而为跨站攻击类型提供额外的安全保障。
 * @author liyibo
 * @date 2026-04-20 10:14
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public class PublicSuffixDomainFilter implements CommonCookieAttributeHandler {
    private final CommonCookieAttributeHandler handler;
    private final PublicSuffixMatcher publicSuffixMatcher;
    private final Map<String, Boolean> localDomainMap;

    private static Map<String, Boolean> createLocalDomainMap() {
        final ConcurrentHashMap<String, Boolean> map = new ConcurrentHashMap<>();
        map.put(".localhost.", Boolean.TRUE);  // RFC 6761
        map.put(".test.", Boolean.TRUE);       // RFC 6761
        map.put(".local.", Boolean.TRUE);      // RFC 6762
        map.put(".local", Boolean.TRUE);
        map.put(".localdomain", Boolean.TRUE);
        return map;
    }

    public PublicSuffixDomainFilter(final CommonCookieAttributeHandler handler, final PublicSuffixMatcher publicSuffixMatcher) {
        this.handler = Args.notNull(handler, "Cookie handler");
        this.publicSuffixMatcher = Args.notNull(publicSuffixMatcher, "Public suffix matcher");
        this.localDomainMap = createLocalDomainMap();
    }

    public PublicSuffixDomainFilter(final CommonCookieAttributeHandler handler, final PublicSuffixList suffixList) {
        Args.notNull(handler, "Cookie handler");
        Args.notNull(suffixList, "Public suffix list");
        this.handler = handler;
        this.publicSuffixMatcher = new PublicSuffixMatcher(suffixList.getRules(), suffixList.getExceptions());
        this.localDomainMap = createLocalDomainMap();
    }

    @Override
    public boolean match(final Cookie cookie, final CookieOrigin origin) {
        final String host = cookie.getDomain();
        if (host == null)
            return false;

        final int i = host.indexOf('.');
        if (i >= 0) {
            final String domain = host.substring(i);
            if (!this.localDomainMap.containsKey(domain)) {
                if (this.publicSuffixMatcher.matches(host))
                    return false;
            }
        } else {
            if (!host.equalsIgnoreCase(origin.getHost())) {
                if (this.publicSuffixMatcher.matches(host))
                    return false;
            }
        }
        return handler.match(cookie, origin);
    }

    @Override
    public void parse(final SetCookie cookie, final String value) throws MalformedCookieException {
        handler.parse(cookie, value);
    }

    @Override
    public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
        handler.validate(cookie, origin);
    }

    @Override
    public String getAttributeName() {
        return handler.getAttributeName();
    }

    public static CommonCookieAttributeHandler decorate(final CommonCookieAttributeHandler handler,
                                                        final PublicSuffixMatcher publicSuffixMatcher) {
        Args.notNull(handler, "Cookie attribute handler");
        return publicSuffixMatcher != null ? new PublicSuffixDomainFilter(handler, publicSuffixMatcher) : handler;
    }
}
