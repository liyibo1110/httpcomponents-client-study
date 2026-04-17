package com.github.liyibo1110.hc.client5.http.psl;

import com.github.liyibo1110.hc.core5.annotation.Contract;
import com.github.liyibo1110.hc.core5.annotation.ThreadingBehavior;
import com.github.liyibo1110.hc.core5.util.Args;

import java.util.Collections;
import java.util.List;

/**
 * public suffix是一组通过点号连接的DNS名称或通配符。它代表域名中不受单个注册人控制的部分。
 * 可从publicsuffix.org获取最新的后缀列表（说白了就是com、com.cn这种的公共域名后缀）。
 * @author liyibo
 * @date 2026-04-16 14:11
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class PublicSuffixList {
    private final DomainType type;
    private final List<String> rules;
    private final List<String> exceptions;

    public PublicSuffixList(final DomainType type, final List<String> rules, final List<String> exceptions) {
        this.type = Args.notNull(type, "Domain type");
        this.rules = Collections.unmodifiableList(Args.notNull(rules, "Domain suffix rules"));
        this.exceptions = Collections.unmodifiableList(exceptions != null ? exceptions : Collections.emptyList());
    }

    public PublicSuffixList(final List<String> rules, final List<String> exceptions) {
        this(DomainType.UNKNOWN, rules, exceptions);
    }

    public DomainType getType() {
        return type;
    }

    public List<String> getRules() {
        return rules;
    }

    public List<String> getExceptions() {
        return exceptions;
    }
}
