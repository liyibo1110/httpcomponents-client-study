package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.core5.annotation.Internal;
import com.github.liyibo1110.hc.core5.util.Args;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 一个线程安全的递增标识符。
 * @author liyibo
 * @date 2026-04-16 16:17
 */
@Internal
public final class PrefixedIncrementingId {
    private final AtomicLong count = new AtomicLong(0);
    private final String prefix0;
    private final String prefix1;
    private final String prefix2;
    private final String prefix3;
    private final String prefix4;
    private final String prefix5;
    private final String prefix6;
    private final String prefix7;
    private final String prefix8;
    private final String prefix9;

    public PrefixedIncrementingId(final String prefix) {
        this.prefix0 = Args.notNull(prefix, "prefix");
        this.prefix1 = prefix0 + '0';
        this.prefix2 = prefix1 + '0';
        this.prefix3 = prefix2 + '0';
        this.prefix4 = prefix3 + '0';
        this.prefix5 = prefix4 + '0';
        this.prefix6 = prefix5 + '0';
        this.prefix7 = prefix6 + '0';
        this.prefix8 = prefix7 + '0';
        this.prefix9 = prefix8 + '0';
    }

    public long getNextNumber() {
        return count.incrementAndGet();
    }

    public String getNextId() {
        return createId(count.incrementAndGet());
    }

    /**
     * 根据该实例的前缀和指定值（用零补齐）生成一个ID。
     * 这相当于手动编写的String.format(“ex-%010d”, value)，经过优化以减少内存分配和CPU开销。
     */
    String createId(final long value) {
        final String longString = Long.toString(value);
        switch (longString.length()) {
            case 1:
                return prefix9 + longString;
            case 2:
                return prefix8 + longString;
            case 3:
                return prefix7 + longString;
            case 4:
                return prefix6 + longString;
            case 5:
                return prefix5 + longString;
            case 6:
                return prefix4 + longString;
            case 7:
                return prefix3 + longString;
            case 8:
                return prefix2 + longString;
            case 9:
                return prefix1 + longString;
            default:
                return prefix0 + longString;
        }
    }
}
