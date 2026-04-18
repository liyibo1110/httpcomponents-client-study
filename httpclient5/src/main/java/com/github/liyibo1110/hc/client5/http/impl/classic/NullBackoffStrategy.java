package com.github.liyibo1110.hc.client5.http.impl.classic;

import com.github.liyibo1110.hc.client5.http.classic.ConnectionBackoffStrategy;
import com.github.liyibo1110.hc.core5.http.HttpResponse;

/**
 * ConnectionBackoffStrategy接口的实现，不执行任何backoff。
 * @author liyibo
 * @date 2026-04-17 12:05
 */
public class NullBackoffStrategy implements ConnectionBackoffStrategy {
    @Override
    public boolean shouldBackoff(final Throwable t) {
        return false;
    }

    @Override
    public boolean shouldBackoff(final HttpResponse response) {
        return false;
    }
}
