package com.github.liyibo1110.hc.client5.http.impl.classic;

/**
 * Clock接口的系统时间实现类。
 * @author liyibo
 * @date 2026-04-16 17:59
 */
class SystemClock implements Clock {

    @Override
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }
}
