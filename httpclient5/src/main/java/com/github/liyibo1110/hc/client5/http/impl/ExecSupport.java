package com.github.liyibo1110.hc.client5.http.impl;

import com.github.liyibo1110.hc.core5.annotation.Internal;

/**
 * request运行时的相关工具方法。
 * @author liyibo
 * @date 2026-04-16 16:22
 */
@Internal
public class ExecSupport {
    private static final PrefixedIncrementingId INCREMENTING_ID = new PrefixedIncrementingId("ex-");

    public static long getNextExecNumber() {
        return INCREMENTING_ID.getNextNumber();
    }

    public static String getNextExchangeId() {
        return INCREMENTING_ID.getNextId();
    }
}
