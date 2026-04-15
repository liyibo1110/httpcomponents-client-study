package com.github.liyibo1110.hc.client5.http.classic.methods;

import com.github.liyibo1110.hc.client5.http.config.Configurable;
import com.github.liyibo1110.hc.core5.http.ClassicHttpRequest;

/**
 * ClassicHttpRequest接口的扩展版本，提供了用于访问请求属性（如请求URI和方法类型）的便捷方法。
 * @author liyibo
 * @date 2026-04-14 17:45
 */
public interface HttpUriRequest extends ClassicHttpRequest, Configurable {

    /**
     * 中止请求的执行。
     */
    void abort() throws UnsupportedOperationException;

    /**
     * 检查请求执行是否已被中止。
     */
    boolean isAborted();
}
