package com.github.liyibo1110.hc.client5.http.psl;

import com.github.liyibo1110.hc.client5.http.utils.DnsUtils;
import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.Args;

import java.net.IDN;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一个实用类，用于检测DNS名称是否与psl中的内容匹配。
 * 最新的后缀列表可从publicsuffix.org获取
 * @author liyibo
 * @date 2026-04-16 14:21
 */
@Contract(threading = ThreadingBehavior.SAFE)
public class PublicSuffixMatcher {
    private final Map<String, DomainType> rules;
    private final Map<String, DomainType> exceptions;

    public PublicSuffixMatcher(final Collection<String> rules, final Collection<String> exceptions) {
        this(DomainType.UNKNOWN, rules, exceptions);
    }

    public PublicSuffixMatcher(final DomainType domainType, final Collection<String> rules, final Collection<String> exceptions) {
        Args.notNull(domainType,  "Domain type");
        Args.notNull(rules,  "Domain suffix rules");
        this.rules = new ConcurrentHashMap<>(rules.size());
        for (final String rule: rules)
            this.rules.put(rule, domainType);

        this.exceptions = new ConcurrentHashMap<>();
        if (exceptions != null) {
            for (final String exception: exceptions)
                this.exceptions.put(exception, domainType);
        }
    }

    public PublicSuffixMatcher(final Collection<PublicSuffixList> lists) {
        Args.notNull(lists,  "Domain suffix lists");
        this.rules = new ConcurrentHashMap<>();
        this.exceptions = new ConcurrentHashMap<>();
        for (final PublicSuffixList list: lists) {
            final DomainType domainType = list.getType();
            final List<String> rules = list.getRules();
            for (final String rule: rules)
                this.rules.put(rule, domainType);

            final List<String> exceptions = list.getExceptions();
            if (exceptions != null) {
                for (final String exception: exceptions)
                    this.exceptions.put(exception, domainType);
            }
        }
    }

    private static DomainType findEntry(final Map<String, DomainType> map, final String rule) {
        if (map == null)
            return null;
        return map.get(rule);
    }

    private static boolean match(final DomainType domainType, final DomainType expectedType) {
        return domainType != null && (expectedType == null || domainType.equals(expectedType));
    }

    public String getDomainRoot(final String domain) {
        return getDomainRoot(domain, null);
    }

    /**
     * 返回给定域名中可注册的部分（就是非公共域名的那部分字符串）。
     * 如果该域名代表公共后缀，则返回null
     */
    public String getDomainRoot(final String domain, final DomainType expectedType) {
        if (domain == null)
            return null;
        if (domain.startsWith("."))
            return null;

        String segment = DnsUtils.normalize(domain);
        String result = null;
        while (segment != null) {
            // An exception rule takes priority over any other matching rule.
            final String key = IDN.toUnicode(segment);
            final DomainType exceptionRule = findEntry(exceptions, key);
            if (match(exceptionRule, expectedType))
                return segment;

            final DomainType domainRule = findEntry(rules, key);
            if (match(domainRule, expectedType)) {
                if (domainRule == DomainType.PRIVATE)
                    return segment;
                return result;
            }

            final int nextdot = segment.indexOf('.');
            final String nextSegment = nextdot != -1 ? segment.substring(nextdot + 1) : null;

            if (nextSegment != null) {
                final DomainType wildcardDomainRule = findEntry(rules, "*." + IDN.toUnicode(nextSegment));
                if (match(wildcardDomainRule, expectedType)) {
                    if (wildcardDomainRule == DomainType.PRIVATE)
                        return segment;
                    return result;
                }
            }
            result = segment;
            segment = nextSegment;
        }

        // If no expectations then this result is good.
        if (expectedType == null || expectedType == DomainType.UNKNOWN)
            return result;

        // If we did have expectations apparently there was no match
        return null;
    }

    public boolean matches(final String domain) {
        return matches(domain, null);
    }

    public boolean matches(final String domain, final DomainType expectedType) {
        if (domain == null)
            return false;
        final String domainRoot = getDomainRoot(domain.startsWith(".") ? domain.substring(1) : domain, expectedType);
        return domainRoot == null;
    }
}
